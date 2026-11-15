export function motionSkipped(): boolean {
  return (
    globalThis.document.hidden || globalThis.matchMedia('(prefers-reduced-motion: reduce)').matches
  )
}

type MotionNode = InstanceType<typeof globalThis.Element>
type MotionElement = InstanceType<typeof globalThis.HTMLElement>
type MotionAnimation = InstanceType<typeof globalThis.Animation>
type Ease = 'reveal' | 'glide' | 'exit' | 'settle' | 'press'
const activeAnimations = new Set<MotionAnimation>()

/** 对齐参考项目 expo.out / power4.out / power2.in / back.out(1.6)。 */
export function motionFrames(
  render: (progress: number) => globalThis.Keyframe,
  ease: Ease,
): globalThis.Keyframe[] {
  return Array.from({ length: 61 }, (_, index) => {
    const p = index / 60
    const t =
      ease === 'reveal'
        ? p === 1
          ? 1
          : 1 - 2 ** (-10 * p)
        : ease === 'glide'
          ? 1 - (1 - p) ** 5
          : ease === 'exit'
            ? p ** 3
            : ease === 'press'
              ? 1 - (1 - p) ** 3
              : 1 + 2.6 * (p - 1) ** 3 + 1.6 * (p - 1) ** 2
    return { ...render(t), offset: p }
  })
}

export function animateMotion(
  element: MotionNode,
  frames: globalThis.Keyframe[],
  duration: number,
  delay = 0,
  id = '',
): MotionAnimation {
  const animation = element.animate(frames, {
    id,
    duration,
    delay,
    easing: 'linear',
    fill: 'backwards',
  })
  activeAnimations.add(animation)
  const remove = () => activeAnimations.delete(animation)
  animation.addEventListener('finish', remove, { once: true })
  animation.addEventListener('cancel', remove, { once: true })
  return animation
}

function settleActiveMotion(): void {
  if (motionSkipped()) [...activeAnimations].forEach((animation) => animation.finish())
}
globalThis.document.addEventListener('visibilitychange', settleActiveMotion)
globalThis
  .matchMedia('(prefers-reduced-motion: reduce)')
  .addEventListener('change', settleActiveMotion)

const pageAnimations = new WeakMap<MotionNode, MotionAnimation[]>()
export function cancelPageMotion(element: MotionNode): void {
  pageAnimations.get(element)?.forEach((animation) => animation.cancel())
  pageAnimations.delete(element)
}

export function pageEnter(element: MotionNode, done: () => void): void {
  cancelPageMotion(element)
  if (motionSkipped()) {
    done()
    return
  }
  const animations = [
    animateMotion(
      element,
      motionFrames((t) => ({ transform: `scale(${0.994 + 0.006 * t})` }), 'glide'),
      620,
      0,
      'page-enter',
    ),
  ]
  element.querySelectorAll('.page-reveal').forEach((item, index) => {
    animations.push(
      animateMotion(
        item,
        motionFrames(
          (t) => ({ opacity: Math.min(1, t), transform: `translateY(${12 * (1 - t)}px)` }),
          'reveal',
        ),
        620,
        40 + index * 50,
        'page-section-enter',
      ),
    )
  })
  pageAnimations.set(element, animations)
  void Promise.allSettled(animations.map((animation) => animation.finished)).then(() => {
    if (pageAnimations.get(element) !== animations) return
    pageAnimations.delete(element)
    done()
  })
}

export function pageLeave(element: MotionNode, done: () => void): void {
  cancelPageMotion(element)
  // out-in 离场即使跳过动画，也要离开本轮 Vue 补丁后再切换。
  if (motionSkipped()) {
    globalThis.setTimeout(done, 0)
    return
  }
  const animations = [
    animateMotion(
      element,
      motionFrames((t) => ({ opacity: 1 - t, transform: `scale(${1 - 0.012 * t})` }), 'exit'),
      180,
      0,
      'page-leave',
    ),
  ]
  pageAnimations.set(element, animations)
  void animations[0]!.finished.then(
    () => {
      if (pageAnimations.get(element) !== animations) return
      pageAnimations.delete(element)
      done()
    },
    () => {},
  )
}

export function revealRows(rows: ArrayLike<MotionNode>): MotionAnimation[] {
  if (motionSkipped()) return []
  return Array.from(rows)
    .slice(0, 14)
    .map((row, index) =>
      animateMotion(
        row,
        motionFrames(
          (t) => ({ opacity: Math.min(1, t), transform: `translateY(${6 * (1 - t)}px)` }),
          'reveal',
        ),
        500,
        index * 25,
        'data-row-enter',
      ),
    )
}

export function slideThumb(
  thumb: MotionElement,
  target: MotionElement,
  instant: boolean,
  previous?: MotionAnimation,
  axis: 'x' | 'y' = 'y',
): MotionAnimation | undefined {
  const parent = thumb.offsetParent as MotionElement | null
  if (!parent) return
  const pr = parent.getBoundingClientRect()
  if (!pr.width || !pr.height) return
  const tr = target.getBoundingClientRect()
  const old = thumb.getBoundingClientRect()
  const horizontal = axis === 'x'
  const size = horizontal ? 'width' : 'height'
  const edge = horizontal ? 'left' : 'top'
  const border = horizontal ? parent.clientLeft : parent.clientTop
  const scroll = horizontal ? parent.scrollLeft : parent.scrollTop
  const extent = horizontal ? parent.offsetWidth : parent.offsetHeight
  const scale = extent ? pr[size] / extent : 1
  const factor = scale > 0.01 ? scale : 1
  const from = (old[edge] - pr[edge]) / factor - border + scroll
  const fromSize = old[size] / factor
  const to = (tr[edge] - pr[edge]) / factor - border + scroll
  const toSize = tr[size] / factor
  const translate = horizontal ? 'translateX' : 'translateY'
  previous?.cancel()
  thumb.style.transform = `${translate}(${to}px)`
  thumb.style[size] = `${toSize}px`
  if (instant || motionSkipped()) return
  return animateMotion(
    thumb,
    motionFrames(
      (t) => ({
        transform: `${translate}(${from + (to - from) * t}px)`,
        [size]: `${fromSize + (toSize - fromSize) * t}px`,
      }),
      'glide',
    ),
    520,
    0,
    horizontal ? 'segment-slide' : 'navigation-slide',
  )
}

export function firstTimeInSession(key: string): boolean {
  try {
    const storageKey = `keel.intro.${key}`
    if (globalThis.sessionStorage.getItem(storageKey)) return false
    globalThis.sessionStorage.setItem(storageKey, '1')
    return true
  } catch {
    return false
  }
}

/** 与参考项目 back.out(1.6) 相同的回弹曲线，采样后交给浏览器合成。 */
export function settleFrames(x: number, y: number): globalThis.Keyframe[] {
  return Array.from({ length: 61 }, (_, index) => {
    const progress = index / 60
    const remaining = progress - 1
    const eased = 1 + 2.6 * remaining ** 3 + 1.6 * remaining ** 2
    return {
      offset: progress,
      transform: `translate(${x * (1 - eased)}px, ${y * (1 - eased)}px) scale(${0.82 + 0.18 * eased})`,
      opacity: Math.min(1, Math.max(0, eased)),
    }
  })
}

import type { Directive } from 'vue'

import { animateMotion, motionFrames, motionSkipped } from '@/lib/motion'

type Button = InstanceType<typeof globalThis.HTMLElement>
const cleanups = new WeakMap<Button, () => void>()

/** 对齐参考项目的按压回弹；主操作通过 data-press-ripple 开启涟漪。 */
export const vPress: Directive<Button> = {
  mounted(element) {
    let animation: InstanceType<typeof globalThis.Animation> | undefined
    let pressed = false
    const ripples = new Set<InstanceType<typeof globalThis.Animation>>()
    const reducedMotion = globalThis.matchMedia('(prefers-reduced-motion: reduce)')

    function ripple(event?: InstanceType<typeof globalThis.PointerEvent>): void {
      if (element.dataset.pressRipple !== 'true' || motionSkipped()) return
      const rect = element.getBoundingClientRect()
      const x = event
        ? ((event.clientX - rect.left) / rect.width) * element.offsetWidth
        : element.offsetWidth / 2
      const y = event
        ? ((event.clientY - rect.top) / rect.height) * element.offsetHeight
        : element.offsetHeight / 2
      const radius = Math.max(
        Math.hypot(x, y),
        Math.hypot(element.offsetWidth - x, y),
        Math.hypot(x, element.offsetHeight - y),
        Math.hypot(element.offsetWidth - x, element.offsetHeight - y),
      )
      const clip = globalThis.document.createElement('span')
      clip.className = 'button-ripple-clip'
      clip.setAttribute('aria-hidden', 'true')
      const dot = globalThis.document.createElement('span')
      dot.className = 'button-ripple-dot'
      dot.style.left = `${x}px`
      dot.style.top = `${y}px`
      clip.appendChild(dot)
      element.appendChild(clip)
      const effect = animateMotion(
        dot,
        Array.from({ length: 61 }, (_, index) => {
          const p = index / 60
          const spread = p === 1 ? 1 : 1 - 2 ** (-10 * p)
          const fade = Math.max(0, (p * 620 - 200) / 420)
          return {
            offset: p,
            transform: `translate(-50%, -50%) scale(${1 + ((radius * 2.2) / 20 - 1) * spread})`,
            opacity: (1 - fade) ** 2,
          }
        }),
        620,
        0,
        'button-ripple',
      )
      ripples.add(effect)
      const remove = () => {
        ripples.delete(effect)
        clip.remove()
      }
      effect.addEventListener('finish', remove, { once: true })
      effect.addEventListener('cancel', remove, { once: true })
    }

    function animate(press: boolean): void {
      const from = Number.parseFloat(globalThis.getComputedStyle(element).scale) || 1
      animation?.cancel()
      if (motionSkipped()) return
      const to = press ? 0.955 : 1
      animation = animateMotion(
        element,
        motionFrames(
          (t) => ({ scale: String(from + (to - from) * t) }),
          press ? 'press' : 'settle',
        ),
        press ? 140 : 460,
        0,
        press ? 'button-press' : 'button-release',
      )
      if (press) animation.effect?.updateTiming({ fill: 'forwards' })
    }

    function release(): void {
      if (!pressed) return
      pressed = false
      animate(false)
    }

    function press(event?: InstanceType<typeof globalThis.PointerEvent>): void {
      if (element.dataset.pressAnimation === 'false') return
      if (pressed || element.matches(':disabled, [aria-disabled="true"]') || motionSkipped()) return
      pressed = true
      animate(true)
      ripple(event)
    }

    function pointerDown(event: InstanceType<typeof globalThis.PointerEvent>): void {
      if (event.button === 0) press(event)
    }

    function keyDown(event: InstanceType<typeof globalThis.KeyboardEvent>): void {
      if (!event.repeat && (event.key === 'Enter' || event.key === ' ')) press()
    }

    function keyUp(event: InstanceType<typeof globalThis.KeyboardEvent>): void {
      if (event.key === 'Enter' || event.key === ' ') release()
    }

    function resetSkippedMotion(): void {
      if (!motionSkipped()) return
      pressed = false
      animation?.cancel()
      ripples.forEach((effect) => effect.cancel())
    }

    element.addEventListener('pointerdown', pointerDown)
    element.addEventListener('keydown', keyDown)
    element.addEventListener('blur', release)
    globalThis.addEventListener('pointerup', release)
    globalThis.addEventListener('pointercancel', release)
    globalThis.addEventListener('keyup', keyUp)
    globalThis.addEventListener('blur', release)
    globalThis.document.addEventListener('visibilitychange', resetSkippedMotion)
    reducedMotion.addEventListener('change', resetSkippedMotion)
    cleanups.set(element, () => {
      animation?.cancel()
      ripples.forEach((effect) => effect.cancel())
      element.removeEventListener('pointerdown', pointerDown)
      element.removeEventListener('keydown', keyDown)
      element.removeEventListener('blur', release)
      globalThis.removeEventListener('pointerup', release)
      globalThis.removeEventListener('pointercancel', release)
      globalThis.removeEventListener('keyup', keyUp)
      globalThis.removeEventListener('blur', release)
      globalThis.document.removeEventListener('visibilitychange', resetSkippedMotion)
      reducedMotion.removeEventListener('change', resetSkippedMotion)
    })
  },
  unmounted(element) {
    cleanups.get(element)?.()
    cleanups.delete(element)
  },
}

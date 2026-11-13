export function motionSkipped(): boolean {
  return (
    globalThis.document.hidden || globalThis.matchMedia('(prefers-reduced-motion: reduce)').matches
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

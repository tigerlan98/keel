import type { Directive } from 'vue'

import { animateMotion, motionFrames, motionSkipped } from '@/lib/motion'

type Button = InstanceType<typeof globalThis.HTMLButtonElement>
const cleanups = new WeakMap<Button, () => void>()

/** 顶栏按钮沿用参考项目的按下收缩与松开回弹。 */
export const vPress: Directive<Button> = {
  mounted(element) {
    let animation: InstanceType<typeof globalThis.Animation> | undefined
    let pressed = false

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

    function press(): void {
      if (pressed || element.disabled) return
      pressed = true
      animate(true)
    }

    function pointerDown(event: InstanceType<typeof globalThis.PointerEvent>): void {
      if (event.button === 0) press()
    }

    function keyDown(event: InstanceType<typeof globalThis.KeyboardEvent>): void {
      if (!event.repeat && (event.key === 'Enter' || event.key === ' ')) press()
    }

    function keyUp(event: InstanceType<typeof globalThis.KeyboardEvent>): void {
      if (event.key === 'Enter' || event.key === ' ') release()
    }

    element.addEventListener('pointerdown', pointerDown)
    element.addEventListener('keydown', keyDown)
    element.addEventListener('blur', release)
    globalThis.addEventListener('pointerup', release)
    globalThis.addEventListener('pointercancel', release)
    globalThis.addEventListener('keyup', keyUp)
    globalThis.addEventListener('blur', release)
    cleanups.set(element, () => {
      animation?.cancel()
      element.removeEventListener('pointerdown', pointerDown)
      element.removeEventListener('keydown', keyDown)
      element.removeEventListener('blur', release)
      globalThis.removeEventListener('pointerup', release)
      globalThis.removeEventListener('pointercancel', release)
      globalThis.removeEventListener('keyup', keyUp)
      globalThis.removeEventListener('blur', release)
    })
  },
  unmounted(element) {
    cleanups.get(element)?.()
    cleanups.delete(element)
  },
}

import { defineStore } from 'pinia'
import { onScopeDispose, ref } from 'vue'

export type ToastType = 'success' | 'error' | 'info'
export interface ToastItem {
  id: number
  message: string
  type: ToastType
  duration: number
}

export const useToastStore = defineStore('toast', () => {
  const items = ref<ToastItem[]>([])
  const paused = ref(false)
  const timers = new Map<number, { handle: number; endsAt: number; remaining: number }>()
  let seed = 0

  function dismiss(id: number): void {
    items.value = items.value.filter((item) => item.id !== id)
    window.clearTimeout(timers.get(id)?.handle)
    timers.delete(id)
  }
  function schedule(id: number, duration: number): void {
    timers.set(id, {
      handle: paused.value ? 0 : window.setTimeout(() => dismiss(id), duration),
      endsAt: Date.now() + duration,
      remaining: duration,
    })
  }
  function show(message: string, type: ToastType = 'info'): void {
    const id = ++seed
    const duration = type === 'error' ? 4200 : 2600
    items.value.push({ id, message, type, duration })
    if (items.value.length > 4) dismiss(items.value[0]!.id)
    schedule(id, duration)
  }
  function pause(): void {
    if (paused.value) return
    paused.value = true
    timers.forEach((timer) => {
      window.clearTimeout(timer.handle)
      timer.remaining = Math.max(0, timer.endsAt - Date.now())
    })
  }
  function resume(): void {
    if (!paused.value) return
    paused.value = false
    timers.forEach((timer, id) => schedule(id, Math.max(600, timer.remaining)))
  }
  function hide(): void {
    timers.forEach((timer) => window.clearTimeout(timer.handle))
    timers.clear()
    items.value = []
  }
  onScopeDispose(hide)
  return { items, paused, show, dismiss, pause, resume, hide }
})

import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ToastType = 'success' | 'error' | 'info'

/**
 * 轻量全局提示，用于 403、操作成功等跨页面反馈。
 */
export const useToastStore = defineStore('toast', () => {
  const visible = ref(false)
  const message = ref('')
  const type = ref<ToastType>('info')
  let timer: number | undefined

  function show(text: string, kind: ToastType = 'info'): void {
    message.value = text
    type.value = kind
    visible.value = true
    window.clearTimeout(timer)
    timer = window.setTimeout(() => {
      visible.value = false
    }, 3000)
  }

  function hide(): void {
    visible.value = false
    window.clearTimeout(timer)
  }

  return { visible, message, type, show, hide }
})

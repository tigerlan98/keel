<script setup lang="ts">
import { CircleAlert, CircleCheck, Info, X } from '@lucide/vue'
import { computed } from 'vue'

import { useToastStore } from '@/stores/toast'

const toast = useToastStore()

const icon = computed(() => {
  if (toast.type === 'success') {
    return CircleCheck
  }
  if (toast.type === 'error') {
    return CircleAlert
  }
  return Info
})

const tone = computed(() => {
  if (toast.type === 'success') {
    return {
      iconWrap: 'bg-success/10 text-success',
      text: 'text-ink',
    }
  }
  if (toast.type === 'error') {
    return {
      iconWrap: 'bg-destructive/10 text-destructive',
      text: 'text-ink',
    }
  }
  return {
    iconWrap: 'bg-info/10 text-info',
    text: 'text-ink',
  }
})
</script>

<template>
  <Teleport to="body">
    <Transition name="toast">
      <div
        v-if="toast.visible"
        class="fixed right-5 top-5 z-[100] flex w-[min(380px,calc(100vw-2.5rem))] items-start gap-3 rounded-2xl border border-line-strong bg-[var(--floating-surface)] p-3.5 shadow-[var(--shadow-e3)] backdrop-blur-xl"
        role="status"
        aria-live="polite"
      >
        <span class="grid h-7 w-7 shrink-0 place-items-center rounded-full" :class="tone.iconWrap">
          <component :is="icon" :size="15" :stroke-width="1.8" aria-hidden="true" />
        </span>
        <p class="min-w-0 flex-1 pt-0.5 text-[12.5px] leading-5" :class="tone.text">
          {{ toast.message }}
        </p>
        <button
          type="button"
          class="inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-md text-ink-soft transition-colors hover:bg-ink/5 hover:text-ink"
          aria-label="关闭提示"
          @click="toast.hide()"
        >
          <X :size="14" aria-hidden="true" />
        </button>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>

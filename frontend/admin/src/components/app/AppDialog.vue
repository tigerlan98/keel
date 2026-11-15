<script setup lang="ts">
import { X } from '@lucide/vue'
import { DialogContent, DialogOverlay, DialogPortal, DialogRoot, DialogTitle } from 'reka-ui'
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    open: boolean
    title: string
    size?: 'sm' | 'md' | 'lg'
    initialFocusId?: string
  }>(),
  {
    size: 'md',
    initialFocusId: '',
  },
)

const emit = defineEmits<{ close: [] }>()

const sizeClass = computed(
  () =>
    ({
      sm: 'max-w-[420px]',
      md: 'max-w-[560px]',
      lg: 'max-w-[680px]',
    })[props.size],
)

function handleOpenChange(value: boolean): void {
  if (!value) {
    emit('close')
  }
}

function handleOpenAutoFocus(event: InstanceType<typeof globalThis.Event>): void {
  if (!props.initialFocusId) return
  const target = globalThis.document.getElementById(props.initialFocusId)
  if (target) {
    event.preventDefault()
    target.focus()
  }
}
</script>

<template>
  <DialogRoot :open="open" @update:open="handleOpenChange">
    <DialogPortal>
      <DialogOverlay class="dialog-overlay fixed inset-0 z-[100] bg-[var(--overlay-bg)]" />
      <DialogContent
        class="dialog-content fixed left-1/2 top-1/2 z-[100] flex max-h-[calc(100dvh-48px)] w-[calc(100%-3rem)] -translate-x-1/2 -translate-y-1/2 flex-col overflow-hidden rounded-[24px] bg-surface text-ink focus:outline-none"
        :class="sizeClass"
        @open-auto-focus="handleOpenAutoFocus"
      >
        <header class="flex shrink-0 items-start justify-between gap-4 px-[22px] pb-1.5 pt-5">
          <DialogTitle class="text-[16px] font-bold tracking-[-0.015em] text-ink">
            {{ title }}
          </DialogTitle>
          <button
            type="button"
            class="inline-flex h-8 w-8 items-center justify-center rounded-[8px] text-ink-soft transition-colors hover:bg-ink/5 hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/30"
            aria-label="关闭"
            @click="emit('close')"
          >
            <X :size="16" aria-hidden="true" />
          </button>
        </header>

        <div class="min-h-0 flex-1 overflow-y-auto px-[22px] pb-5 pt-3.5">
          <slot />
        </div>

        <footer
          v-if="$slots.footer"
          class="flex shrink-0 flex-wrap items-center justify-end gap-2 border-t border-line-soft px-[22px] py-3.5"
        >
          <slot name="footer" />
        </footer>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>

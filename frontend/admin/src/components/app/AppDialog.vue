<script setup lang="ts">
defineProps<{
  open: boolean
  title: string
}>()

const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      role="dialog"
      aria-modal="true"
      @click.self="emit('close')"
      @keydown.esc="emit('close')"
    >
      <div class="flex max-h-[85vh] w-full max-w-lg flex-col rounded-xl border bg-background shadow-xl">
        <header class="flex items-center justify-between border-b px-5 py-3">
          <h2 class="text-base font-semibold">{{ title }}</h2>
          <button
            type="button"
            class="rounded-md px-2 text-xl leading-none text-muted-foreground hover:text-foreground"
            aria-label="关闭"
            @click="emit('close')"
          >
            ×
          </button>
        </header>
        <div class="flex-1 overflow-y-auto p-5">
          <slot />
        </div>
        <footer v-if="$slots.footer" class="flex justify-end gap-2 border-t px-5 py-3">
          <slot name="footer" />
        </footer>
      </div>
    </div>
  </Teleport>
</template>

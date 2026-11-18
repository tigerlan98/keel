<script setup lang="ts">
import { CircleAlert, RefreshCw } from '@lucide/vue'

withDefaults(
  defineProps<{
    message: string
    retryText?: string
  }>(),
  {
    retryText: '重试',
  },
)

const emit = defineEmits<{ retry: [] }>()
</script>

<template>
  <div
    class="flex flex-wrap items-center gap-3 border-b border-destructive/15 bg-destructive/5 px-5 py-3 text-[12.5px] text-destructive"
  >
    <CircleAlert :size="15" :stroke-width="1.8" aria-hidden="true" />
    <span class="min-w-0 flex-1">{{ message }}</span>
    <button
      type="button"
      class="inline-flex h-7 items-center gap-1.5 rounded-md px-2 font-medium underline-offset-4 hover:bg-destructive/10 hover:underline"
      @click="emit('retry')"
    >
      <RefreshCw :size="13" aria-hidden="true" />
      {{ retryText }}
    </button>
  </div>
</template>

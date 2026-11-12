<script setup lang="ts">
import { computed } from 'vue'

import { useToastStore } from '@/stores/toast'

const toast = useToastStore()

const tone = computed(() => {
  if (toast.type === 'success') {
    return 'border-emerald-200 bg-emerald-50 text-emerald-800'
  }
  if (toast.type === 'error') {
    return 'border-destructive/30 bg-destructive/10 text-destructive'
  }
  return 'border-border bg-background text-foreground'
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="toast.visible"
      class="fixed right-4 top-4 z-[100] max-w-sm rounded-lg border px-4 py-3 text-sm shadow-lg"
      :class="tone"
      role="status"
      aria-live="polite"
    >
      {{ toast.message }}
    </div>
  </Teleport>
</template>

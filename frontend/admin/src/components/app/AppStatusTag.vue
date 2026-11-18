<script setup lang="ts">
import { computed } from 'vue'

type Tone = 'success' | 'danger' | 'warning' | 'neutral' | 'info'

const props = withDefaults(
  defineProps<{
    tone?: Tone
    label: string
    dot?: boolean
  }>(),
  {
    tone: 'neutral',
    dot: true,
  },
)

const toneClasses = computed(
  () =>
    ({
      success: 'border-success/20 bg-success/10 text-success',
      danger: 'border-destructive/20 bg-destructive/10 text-destructive',
      warning: 'border-warning/20 bg-warning/10 text-warning',
      info: 'border-info/20 bg-info/10 text-info',
      neutral: 'border-line-soft bg-ink/5 text-ink-muted',
    })[props.tone],
)
</script>

<template>
  <span
    class="inline-flex w-fit items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-[11px] font-medium"
    :class="toneClasses"
  >
    <span v-if="dot" class="h-1.5 w-1.5 rounded-full bg-current" aria-hidden="true"></span>
    {{ label }}
  </span>
</template>

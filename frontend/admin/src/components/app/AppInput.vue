<script setup lang="ts">
import { computed } from 'vue'

import { cn } from '@/lib/utils'

defineOptions({ inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    invalid?: boolean
    disabled?: boolean
    type?: string
  }>(),
  {
    invalid: false,
    disabled: false,
    type: 'text',
  },
)

const model = defineModel<string>({ default: '' })

const classes = computed(() =>
  cn(
    'h-9 w-full rounded-[10px] border border-line-soft bg-[var(--input-surface)] px-3 text-[13px] text-ink outline-none transition-[border-color,box-shadow,background-color] duration-150 placeholder:text-ink-faint hover:border-line-strong focus-visible:border-brand focus-visible:bg-[var(--surface-solid)] focus-visible:ring-2 focus-visible:ring-brand/20 disabled:cursor-not-allowed disabled:bg-ink/5 disabled:text-ink-soft',
    props.invalid &&
      'border-destructive focus-visible:border-destructive focus-visible:ring-destructive/15',
  ),
)
</script>

<template>
  <div class="relative w-full">
    <span
      v-if="$slots.leading"
      class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-faint"
    >
      <slot name="leading" />
    </span>
    <input
      v-bind="$attrs"
      v-model="model"
      :type="type"
      :disabled="disabled"
      :aria-invalid="invalid ? 'true' : undefined"
      :class="[classes, $slots.leading && 'pl-9']"
    />
  </div>
</template>

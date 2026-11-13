<script setup lang="ts">
import { ChevronDown } from '@lucide/vue'
import { computed } from 'vue'

import { cn } from '@/lib/utils'

defineOptions({ inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    invalid?: boolean
    disabled?: boolean
    size?: 'sm' | 'md'
  }>(),
  {
    invalid: false,
    disabled: false,
    size: 'md',
  },
)

const model = defineModel<string | number>()

const classes = computed(() =>
  cn(
    'w-full appearance-none rounded-[10px] border border-line-soft bg-[var(--input-surface)] pl-3 pr-9 text-[13px] text-ink outline-none transition-[border-color,box-shadow,background-color] duration-150 hover:border-line-strong focus-visible:border-brand focus-visible:bg-[var(--surface-solid)] focus-visible:ring-2 focus-visible:ring-brand/20 disabled:cursor-not-allowed disabled:bg-ink/5 disabled:text-ink-soft',
    props.size === 'sm' ? 'h-8' : 'h-9',
    props.invalid &&
      'border-destructive focus-visible:border-destructive focus-visible:ring-destructive/15',
  ),
)
</script>

<template>
  <div class="relative w-full">
    <select v-bind="$attrs" v-model="model" :disabled="disabled" :class="classes">
      <slot />
    </select>
    <ChevronDown
      :size="14"
      class="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-ink-faint"
      aria-hidden="true"
    />
  </div>
</template>

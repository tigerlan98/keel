<script setup lang="ts">
import { LoaderCircle } from '@lucide/vue'
import { cva } from 'class-variance-authority'
import { computed } from 'vue'

import { cn } from '@/lib/utils'

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-1.5 whitespace-nowrap rounded-[10px] text-[13px] font-semibold outline-none transition-[background-color,border-color,box-shadow,color,transform] duration-150 focus-visible:ring-2 focus-visible:ring-brand/30 focus-visible:ring-offset-2 focus-visible:ring-offset-canvas active:translate-y-px disabled:pointer-events-none disabled:opacity-45',
  {
    variants: {
      variant: {
        primary: 'bg-ink text-white shadow-[0_10px_24px_-18px_rgba(12,23,22,0.8)] hover:bg-ink/90',
        brand:
          'bg-brand text-white shadow-[0_10px_24px_-18px_rgba(62,139,126,0.7)] hover:bg-brand-strong',
        outline:
          'border border-line-soft bg-white/65 text-ink shadow-[var(--shadow-e1)] hover:border-line-strong hover:bg-white',
        ghost: 'text-ink-muted hover:bg-ink/5 hover:text-ink',
        subtle: 'bg-brand/10 text-brand-strong hover:bg-brand/15',
        danger: 'bg-destructive text-white hover:bg-destructive/90',
        'danger-ghost': 'text-destructive hover:bg-destructive/10',
      },
      size: {
        sm: 'h-8 px-3',
        md: 'h-9 px-4',
        lg: 'h-10 px-5 text-[13.5px]',
        icon: 'h-9 w-9 px-0',
        'icon-sm': 'h-8 w-8 px-0',
      },
    },
    defaultVariants: {
      variant: 'primary',
      size: 'md',
    },
  },
)

type ButtonVariant =
  'primary' | 'brand' | 'outline' | 'ghost' | 'subtle' | 'danger' | 'danger-ghost'

type ButtonSize = 'sm' | 'md' | 'lg' | 'icon' | 'icon-sm'

type Props = {
  loading?: boolean
  disabled?: boolean
  type?: 'button' | 'submit' | 'reset'
  variant?: ButtonVariant
  size?: ButtonSize
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  disabled: false,
  type: 'button',
  variant: 'primary',
  size: 'md',
})

const classes = computed(() => cn(buttonVariants({ variant: props.variant, size: props.size })))
</script>

<template>
  <button :type="type" :disabled="disabled || loading" :class="classes">
    <LoaderCircle v-if="loading" :size="14" class="shrink-0 animate-spin" aria-hidden="true" />
    <slot />
  </button>
</template>

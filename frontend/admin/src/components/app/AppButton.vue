<script setup lang="ts">
import { LoaderCircle } from '@lucide/vue'
import { cva } from 'class-variance-authority'
import { computed, type Component } from 'vue'

import { cn } from '@/lib/utils'
import { vPress } from '@/directives/press'

const buttonVariants = cva(
  'app-button inline-flex shrink-0 select-none items-center justify-center gap-1.5 whitespace-nowrap rounded-[12px] text-[13px] font-semibold outline-none transition-[background-color,border-color,box-shadow,color] duration-150 focus-visible:ring-2 focus-visible:ring-brand/30 focus-visible:ring-offset-2 focus-visible:ring-offset-canvas disabled:pointer-events-none disabled:opacity-45',
  {
    variants: {
      variant: {
        primary:
          'bg-[var(--button-primary-bg)] text-white shadow-[var(--shadow-button)] hover:bg-[var(--button-primary-hover)] hover:shadow-[var(--shadow-button-hover)]',
        brand:
          'bg-[var(--button-primary-bg)] text-white shadow-[var(--shadow-button)] hover:bg-[var(--button-primary-hover)] hover:shadow-[var(--shadow-button-hover)]',
        outline:
          'border border-line-soft bg-[var(--input-surface)] text-ink shadow-[var(--shadow-e1)] hover:border-line-strong hover:bg-[var(--hover-surface)]',
        ghost: 'text-ink-muted hover:bg-ink/5 hover:text-ink',
        subtle: 'bg-brand/10 text-brand-strong hover:bg-brand/15',
        danger: 'bg-destructive text-white hover:bg-destructive/90',
        'danger-ghost': 'text-destructive hover:bg-destructive/10',
      },
      size: {
        sm: 'h-8 rounded-[8px] px-3',
        md: 'h-9 px-4',
        lg: 'h-10 px-5 text-[13.5px]',
        icon: 'h-9 w-9 rounded-[8px] px-0',
        'icon-sm': 'h-8 w-8 rounded-[8px] px-0',
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
  icon?: Component
  loading?: boolean
  disabled?: boolean
  pressAnimation?: boolean
  type?: 'button' | 'submit' | 'reset'
  variant?: ButtonVariant
  size?: ButtonSize
}

const props = withDefaults(defineProps<Props>(), {
  icon: undefined,
  loading: false,
  disabled: false,
  pressAnimation: true,
  type: 'button',
  variant: 'primary',
  size: 'md',
})

const classes = computed(() => cn(buttonVariants({ variant: props.variant, size: props.size })))
</script>

<template>
  <button
    v-press
    :type="type"
    :disabled="disabled || loading"
    :aria-busy="loading"
    :class="classes"
    :data-press-animation="pressAnimation"
    :data-press-ripple="pressAnimation && ['primary', 'brand', 'danger'].includes(variant)"
  >
    <LoaderCircle v-if="loading" :size="14" class="shrink-0 animate-spin" aria-hidden="true" />
    <component :is="icon" v-else-if="icon" :size="14" class="shrink-0" aria-hidden="true" />
    <slot />
  </button>
</template>

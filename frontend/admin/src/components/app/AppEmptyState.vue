<script setup lang="ts">
import { Inbox } from '@lucide/vue'

withDefaults(
  defineProps<{
    title?: string
    description?: string
  }>(),
  {
    title: '暂无数据',
    description: '',
  },
)
</script>

<template>
  <div class="flex flex-col items-center justify-center gap-3 px-6 py-12 text-center">
    <div
      class="grid h-11 w-11 place-items-center rounded-2xl border border-white/80 bg-white/70 text-ink-soft shadow-[var(--shadow-e1)]"
    >
      <slot name="icon">
        <Inbox :size="20" :stroke-width="1.7" aria-hidden="true" />
      </slot>
    </div>
    <div class="space-y-1">
      <p class="text-[13px] font-semibold text-ink">{{ title }}</p>
      <p v-if="description" class="mx-auto max-w-[38ch] text-[12px] leading-5 text-ink-soft">
        {{ description }}
      </p>
    </div>
    <slot name="actions" />
  </div>
</template>

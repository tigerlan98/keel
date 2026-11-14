<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <div class="page-reveal flex flex-wrap items-end justify-between gap-4">
    <div class="min-w-0 space-y-1.5">
      <h1 class="text-[22px] font-bold leading-tight tracking-[-0.03em] text-ink md:text-[28px]">
        {{ title }}
      </h1>
      <p v-if="description" class="max-w-[68ch] text-[13px] leading-6 text-ink-muted">
        {{ description }}
      </p>
    </div>

    <div v-if="$slots.actions" class="flex shrink-0 flex-wrap items-center gap-2">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  for?: string
  hint?: string
  error?: string
  errorId?: string
  required?: boolean
}>()
</script>

<template>
  <div class="min-w-0 space-y-1.5">
    <label
      :for="$props.for"
      class="flex items-center gap-1 text-[12px] font-semibold leading-5 text-ink-muted"
    >
      {{ label }}
      <span v-if="required" class="text-destructive" aria-hidden="true">*</span>
    </label>
    <slot />
    <p v-if="error" :id="errorId" class="text-[11.5px] leading-5 text-destructive" role="alert">
      {{ error }}
    </p>
    <p v-else-if="hint" class="text-[11.5px] leading-5 text-ink-soft">
      {{ hint }}
    </p>
  </div>
</template>

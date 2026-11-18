<script setup lang="ts">
const emit = defineEmits<{ submit: [] }>()
</script>

<template>
  <form
    class="app-filter-bar app-panel page-reveal flex flex-wrap items-end gap-x-4 gap-y-3 px-5 py-4"
    @submit.prevent="emit('submit')"
  >
    <div class="flex flex-1 flex-wrap items-end gap-3">
      <slot />
    </div>
    <div v-if="$slots.actions" class="flex items-center gap-2">
      <slot name="actions" />
    </div>
  </form>
</template>

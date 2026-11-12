<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <div class="space-y-1">
    <h1 class="text-2xl font-semibold tracking-tight">{{ title }}</h1>
    <p v-if="description" class="text-sm text-muted-foreground">{{ description }}</p>
  </div>
</template>

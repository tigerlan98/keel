<script setup lang="ts">
withDefaults(
  defineProps<{
    showFooter?: boolean
  }>(),
  {
    showFooter: true,
  },
)
</script>

<template>
  <section class="app-panel page-reveal">
    <header
      v-if="$slots.header || $slots.tools"
      class="flex min-h-12 flex-wrap items-center gap-3 border-b border-line-soft px-5 py-3"
    >
      <div class="min-w-0">
        <slot name="header" />
      </div>
      <div v-if="$slots.tools" class="ml-auto flex items-center gap-1.5">
        <slot name="tools" />
      </div>
    </header>
    <slot />
    <footer v-if="showFooter && $slots.footer" class="border-t border-line-soft px-5 py-3">
      <slot name="footer" />
    </footer>
  </section>
</template>

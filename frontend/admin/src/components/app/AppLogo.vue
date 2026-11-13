<script setup lang="ts">
withDefaults(
  defineProps<{
    collapsed?: boolean
  }>(),
  {
    collapsed: false,
  },
)
</script>

<template>
  <div class="flex h-16 items-center gap-2.5 px-4">
    <span
      class="grid h-9 w-9 shrink-0 place-items-center rounded-[11px] bg-ink text-white shadow-[0_8px_20px_-14px_rgba(12,23,22,0.9)]"
      :title="collapsed ? 'Keel 管理端' : undefined"
    >
      <svg width="21" height="21" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M6.5 4v16" stroke="currentColor" stroke-width="1.8" stroke-linecap="square" />
        <path d="M7 11.5 19 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="square" />
        <path d="M7 12.5 19 20" stroke="currentColor" stroke-width="1.8" stroke-linecap="square" />
        <circle cx="7" cy="12" r="2" fill="#6AC4B3" />
      </svg>
    </span>
    <div v-if="!collapsed" class="min-w-0">
      <span
        class="block truncate font-sans text-[12px] font-semibold leading-none tracking-[0.3em] text-ink"
      >
        KEEL
      </span>
      <span class="mt-1.5 block truncate text-[9px] leading-none tracking-[0.12em] text-ink-faint">
        ADMIN CONSOLE
      </span>
    </div>
  </div>
</template>

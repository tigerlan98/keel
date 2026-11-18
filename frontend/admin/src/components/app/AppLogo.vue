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
  <div class="flex h-16 shrink-0 items-center gap-[11px] px-[18px]">
    <span
      class="grid h-9 w-9 shrink-0 place-items-center rounded-[11px] bg-[#131619] text-white shadow-[0_8px_20px_-14px_rgba(12,23,22,0.9)]"
      :title="collapsed ? 'Keel 管理端' : undefined"
    >
      <span class="brand-dot" aria-hidden="true"></span>
    </span>
    <div v-if="!collapsed" class="min-w-0">
      <span class="block truncate font-sans text-[15px] font-bold leading-none text-ink">
        Keel 管理后台
      </span>
      <span class="mt-1.5 block truncate text-[10px] leading-none tracking-[0.08em] text-ink-soft">
        ADMIN CONSOLE
      </span>
    </div>
  </div>
</template>

<style scoped>
.brand-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--brand-bright);
  box-shadow:
    0 0 0 4px rgba(130, 221, 45, 0.2),
    0 0 12px rgba(130, 221, 45, 0.6);
}
</style>

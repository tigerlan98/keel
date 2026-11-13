<script setup lang="ts">
import { ChevronDown } from '@lucide/vue'
import { ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import type { MenuNode } from '@/api/types'
import { menuContainsPath, menuIcon } from '@/lib/menus'

const props = withDefaults(
  defineProps<{
    menus: MenuNode[]
    collapsed?: boolean
    depth?: number
  }>(),
  {
    collapsed: false,
    depth: 0,
  },
)

const route = useRoute()
const openGroups = ref<Record<number, boolean>>({})

function isOpen(menu: MenuNode): boolean {
  return openGroups.value[menu.id] !== false
}

function toggle(menu: MenuNode): void {
  openGroups.value[menu.id] = !isOpen(menu)
}

function isGroupActive(menu: MenuNode): boolean {
  return menuContainsPath(menu, route.path)
}

watch(
  () => route.path,
  (path) => {
    for (const menu of props.menus) {
      if (menu.children?.length && menuContainsPath(menu, path)) {
        openGroups.value[menu.id] = true
      }
    }
  },
  { immediate: true },
)
</script>

<template>
  <ul class="space-y-0.5">
    <li v-for="menu in menus" :key="menu.id">
      <RouterLink
        v-if="menu.type === 'MENU' && menu.path"
        :to="menu.path"
        :title="collapsed ? menu.name : undefined"
        :aria-label="collapsed ? menu.name : undefined"
        class="nav-item relative flex items-center gap-2.5 rounded-[10px] font-medium text-ink-muted transition-[background-color,color,box-shadow] duration-150 hover:bg-white/70 hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/30"
        :class="collapsed ? 'h-10 w-10 justify-center text-[12.5px]' : 'h-9 px-3 text-[12.5px]'"
        active-class="bg-white/80 text-ink shadow-[var(--shadow-e1)] ring-1 ring-white/80 before:absolute before:left-0 before:top-1/2 before:h-4 before:w-[2px] before:-translate-y-1/2 before:rounded-full before:bg-brand"
      >
        <component
          :is="menuIcon(menu.icon)"
          :size="16"
          :stroke-width="1.7"
          class="nav-icon shrink-0"
          aria-hidden="true"
        />
        <span v-if="!collapsed" class="truncate">{{ menu.name }}</span>
      </RouterLink>

      <template v-else>
        <button
          v-if="!collapsed"
          type="button"
          class="flex w-full items-center gap-2 rounded-lg px-3 pb-1.5 pt-3 text-left text-[10.5px] font-semibold uppercase tracking-[0.1em] transition-colors"
          :class="isGroupActive(menu) ? 'text-ink-muted' : 'text-ink-faint hover:text-ink-muted'"
          :aria-expanded="isOpen(menu)"
          @click="toggle(menu)"
        >
          <component
            :is="menuIcon(menu.icon)"
            :size="13"
            :stroke-width="1.7"
            class="shrink-0"
            aria-hidden="true"
          />
          <span class="truncate">{{ menu.name }}</span>
          <ChevronDown
            :size="12"
            class="ml-auto shrink-0 transition-transform duration-200"
            :class="!isOpen(menu) && '-rotate-90'"
            aria-hidden="true"
          />
        </button>
        <div v-else class="mx-2 my-2 border-t border-line-soft"></div>

        <MenuTree
          v-if="isOpen(menu) && menu.children?.length"
          :menus="menu.children"
          :collapsed="collapsed"
          :depth="depth + 1"
          :class="collapsed ? '' : 'ml-2 border-l border-line-soft pl-2'"
        />
      </template>
    </li>
  </ul>
</template>

<style scoped>
.nav-item[aria-current='page'] .nav-icon {
  color: var(--brand);
}
</style>

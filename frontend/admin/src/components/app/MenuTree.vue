<script setup lang="ts">
import { RouterLink } from 'vue-router'

import type { MenuNode } from '@/api/types'
import { menuIcon } from '@/lib/menus'

defineProps<{ menus: MenuNode[] }>()
</script>

<template>
  <ul class="space-y-1">
    <li v-for="menu in menus" :key="menu.id">
      <RouterLink
        v-if="menu.type === 'MENU' && menu.path"
        :to="menu.path"
        active-class="bg-accent text-accent-foreground"
        class="flex h-9 items-center gap-2 rounded-md px-3 text-sm font-medium text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground"
      >
        <component :is="menuIcon(menu.icon)" class="h-4 w-4 shrink-0" />
        <span class="truncate">{{ menu.name }}</span>
      </RouterLink>

      <div
        v-else
        class="flex items-center gap-2 px-3 pb-1 pt-2 text-xs font-medium uppercase tracking-wide text-muted-foreground"
      >
        <component :is="menuIcon(menu.icon)" class="h-3.5 w-3.5 shrink-0" />
        <span class="truncate">{{ menu.name }}</span>
      </div>

      <MenuTree
        v-if="menu.type !== 'MENU' && menu.children?.length"
        :menus="menu.children"
        class="ml-3 border-l pl-2"
      />
    </li>
  </ul>
</template>

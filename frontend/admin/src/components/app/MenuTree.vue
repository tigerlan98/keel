<script setup lang="ts">
import { ChevronDown } from '@lucide/vue'
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
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
const root = ref<InstanceType<typeof globalThis.HTMLElement> | null>(null)
const selection = ref({ top: 0, height: 38, visible: false })
let observer: InstanceType<typeof globalThis.ResizeObserver> | undefined
async function syncSelection(): Promise<void> {
  if (props.depth !== 0) return
  await nextTick()
  const active =
    root.value?.querySelector<InstanceType<typeof globalThis.HTMLElement>>('[aria-current="page"]')
  if (!active || !root.value) {
    selection.value.visible = false
    return
  }
  const box = active.getBoundingClientRect()
  selection.value = {
    top: box.top - root.value.getBoundingClientRect().top,
    height: box.height,
    visible: true,
  }
}
watch([() => route.path, () => props.collapsed, openGroups], syncSelection, {
  deep: true,
  flush: 'post',
})
onMounted(() => {
  if (props.depth !== 0 || !root.value) return
  observer = new globalThis.ResizeObserver(() => {
    void syncSelection()
  })
  observer.observe(root.value)
  void syncSelection()
})
onUnmounted(() => observer?.disconnect())

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
  <ul ref="root" class="space-y-0.5" :class="depth === 0 && 'nav-root'">
    <li
      v-if="depth === 0"
      aria-hidden="true"
      class="nav-selection"
      :style="{
        transform: `translateY(${selection.top}px)`,
        height: `${selection.height}px`,
        opacity: selection.visible ? 1 : 0,
      }"
    ></li>
    <li v-for="menu in menus" :key="menu.id">
      <RouterLink
        v-if="menu.type === 'MENU' && menu.path"
        :to="menu.path"
        :title="collapsed ? menu.name : undefined"
        :aria-label="collapsed ? menu.name : undefined"
        class="nav-item relative flex items-center gap-[11px] rounded-xl font-medium text-ink-muted transition-[background-color,color,box-shadow] duration-150 hover:bg-[var(--hover-surface)] hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/30"
        :class="
          collapsed ? 'h-10 w-10 justify-center text-[13px]' : 'h-[38px] px-[11px] text-[13px]'
        "
        active-class="is-active text-ink"
      >
        <component
          :is="menuIcon(menu.icon)"
          :size="18"
          :stroke-width="1.7"
          class="nav-icon shrink-0 text-ink-soft"
          aria-hidden="true"
        />
        <span v-if="!collapsed" class="truncate">{{ menu.name }}</span>
      </RouterLink>

      <template v-else>
        <button
          v-if="!collapsed"
          type="button"
          class="flex w-full items-center gap-2 rounded-lg px-[11px] pb-2 pt-5 text-left text-[11px] font-semibold tracking-[0.06em] transition-colors"
          :class="isGroupActive(menu) ? 'text-ink-muted' : 'text-ink-faint hover:text-ink-muted'"
          :aria-expanded="isOpen(menu)"
          @click="toggle(menu)"
        >
          <span class="truncate">{{ menu.name }}</span>
          <ChevronDown
            :size="12"
            class="ml-auto shrink-0 transition-transform duration-200"
            :class="!isOpen(menu) && '-rotate-90'"
            aria-hidden="true"
          />
        </button>
        <div v-else class="mx-2 my-2 border-t border-line-soft"></div>

        <Transition name="nav-expand" @after-enter="syncSelection" @after-leave="syncSelection">
          <div v-if="isOpen(menu) && menu.children?.length" class="nav-branch">
            <div>
              <MenuTree
                :menus="menu.children"
                :collapsed="collapsed"
                :depth="depth + 1"
                :class="!collapsed && depth > 0 ? 'ml-2 border-l border-line-soft pl-2' : ''"
              />
            </div>
          </div>
        </Transition>
      </template>
    </li>
  </ul>
</template>

<style scoped>
.nav-root {
  position: relative;
  isolation: isolate;
}
.nav-selection {
  position: absolute;
  z-index: -1;
  inset: 0 0 auto;
  margin: 0 !important;
  pointer-events: none;
  border-radius: 12px;
  border: 1px solid var(--glass-edge);
  background: var(--pill-surface);
  box-shadow:
    var(--shadow-e2),
    var(--inset-edge);
  transition:
    transform 420ms cubic-bezier(0.16, 1, 0.3, 1),
    height 280ms ease,
    opacity 180ms ease;
}
.nav-branch {
  display: grid;
  grid-template-rows: 1fr;
}
.nav-branch > div {
  min-height: 0;
  overflow: hidden;
}
.nav-expand-enter-active,
.nav-expand-leave-active {
  transition:
    grid-template-rows 300ms cubic-bezier(0.16, 1, 0.3, 1),
    opacity 220ms ease;
}
.nav-expand-enter-from,
.nav-expand-leave-to {
  grid-template-rows: 0fr;
  opacity: 0;
}
.nav-item.is-active {
  font-weight: 650;
}

.nav-item[aria-current='page'] .nav-icon {
  color: var(--brand);
}
</style>

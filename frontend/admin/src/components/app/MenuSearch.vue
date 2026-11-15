<script setup lang="ts">
import { Search } from '@lucide/vue'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import type { MenuNode } from '@/api/types'
import AppDialog from '@/components/app/AppDialog.vue'
import AppInput from '@/components/app/AppInput.vue'
import { vPress } from '@/directives/press'
import { menuIcon } from '@/lib/menus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const emit = defineEmits<{ open: [] }>()
const router = useRouter()
const open = ref(false)
const query = ref('')

function collect(menus: MenuNode[]): MenuNode[] {
  return menus.flatMap((menu) => [
    ...(menu.type === 'MENU' && menu.path ? [menu] : []),
    ...collect(menu.children ?? []),
  ])
}

const results = computed(() => {
  const keyword = query.value.trim().toLocaleLowerCase()
  return [
    {
      id: -1,
      name: '工作台',
      path: '/',
      type: 'MENU' as const,
      icon: 'layout-dashboard',
      children: [],
    },
    ...collect(auth.menus),
  ].filter((menu) => menu.name.toLocaleLowerCase().includes(keyword))
})

function show(): void {
  emit('open')
  query.value = ''
  open.value = true
}

function navigate(menu: Pick<MenuNode, 'path'>): void {
  if (!menu.path) return
  open.value = false
  void router.push(menu.path)
}

function handleKeydown(event: InstanceType<typeof globalThis.KeyboardEvent>): void {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    if (open.value) open.value = false
    else show()
  }
}

onMounted(() => globalThis.document.addEventListener('keydown', handleKeydown))
onUnmounted(() => globalThis.document.removeEventListener('keydown', handleKeydown))
</script>

<template>
  <button
    v-press
    type="button"
    class="menu-search-trigger app-surface flex h-9 w-9 items-center justify-center gap-2 rounded-xl px-2 text-[12.5px] text-ink-soft focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/30 xl:w-60 xl:justify-start xl:px-3"
    aria-label="搜索菜单"
    aria-keyshortcuts="Control+k Meta+k"
    @click="show"
  >
    <Search :size="15" aria-hidden="true" />
    <span class="hidden flex-1 text-left xl:block">搜索菜单</span>
    <kbd class="hidden rounded-md border border-line-strong px-1.5 font-sans text-[11px] xl:block"
      >Ctrl K</kbd
    >
  </button>

  <AppDialog :open="open" title="搜索菜单" initial-focus-id="menu-search" @close="open = false">
    <form @submit.prevent="results[0] && navigate(results[0])">
      <label for="menu-search" class="sr-only">菜单名称</label>
      <AppInput id="menu-search" v-model="query" placeholder="输入菜单名称…" autocomplete="off">
        <template #leading><Search :size="15" aria-hidden="true" /></template>
      </AppInput>
    </form>
    <nav class="mt-3 space-y-1" aria-label="菜单搜索结果">
      <button
        v-for="menu in results"
        :key="menu.id"
        type="button"
        class="flex min-h-11 w-full items-center gap-3 rounded-xl px-3 text-left text-[13px] text-ink-muted transition-colors hover:bg-brand/10 hover:text-brand focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/30"
        @click="navigate(menu)"
      >
        <component :is="menuIcon(menu.icon)" :size="17" aria-hidden="true" />
        {{ menu.name }}
      </button>
      <p v-if="results.length === 0" role="status" class="py-6 text-center text-sm text-ink-muted">
        没有匹配的菜单，请尝试其他名称。
      </p>
    </nav>
  </AppDialog>
</template>

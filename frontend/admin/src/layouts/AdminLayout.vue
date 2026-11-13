<script setup lang="ts">
import { ChevronRight, LayoutDashboard, Menu, PanelLeftClose, PanelLeftOpen, X } from '@lucide/vue'
import { computed, nextTick, onMounted, onUnmounted, ref, useTemplateRef, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'

import AppConfirm from '@/components/app/AppConfirm.vue'
import AppearanceSettings from '@/components/app/AppearanceSettings.vue'
import AppLogo from '@/components/app/AppLogo.vue'
import MenuTree from '@/components/app/MenuTree.vue'
import MenuSearch from '@/components/app/MenuSearch.vue'
import UserMenu from '@/components/app/UserMenu.vue'
import { findMenuTrail } from '@/lib/menus'
import { useAuthStore } from '@/stores/auth'

const COLLAPSE_KEY = 'keel.sidebar.collapsed'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

function readCollapsedState(): boolean {
  try {
    return globalThis.localStorage.getItem(COLLAPSE_KEY) === 'true'
  } catch {
    return false
  }
}

const collapsed = ref(readCollapsedState())
const mobileOpen = ref(false)
const logoutOpen = ref(false)
const loggingOut = ref(false)
const drawerCloseRef = useTemplateRef<{ focus: () => void }>('drawerClose')

const displayName = computed(() => auth.user?.nickname || auth.user?.username || '未登录')
const menuTrail = computed(() => findMenuTrail(auth.menus, route.path))
const groupTitle = computed(() =>
  menuTrail.value.length > 1 ? String(menuTrail.value[0]?.name ?? '管理端') : '管理端',
)
const pageTitle = computed(() =>
  String(route.meta.title ?? menuTrail.value.at(-1)?.name ?? '管理端'),
)

function toggleCollapsed(): void {
  collapsed.value = !collapsed.value
  try {
    globalThis.localStorage.setItem(COLLAPSE_KEY, String(collapsed.value))
  } catch {
    // 忽略隐私模式下不可用的本地存储
  }
}

function openChangePassword(): void {
  void router.push({ name: 'change-password' })
}

async function confirmLogout(): Promise<void> {
  loggingOut.value = true
  try {
    await auth.logout()
  } finally {
    loggingOut.value = false
    logoutOpen.value = false
    await router.push({ name: 'login' })
  }
}

function handleKeydown(event: InstanceType<typeof globalThis.KeyboardEvent>): void {
  if (event.key === 'Escape') {
    mobileOpen.value = false
  }
}

watch(
  () => route.path,
  () => {
    mobileOpen.value = false
  },
)

watch(mobileOpen, async (open) => {
  if (open) {
    await nextTick()
    drawerCloseRef.value?.focus()
  }
})

onMounted(() => {
  globalThis.document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  globalThis.document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div class="app-frame">
    <div class="app-shell" :class="{ 'is-collapsed': collapsed }">
      <aside class="app-rail">
        <AppLogo :collapsed="collapsed" />

        <nav class="min-h-0 flex-1 overflow-y-auto px-3 pb-3 pt-1" aria-label="主菜单">
          <RouterLink
            to="/"
            class="workbench-link"
            :class="{ 'is-active': route.name === 'home', 'is-compact': collapsed }"
            :title="collapsed ? '工作台' : undefined"
          >
            <LayoutDashboard :size="18" aria-hidden="true" /><span v-if="!collapsed">工作台</span>
          </RouterLink>
          <MenuTree :menus="auth.menus" :collapsed="collapsed" />
          <p
            v-if="auth.menus.length === 0 && !collapsed"
            class="px-3 py-4 text-[11px] leading-5 text-ink-soft"
          >
            暂无可见菜单
          </p>
        </nav>

        <div
          v-if="!collapsed"
          class="flex items-center justify-between border-t border-line-soft px-4 py-3 text-[9.5px] tracking-[0.12em] text-ink-faint"
        >
          <span>v1.0.0</span>
          <span class="inline-flex items-center gap-1.5">
            <span class="h-1.5 w-1.5 rounded-full bg-brand"></span>
            运行中
          </span>
        </div>
      </aside>

      <div class="app-column">
        <header class="app-topbar">
          <button
            type="button"
            class="topbar-icon-button inline-flex lg:hidden"
            aria-label="打开导航"
            @click="mobileOpen = true"
          >
            <Menu :size="17" aria-hidden="true" />
          </button>

          <button
            type="button"
            class="topbar-icon-button hidden lg:inline-flex"
            :aria-label="collapsed ? '展开侧栏' : '收起侧栏'"
            @click="toggleCollapsed"
          >
            <PanelLeftOpen v-if="collapsed" :size="16" aria-hidden="true" />
            <PanelLeftClose v-else :size="16" aria-hidden="true" />
          </button>

          <div class="flex min-w-0 items-center gap-1.5 text-[12px]">
            <span class="hidden whitespace-nowrap text-ink-faint md:inline">{{ groupTitle }}</span>
            <ChevronRight
              :size="12"
              class="hidden shrink-0 text-ink-faint md:block"
              aria-hidden="true"
            />
            <span class="truncate font-semibold text-ink">{{ pageTitle }}</span>
          </div>

          <div class="ml-auto flex items-center gap-2">
            <MenuSearch @open="mobileOpen = false" />
            <AppearanceSettings />
            <UserMenu
              :name="displayName"
              :username="auth.user?.username ?? ''"
              :super-admin="auth.superAdmin"
              @change-password="openChangePassword"
              @logout="logoutOpen = true"
            />
          </div>
        </header>

        <main class="app-main">
          <div class="app-main-inner">
            <RouterView v-slot="{ Component, route: currentRoute }">
              <Transition name="page" mode="out-in">
                <div :key="currentRoute.path" class="route-page"><component :is="Component" /></div>
              </Transition>
            </RouterView>
          </div>
        </main>
      </div>
    </div>

    <Teleport to="body">
      <Transition name="drawer">
        <div v-if="mobileOpen" class="fixed inset-0 z-[70]">
          <div class="absolute inset-0 bg-[var(--overlay-bg)]" @click="mobileOpen = false"></div>
          <aside
            class="drawer-panel absolute inset-y-0 left-0 flex w-[272px] flex-col border-r border-line-strong bg-[var(--rail-surface)] shadow-[var(--shadow-e3)] backdrop-blur-xl"
            role="dialog"
            aria-modal="true"
            aria-label="导航菜单"
          >
            <div class="flex items-center justify-between pr-3">
              <AppLogo />
              <button
                ref="drawerClose"
                type="button"
                class="topbar-icon-button inline-flex"
                aria-label="关闭导航"
                @click="mobileOpen = false"
              >
                <X :size="16" aria-hidden="true" />
              </button>
            </div>
            <nav class="min-h-0 flex-1 overflow-y-auto px-3 pb-4" aria-label="主菜单">
              <RouterLink
                to="/"
                class="workbench-link"
                :class="{ 'is-active': route.name === 'home' }"
                ><LayoutDashboard :size="18" aria-hidden="true" />工作台</RouterLink
              >
              <MenuTree :menus="auth.menus" />
              <p v-if="auth.menus.length === 0" class="px-3 py-4 text-[11px] text-ink-soft">
                暂无可见菜单
              </p>
            </nav>
            <div class="border-t border-line-soft px-4 py-3 text-[9.5px] text-ink-faint">
              v1.0.0
            </div>
          </aside>
        </div>
      </Transition>
    </Teleport>

    <AppConfirm
      :open="logoutOpen"
      title="退出登录"
      message="退出后需要重新登录，确定继续吗？"
      confirm-text="退出登录"
      :loading="loggingOut"
      @close="logoutOpen = false"
      @confirm="confirmLogout"
    />
  </div>
</template>

<style scoped>
.app-frame {
  min-height: 100dvh;
  padding: 8px;
  background: var(--canvas-deep);
}

.app-shell {
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  height: calc(100dvh - 16px);
  overflow: hidden;
  border: 10px solid #17191d;
  border-radius: 38px;
  background:
    radial-gradient(55% 45% at 92% 0%, rgba(130, 221, 45, 0.07), transparent 70%),
    radial-gradient(45% 40% at 20% 100%, rgba(35, 109, 233, 0.055), transparent 70%), var(--canvas);
  box-shadow:
    0 24px 70px rgba(17, 23, 27, 0.24),
    0 3px 12px rgba(17, 23, 27, 0.16);
  transition: grid-template-columns 0.42s cubic-bezier(0.16, 1, 0.3, 1);
}

.app-shell.is-collapsed {
  grid-template-columns: 76px minmax(0, 1fr);
}

.app-rail {
  display: flex;
  min-height: 0;
  flex-direction: column;
  border-right: 1px solid var(--line-soft);
  background: var(--rail-surface);
  backdrop-filter: blur(22px);
}

.app-column {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
}

.app-topbar {
  display: flex;
  height: 64px;
  flex: none;
  align-items: center;
  gap: 10px;
  padding: 0 20px 0 14px;
  border-bottom: 1px solid var(--line-soft);
  background: var(--topbar-surface);
  backdrop-filter: blur(18px);
}

.app-main {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 26px 28px 40px;
  scrollbar-width: thin;
  scrollbar-color: rgba(12, 23, 22, 0.18) transparent;
}

.app-main-inner {
  width: 100%;
  margin: 0 auto;
}

.workbench-link {
  display: flex;
  align-items: center;
  gap: 11px;
  height: 38px;
  padding: 0 11px;
  margin: 4px 0 8px;
  border-radius: 12px;
  color: var(--ink-muted);
  font-size: 13px;
  transition:
    background-color 180ms ease,
    box-shadow 180ms ease;
}
.workbench-link.is-active {
  background: var(--pill-surface);
  box-shadow: var(--shadow-e2), var(--inset-edge);
  color: var(--brand);
  font-weight: 650;
}
.workbench-link.is-compact {
  justify-content: center;
  padding: 0;
}
.workbench-link:focus-visible {
  outline: 2px solid var(--brand);
  outline-offset: 2px;
}

.topbar-icon-button {
  height: 32px;
  width: 32px;
  flex: none;
  align-items: center;
  justify-content: center;
  border-radius: 9px;
  color: var(--ink-muted);
  transition:
    background-color 0.15s ease,
    color 0.15s ease;
}

.topbar-icon-button:hover {
  color: var(--ink);
  background: var(--hover-surface);
}

.topbar-icon-button:focus-visible {
  outline: none;
  box-shadow: 0 0 0 2px var(--brand-ring);
}

.page-enter-active,
.page-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.drawer-enter-active,
.drawer-leave-active {
  transition: opacity 0.2s ease;
}

.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
}

.drawer-enter-active .drawer-panel,
.drawer-leave-active .drawer-panel {
  transition: transform 0.28s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.drawer-enter-from .drawer-panel,
.drawer-leave-to .drawer-panel {
  transform: translateX(-100%);
}

@media (max-width: 1023px) {
  .app-frame {
    padding: 0;
    background: var(--canvas);
  }

  .app-shell,
  .app-shell.is-collapsed {
    grid-template-columns: minmax(0, 1fr);
    height: 100dvh;
    border: 0;
    border-radius: 0;
    box-shadow: none;
  }

  .app-rail {
    display: none;
  }

  .app-topbar {
    height: 58px;
    padding: 0 16px;
  }

  .app-main {
    padding: 18px 16px 28px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .app-shell,
  .page-enter-active,
  .page-leave-active,
  .drawer-enter-active,
  .drawer-leave-active,
  .drawer-enter-active .drawer-panel,
  .drawer-leave-active .drawer-panel {
    transition-duration: 0.001ms !important;
  }
}
</style>

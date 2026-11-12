<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterView, useRouter } from 'vue-router'

import MenuTree from '@/components/app/MenuTree.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const loggingOut = ref(false)

const displayName = computed(() => auth.user?.nickname || auth.user?.username || '未登录')

async function handleLogout() {
  loggingOut.value = true
  try {
    await auth.logout()
  } finally {
    loggingOut.value = false
    await router.push({ name: 'login' })
  }
}
</script>

<template>
  <div class="min-h-screen bg-muted/30">
    <aside class="fixed inset-y-0 left-0 hidden w-60 flex-col border-r bg-background lg:flex">
      <div class="flex h-14 items-center border-b px-5 text-base font-semibold">Keel Admin</div>
      <nav class="flex-1 overflow-y-auto p-3">
        <MenuTree :menus="auth.menus" />
        <p v-if="auth.menus.length === 0" class="px-3 py-4 text-xs text-muted-foreground">
          暂无可见菜单
        </p>
      </nav>
    </aside>

    <div class="lg:pl-60">
      <header class="flex h-14 items-center justify-between border-b bg-background px-4 sm:px-6">
        <span class="text-sm text-muted-foreground">管理端</span>

        <div class="flex items-center gap-3">
          <span class="text-sm">{{ displayName }}</span>
          <button
            type="button"
            :disabled="loggingOut"
            class="text-sm text-muted-foreground underline-offset-4 hover:underline disabled:opacity-50"
            @click="handleLogout"
          >
            {{ loggingOut ? '退出中...' : '退出登录' }}
          </button>
        </div>
      </header>

      <nav class="border-b bg-background p-3 lg:hidden">
        <MenuTree :menus="auth.menus" />
        <p v-if="auth.menus.length === 0" class="px-3 py-2 text-xs text-muted-foreground">
          暂无可见菜单
        </p>
      </nav>

      <main class="p-4 sm:p-6">
        <RouterView />
      </main>
    </div>
  </div>
</template>

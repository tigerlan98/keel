<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'

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
    <aside class="fixed inset-y-0 left-0 hidden w-60 border-r bg-background lg:flex lg:flex-col">
      <div class="flex h-14 items-center border-b px-5 text-base font-semibold">Keel Admin</div>
      <nav class="flex-1 space-y-1 p-3">
        <RouterLink
          to="/"
          exact-active-class="bg-accent text-accent-foreground"
          class="flex h-9 items-center rounded-md px-3 text-sm font-medium text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground"
        >
          首页
        </RouterLink>
        <RouterLink
          to="/iam/users"
          active-class="bg-accent text-accent-foreground"
          class="flex h-9 items-center rounded-md px-3 text-sm font-medium text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground"
        >
          用户管理
        </RouterLink>
      </nav>
      <div class="border-t p-4 text-xs text-muted-foreground">菜单将在接入 RBAC 后由后端驱动</div>
    </aside>

    <div class="lg:pl-60">
      <header class="flex h-14 items-center justify-between border-b bg-background px-4 sm:px-6">
        <nav class="flex items-center gap-4 text-sm lg:hidden">
          <RouterLink to="/" class="text-muted-foreground hover:text-foreground">首页</RouterLink>
          <RouterLink to="/iam/users" class="text-muted-foreground hover:text-foreground">
            用户
          </RouterLink>
        </nav>
        <span class="hidden text-sm text-muted-foreground lg:inline">管理端</span>

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
      <main class="p-4 sm:p-6">
        <RouterView />
      </main>
    </div>
  </div>
</template>

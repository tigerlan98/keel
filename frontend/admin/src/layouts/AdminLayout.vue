<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="min-h-screen bg-muted/30">
    <aside class="fixed inset-y-0 left-0 hidden w-60 border-r bg-background lg:flex lg:flex-col">
      <div class="flex h-14 items-center border-b px-5 text-base font-semibold">Keel Admin</div>
      <nav class="flex-1 space-y-1 p-3">
        <RouterLink
          to="/"
          class="flex h-9 items-center rounded-md px-3 text-sm font-medium text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground"
        >
          首页
        </RouterLink>
      </nav>
      <div class="border-t p-4 text-xs text-muted-foreground">菜单将在接入 RBAC 后由后端驱动</div>
    </aside>

    <div class="lg:pl-60">
      <header class="flex h-14 items-center justify-between border-b bg-background px-6">
        <span class="text-sm text-muted-foreground">管理端基础布局</span>
        <span class="text-sm text-muted-foreground">未登录</span>
      </header>
      <main class="p-6">
        <RouterView />
      </main>
    </div>
  </div>
</template>

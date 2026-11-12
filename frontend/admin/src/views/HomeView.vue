<script setup lang="ts">
import PageHeader from '@/components/app/PageHeader.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
</script>

<template>
  <PageHeader
    :title="auth.menus.length > 0 ? '暂无可访问页面' : '暂无可用菜单'"
    :description="
      auth.menus.length > 0
        ? '已授权的菜单尚未在管理端实现，请等待前端发版。'
        : '当前账号尚未被授予任何可见菜单，请联系管理员。'
    "
  />

  <div class="mt-6 rounded-xl border bg-card p-6 text-sm text-muted-foreground">
    账号：{{ auth.user?.username }}。菜单与按钮权限由角色授权决定。
  </div>
</template>

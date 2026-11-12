<script setup lang="ts">
import PageHeader from '@/components/app/PageHeader.vue'

const placeholders = [
  { title: '认证与权限', description: '登录、/auth/me、权限码与菜单树将在 Phase 1B 接入。' },
  { title: '业务模块', description: '用户、角色、资源、系统参数与审计日志按阶段实现。' },
  { title: 'API 契约', description: '类型由后端 OpenAPI 快照生成，禁止手写接口类型。' },
]
</script>

<template>
  <PageHeader title="首页" description="Keel 管理端基础工程已就绪，业务页面将在后续阶段接入。" />

  <div class="mt-6 grid gap-4 md:grid-cols-3">
    <section v-for="item in placeholders" :key="item.title" class="rounded-xl border bg-card p-5">
      <h2 class="text-sm font-medium">{{ item.title }}</h2>
      <p class="mt-2 text-sm leading-6 text-muted-foreground">{{ item.description }}</p>
    </section>
  </div>
</template>

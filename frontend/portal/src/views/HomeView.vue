<script setup lang="ts">
import { ArrowRight } from '@lucide/vue'
</script>

<template>
  <div class="min-h-screen bg-background">
    <header class="border-b">
      <div class="mx-auto flex h-14 max-w-5xl items-center justify-between px-4 sm:px-6">
        <span class="text-base font-semibold">Keel</span>
        <span class="text-sm text-muted-foreground">Portal</span>
      </div>
    </header>

    <main class="mx-auto max-w-5xl px-4 py-16 sm:px-6 sm:py-24">
      <div class="max-w-2xl space-y-6">
        <p class="text-sm font-medium text-muted-foreground">匿名公开站点</p>
        <h1 class="text-4xl font-semibold tracking-tight sm:text-5xl">Keel Portal 基础工程</h1>
        <p class="text-base leading-7 text-muted-foreground">
          Portal 骨架已就绪。公开配置接口、首页内容与信息架构将在 Phase 4 按设计文档接入。
        </p>
        <a
          href="/api/v1/public/config"
          class="inline-flex items-center gap-2 text-sm font-medium underline-offset-4 hover:underline"
        >
          查看公开配置接口占位
          <ArrowRight class="size-4" />
        </a>
      </div>
    </main>
  </div>
</template>

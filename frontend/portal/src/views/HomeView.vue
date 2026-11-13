<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { ArrowRight } from '@lucide/vue'

import { ApiError } from '@/api/http'
import { fetchPublicSiteConfig } from '@/api/public'
import type { PublicSiteConfig } from '@/api/types'

const config = ref<PublicSiteConfig | null>(null)
const loading = ref(true)
const error = ref('')

const systemName = computed(() => config.value?.systemName || 'Keel')
const logoUrl = computed(() => config.value?.logoUrl || '')

async function load() {
  loading.value = true
  error.value = ''
  try {
    config.value = await fetchPublicSiteConfig()
  } catch (caught) {
    config.value = null
    error.value = caught instanceof ApiError ? caught.message : '加载公开配置失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="min-h-screen bg-background">
    <header class="border-b">
      <div class="mx-auto flex h-14 max-w-5xl items-center justify-between px-4 sm:px-6">
        <div class="flex items-center gap-2">
          <img
            v-if="logoUrl"
            :src="logoUrl"
            alt="站点 Logo"
            class="size-6 rounded object-contain"
          />
          <span class="text-base font-semibold">{{ systemName }}</span>
        </div>
        <span class="text-sm text-muted-foreground">Portal</span>
      </div>
    </header>

    <main class="mx-auto max-w-5xl px-4 py-16 sm:px-6 sm:py-24">
      <div class="max-w-2xl space-y-6">
        <p class="text-sm font-medium text-muted-foreground">匿名公开站点</p>
        <h1 class="text-4xl font-semibold tracking-tight sm:text-5xl">{{ systemName }} Portal</h1>
        <p class="text-base leading-7 text-muted-foreground">
          Portal 已接入公开配置接口，系统名称与 Logo 来自后端
          <code class="rounded bg-muted px-1.5 py-0.5 font-mono text-sm">
            GET /api/v1/public/system/config
          </code>
          。
        </p>

        <div class="rounded-xl border bg-card p-4 text-sm">
          <p v-if="loading" class="text-muted-foreground">正在加载公开配置...</p>
          <div v-else-if="error" class="flex flex-wrap items-center gap-3">
            <span class="text-destructive">{{ error }}</span>
            <button
              type="button"
              class="text-primary underline-offset-4 hover:underline"
              @click="load"
            >
              重试
            </button>
          </div>
          <dl v-else class="grid gap-2 sm:grid-cols-[6rem_1fr]">
            <dt class="text-muted-foreground">系统名称</dt>
            <dd>{{ systemName }}</dd>
            <dt class="text-muted-foreground">Logo 地址</dt>
            <dd class="break-all font-mono text-xs">{{ logoUrl || '未配置' }}</dd>
          </dl>
        </div>

        <a
          href="/api/v1/public/system/config"
          target="_blank"
          rel="noreferrer"
          class="inline-flex items-center gap-2 text-sm font-medium underline-offset-4 hover:underline"
        >
          查看公开配置接口
          <ArrowRight class="size-4" />
        </a>
      </div>
    </main>
  </div>
</template>

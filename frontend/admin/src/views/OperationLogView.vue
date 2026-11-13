<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { ApiError } from '@/api/http'
import { fetchOperationLog, fetchOperationLogs } from '@/api/logs'
import type { OperationLogDetail, OperationLogSummary } from '@/api/types'
import AppDialog from '@/components/app/AppDialog.vue'
import PageHeader from '@/components/app/PageHeader.vue'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()

const MODULES = ['iam', 'system', 'audit']
const ACTIONS = ['CREATE', 'UPDATE', 'DELETE', 'STATUS_CHANGE', 'ASSIGN']

const filters = reactive({
  startTime: '',
  endTime: '',
  username: '',
  module: '',
  action: '',
  success: '',
})
const page = ref(1)
const size = ref(20)
const sort = ref('createdAt,desc')
const items = ref<OperationLogSummary[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<OperationLogDetail | null>(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchOperationLogs({
      page: page.value,
      size: size.value,
      sort: sort.value,
      startTime: filters.startTime || undefined,
      endTime: filters.endTime || undefined,
      username: filters.username.trim() || undefined,
      module: filters.module || undefined,
      action: filters.action || undefined,
      success: filters.success === '' ? undefined : filters.success === 'true',
    })
    items.value = result.items
    total.value = result.total
  } catch (caught) {
    items.value = []
    total.value = 0
    error.value = caught instanceof ApiError ? caught.message : '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  void load()
}

function resetFilters() {
  filters.startTime = ''
  filters.endTime = ''
  filters.username = ''
  filters.module = ''
  filters.action = ''
  filters.success = ''
  sort.value = 'createdAt,desc'
  page.value = 1
  void load()
}

function changeSize() {
  page.value = 1
  void load()
}

function previousPage() {
  if (page.value > 1) {
    page.value -= 1
    void load()
  }
}

function nextPage() {
  if (page.value < totalPages.value) {
    page.value += 1
    void load()
  }
}

async function openDetail(log: OperationLogSummary) {
  if (log.id === undefined) {
    return
  }
  const id = log.id
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await fetchOperationLog(id)
  } catch (caught) {
    toast.show(caught instanceof ApiError ? caught.message : '加载日志详情失败', 'error')
    detailOpen.value = false
  } finally {
    detailLoading.value = false
  }
}

function formatDateTime(value: string | null | undefined) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

onMounted(() => {
  void load()
})
</script>

<template>
  <PageHeader title="操作日志" description="记录增删改、状态变更与授权操作，仅支持查询。" />

  <div class="mt-6 rounded-xl border bg-card">
    <form class="flex flex-wrap items-end gap-3 border-b p-4" @submit.prevent="search">
      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="op-start">开始时间</label>
        <input
          id="op-start"
          v-model="filters.startTime"
          type="datetime-local"
          class="flex h-9 w-52 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none focus-visible:border-ring"
        />
      </div>

      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="op-end">结束时间</label>
        <input
          id="op-end"
          v-model="filters.endTime"
          type="datetime-local"
          class="flex h-9 w-52 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none focus-visible:border-ring"
        />
      </div>

      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="op-username">操作人</label>
        <input
          id="op-username"
          v-model="filters.username"
          type="text"
          maxlength="64"
          placeholder="用户名前缀"
          class="flex h-9 w-40 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none focus-visible:border-ring"
        />
      </div>

      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="op-module">模块</label>
        <select
          id="op-module"
          v-model="filters.module"
          class="flex h-9 w-32 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none focus-visible:border-ring"
        >
          <option value="">全部</option>
          <option v-for="module in MODULES" :key="module" :value="module">{{ module }}</option>
        </select>
      </div>

      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="op-action">动作</label>
        <select
          id="op-action"
          v-model="filters.action"
          class="flex h-9 w-40 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none focus-visible:border-ring"
        >
          <option value="">全部</option>
          <option v-for="action in ACTIONS" :key="action" :value="action">{{ action }}</option>
        </select>
      </div>

      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="op-success">结果</label>
        <select
          id="op-success"
          v-model="filters.success"
          class="flex h-9 w-28 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none focus-visible:border-ring"
        >
          <option value="">全部</option>
          <option value="true">成功</option>
          <option value="false">失败</option>
        </select>
      </div>

      <button
        type="submit"
        class="inline-flex h-9 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90 disabled:pointer-events-none disabled:opacity-50"
        :disabled="loading"
      >
        查询
      </button>
      <button
        type="button"
        class="inline-flex h-9 items-center justify-center rounded-md border border-input px-4 text-sm font-medium transition-colors hover:bg-accent disabled:pointer-events-none disabled:opacity-50"
        :disabled="loading"
        @click="resetFilters"
      >
        重置
      </button>
    </form>

    <div
      v-if="error"
      class="flex items-center justify-between gap-3 border-b bg-destructive/5 px-4 py-3 text-sm text-destructive"
    >
      <span>{{ error }}</span>
      <button type="button" class="shrink-0 underline-offset-4 hover:underline" @click="load">
        重试
      </button>
    </div>

    <div class="overflow-x-auto">
      <table class="w-full min-w-[1000px] text-sm">
        <thead class="border-b bg-muted/40 text-left text-xs text-muted-foreground">
          <tr>
            <th class="px-4 py-3 font-medium">时间</th>
            <th class="px-4 py-3 font-medium">操作人</th>
            <th class="px-4 py-3 font-medium">模块</th>
            <th class="px-4 py-3 font-medium">动作</th>
            <th class="px-4 py-3 font-medium">请求</th>
            <th class="px-4 py-3 font-medium">结果</th>
            <th class="px-4 py-3 font-medium">耗时</th>
            <th class="px-4 py-3 font-medium">IP</th>
            <th class="px-4 py-3 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="9" class="px-4 py-10 text-center text-muted-foreground">加载中...</td>
          </tr>
          <tr v-else-if="items.length === 0">
            <td colspan="9" class="px-4 py-10 text-center text-muted-foreground">暂无数据</td>
          </tr>
          <template v-else>
            <tr v-for="log in items" :key="log.id" class="border-b last:border-0">
              <td class="px-4 py-3 text-muted-foreground">{{ formatDateTime(log.createdAt) }}</td>
              <td class="px-4 py-3">{{ log.username || '-' }}</td>
              <td class="px-4 py-3 font-mono text-xs">{{ log.module }}</td>
              <td class="px-4 py-3 font-mono text-xs">{{ log.action }}</td>
              <td class="max-w-72 px-4 py-3">
                <span class="block truncate font-mono text-xs" :title="log.requestUri ?? ''">
                  {{ log.requestMethod }} {{ log.requestUri }}
                </span>
              </td>
              <td class="px-4 py-3">
                <span
                  class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                  :class="
                    log.success
                      ? 'bg-emerald-50 text-emerald-700'
                      : 'bg-destructive/10 text-destructive'
                  "
                >
                  {{ log.success ? '成功' : '失败' }}
                </span>
              </td>
              <td class="px-4 py-3 text-muted-foreground">
                {{ log.durationMs == null ? '-' : `${log.durationMs} ms` }}
              </td>
              <td class="px-4 py-3 text-muted-foreground">{{ log.ip || '-' }}</td>
              <td class="px-4 py-3 text-right">
                <button type="button" class="text-primary hover:underline" @click="openDetail(log)">
                  详情
                </button>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>

    <div class="flex flex-wrap items-center justify-between gap-3 border-t p-4 text-sm">
      <span class="text-muted-foreground">共 {{ total }} 条</span>
      <div class="flex items-center gap-2">
        <label class="flex items-center gap-1 text-muted-foreground">
          每页
          <select
            v-model.number="size"
            class="h-8 rounded-md border border-input bg-transparent px-2 text-sm outline-none focus-visible:border-ring"
            @change="changeSize"
          >
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
          </select>
        </label>
        <button
          type="button"
          class="inline-flex h-8 items-center rounded-md border border-input px-3 text-sm disabled:pointer-events-none disabled:opacity-40"
          :disabled="page <= 1 || loading"
          @click="previousPage"
        >
          上一页
        </button>
        <span class="min-w-16 text-center text-muted-foreground">{{ page }} / {{ totalPages }}</span>
        <button
          type="button"
          class="inline-flex h-8 items-center rounded-md border border-input px-3 text-sm disabled:pointer-events-none disabled:opacity-40"
          :disabled="page >= totalPages || loading"
          @click="nextPage"
        >
          下一页
        </button>
      </div>
    </div>
  </div>

  <AppDialog :open="detailOpen" title="操作日志详情" @close="detailOpen = false">
    <p v-if="detailLoading" class="py-6 text-center text-sm text-muted-foreground">加载中...</p>
    <div v-else-if="detail" class="space-y-3 text-sm">
      <div class="grid grid-cols-[6rem_1fr] gap-2">
        <span class="text-muted-foreground">时间</span>
        <span>{{ formatDateTime(detail.createdAt) }}</span>
        <span class="text-muted-foreground">操作人</span>
        <span>{{ detail.username || '-' }}</span>
        <span class="text-muted-foreground">模块 / 动作</span>
        <span class="font-mono text-xs">{{ detail.module }} / {{ detail.action }}</span>
        <span class="text-muted-foreground">方法</span>
        <span class="font-mono text-xs">{{ detail.method || '-' }}</span>
        <span class="text-muted-foreground">请求</span>
        <span class="font-mono text-xs">
          {{ detail.requestMethod }} {{ detail.requestUri }}
        </span>
        <span class="text-muted-foreground">链路</span>
        <span class="font-mono text-xs">{{ detail.traceId || '-' }}</span>
        <span class="text-muted-foreground">来源</span>
        <span class="text-xs">{{ detail.ip || '-' }} · {{ detail.userAgent || '-' }}</span>
        <span class="text-muted-foreground">结果</span>
        <span>{{ detail.success ? '成功' : `失败：${detail.errorMessage || '-'}` }}</span>
      </div>
      <div class="space-y-1">
        <p class="text-muted-foreground">请求参数</p>
        <pre class="max-h-64 overflow-auto whitespace-pre-wrap break-all rounded-md bg-muted p-3 font-mono text-xs">{{ detail.requestParams || '(空)' }}</pre>
      </div>
    </div>
  </AppDialog>
</template>

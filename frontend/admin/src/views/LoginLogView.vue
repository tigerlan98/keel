<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { ApiError } from '@/api/http'
import { fetchLoginLog, fetchLoginLogs } from '@/api/logs'
import type { LoginLogDetail, LoginLogSummary } from '@/api/types'
import AppDialog from '@/components/app/AppDialog.vue'
import PageHeader from '@/components/app/PageHeader.vue'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()

const ACTIONS = ['LOGIN', 'LOGOUT', 'LOGIN_FAILED'] as const
type LoginActionFilter = (typeof ACTIONS)[number]

const filters = reactive({
  startTime: '',
  endTime: '',
  username: '',
  action: '' as '' | LoginActionFilter,
  success: '',
  ip: '',
})
const page = ref(1)
const size = ref(20)
const sort = ref('createdAt,desc')
const items = ref<LoginLogSummary[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<LoginLogDetail | null>(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchLoginLogs({
      page: page.value,
      size: size.value,
      sort: sort.value,
      startTime: filters.startTime || undefined,
      endTime: filters.endTime || undefined,
      username: filters.username.trim() || undefined,
      action: filters.action || undefined,
      success: filters.success === '' ? undefined : filters.success === 'true',
      ip: filters.ip.trim() || undefined,
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
  filters.action = ''
  filters.success = ''
  filters.ip = ''
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

async function openDetail(log: LoginLogSummary) {
  if (log.id === undefined) {
    return
  }
  const id = log.id
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await fetchLoginLog(id)
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
  <PageHeader title="登录日志" description="记录登录、登出与登录失败，仅支持查询。" />

  <div class="mt-6 rounded-xl border bg-card">
    <form class="flex flex-wrap items-end gap-3 border-b p-4" @submit.prevent="search">
      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="login-start">开始时间</label>
        <input
          id="login-start"
          v-model="filters.startTime"
          type="datetime-local"
          class="flex h-9 w-52 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none focus-visible:border-ring"
        />
      </div>

      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="login-end">结束时间</label>
        <input
          id="login-end"
          v-model="filters.endTime"
          type="datetime-local"
          class="flex h-9 w-52 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none focus-visible:border-ring"
        />
      </div>

      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="login-username">登录名</label>
        <input
          id="login-username"
          v-model="filters.username"
          type="text"
          maxlength="64"
          placeholder="登录名前缀"
          class="flex h-9 w-40 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none focus-visible:border-ring"
        />
      </div>

      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="login-action">动作</label>
        <select
          id="login-action"
          v-model="filters.action"
          class="flex h-9 w-36 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none focus-visible:border-ring"
        >
          <option value="">全部</option>
          <option v-for="action in ACTIONS" :key="action" :value="action">{{ action }}</option>
        </select>
      </div>

      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="login-success">结果</label>
        <select
          id="login-success"
          v-model="filters.success"
          class="flex h-9 w-28 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none focus-visible:border-ring"
        >
          <option value="">全部</option>
          <option value="true">成功</option>
          <option value="false">失败</option>
        </select>
      </div>

      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="login-ip">IP</label>
        <input
          id="login-ip"
          v-model="filters.ip"
          type="text"
          maxlength="64"
          class="flex h-9 w-36 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none focus-visible:border-ring"
        />
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
      <table class="w-full min-w-[900px] text-sm">
        <thead class="border-b bg-muted/40 text-left text-xs text-muted-foreground">
          <tr>
            <th class="px-4 py-3 font-medium">时间</th>
            <th class="px-4 py-3 font-medium">登录名</th>
            <th class="px-4 py-3 font-medium">动作</th>
            <th class="px-4 py-3 font-medium">账号体系</th>
            <th class="px-4 py-3 font-medium">终端</th>
            <th class="px-4 py-3 font-medium">IP</th>
            <th class="px-4 py-3 font-medium">结果</th>
            <th class="px-4 py-3 font-medium">失败原因</th>
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
              <td class="px-4 py-3">{{ log.username }}</td>
              <td class="px-4 py-3 font-mono text-xs">{{ log.action }}</td>
              <td class="px-4 py-3 text-muted-foreground">{{ log.loginType }}</td>
              <td class="px-4 py-3 text-muted-foreground">{{ log.device }}</td>
              <td class="px-4 py-3 text-muted-foreground">{{ log.ip || '-' }}</td>
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
              <td class="px-4 py-3 font-mono text-xs text-muted-foreground">
                {{ log.failureReason || '-' }}
              </td>
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

  <AppDialog :open="detailOpen" title="登录日志详情" @close="detailOpen = false">
    <p v-if="detailLoading" class="py-6 text-center text-sm text-muted-foreground">加载中...</p>
    <div v-else-if="detail" class="space-y-3 text-sm">
      <div class="grid grid-cols-[6rem_1fr] gap-2">
        <span class="text-muted-foreground">时间</span>
        <span>{{ formatDateTime(detail.createdAt) }}</span>
        <span class="text-muted-foreground">登录名</span>
        <span>{{ detail.username }}</span>
        <span class="text-muted-foreground">动作</span>
        <span class="font-mono text-xs">{{ detail.action }}</span>
        <span class="text-muted-foreground">账号体系</span>
        <span>{{ detail.loginType }}</span>
        <span class="text-muted-foreground">终端</span>
        <span>{{ detail.device }}</span>
        <span class="text-muted-foreground">链路</span>
        <span class="font-mono text-xs">{{ detail.traceId || '-' }}</span>
        <span class="text-muted-foreground">来源</span>
        <span class="text-xs">{{ detail.ip || '-' }} · {{ detail.userAgent || '-' }}</span>
        <span class="text-muted-foreground">结果</span>
        <span>{{ detail.success ? '成功' : `失败：${detail.failureReason || '-'}` }}</span>
      </div>
    </div>
  </AppDialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { ApiError } from '@/api/http'
import type { UserStatus, UserSummary } from '@/api/types'
import { fetchUsers } from '@/api/users'
import PageHeader from '@/components/app/PageHeader.vue'

const filters = reactive({
  username: '',
  status: '' as '' | UserStatus,
})

const page = ref(1)
const size = ref(20)
const sort = ref('createdAt,desc')
const items = ref<UserSummary[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchUsers({
      page: page.value,
      size: size.value,
      sort: sort.value,
      username: filters.username.trim() || undefined,
      status: filters.status || undefined,
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
  filters.username = ''
  filters.status = ''
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

function statusText(status: UserStatus) {
  return status === 'ENABLED' ? '启用' : '禁用'
}

function statusClass(status: UserStatus) {
  return status === 'ENABLED'
    ? 'bg-emerald-50 text-emerald-700'
    : 'bg-destructive/10 text-destructive'
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  void load()
})
</script>

<template>
  <PageHeader title="用户管理" description="查询系统用户，支持用户名前缀与状态筛选。" />

  <div class="mt-6 rounded-xl border bg-card">
    <form class="flex flex-wrap items-end gap-3 border-b p-4" @submit.prevent="search">
      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="user-username">用户名</label>
        <input
          id="user-username"
          v-model="filters.username"
          type="text"
          maxlength="64"
          placeholder="用户名前缀"
          class="flex h-9 w-52 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
        />
      </div>

      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="user-status">状态</label>
        <select
          id="user-status"
          v-model="filters.status"
          class="flex h-9 w-32 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
        >
          <option value="">全部</option>
          <option value="ENABLED">启用</option>
          <option value="DISABLED">禁用</option>
        </select>
      </div>

      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="user-sort">排序</label>
        <select
          id="user-sort"
          v-model="sort"
          class="flex h-9 w-40 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
        >
          <option value="createdAt,desc">创建时间 ↓</option>
          <option value="createdAt,asc">创建时间 ↑</option>
          <option value="username,asc">用户名 ↑</option>
          <option value="username,desc">用户名 ↓</option>
          <option value="lastLoginAt,desc">最近登录 ↓</option>
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
      <table class="w-full min-w-[720px] text-sm">
        <thead class="border-b bg-muted/40 text-left text-xs text-muted-foreground">
          <tr>
            <th class="px-4 py-3 font-medium">用户名</th>
            <th class="px-4 py-3 font-medium">昵称</th>
            <th class="px-4 py-3 font-medium">状态</th>
            <th class="px-4 py-3 font-medium">最近登录</th>
            <th class="px-4 py-3 font-medium">创建时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="5" class="px-4 py-10 text-center text-muted-foreground">加载中...</td>
          </tr>
          <tr v-else-if="items.length === 0">
            <td colspan="5" class="px-4 py-10 text-center text-muted-foreground">暂无数据</td>
          </tr>
          <template v-else>
            <tr v-for="user in items" :key="user.id" class="border-b last:border-0">
              <td class="px-4 py-3 font-medium">{{ user.username }}</td>
              <td class="px-4 py-3 text-muted-foreground">{{ user.nickname || '-' }}</td>
              <td class="px-4 py-3">
                <span
                  class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                  :class="statusClass(user.status)"
                >
                  {{ statusText(user.status) }}
                </span>
              </td>
              <td class="px-4 py-3 text-muted-foreground">
                {{ formatDateTime(user.lastLoginAt) }}
              </td>
              <td class="px-4 py-3 text-muted-foreground">{{ formatDateTime(user.createdAt) }}</td>
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
</template>

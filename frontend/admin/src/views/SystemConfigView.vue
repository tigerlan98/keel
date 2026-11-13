<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { fetchConfig, fetchConfigs, updateConfig } from '@/api/configs'
import { ApiError } from '@/api/http'
import type { ConfigDetail, ConfigSummary } from '@/api/types'
import AppDialog from '@/components/app/AppDialog.vue'
import PageHeader from '@/components/app/PageHeader.vue'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()

const filters = reactive({
  group: '',
  keyword: '',
})
const page = ref(1)
const size = ref(20)
const sort = ref('updatedAt,desc')
const items = ref<ConfigSummary[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

const editOpen = ref(false)
const editing = ref<ConfigDetail | null>(null)
const saving = ref(false)
const formError = ref('')
const form = reactive({
  configValue: '',
  description: '',
  publicFlag: false,
  version: 0,
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchConfigs({
      page: page.value,
      size: size.value,
      sort: sort.value,
      group: filters.group || undefined,
      keyword: filters.keyword.trim() || undefined,
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
  filters.group = ''
  filters.keyword = ''
  sort.value = 'updatedAt,desc'
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

async function openEdit(config: ConfigSummary) {
  if (config.id === undefined) {
    return
  }
  formError.value = ''
  try {
    const detail = await fetchConfig(config.id)
    editing.value = detail
    form.configValue = detail.configValue ?? ''
    form.description = detail.description ?? ''
    form.publicFlag = detail.publicFlag ?? false
    form.version = detail.version ?? 0
    editOpen.value = true
  } catch (caught) {
    toast.show(caught instanceof ApiError ? caught.message : '加载参数失败', 'error')
  }
}

async function save() {
  if (!editing.value || editing.value.id === undefined) {
    return
  }
  const id = editing.value.id
  if (!form.configValue) {
    formError.value = '请输入参数值'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    await updateConfig(id, {
      configValue: form.configValue,
      description: form.description.trim() || undefined,
      publicFlag: form.publicFlag,
      version: form.version,
    })
    toast.show('参数已更新', 'success')
    editOpen.value = false
    await load()
  } catch (caught) {
    formError.value = caught instanceof ApiError ? caught.message : '保存失败，请稍后重试'
  } finally {
    saving.value = false
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
  <PageHeader title="系统参数" description="查看并维护系统参数值；参数新增与删除仅由数据库迁移维护。" />

  <div class="mt-6 rounded-xl border bg-card">
    <form class="flex flex-wrap items-end gap-3 border-b p-4" @submit.prevent="search">
      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="config-group">分组</label>
        <select
          id="config-group"
          v-model="filters.group"
          class="flex h-9 w-36 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
        >
          <option value="">全部</option>
          <option value="SYSTEM">SYSTEM</option>
          <option value="AUTH">AUTH</option>
          <option value="AUDIT">AUDIT</option>
        </select>
      </div>

      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="config-keyword">参数 Key</label>
        <input
          id="config-keyword"
          v-model="filters.keyword"
          type="text"
          maxlength="128"
          placeholder="key 前缀"
          class="flex h-9 w-56 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
        />
      </div>

      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="config-sort">排序</label>
        <select
          id="config-sort"
          v-model="sort"
          class="flex h-9 w-36 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
        >
          <option value="updatedAt,desc">更新时间 ↓</option>
          <option value="updatedAt,asc">更新时间 ↑</option>
          <option value="configKey,asc">Key ↑</option>
          <option value="configKey,desc">Key ↓</option>
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
            <th class="px-4 py-3 font-medium">参数 Key</th>
            <th class="px-4 py-3 font-medium">参数值</th>
            <th class="px-4 py-3 font-medium">分组</th>
            <th class="px-4 py-3 font-medium">公开</th>
            <th class="px-4 py-3 font-medium">说明</th>
            <th class="px-4 py-3 font-medium">更新时间</th>
            <th class="px-4 py-3 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7" class="px-4 py-10 text-center text-muted-foreground">加载中...</td>
          </tr>
          <tr v-else-if="items.length === 0">
            <td colspan="7" class="px-4 py-10 text-center text-muted-foreground">暂无数据</td>
          </tr>
          <template v-else>
            <tr v-for="config in items" :key="config.id" class="border-b last:border-0">
              <td class="px-4 py-3 font-mono text-xs">{{ config.configKey }}</td>
              <td class="max-w-72 px-4 py-3">
                <span class="block truncate font-mono text-xs" :title="config.configValue">
                  {{ config.configValue || '(空)' }}
                </span>
              </td>
              <td class="px-4 py-3 text-muted-foreground">{{ config.configGroup }}</td>
              <td class="px-4 py-3">
                <span
                  class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                  :class="
                    config.publicFlag
                      ? 'bg-emerald-50 text-emerald-700'
                      : 'bg-muted text-muted-foreground'
                  "
                >
                  {{ config.publicFlag ? '公开' : '私有' }}
                </span>
              </td>
              <td class="max-w-56 px-4 py-3 text-muted-foreground">
                <span class="block truncate" :title="config.description ?? ''">
                  {{ config.description || '-' }}
                </span>
              </td>
              <td class="px-4 py-3 text-muted-foreground">
                {{ formatDateTime(config.updatedAt) }}
              </td>
              <td class="px-4 py-3 text-right">
                <button
                  v-permission="'sys:config:update'"
                  type="button"
                  class="text-primary hover:underline"
                  @click="openEdit(config)"
                >
                  编辑
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

  <AppDialog :open="editOpen" title="编辑系统参数" @close="editOpen = false">
    <form id="config-form" class="space-y-4" @submit.prevent="save">
      <div class="space-y-1">
        <label class="text-sm font-medium" for="config-form-key">参数 Key</label>
        <input
          id="config-form-key"
          :value="editing?.configKey"
          type="text"
          disabled
          class="flex h-9 w-full rounded-md border border-input bg-muted px-3 font-mono text-xs"
        />
        <p class="text-xs text-muted-foreground">参数 Key 不可修改</p>
      </div>

      <div class="space-y-1">
        <label class="text-sm font-medium" for="config-form-value">参数值</label>
        <textarea
          id="config-form-value"
          v-model="form.configValue"
          rows="4"
          maxlength="4000"
          class="flex w-full rounded-md border border-input bg-transparent px-3 py-2 font-mono text-xs outline-none focus-visible:border-ring"
        />
        <p class="text-xs text-muted-foreground">已知参数 Key 会按类型校验取值范围</p>
      </div>

      <div class="space-y-1">
        <label class="text-sm font-medium" for="config-form-description">说明</label>
        <input
          id="config-form-description"
          v-model="form.description"
          type="text"
          maxlength="255"
          class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm outline-none focus-visible:border-ring"
        />
      </div>

      <label class="flex items-center gap-2 text-sm">
        <input v-model="form.publicFlag" type="checkbox" />
        公开参数（仅 system.* 参数允许公开）
      </label>

      <p v-if="formError" class="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">
        {{ formError }}
      </p>
    </form>
    <template #footer>
      <button
        type="button"
        class="inline-flex h-9 items-center rounded-md border border-input px-4 text-sm hover:bg-accent"
        @click="editOpen = false"
      >
        取消
      </button>
      <button
        type="submit"
        form="config-form"
        :disabled="saving"
        class="inline-flex h-9 items-center rounded-md bg-primary px-4 text-sm text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
      >
        {{ saving ? '保存中...' : '保存' }}
      </button>
    </template>
  </AppDialog>
</template>

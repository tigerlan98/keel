<script setup lang="ts">
import { Eye, RefreshCw, Search } from '@lucide/vue'
import AppButton from '@/components/app/AppButton.vue'
import AppDataTable from '@/components/app/AppDataTable.vue'
import AppErrorState from '@/components/app/AppErrorState.vue'
import AppField from '@/components/app/AppField.vue'
import AppFilterBar from '@/components/app/AppFilterBar.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppPagination from '@/components/app/AppPagination.vue'
import AppPanel from '@/components/app/AppPanel.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import AppStatusTag from '@/components/app/AppStatusTag.vue'

import { onMounted, reactive, ref } from 'vue'

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
const columns = [
  { key: 'createdAt', label: '时间' },
  { key: 'username', label: '操作人' },
  { key: 'module', label: '模块' },
  { key: 'action', label: '动作' },
  { key: 'request', label: '请求' },
  { key: 'success', label: '结果' },
  { key: 'durationMs', label: '耗时' },
  { key: 'ip', label: 'IP' },
  { key: 'actions', label: '操作', align: 'right' as const, sticky: true, width: '80px' },
]

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

function changeSize(value: number) {
  size.value = value
  page.value = 1
  void load()
}
function changePage(value: number) {
  page.value = value
  void load()
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

  <div class="mt-5 space-y-4">
    <AppFilterBar @submit="search"
      ><AppField label="开始时间" for="op-start" class="w-[220px]"
        ><AppInput id="op-start" v-model="filters.startTime" type="datetime-local" /></AppField
      ><AppField label="结束时间" for="op-end" class="w-[220px]"
        ><AppInput id="op-end" v-model="filters.endTime" type="datetime-local" /></AppField
      ><AppField label="操作人" for="op-username" class="w-[180px]"
        ><AppInput
          id="op-username"
          v-model="filters.username"
          maxlength="64"
          type="text"
          placeholder="用户名前缀" /></AppField
      ><AppField label="模块" for="op-module" class="w-[150px]"
        ><AppSelect
          id="op-module"
          v-model="filters.module"
          :options="[
            { label: '全部', value: '' },
            ...MODULES.map((value) => ({ label: value, value })),
          ]" /></AppField
      ><AppField label="动作" for="op-action" class="w-[180px]"
        ><AppSelect
          id="op-action"
          v-model="filters.action"
          :options="[
            { label: '全部', value: '' },
            ...ACTIONS.map((value) => ({ label: value, value })),
          ]" /></AppField
      ><AppField label="结果" for="op-success" class="w-[130px]"
        ><AppSelect
          id="op-success"
          v-model="filters.success"
          :options="[
            { label: '全部', value: '' },
            { label: '成功', value: 'true' },
            { label: '失败', value: 'false' },
          ]"
      /></AppField>
      <template #actions
        ><AppButton type="submit" :loading="loading" :icon="Search">查询</AppButton
        ><AppButton variant="outline" :disabled="loading" :icon="RefreshCw" @click="resetFilters"
          >重置</AppButton
        ></template
      >
    </AppFilterBar>
    <AppPanel>
      <template #header
        ><div class="flex items-center gap-2.5">
          <span class="section-label">操作日志</span
          ><span class="text-[11.5px] text-ink-soft">共 {{ total }} 条</span>
        </div></template
      >
      <template #tools
        ><AppButton
          variant="outline"
          size="icon-sm"
          aria-label="刷新"
          title="刷新"
          :loading="loading"
          @click="load"
          ><RefreshCw :size="14" /></AppButton
      ></template>
      <AppErrorState v-if="error" :message="error" @retry="load" />
      <AppDataTable v-else :columns="columns" :rows="items" :loading="loading" :min-width="1200">
        <template #cell-createdAt="{ row }"
          ><span class="tnum text-[12px] text-ink-soft">{{
            formatDateTime(row.createdAt)
          }}</span></template
        >
        <template #cell-username="{ row }"
          ><span class="font-semibold text-ink">{{ row.username || '—' }}</span></template
        >
        <template #cell-module="{ row }"
          ><span class="code-chip">{{ row.module }}</span></template
        >
        <template #cell-action="{ row }"
          ><span class="code-chip">{{ row.action }}</span></template
        >
        <template #cell-request="{ row }"
          ><span
            class="block max-w-80 truncate text-[12px] text-ink-muted"
            :title="row.requestUri ?? ''"
            >{{ row.requestMethod }} {{ row.requestUri }}</span
          ></template
        >
        <template #cell-success="{ row }"
          ><AppStatusTag
            :tone="row.success ? 'success' : 'danger'"
            :label="row.success ? '成功' : '失败'"
        /></template>
        <template #cell-durationMs="{ row }"
          ><span class="tnum text-ink-muted">{{
            row.durationMs == null ? '—' : row.durationMs + ' ms'
          }}</span></template
        >
        <template #cell-ip="{ row }"
          ><span class="text-[12px] text-ink-soft">{{ row.ip || '—' }}</span></template
        >
        <template #cell-actions="{ row }"
          ><AppButton
            variant="ghost"
            size="icon-sm"
            aria-label="详情"
            title="详情"
            @click="openDetail(row)"
            ><Eye :size="14" /></AppButton
        ></template>
      </AppDataTable>
      <template v-if="!error && total > 0" #footer
        ><AppPagination
          :page="page"
          :page-size="size"
          :total="total"
          :loading="loading"
          @update:page="changePage"
          @update:page-size="changeSize"
      /></template>
    </AppPanel>
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
        <span class="font-mono text-xs"> {{ detail.requestMethod }} {{ detail.requestUri }} </span>
        <span class="text-muted-foreground">链路</span>
        <span class="font-mono text-xs">{{ detail.traceId || '-' }}</span>
        <span class="text-muted-foreground">来源</span>
        <span class="text-xs">{{ detail.ip || '-' }} · {{ detail.userAgent || '-' }}</span>
        <span class="text-muted-foreground">结果</span>
        <span>{{ detail.success ? '成功' : `失败：${detail.errorMessage || '-'}` }}</span>
      </div>
      <div class="space-y-1">
        <p class="text-muted-foreground">请求参数</p>
        <pre
          class="max-h-64 overflow-auto whitespace-pre-wrap break-all rounded-md bg-muted p-3 font-mono text-xs"
          >{{ detail.requestParams || '(空)' }}</pre>
      </div>
    </div>
  </AppDialog>
</template>

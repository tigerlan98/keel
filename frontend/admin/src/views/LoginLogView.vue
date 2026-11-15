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
const columns = [
  { key: 'createdAt', label: '时间' },
  { key: 'username', label: '登录名' },
  { key: 'action', label: '动作' },
  { key: 'loginType', label: '账号体系' },
  { key: 'device', label: '终端' },
  { key: 'ip', label: 'IP' },
  { key: 'success', label: '结果' },
  { key: 'failureReason', label: '失败原因' },
  { key: 'actions', label: '操作', align: 'right' as const, sticky: true, width: '80px' },
]

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

function changeSize(value: number) {
  size.value = value
  page.value = 1
  void load()
}
function changePage(value: number) {
  page.value = value
  void load()
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

  <div class="mt-5 space-y-4">
    <AppFilterBar @submit="search"
      ><AppField label="开始时间" for="login-start" class="w-[220px]"
        ><AppInput id="login-start" v-model="filters.startTime" type="datetime-local" /></AppField
      ><AppField label="结束时间" for="login-end" class="w-[220px]"
        ><AppInput id="login-end" v-model="filters.endTime" type="datetime-local" /></AppField
      ><AppField label="登录名" for="login-username" class="w-[180px]"
        ><AppInput
          id="login-username"
          v-model="filters.username"
          maxlength="64"
          type="text"
          placeholder="登录名前缀" /></AppField
      ><AppField label="动作" for="login-action" class="w-[180px]"
        ><AppSelect
          id="login-action"
          v-model="filters.action"
          :options="[
            { label: '全部', value: '' },
            ...ACTIONS.map((value) => ({ label: value, value })),
          ]" /></AppField
      ><AppField label="结果" for="login-success" class="w-[130px]"
        ><AppSelect
          id="login-success"
          v-model="filters.success"
          :options="[
            { label: '全部', value: '' },
            { label: '成功', value: 'true' },
            { label: '失败', value: 'false' },
          ]" /></AppField
      ><AppField label="IP" for="login-ip" class="w-[150px]"
        ><AppInput id="login-ip" v-model="filters.ip" type="text" maxlength="64"
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
          <span class="section-label">登录日志</span
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
      <AppDataTable v-else :columns="columns" :rows="items" :loading="loading" :min-width="1100">
        <template #cell-createdAt="{ row }"
          ><span class="tnum text-[12px] text-ink-soft">{{
            formatDateTime(row.createdAt)
          }}</span></template
        >
        <template #cell-username="{ row }"
          ><span class="font-semibold text-ink">{{ row.username }}</span></template
        >
        <template #cell-action="{ row }"
          ><span class="code-chip">{{ row.action }}</span></template
        >
        <template #cell-loginType="{ row }"
          ><span class="text-ink-muted">{{ row.loginType }}</span></template
        >
        <template #cell-device="{ row }"
          ><span class="text-ink-muted">{{ row.device }}</span></template
        >
        <template #cell-ip="{ row }"
          ><span class="text-[12px] text-ink-soft">{{ row.ip || '—' }}</span></template
        >
        <template #cell-success="{ row }"
          ><AppStatusTag
            :tone="row.success ? 'success' : 'danger'"
            :label="row.success ? '成功' : '失败'"
        /></template>
        <template #cell-failureReason="{ row }"
          ><span class="text-[12px] text-ink-muted">{{ row.failureReason || '—' }}</span></template
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

<script setup lang="ts">
import { Pencil, RefreshCw } from '@lucide/vue'
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
const columns = [
  { key: 'configKey', label: '参数 Key' },
  { key: 'configValue', label: '参数值' },
  { key: 'configGroup', label: '分组' },
  { key: 'publicFlag', label: '公开' },
  { key: 'description', label: '说明' },
  { key: 'updatedAt', label: '更新时间' },
  { key: 'actions', label: '操作', align: 'right' as const, sticky: true, width: '80px' },
]

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

function changeSize(value: number) {
  size.value = value
  page.value = 1
  void load()
}
function changePage(value: number) {
  page.value = value
  void load()
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
  <PageHeader
    title="系统参数"
    description="查看并维护系统参数值；参数新增与删除仅由数据库迁移维护。"
  />

  <div class="mt-5 space-y-4">
    <AppFilterBar @submit="search"
      ><AppField label="分组" for="config-group" class="w-[150px]"
        ><AppSelect
          id="config-group"
          v-model="filters.group"
          :options="[
            { label: '全部', value: '' },
            { label: 'SYSTEM', value: 'SYSTEM' },
            { label: 'AUTH', value: 'AUTH' },
            { label: 'AUDIT', value: 'AUDIT' },
          ]" /></AppField
      ><AppField label="参数 Key" for="config-keyword" class="w-[220px]"
        ><AppInput
          id="config-keyword"
          v-model="filters.keyword"
          maxlength="128"
          type="text"
          placeholder="key 前缀" /></AppField
      ><AppField label="排序" for="config-sort" class="w-[180px]"
        ><AppSelect
          id="config-sort"
          v-model="sort"
          :options="[
            { label: '更新时间 ↓', value: 'updatedAt,desc' },
            { label: '更新时间 ↑', value: 'updatedAt,asc' },
            { label: 'Key ↑', value: 'configKey,asc' },
            { label: 'Key ↓', value: 'configKey,desc' },
          ]"
      /></AppField>
      <template #actions
        ><AppButton type="submit" :loading="loading">查询</AppButton
        ><AppButton variant="outline" :disabled="loading" @click="resetFilters"
          >重置</AppButton
        ></template
      >
    </AppFilterBar>
    <AppPanel>
      <template #header
        ><div class="flex items-center gap-2.5">
          <span class="section-label">参数列表</span
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
        <template #cell-configKey="{ row }"
          ><span class="code-chip">{{ row.configKey }}</span></template
        >
        <template #cell-configValue="{ row }"
          ><span class="block max-w-64 truncate text-ink" :title="row.configValue">{{
            row.configValue || '(空)'
          }}</span></template
        >
        <template #cell-configGroup="{ row }"
          ><span class="text-ink-muted">{{ row.configGroup }}</span></template
        >
        <template #cell-publicFlag="{ row }"
          ><AppStatusTag
            :dot="false"
            :tone="row.publicFlag ? 'info' : 'neutral'"
            :label="row.publicFlag ? '公开' : '私有'"
        /></template>
        <template #cell-description="{ row }"
          ><span class="block max-w-64 truncate text-ink-muted" :title="row.description ?? ''">{{
            row.description || '—'
          }}</span></template
        >
        <template #cell-updatedAt="{ row }"
          ><span class="tnum text-[12px] text-ink-soft">{{
            formatDateTime(row.updatedAt)
          }}</span></template
        >
        <template #cell-actions="{ row }"
          ><AppButton
            v-permission="'sys:config:update'"
            variant="ghost"
            size="icon-sm"
            aria-label="编辑"
            title="编辑"
            @click="openEdit(row)"
            ><Pencil :size="14" /></AppButton
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

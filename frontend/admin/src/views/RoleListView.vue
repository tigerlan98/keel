<script setup lang="ts">
import { Pencil, Plus, RefreshCw, ShieldCheck, Trash2 } from '@lucide/vue'
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
import {
  createRole,
  deleteRole,
  fetchRole,
  fetchRoleResourceIds,
  fetchRoles,
  replaceRoleResources,
  updateRole,
} from '@/api/roles'
import { fetchResourceTree } from '@/api/resources'
import type { ResourceNode, RoleDetail, RoleStatus, RoleSummary } from '@/api/types'
import AppDialog from '@/components/app/AppDialog.vue'
import PageHeader from '@/components/app/PageHeader.vue'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()

const filters = reactive({
  keyword: '',
  status: '' as '' | RoleStatus,
})
const page = ref(1)
const size = ref(20)
const sort = ref('sort,asc')
const items = ref<RoleSummary[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
const columns = [
  { key: 'code', label: '编码' },
  { key: 'name', label: '名称' },
  { key: 'description', label: '描述' },
  { key: 'status', label: '状态' },
  { key: 'sort', label: '排序' },
  { key: 'createdAt', label: '创建时间' },
  { key: 'actions', label: '操作', align: 'right' as const, sticky: true },
]

const dialogOpen = ref(false)
const editing = ref<RoleDetail | null>(null)
const saving = ref(false)
const formError = ref('')
const form = reactive({
  code: '',
  name: '',
  description: '',
  status: 'ENABLED' as RoleStatus,
  sort: 0,
})

const deleteTarget = ref<RoleSummary | null>(null)
const deleting = ref(false)

const assignTarget = ref<RoleSummary | null>(null)
const resourceOptions = ref<{ node: ResourceNode; depth: number }[]>([])
const selectedResourceIds = ref<number[]>([])
const assignLoading = ref(false)
const assignSaving = ref(false)
const assignError = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchRoles({
      page: page.value,
      size: size.value,
      sort: sort.value,
      keyword: filters.keyword.trim() || undefined,
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
  filters.keyword = ''
  filters.status = ''
  sort.value = 'sort,asc'
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

function openCreate() {
  editing.value = null
  form.code = ''
  form.name = ''
  form.description = ''
  form.status = 'ENABLED'
  form.sort = 0
  formError.value = ''
  dialogOpen.value = true
}

async function openEdit(role: RoleSummary) {
  formError.value = ''
  try {
    const detail = await fetchRole(role.id)
    editing.value = detail
    form.code = detail.code
    form.name = detail.name
    form.description = detail.description ?? ''
    form.status = detail.status
    form.sort = detail.sort
    dialogOpen.value = true
  } catch (caught) {
    toast.show(caught instanceof ApiError ? caught.message : '加载角色失败', 'error')
  }
}

async function save() {
  formError.value = ''
  if (!form.name.trim()) {
    formError.value = '请输入角色名称'
    return
  }
  if (!editing.value && !/^[A-Z][A-Z0-9_]*$/.test(form.code)) {
    formError.value = '角色编码必须为大写字母、数字和下划线'
    return
  }

  saving.value = true
  try {
    if (editing.value) {
      await updateRole(editing.value.id, {
        name: form.name.trim(),
        description: form.description.trim() || null,
        status: form.status,
        sort: form.sort,
        version: editing.value.version,
      })
      toast.show('角色已更新', 'success')
    } else {
      await createRole({
        code: form.code.trim(),
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        status: form.status,
        sort: form.sort,
      })
      toast.show('角色已创建', 'success')
    }
    dialogOpen.value = false
    await load()
  } catch (caught) {
    formError.value = caught instanceof ApiError ? caught.message : '保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

async function confirmDelete() {
  if (!deleteTarget.value) {
    return
  }
  deleting.value = true
  try {
    await deleteRole(deleteTarget.value.id)
    toast.show('角色已删除', 'success')
    deleteTarget.value = null
    await load()
  } catch (caught) {
    toast.show(caught instanceof ApiError ? caught.message : '删除失败', 'error')
  } finally {
    deleting.value = false
  }
}

async function openAssign(role: RoleSummary) {
  assignTarget.value = role
  assignLoading.value = true
  assignError.value = ''
  selectedResourceIds.value = []
  try {
    const [tree, ids] = await Promise.all([fetchResourceTree(), fetchRoleResourceIds(role.id)])
    resourceOptions.value = flatten(tree)
    selectedResourceIds.value = ids
  } catch (caught) {
    assignError.value = caught instanceof ApiError ? caught.message : '加载资源失败'
  } finally {
    assignLoading.value = false
  }
}

function toggleResource(id: number) {
  const index = selectedResourceIds.value.indexOf(id)
  if (index >= 0) {
    selectedResourceIds.value.splice(index, 1)
  } else {
    selectedResourceIds.value.push(id)
  }
}

async function saveAssign() {
  if (!assignTarget.value) {
    return
  }
  assignSaving.value = true
  assignError.value = ''
  try {
    await replaceRoleResources(assignTarget.value.id, selectedResourceIds.value)
    toast.show('资源授权已更新', 'success')
    assignTarget.value = null
  } catch (caught) {
    assignError.value = caught instanceof ApiError ? caught.message : '保存失败，请稍后重试'
  } finally {
    assignSaving.value = false
  }
}

function flatten(nodes: ResourceNode[], depth = 0): { node: ResourceNode; depth: number }[] {
  const result: { node: ResourceNode; depth: number }[] = []
  for (const node of nodes) {
    result.push({ node, depth })
    result.push(...flatten(node.children ?? [], depth + 1))
  }
  return result
}

function statusText(status: RoleStatus) {
  return status === 'ENABLED' ? '启用' : '禁用'
}

function formatDateTime(value: string | null | undefined) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

onMounted(() => {
  void load()
})
</script>

<template>
  <PageHeader title="角色管理" description="角色决定用户可见的菜单与可用的操作权限。">
    <template #actions
      ><AppButton v-permission="'iam:role:create'" @click="openCreate"
        ><Plus :size="14" />新增角色</AppButton
      ></template
    >
  </PageHeader>
  <div class="mt-5 space-y-4">
    <AppFilterBar @submit="search">
      <AppField label="关键字" for="role-keyword" class="w-[220px]"
        ><AppInput
          id="role-keyword"
          v-model="filters.keyword"
          maxlength="64"
          placeholder="角色名 / 编码前缀"
      /></AppField>
      <AppField label="状态" for="role-status" class="w-[150px]"
        ><AppSelect
          id="role-status"
          v-model="filters.status"
          :options="[
            { label: '全部', value: '' },
            { label: '启用', value: 'ENABLED' },
            { label: '禁用', value: 'DISABLED' },
          ]"
      /></AppField>
      <AppField label="排序" for="role-sort" class="w-[180px]"
        ><AppSelect
          id="role-sort"
          v-model="sort"
          :options="[
            { label: '排序 ↑', value: 'sort,asc' },
            { label: '排序 ↓', value: 'sort,desc' },
            { label: '创建时间 ↓', value: 'createdAt,desc' },
            { label: '编码 ↑', value: 'code,asc' },
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
          <span class="section-label">角色列表</span
          ><span class="text-[11.5px] text-ink-soft">共 {{ total }} 条</span>
        </div></template
      >
      <template #tools
        ><AppButton
          variant="outline"
          size="icon-sm"
          :loading="loading"
          aria-label="刷新"
          title="刷新"
          @click="load"
          ><RefreshCw :size="14" /></AppButton
      ></template>
      <AppErrorState v-if="error" :message="error" @retry="load" />
      <AppDataTable
        v-else
        :columns="columns"
        :rows="items"
        :loading="loading"
        :min-width="900"
        empty-title="暂无角色"
        empty-description="调整筛选条件，或新增一个角色。"
      >
        <template #cell-code="{ row }"
          ><span class="code-chip">{{ row.code }}</span></template
        >
        <template #cell-name="{ row }"
          ><span class="font-semibold text-ink">{{ row.name }}</span
          ><span v-if="row.code === 'SUPER_ADMIN'" class="ml-2 text-[11px] text-ink-soft"
            >内置</span
          ></template
        >
        <template #cell-description="{ row }"
          ><span class="text-ink-muted">{{ row.description || '—' }}</span></template
        >
        <template #cell-status="{ row }"
          ><AppStatusTag
            :tone="row.status === 'ENABLED' ? 'success' : 'danger'"
            :label="statusText(row.status)"
        /></template>
        <template #cell-createdAt="{ row }"
          ><span class="tnum text-[12px] text-ink-soft">{{
            formatDateTime(row.createdAt)
          }}</span></template
        >
        <template #cell-actions="{ row }"
          ><div class="flex justify-end gap-0.5">
            <AppButton
              v-permission="'iam:role:update'"
              variant="ghost"
              size="icon-sm"
              aria-label="编辑"
              title="编辑"
              @click="openEdit(row)"
              ><Pencil :size="14"
            /></AppButton>
            <AppButton
              v-permission="'iam:role:assign-resource'"
              variant="ghost"
              size="icon-sm"
              aria-label="授权"
              title="授权"
              @click="openAssign(row)"
              ><ShieldCheck :size="14"
            /></AppButton>
            <AppButton
              v-permission="'iam:role:delete'"
              variant="danger-ghost"
              size="icon-sm"
              :disabled="row.code === 'SUPER_ADMIN'"
              aria-label="删除"
              title="删除"
              @click="deleteTarget = row"
              ><Trash2 :size="14"
            /></AppButton></div
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
  <AppDialog
    :open="dialogOpen"
    :title="editing ? '编辑角色' : '新增角色'"
    @close="dialogOpen = false"
  >
    <form id="role-form" class="space-y-4" @submit.prevent="save">
      <div class="space-y-1">
        <label class="text-sm font-medium" for="role-code">编码</label>
        <input
          id="role-code"
          v-model="form.code"
          type="text"
          maxlength="64"
          :disabled="!!editing"
          placeholder="如 OPS_ADMIN"
          class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm outline-none focus-visible:border-ring disabled:bg-muted"
        />
        <p v-if="editing" class="text-xs text-muted-foreground">编码创建后不可修改</p>
      </div>
      <div class="space-y-1">
        <label class="text-sm font-medium" for="role-name">名称</label>
        <input
          id="role-name"
          v-model="form.name"
          type="text"
          maxlength="64"
          class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm outline-none focus-visible:border-ring"
        />
      </div>
      <div class="space-y-1">
        <label class="text-sm font-medium" for="role-description">描述</label>
        <textarea
          id="role-description"
          v-model="form.description"
          maxlength="255"
          rows="3"
          class="flex w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm outline-none focus-visible:border-ring"
        />
      </div>
      <div class="grid grid-cols-2 gap-4">
        <div class="space-y-1">
          <label class="text-sm font-medium" for="role-form-status">状态</label>
          <AppSelect
            id="role-form-status"
            v-model="form.status"
            :options="[
              { label: '启用', value: 'ENABLED' },
              { label: '禁用', value: 'DISABLED' },
            ]"
          />
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium" for="role-form-sort">排序</label>
          <input
            id="role-form-sort"
            v-model.number="form.sort"
            type="number"
            min="0"
            class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm outline-none focus-visible:border-ring"
          />
        </div>
      </div>
      <p v-if="formError" class="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">
        {{ formError }}
      </p>
    </form>
    <template #footer>
      <button
        type="button"
        class="inline-flex h-9 items-center rounded-md border border-input px-4 text-sm hover:bg-accent"
        @click="dialogOpen = false"
      >
        取消
      </button>
      <button
        type="submit"
        form="role-form"
        :disabled="saving"
        class="inline-flex h-9 items-center rounded-md bg-primary px-4 text-sm text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
      >
        {{ saving ? '保存中...' : '保存' }}
      </button>
    </template>
  </AppDialog>

  <AppDialog :open="!!deleteTarget" title="删除角色" @close="deleteTarget = null">
    <p class="text-sm text-muted-foreground">
      确定删除角色「{{ deleteTarget?.name }}」吗？该操作不可撤销。
    </p>
    <template #footer>
      <button
        type="button"
        class="inline-flex h-9 items-center rounded-md border border-input px-4 text-sm hover:bg-accent"
        @click="deleteTarget = null"
      >
        取消
      </button>
      <button
        type="button"
        :disabled="deleting"
        class="inline-flex h-9 items-center rounded-md bg-destructive px-4 text-sm text-white hover:bg-destructive/90 disabled:opacity-50"
        @click="confirmDelete"
      >
        {{ deleting ? '删除中...' : '删除' }}
      </button>
    </template>
  </AppDialog>

  <AppDialog
    :open="!!assignTarget"
    :title="`资源授权 - ${assignTarget?.name ?? ''}`"
    @close="assignTarget = null"
  >
    <div v-if="assignLoading" class="py-8 text-center text-sm text-muted-foreground">加载中...</div>
    <div v-else class="space-y-1">
      <label
        v-for="item in resourceOptions"
        :key="item.node.id"
        class="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-accent"
        :style="{ paddingLeft: `${item.depth * 16 + 8}px` }"
      >
        <input
          type="checkbox"
          :checked="selectedResourceIds.includes(item.node.id)"
          @change="toggleResource(item.node.id)"
        />
        <span class="font-medium">{{ item.node.name }}</span>
        <span class="font-mono text-xs text-muted-foreground">{{ item.node.code }}</span>
        <span class="ml-auto text-xs text-muted-foreground">{{ item.node.type }}</span>
      </label>
    </div>
    <p
      v-if="assignError"
      class="mt-3 rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive"
    >
      {{ assignError }}
    </p>
    <template #footer>
      <button
        type="button"
        class="inline-flex h-9 items-center rounded-md border border-input px-4 text-sm hover:bg-accent"
        @click="assignTarget = null"
      >
        取消
      </button>
      <button
        type="button"
        :disabled="assignSaving || assignLoading"
        class="inline-flex h-9 items-center rounded-md bg-primary px-4 text-sm text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
        @click="saveAssign"
      >
        {{ assignSaving ? '保存中...' : '保存授权' }}
      </button>
    </template>
  </AppDialog>
</template>

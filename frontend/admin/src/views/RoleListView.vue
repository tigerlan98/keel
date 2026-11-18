<script setup lang="ts">
import { Pencil, Plus, RefreshCw, Search, Shield, Trash2, TriangleAlert } from '@lucide/vue'
import AppButton from '@/components/app/AppButton.vue'
import AppDataTable from '@/components/app/AppDataTable.vue'
import AppErrorState from '@/components/app/AppErrorState.vue'
import AppField from '@/components/app/AppField.vue'
import AppFilterBar from '@/components/app/AppFilterBar.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppPagination from '@/components/app/AppPagination.vue'
import AppPanel from '@/components/app/AppPanel.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import AppConfirm from '@/components/app/AppConfirm.vue'
import PermissionMatrix from '@/components/app/PermissionMatrix.vue'
import PermissionTrace from '@/components/app/PermissionTrace.vue'
import RolePermissionDrawer from '@/components/app/RolePermissionDrawer.vue'
import { usePermissionGraph } from '@/composables/usePermissionGraph'
import { useAuthStore } from '@/stores/auth'

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
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
type RoleView = 'table' | 'matrix' | 'trace'
const view = ref<RoleView>('table')
const views: { value: RoleView; label: string }[] = [
  { value: 'table', label: '列表' },
  { value: 'matrix', label: '矩阵' },
  { value: 'trace', label: '溯源' },
]
const {
  graph: permissionGraph,
  loading: graphLoading,
  error: graphError,
  load: loadGraph,
  invalidate: invalidateGraph,
} = usePermissionGraph()
const matrix = ref<InstanceType<typeof PermissionMatrix> | null>(null)
const matrixSaving = ref(false)
const canAssign = computed(() => auth.hasPermission('iam:role:assign-resource'))
const discardOpen = ref(false)
let discardResolver: ((accepted: boolean) => void) | undefined
function askDiscard(): Promise<boolean> {
  if (discardOpen.value) return Promise.resolve(false)
  discardOpen.value = true
  return new Promise((resolve) => {
    discardResolver = resolve
  })
}
function settleDiscard(accepted: boolean): void {
  discardOpen.value = false
  discardResolver?.(accepted)
  discardResolver = undefined
}
async function requestView(next: RoleView): Promise<void> {
  if (view.value === next) return
  if (matrixSaving.value) {
    toast.show('权限正在保存，请稍候', 'info')
    return
  }
  if (matrix.value?.dirty && !(await askDiscard())) return
  matrix.value?.reset()
  view.value = next
  if (next !== 'trace' && route.query.trace) {
    const query = { ...route.query }
    delete query.trace
    void router.replace({ query })
  }
  if (next !== 'table') await loadGraph()
}
const traceFocus = computed<string | null>({
  get: () => (typeof route.query.trace === 'string' ? route.query.trace : null),
  set: (value) => {
    const query = { ...route.query }
    if (value) query.trace = value
    else delete query.trace
    void router.replace({ query })
  },
})
watch(
  () => route.query.trace,
  (value) => {
    if (typeof value === 'string' && value) void requestView('trace')
  },
  { immediate: true },
)
onBeforeRouteLeave(async () => {
  if (!auth.user) return true
  if (matrixSaving.value) return false
  return !matrix.value?.dirty || (await askDiscard())
})
function warnBeforeUnload(event: InstanceType<typeof globalThis.BeforeUnloadEvent>): void {
  if (matrix.value?.dirty || matrixSaving.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}
onMounted(() => globalThis.addEventListener('beforeunload', warnBeforeUnload))
onUnmounted(() => {
  globalThis.removeEventListener('beforeunload', warnBeforeUnload)
  discardResolver?.(false)
})
async function saveMatrix(changes: { roleId: number; resourceIds: number[] }[]): Promise<void> {
  if (!canAssign.value || matrixSaving.value) return
  matrixSaving.value = true
  let saved = 0
  try {
    for (const change of changes) {
      await replaceRoleResources(change.roleId, change.resourceIds)
      matrix.value?.accept(change)
      saved += 1
      invalidateGraph()
    }
    toast.show(`已更新 ${saved} 个角色的权限`, 'success')
  } catch (caught) {
    toast.show(
      `${saved ? `已保存 ${saved} 个角色，其余改动保留。` : ''}${caught instanceof ApiError ? caught.message : '权限保存失败，请重试'}`,
      'error',
    )
  } finally {
    matrixSaving.value = false
  }
}
function roleUserCount(roleId: number): number | string {
  if (!permissionGraph.value?.usersVisible) return '—'
  return permissionGraph.value.users.filter((user) =>
    (user.roles ?? []).some((role) => role.id === roleId),
  ).length
}
async function refreshRelations(): Promise<void> {
  invalidateGraph()
  if (auth.hasPermission('iam:resource:list')) await loadGraph()
}

const filters = reactive({
  keyword: '',
  status: '' as '' | RoleStatus,
})
const page = ref(1)
const size = ref(10)
const sort = ref('sort,asc')
const items = ref<RoleSummary[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
const columns = [
  { key: 'name', label: '角色', width: '43%' },
  { key: 'description', label: '描述' },
  { key: 'userCount', label: '用户数', align: 'right' as const, width: '90px' },
  { key: 'sort', label: '排序', align: 'right' as const, width: '80px' },
  { key: 'status', label: '状态', width: '96px' },
  { key: 'createdAt', label: '创建时间', width: '168px' },
  { key: 'actions', label: '操作', align: 'right' as const, sticky: true, width: '120px' },
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
const deleteError = ref('')

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
  if (saving.value) return
  formError.value = ''
  if (!form.name.trim()) {
    formError.value = '请输入角色名称'
    return
  }
  if (!editing.value && !/^[A-Z][A-Z0-9_]*$/.test(form.code)) {
    formError.value = '角色编码必须为大写字母、数字和下划线'
    return
  }
  if (!Number.isInteger(form.sort) || form.sort < 0 || form.sort > 2147483647) {
    formError.value = '显示排序必须为 0–2147483647 的整数'
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
      toast.show('角色已新增', 'success')
    }
    dialogOpen.value = false
    await Promise.all([load(), refreshRelations()])
  } catch (caught) {
    formError.value = caught instanceof ApiError ? caught.message : '保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

async function confirmDelete() {
  if (!deleteTarget.value || deleting.value || deleteTarget.value.code === 'SUPER_ADMIN') {
    return
  }
  deleting.value = true
  deleteError.value = ''
  try {
    await deleteRole(deleteTarget.value.id)
    toast.show('角色已删除', 'success')
    deleteTarget.value = null
    if (items.value.length === 1 && page.value > 1) page.value -= 1
    await Promise.all([load(), refreshRelations()])
  } catch (caught) {
    deleteError.value = caught instanceof ApiError ? caught.message : '删除失败，请重试'
  } finally {
    deleting.value = false
  }
}

async function openAssign(role: RoleSummary) {
  if (assignLoading.value) return
  if (!auth.hasPermission('iam:resource:list')) {
    toast.show('查看授权需要资源列表权限', 'error')
    return
  }
  assignTarget.value = role
  assignLoading.value = true
  assignError.value = ''
  selectedResourceIds.value = []
  resourceOptions.value = []
  try {
    const [tree, ids] = await Promise.all([
      fetchResourceTree(),
      role.code === 'SUPER_ADMIN' ? Promise.resolve([]) : fetchRoleResourceIds(role.id),
    ])
    resourceOptions.value = flatten(tree)
    selectedResourceIds.value =
      role.code === 'SUPER_ADMIN' ? resourceOptions.value.map((item) => item.node.id) : ids
  } catch (caught) {
    assignError.value = caught instanceof ApiError ? caught.message : '加载资源失败'
  } finally {
    assignLoading.value = false
  }
}

function requestDelete(role: RoleSummary): void {
  deleteError.value = ''
  deleteTarget.value = role
}

async function saveAssign() {
  if (
    !assignTarget.value ||
    assignTarget.value.code === 'SUPER_ADMIN' ||
    !canAssign.value ||
    assignLoading.value ||
    (assignError.value && !resourceOptions.value.length)
  ) {
    return
  }
  assignSaving.value = true
  assignError.value = ''
  try {
    await replaceRoleResources(assignTarget.value.id, selectedResourceIds.value)
    toast.show('资源授权已更新', 'success')
    assignTarget.value = null
    await refreshRelations()
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
  return status === 'ENABLED' ? '正常' : '停用'
}

function formatDateTime(value: string | null | undefined) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

onMounted(() => {
  void load()
  if (auth.hasPermission('iam:resource:list')) void loadGraph()
})
</script>
<template>
  <div class="role-management">
    <PageHeader
      class="role-page-header"
      title="角色管理"
      description="角色决定用户可见的菜单与可用的按钮权限。"
    >
      <template #actions>
        <div class="role-view-switch" role="group" aria-label="角色视图">
          <button
            v-for="option in views"
            :key="option.value"
            type="button"
            :aria-pressed="view === option.value"
            :class="{ 'is-active': view === option.value }"
            @click="requestView(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
        <AppButton v-permission="'iam:role:create'" @click="openCreate"
          ><Plus :size="16" :stroke-width="1.6" />新增角色</AppButton
        >
      </template>
    </PageHeader>
    <div v-if="view === 'table'" class="mt-5 space-y-4">
      <AppFilterBar class="role-filter" @submit="search">
        <AppField label="关键词" for="role-keyword" class="w-[260px] max-w-full"
          ><AppInput
            id="role-keyword"
            v-model="filters.keyword"
            maxlength="64"
            placeholder="角色名称 / 编码"
            ><template #leading><Search :size="14" :stroke-width="1.6" /></template></AppInput
        ></AppField>
        <AppField label="状态" for="role-status" class="w-[200px] max-w-full"
          ><AppSelect
            id="role-status"
            v-model="filters.status"
            :options="[
              { label: '全部状态', value: '' },
              { label: '正常', value: 'ENABLED' },
              { label: '停用', value: 'DISABLED' },
            ]"
        /></AppField>
        <template #actions
          ><AppButton variant="ghost" :disabled="loading" :icon="RefreshCw" @click="resetFilters"
            >重置</AppButton
          ><AppButton type="submit" :loading="loading" :icon="Search">查询</AppButton></template
        >
      </AppFilterBar>
      <AppPanel class="role-list-panel">
        <template #header
          ><div class="flex items-center gap-3">
            <span class="section-label">角色列表</span
            ><span class="text-[12px] font-semibold text-ink-soft">共 {{ total }} 条</span>
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
            ><RefreshCw :size="16" :stroke-width="1.6" /></AppButton
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
          <template #cell-name="{ row }"
            ><div class="role-cell">
              <div>
                <span>{{ row.name }}</span><span
                  v-if="row.code === 'SUPER_ADMIN'"
                  class="role-builtin"
                  >内置</span
                >
              </div>
              <small>{{ row.code }}</small>
            </div></template
          >
          <template #cell-description="{ row }"
            ><span class="text-[13px] font-medium text-ink-muted">{{
              row.description || '—'
            }}</span></template
          >
          <template #cell-userCount="{ row }"
            ><span
              class="tnum text-[13px]"
              :title="
                permissionGraph?.usersVisible
                  ? '已分配该角色的用户数'
                  : '需要用户及资源列表权限读取完整关系'
              "
              >{{ roleUserCount(row.id) }}</span
            ></template
          >
          <template #cell-sort="{ row }"
            ><span class="tnum text-[12px] text-ink-soft">{{ row.sort }}</span></template
          >
          <template #cell-status="{ row }"
            ><span
              class="role-status"
              :class="row.status === 'ENABLED' ? 'is-enabled' : 'is-disabled'"
              ><i aria-hidden="true" />{{ statusText(row.status) }}</span
            ></template
          >
          <template #cell-createdAt="{ row }"
            ><span class="tnum whitespace-nowrap text-[13px] font-medium text-ink-soft">{{
              formatDateTime(row.createdAt)
            }}</span></template
          >
          <template #cell-actions="{ row }"
            ><span
              v-if="row.code === 'SUPER_ADMIN'"
              class="role-all-permissions"
              title="内置超级管理员拥有全部权限"
              >全部权限</span
            >
            <div v-else class="flex justify-end gap-0.5">
              <AppButton
                v-permission="'iam:role:update'"
                variant="ghost"
                size="icon-sm"
                aria-label="编辑"
                title="编辑"
                @click="openEdit(row)"
                ><Pencil :size="16" :stroke-width="1.6" aria-hidden="true"
              /></AppButton>
              <AppButton
                v-permission="'iam:role:assign-resource'"
                variant="ghost"
                size="icon-sm"
                aria-label="授权"
                :title="row.code === 'SUPER_ADMIN' ? '全部权限（只读）' : '分配权限'"
                @click="openAssign(row)"
                ><Shield :size="16" :stroke-width="1.6" aria-hidden="true"
              /></AppButton>
              <AppButton
                v-permission="'iam:role:delete'"
                variant="ghost"
                size="icon-sm"
                :press-animation="false"
                class="transition-none! hover:bg-destructive/10! hover:text-destructive! focus-visible:text-destructive!"
                :disabled="row.code === 'SUPER_ADMIN'"
                aria-label="删除"
                :title="row.code === 'SUPER_ADMIN' ? '内置角色不可删除' : '删除'"
                @click="requestDelete(row)"
                ><Trash2 :size="16" :stroke-width="1.6" aria-hidden="true"
              /></AppButton></div
          ></template>
        </AppDataTable>
        <template v-if="!error && total > 0" #footer
          ><AppPagination
            appearance="user-list"
            :page="page"
            :page-size="size"
            :total="total"
            :loading="loading"
            @update:page="changePage"
            @update:page-size="changeSize"
        /></template>
      </AppPanel>
    </div>
    <div v-else class="mt-5">
      <div v-if="graphLoading" role="status" class="role-graph-state app-panel">
        <span class="role-loading-orb" />
        <p>正在汇总授权关系…</p>
      </div>
      <AppErrorState v-else-if="graphError" :message="graphError" @retry="loadGraph(true)" />
      <template v-else-if="permissionGraph">
        <PermissionMatrix
          v-if="view === 'matrix'"
          ref="matrix"
          :graph="permissionGraph"
          :editable="canAssign"
          :saving="matrixSaving"
          @save="saveMatrix"
        />
        <PermissionTrace v-else v-model:focus="traceFocus" :graph="permissionGraph" />
      </template>
    </div>
  </div>

  <AppDialog
    :open="dialogOpen"
    :title="editing ? '编辑角色' : '新增角色'"
    initial-focus-id="role-name"
    @close="!saving && (dialogOpen = false)"
  >
    <form id="role-form" class="space-y-4" @submit.prevent="save">
      <AppField label="角色名称" for="role-name" required
        ><AppInput
          id="role-name"
          v-model="form.name"
          maxlength="64"
          placeholder="如：系统管理员"
          :disabled="saving"
      /></AppField>
      <AppField
        label="角色编码"
        for="role-code"
        required
        :hint="editing ? '编码创建后不可修改' : '大写字母开头，可包含数字和下划线'"
        ><AppInput
          id="role-code"
          v-model="form.code"
          maxlength="64"
          :disabled="!!editing || saving"
          placeholder="如 OPS_ADMIN"
      /></AppField>
      <AppField label="显示排序" for="role-form-sort"
        ><input
          id="role-form-sort"
          v-model.number="form.sort"
          type="number"
          min="0"
          max="2147483647"
          :disabled="saving"
          class="app-input h-9 w-full rounded-[10px] border border-line-soft bg-[var(--input-surface)] px-3 text-[13px] outline-none"
      /></AppField>
      <AppField label="备注" for="role-description">
        <textarea
          id="role-description"
          v-model="form.description"
          maxlength="255"
          rows="2"
          :disabled="saving"
          placeholder="角色用途说明"
          class="app-input w-full resize-y rounded-[10px] border border-line-soft bg-[var(--input-surface)] px-3 py-2 text-[13px] outline-none"
        />
      </AppField>
      <AppField label="角色状态" for="role-form-status"
        ><div class="inline-flex items-center gap-2.5">
          <button
            id="role-form-status"
            type="button"
            role="switch"
            aria-label="角色启用状态"
            :aria-checked="form.status === 'ENABLED'"
            :disabled="saving || editing?.code === 'SUPER_ADMIN'"
            class="relative h-[22px] w-[38px] shrink-0 rounded-full transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:opacity-50"
            :class="form.status === 'ENABLED' ? 'bg-brand-bright' : 'bg-ink/20'"
            @click="form.status = form.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'"
          >
            <span
              class="absolute top-0.5 h-[18px] w-[18px] rounded-full bg-white shadow-sm transition-[left]"
              :class="form.status === 'ENABLED' ? 'left-[18px]' : 'left-0.5'"
            /></button
          ><span class="text-[13px] text-ink-muted">{{
            form.status === 'ENABLED' ? '正常' : '停用'
          }}</span>
        </div></AppField
      >
      <p
        v-if="formError"
        role="alert"
        class="rounded-xl border border-destructive/20 bg-destructive/5 px-3 py-2 text-[12px] leading-5 text-destructive"
      >
        {{ formError }}
      </p>
    </form>
    <template #footer
      ><AppButton variant="ghost" :disabled="saving" @click="dialogOpen = false">取消</AppButton
      ><AppButton type="submit" form="role-form" :loading="saving">保存</AppButton></template
    >
  </AppDialog>
  <AppDialog
    :open="!!deleteTarget"
    title="删除角色"
    size="confirm"
    @close="!deleting && (deleteTarget = null)"
  >
    <div class="flex items-start gap-3.5">
      <span
        class="grid h-[38px] w-[38px] shrink-0 place-items-center rounded-xl bg-destructive/10 text-destructive"
        ><TriangleAlert :size="18" :stroke-width="1.6"
      /></span>
      <p class="pt-0.5 text-[13.5px] leading-[1.6] text-ink-muted">
        确认删除角色「{{ deleteTarget?.name }}」吗？删除后无法恢复，已被用户引用的角色不可删除。
      </p>
    </div>
    <p
      v-if="deleteError"
      role="alert"
      class="mt-4 rounded-xl bg-destructive/5 px-3 py-2 text-[12px] text-destructive"
    >
      {{ deleteError }}
    </p>
    <template #footer
      ><AppButton variant="ghost" :disabled="deleting" @click="deleteTarget = null">取消</AppButton
      ><AppButton
        variant="danger"
        :press-animation="false"
        :disabled="deleting"
        class="bg-[#ff5a50]! hover:bg-[#f14c42]! shadow-[0_1px_2px_rgba(150,40,34,.26),0_8px_18px_rgba(255,90,80,.24)]"
        @click="confirmDelete"
        >{{ deleting ? '删除中…' : '删除' }}</AppButton
      ></template
    >
  </AppDialog>
  <RolePermissionDrawer
    v-model="selectedResourceIds"
    :role="assignTarget"
    :options="resourceOptions"
    :loading="assignLoading"
    :saving="assignSaving"
    :error="assignError"
    @close="assignTarget = null"
    @save="saveAssign"
    @retry="assignTarget && openAssign(assignTarget)"
  />
  <AppConfirm
    :open="discardOpen"
    title="放弃未保存的改动"
    message="权限矩阵中还有未保存的改动，离开将放弃这些改动。"
    confirm-text="放弃改动"
    @close="settleDiscard(false)"
    @confirm="settleDiscard(true)"
  />
</template>

<style scoped>
.role-management {
  font-size: 13px;
  line-height: 1.5;
  -webkit-font-smoothing: antialiased;
}
.role-view-switch {
  display: inline-flex;
  gap: 2px;
  padding: 3px;
  border: 1px solid var(--glass-edge);
  border-radius: 13px;
  background: var(--hover-surface);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.4);
}
.role-view-switch button {
  height: 28px;
  padding: 0 12px;
  border-radius: 9px;
  font-size: 12px;
  font-weight: 600;
  color: var(--ink-soft);
  transition:
    background 0.2s,
    color 0.2s,
    box-shadow 0.2s;
}
.role-view-switch button.is-active {
  background: var(--input-surface);
  color: var(--ink);
  box-shadow: var(--shadow-e1), var(--inset-edge);
}
.role-view-switch button:focus-visible {
  outline: 2px solid var(--brand);
  outline-offset: 3px;
}
.role-cell > div {
  display: flex;
  align-items: center;
  gap: 7px;
  font-weight: 620;
  font-size: 13px;
}
.role-cell small {
  display: block;
  margin-top: 1px;
  color: var(--ink-soft);
  font-size: 12px;
  font-weight: 500;
}
.role-builtin {
  font-size: 11px;
  font-weight: 550;
  color: var(--ink-muted);
  padding: 1px 8px;
  background: var(--hover-surface);
  border-radius: 999px;
}
.role-graph-state {
  height: calc(100dvh - 210px);
  min-height: 500px;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 20px;
  color: var(--ink-soft);
  font-size: 12px;
}
.role-loading-orb {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 20%, white, #e5eceb 45%, #c6d8cd);
  box-shadow: 0 10px 30px rgba(46, 58, 62, 0.1);
  animation: role-loading 1.6s ease-in-out infinite;
}
@keyframes role-loading {
  50% {
    opacity: 0.55;
    translate: 0 -5px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .role-view-switch button {
    transition: none;
  }
  .role-loading-orb {
    animation: none;
  }
}
.role-management :deep(.section-label) {
  font-size: 15px;
  font-weight: 650;
}
.role-management :deep(.data-table th) {
  font-size: 12px;
  font-weight: 620;
}
.role-management :deep(.data-table td) {
  font-size: 13px;
}
.role-page-header :deep(p) {
  font-weight: 550;
  line-height: 1.6;
}
.role-filter :deep(> div:first-child) {
  gap: 16px;
}
.role-list-panel :deep(header) {
  min-height: 60px;
  padding: 14px 20px;
}
.role-list-panel :deep(footer) {
  min-height: 64px;
  padding: 12px 20px;
}
.role-list-panel :deep(.section-label::before) {
  width: 7px;
  height: 7px;
  box-shadow: 0 0 0 4px rgba(130, 221, 45, 0.14);
}
.role-list-panel :deep(.data-table th) {
  height: 40px;
  padding: 0 16px;
  white-space: nowrap;
  color: var(--ink-soft);
}
.role-list-panel :deep(.data-table td) {
  height: 48px;
  padding: 0 16px;
}
.role-list-panel :deep(.data-table .col-actions) {
  box-shadow: inset 1px 0 0 var(--surface-edge);
}
.role-list-panel :deep(.data-table td:nth-child(2)) {
  min-width: 180px;
}
.role-all-permissions {
  font-size: 12px;
  font-weight: 550;
  color: var(--ink-soft);
  white-space: nowrap;
}
.role-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
  white-space: nowrap;
}
.role-status i {
  width: 6px;
  height: 6px;
  flex: none;
  border-radius: 50%;
}
.role-status.is-enabled {
  background: rgba(130, 221, 45, 0.14);
  color: #58a021;
}
.role-status.is-enabled i {
  background: var(--brand-bright);
  box-shadow: 0 0 0 3px rgba(130, 221, 45, 0.14);
}
.role-status.is-disabled {
  background: rgba(255, 90, 80, 0.11);
  color: #e0443b;
}
.role-status.is-disabled i {
  background: #ff5a50;
}
:global(.dark) .role-status.is-enabled {
  color: #9be35a;
}
@media (max-width: 640px) {
  .role-view-switch button {
    padding-inline: 10px;
  }
  .role-management :deep(.page-header) {
    gap: 16px;
  }
  .role-management :deep(.app-filter-bar) {
    gap: 12px;
  }
}
</style>

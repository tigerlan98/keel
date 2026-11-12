<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

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
import type {
  ResourceNode,
  RoleDetail,
  RoleStatus,
  RoleSummary,
} from '@/api/types'
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
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

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

function statusClass(status: RoleStatus) {
  return status === 'ENABLED'
    ? 'bg-emerald-50 text-emerald-700'
    : 'bg-destructive/10 text-destructive'
}

function formatDateTime(value: string | null | undefined) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

onMounted(() => {
  void load()
})
</script>

<template>
  <PageHeader title="角色管理" description="维护角色、启停状态与资源授权。" />

  <div class="mt-6 rounded-xl border bg-card">
    <form class="flex flex-wrap items-end gap-3 border-b p-4" @submit.prevent="search">
      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="role-keyword">关键字</label>
        <input
          id="role-keyword"
          v-model="filters.keyword"
          type="text"
          maxlength="64"
          placeholder="角色名/编码前缀"
          class="flex h-9 w-52 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none focus-visible:border-ring"
        />
      </div>

      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="role-status">状态</label>
        <select
          id="role-status"
          v-model="filters.status"
          class="flex h-9 w-32 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none focus-visible:border-ring"
        >
          <option value="">全部</option>
          <option value="ENABLED">启用</option>
          <option value="DISABLED">禁用</option>
        </select>
      </div>

      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="role-sort">排序</label>
        <select
          id="role-sort"
          v-model="sort"
          class="flex h-9 w-40 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none focus-visible:border-ring"
        >
          <option value="sort,asc">排序 ↑</option>
          <option value="sort,desc">排序 ↓</option>
          <option value="createdAt,desc">创建时间 ↓</option>
          <option value="code,asc">编码 ↑</option>
        </select>
      </div>

      <button
        type="submit"
        class="inline-flex h-9 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
        :disabled="loading"
      >
        查询
      </button>
      <button
        type="button"
        class="inline-flex h-9 items-center justify-center rounded-md border border-input px-4 text-sm font-medium hover:bg-accent disabled:opacity-50"
        :disabled="loading"
        @click="resetFilters"
      >
        重置
      </button>
      <button
        v-permission="'iam:role:create'"
        type="button"
        class="ml-auto inline-flex h-9 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        @click="openCreate"
      >
        新增角色
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
      <table class="w-full min-w-[860px] text-sm">
        <thead class="border-b bg-muted/40 text-left text-xs text-muted-foreground">
          <tr>
            <th class="px-4 py-3 font-medium">编码</th>
            <th class="px-4 py-3 font-medium">名称</th>
            <th class="px-4 py-3 font-medium">描述</th>
            <th class="px-4 py-3 font-medium">状态</th>
            <th class="px-4 py-3 font-medium">排序</th>
            <th class="px-4 py-3 font-medium">创建时间</th>
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
            <tr v-for="role in items" :key="role.id" class="border-b last:border-0">
              <td class="px-4 py-3 font-mono text-xs">{{ role.code }}</td>
              <td class="px-4 py-3 font-medium">{{ role.name }}</td>
              <td class="max-w-64 truncate px-4 py-3 text-muted-foreground">
                {{ role.description || '-' }}
              </td>
              <td class="px-4 py-3">
                <span
                  class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                  :class="statusClass(role.status)"
                >
                  {{ statusText(role.status) }}
                </span>
              </td>
              <td class="px-4 py-3 text-muted-foreground">{{ role.sort }}</td>
              <td class="px-4 py-3 text-muted-foreground">{{ formatDateTime(role.createdAt) }}</td>
              <td class="px-4 py-3">
                <div class="flex justify-end gap-3 text-primary">
                  <button
                    v-permission="'iam:role:update'"
                    type="button"
                    class="hover:underline"
                    @click="openEdit(role)"
                  >
                    编辑
                  </button>
                  <button
                    v-permission="'iam:role:assign-resource'"
                    type="button"
                    class="hover:underline"
                    @click="openAssign(role)"
                  >
                    授权
                  </button>
                  <button
                    v-permission="'iam:role:delete'"
                    type="button"
                    class="text-destructive hover:underline"
                    @click="deleteTarget = role"
                  >
                    删除
                  </button>
                </div>
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
            class="h-8 rounded-md border border-input bg-transparent px-2 text-sm outline-none"
            @change="changeSize"
          >
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
          </select>
        </label>
        <button
          type="button"
          class="inline-flex h-8 items-center rounded-md border border-input px-3 text-sm disabled:opacity-40"
          :disabled="page <= 1 || loading"
          @click="previousPage"
        >
          上一页
        </button>
        <span class="min-w-16 text-center text-muted-foreground">{{ page }} / {{ totalPages }}</span>
        <button
          type="button"
          class="inline-flex h-8 items-center rounded-md border border-input px-3 text-sm disabled:opacity-40"
          :disabled="page >= totalPages || loading"
          @click="nextPage"
        >
          下一页
        </button>
      </div>
    </div>
  </div>

  <AppDialog :open="dialogOpen" :title="editing ? '编辑角色' : '新增角色'" @close="dialogOpen = false">
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
          <select
            id="role-form-status"
            v-model="form.status"
            class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm outline-none focus-visible:border-ring"
          >
            <option value="ENABLED">启用</option>
            <option value="DISABLED">禁用</option>
          </select>
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

  <AppDialog
    :open="!!deleteTarget"
    title="删除角色"
    @close="deleteTarget = null"
  >
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
    <p v-if="assignError" class="mt-3 rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">
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

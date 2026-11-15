<script setup lang="ts">
import {
  Ban,
  ChevronRight,
  ChevronsDown,
  ChevronsUp,
  CircleCheck,
  Pencil,
  RefreshCw,
  Search,
} from '@lucide/vue'
import AppButton from '@/components/app/AppButton.vue'
import AppDataTable from '@/components/app/AppDataTable.vue'
import AppErrorState from '@/components/app/AppErrorState.vue'
import AppField from '@/components/app/AppField.vue'
import AppFilterBar from '@/components/app/AppFilterBar.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import AppPanel from '@/components/app/AppPanel.vue'
import AppStatusTag from '@/components/app/AppStatusTag.vue'
import { menuIcon } from '@/lib/menus'
import { computed, onMounted, reactive, ref } from 'vue'

import { ApiError } from '@/api/http'
import {
  fetchResource,
  fetchResourceTree,
  updateResource,
  updateResourceStatus,
} from '@/api/resources'
import type { ResourceNode, ResourceStatus, ResourceType } from '@/api/types'
import AppDialog from '@/components/app/AppDialog.vue'
import PageHeader from '@/components/app/PageHeader.vue'
import { useToastStore } from '@/stores/toast'

interface FlatResource {
  node: ResourceNode
  depth: number
}

const toast = useToastStore()

const nodes = ref<ResourceNode[]>([])
const keyword = ref('')
const collapsedIds = ref(new Set<number>())
const columns = [
  { key: 'name', label: '菜单名称' },
  { key: 'type', label: '类型' },
  { key: 'code', label: '权限标识' },
  { key: 'path', label: '路由' },
  { key: 'sort', label: '排序' },
  { key: 'visible', label: '显示' },
  { key: 'status', label: '状态' },
  { key: 'actions', label: '操作', align: 'right' as const, sticky: true },
]
function matches(node: ResourceNode): boolean {
  const query = keyword.value.trim().toLocaleLowerCase()
  return (
    !query ||
    `${node.name} ${node.code}`.toLocaleLowerCase().includes(query) ||
    (node.children ?? []).some(matches)
  )
}
const treeRows = computed(() => {
  const rows: (ResourceNode & { depth: number })[] = []
  function visit(list: ResourceNode[], depth: number): void {
    for (const node of list) {
      if (!matches(node)) continue
      rows.push({ ...node, depth })
      if (keyword.value.trim() || !collapsedIds.value.has(node.id))
        visit(node.children ?? [], depth + 1)
    }
  }
  visit(nodes.value, 0)
  return rows
})
function toggleBranch(id: number): void {
  const next = new Set(collapsedIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  collapsedIds.value = next
}
const loading = ref(false)
const error = ref('')
const flatResources = computed(() => flatten(nodes.value))

const dialogOpen = ref(false)
const saving = ref(false)
const formError = ref('')
const editingId = ref<number | null>(null)
const editingType = ref<ResourceType>('MENU')
const form = reactive({
  name: '',
  parentId: 0,
  sort: 0,
  icon: '',
  visible: true,
  version: 0,
})

const parentOptions = computed(() => {
  const excluded = new Set<number>()
  if (editingId.value !== null) {
    excluded.add(editingId.value)
    collectIds(findNode(nodes.value, editingId.value), excluded)
  }
  return flatResources.value.filter(
    (item) =>
      (item.node.type === 'DIRECTORY' || item.node.type === 'MENU') && !excluded.has(item.node.id),
  )
})

const parentSelectOptions = computed(() => [
  ...(editingType.value === 'DIRECTORY' || editingType.value === 'MENU'
    ? [{ label: '根节点', value: 0 }]
    : []),
  ...parentOptions.value.map((option) => ({
    label: option.node.name + '（' + option.node.code + '）',
    value: option.node.id,
    depth: option.depth,
  })),
])

async function load() {
  loading.value = true
  error.value = ''
  try {
    nodes.value = await fetchResourceTree()
  } catch (caught) {
    nodes.value = []
    error.value = caught instanceof ApiError ? caught.message : '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function openEdit(node: ResourceNode) {
  formError.value = ''
  try {
    const detail = await fetchResource(node.id)
    editingId.value = node.id
    editingType.value = detail.type
    form.name = detail.name
    form.parentId = detail.parentId
    form.sort = detail.sort
    form.icon = detail.icon ?? ''
    form.visible = detail.visible
    form.version = detail.version
    dialogOpen.value = true
  } catch (caught) {
    toast.show(caught instanceof ApiError ? caught.message : '加载资源失败', 'error')
  }
}

async function save() {
  if (editingId.value === null) {
    return
  }
  if (!form.name.trim()) {
    formError.value = '请输入资源名称'
    return
  }

  saving.value = true
  formError.value = ''
  try {
    await updateResource(editingId.value, {
      name: form.name.trim(),
      parentId: form.parentId,
      sort: form.sort,
      icon: form.icon.trim() || null,
      visible: form.visible,
      version: form.version,
    })
    toast.show('资源已更新', 'success')
    dialogOpen.value = false
    await load()
  } catch (caught) {
    formError.value = caught instanceof ApiError ? caught.message : '保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

async function toggleStatus(node: ResourceNode) {
  const next: ResourceStatus = node.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    const detail = await fetchResource(node.id)
    await updateResourceStatus(node.id, next, detail.version)
    toast.show(next === 'ENABLED' ? '资源已启用' : '资源已禁用', 'success')
    await load()
  } catch (caught) {
    toast.show(caught instanceof ApiError ? caught.message : '状态更新失败', 'error')
  }
}

function flatten(list: ResourceNode[], depth = 0): FlatResource[] {
  const result: FlatResource[] = []
  for (const node of list) {
    result.push({ node, depth })
    result.push(...flatten(node.children ?? [], depth + 1))
  }
  return result
}

function findNode(list: ResourceNode[], id: number): ResourceNode | null {
  for (const node of list) {
    if (node.id === id) {
      return node
    }
    const found = findNode(node.children ?? [], id)
    if (found) {
      return found
    }
  }
  return null
}

function collectIds(node: ResourceNode | null, target: Set<number>) {
  if (!node) {
    return
  }
  for (const child of node.children ?? []) {
    target.add(child.id)
    collectIds(child, target)
  }
}

function typeLabel(type: ResourceType) {
  return { DIRECTORY: '目录', MENU: '菜单', BUTTON: '按钮', API: '接口' }[type]
}

function typeClass(type: ResourceType) {
  if (type === 'DIRECTORY') {
    return 'bg-sky-50 text-sky-700'
  }
  if (type === 'MENU') {
    return 'bg-violet-50 text-violet-700'
  }
  if (type === 'BUTTON') {
    return 'bg-amber-50 text-amber-700'
  }
  return 'bg-muted text-muted-foreground'
}

onMounted(() => {
  void load()
})
</script>

<template>
  <PageHeader title="菜单管理" description="管理目录、菜单与按钮，配置展示顺序、图标和启停状态。" />
  <div class="mt-5 space-y-4">
    <AppFilterBar @submit="keyword = keyword.trim()">
      <AppField label="关键字" for="menu-keyword" class="w-[240px]"
        ><AppInput
          id="menu-keyword"
          v-model="keyword"
          placeholder="菜单名称 / 权限标识"
          maxlength="64"
      /></AppField>
      <template #actions
        ><AppButton type="submit" :icon="Search">查询</AppButton
        ><AppButton variant="outline" :icon="RefreshCw" @click="keyword = ''"
          >重置</AppButton
        ></template
      >
    </AppFilterBar>
    <AppPanel>
      <template #header
        ><div class="flex items-center gap-2.5">
          <span class="section-label">菜单列表</span
          ><span class="text-[11.5px] text-ink-soft">共 {{ flatResources.length }} 项</span>
        </div></template
      >
      <template #tools>
        <AppButton
          variant="outline"
          size="icon-sm"
          aria-label="全部展开"
          title="全部展开"
          @click="collapsedIds = new Set()"
          ><ChevronsDown :size="14"
        /></AppButton>
        <AppButton
          variant="outline"
          size="icon-sm"
          aria-label="全部收起"
          title="全部收起"
          @click="
            collapsedIds = new Set(
              flatResources
                .filter((item) => item.node.children?.length)
                .map((item) => item.node.id),
            )
          "
          ><ChevronsUp :size="14"
        /></AppButton>
        <AppButton
          variant="outline"
          size="icon-sm"
          :loading="loading"
          aria-label="刷新"
          title="刷新"
          @click="load"
          ><RefreshCw :size="14"
        /></AppButton>
      </template>
      <AppErrorState v-if="error" :message="error" @retry="load" />
      <AppDataTable
        v-else
        :columns="columns"
        :rows="treeRows"
        :animation-key="nodes.map((node) => node.id).join(',')"
        :loading="loading"
        :min-width="1040"
        empty-title="暂无菜单"
        empty-description="请调整查询条件。"
      >
        <template #cell-name="{ row }"
          ><div class="flex items-center gap-2" :style="{ paddingLeft: `${row.depth * 20}px` }">
            <button
              v-if="row.children?.length"
              type="button"
              class="grid h-6 w-6 shrink-0 place-items-center rounded-md text-ink-soft transition-colors hover:bg-ink/5 focus-visible:ring-2 focus-visible:ring-brand/30"
              :aria-label="`${collapsedIds.has(row.id) ? '展开' : '收起'}${row.name}`"
              :aria-expanded="!!keyword || !collapsedIds.has(row.id)"
              @click="toggleBranch(row.id)"
            >
              <ChevronRight
                :size="14"
                class="transition-transform duration-200"
                :class="(!collapsedIds.has(row.id) || !!keyword) && 'rotate-90'"
              /></button
            ><span v-else class="w-6 shrink-0"></span>
            <component :is="menuIcon(row.icon)" :size="16" class="shrink-0 text-ink-soft" /><span
              class="font-semibold text-ink"
              >{{ row.name }}</span
            >
          </div></template
        >
        <template #cell-type="{ row }"
          ><span
            class="rounded-md px-2 py-1 text-[11px] font-medium"
            :class="typeClass(row.type)"
            >{{ typeLabel(row.type) }}</span
          ></template
        >
        <template #cell-code="{ row }"
          ><span class="code-chip">{{ row.code }}</span></template
        >
        <template #cell-path="{ row }"
          ><span class="text-[12px] text-ink-muted">{{ row.path || '—' }}</span></template
        >
        <template #cell-visible="{ row }"
          ><span class="text-[12px] text-ink-soft">{{
            row.visible ? '显示' : '隐藏'
          }}</span></template
        >
        <template #cell-status="{ row }"
          ><AppStatusTag
            :tone="row.status === 'ENABLED' ? 'success' : 'danger'"
            :label="row.status === 'ENABLED' ? '启用' : '禁用'"
        /></template>
        <template #cell-actions="{ row }"
          ><div class="flex justify-end gap-0.5">
            <AppButton
              v-permission="'iam:resource:update'"
              variant="ghost"
              size="icon-sm"
              aria-label="编辑"
              title="编辑"
              @click="openEdit(row)"
              ><Pencil :size="14" /></AppButton
            ><AppButton
              v-permission="'iam:resource:status'"
              :variant="row.status === 'ENABLED' ? 'danger-ghost' : 'ghost'"
              size="icon-sm"
              :aria-label="row.status === 'ENABLED' ? '禁用' : '启用'"
              :title="row.status === 'ENABLED' ? '禁用' : '启用'"
              @click="toggleStatus(row)"
              ><Ban v-if="row.status === 'ENABLED'" :size="14" /><CircleCheck v-else :size="14"
            /></AppButton></div
        ></template>
      </AppDataTable>
    </AppPanel>
  </div>
  <AppDialog :open="dialogOpen" title="编辑资源" @close="dialogOpen = false">
    <form id="resource-form" class="space-y-4" @submit.prevent="save">
      <div class="space-y-1">
        <label class="text-sm font-medium" for="resource-name">名称</label>
        <input
          id="resource-name"
          v-model="form.name"
          type="text"
          maxlength="64"
          class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm outline-none focus-visible:border-ring"
        />
      </div>

      <div class="space-y-1">
        <label class="text-sm font-medium" for="resource-parent">父级</label>
        <AppSelect id="resource-parent" v-model="form.parentId" :options="parentSelectOptions" />
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div class="space-y-1">
          <label class="text-sm font-medium" for="resource-sort">排序</label>
          <input
            id="resource-sort"
            v-model.number="form.sort"
            type="number"
            min="0"
            class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm outline-none focus-visible:border-ring"
          />
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium" for="resource-icon">图标</label>
          <input
            id="resource-icon"
            v-model="form.icon"
            type="text"
            maxlength="64"
            placeholder="如 users"
            class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm outline-none focus-visible:border-ring"
          />
        </div>
      </div>

      <label class="flex items-center gap-2 text-sm">
        <input v-model="form.visible" type="checkbox" />
        在菜单中显示
      </label>

      <p class="text-xs text-muted-foreground">
        类型、权限码、路径与前端组件由迁移维护，不在此处修改。
      </p>

      <p v-if="formError" class="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">
        {{ formError }}
      </p>
    </form>
    <template #footer>
      <AppButton type="button" variant="outline" @click="dialogOpen = false"> 取消 </AppButton>
      <AppButton type="submit" form="resource-form" :disabled="saving" variant="primary">
        {{ saving ? '保存中...' : '保存' }}
      </AppButton>
    </template>
  </AppDialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { ApiError } from '@/api/http'
import { fetchResource, fetchResourceTree, updateResource, updateResourceStatus } from '@/api/resources'
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
      (item.node.type === 'DIRECTORY' || item.node.type === 'MENU') &&
      !excluded.has(item.node.id),
  )
})

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
  <PageHeader
    title="资源管理"
    description="资源由 Flyway 迁移维护；后台只更新展示属性与启停状态。"
  />

  <div class="mt-6 rounded-xl border bg-card">
    <div
      v-if="error"
      class="flex items-center justify-between gap-3 border-b bg-destructive/5 px-4 py-3 text-sm text-destructive"
    >
      <span>{{ error }}</span>
      <button type="button" class="shrink-0 underline-offset-4 hover:underline" @click="load">
        重试
      </button>
    </div>

    <div v-if="loading" class="px-4 py-10 text-center text-sm text-muted-foreground">加载中...</div>
    <div v-else-if="flatResources.length === 0" class="px-4 py-10 text-center text-sm text-muted-foreground">
      暂无数据
    </div>
    <div v-else class="divide-y">
      <div
        v-for="item in flatResources"
        :key="item.node.id"
        class="flex flex-wrap items-center gap-3 px-4 py-3 text-sm hover:bg-muted/40"
        :style="{ paddingLeft: `${item.depth * 20 + 16}px` }"
      >
        <span class="rounded px-2 py-0.5 text-xs font-medium" :class="typeClass(item.node.type)">
          {{ typeLabel(item.node.type) }}
        </span>
        <span class="font-medium">{{ item.node.name }}</span>
        <span class="font-mono text-xs text-muted-foreground">{{ item.node.code }}</span>
        <span v-if="item.node.path" class="text-xs text-muted-foreground">{{ item.node.path }}</span>
        <span
          class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
          :class="
            item.node.status === 'ENABLED'
              ? 'bg-emerald-50 text-emerald-700'
              : 'bg-destructive/10 text-destructive'
          "
        >
          {{ item.node.status === 'ENABLED' ? '启用' : '禁用' }}
        </span>
        <span v-if="!item.node.visible" class="text-xs text-muted-foreground">隐藏</span>
        <span class="ml-auto text-xs text-muted-foreground">排序 {{ item.node.sort }}</span>
        <div class="flex gap-3">
          <button
            v-permission="'iam:resource:update'"
            type="button"
            class="text-primary hover:underline"
            @click="openEdit(item.node)"
          >
            编辑
          </button>
          <button
            v-permission="'iam:resource:status'"
            type="button"
            class="hover:underline"
            :class="item.node.status === 'ENABLED' ? 'text-destructive' : 'text-primary'"
            @click="toggleStatus(item.node)"
          >
            {{ item.node.status === 'ENABLED' ? '禁用' : '启用' }}
          </button>
        </div>
      </div>
    </div>
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
        <select
          id="resource-parent"
          v-model.number="form.parentId"
          class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm outline-none focus-visible:border-ring"
        >
          <option v-if="editingType === 'DIRECTORY' || editingType === 'MENU'" :value="0">
            根节点
          </option>
          <option v-for="option in parentOptions" :key="option.node.id" :value="option.node.id">
            {{ '  '.repeat(option.depth) }}{{ option.node.name }}（{{ option.node.code }}）
          </option>
        </select>
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
      <button
        type="button"
        class="inline-flex h-9 items-center rounded-md border border-input px-4 text-sm hover:bg-accent"
        @click="dialogOpen = false"
      >
        取消
      </button>
      <button
        type="submit"
        form="resource-form"
        :disabled="saving"
        class="inline-flex h-9 items-center rounded-md bg-primary px-4 text-sm text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
      >
        {{ saving ? '保存中...' : '保存' }}
      </button>
    </template>
  </AppDialog>
</template>

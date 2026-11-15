<script setup lang="ts">
import {
  Ban,
  ChevronRight,
  ChevronsDown,
  ChevronsUp,
  CircleCheck,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  Trash2,
} from '@lucide/vue'
import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import { computed, onMounted, ref } from 'vue'
import { z } from 'zod'
import {
  createDepartment,
  updateDepartment,
  deleteDepartment,
  changeDepartmentStatus,
  fetchDepartmentTree,
  fetchDepartment,
  type DepartmentNode,
  type DepartmentDetail,
  type DepartmentOption,
} from '@/api/departments'
import { ApiError } from '@/api/http'
import { departmentSubtreeIds } from '@/lib/departments'
import { useToastStore } from '@/stores/toast'
import AppButton from '@/components/app/AppButton.vue'
import AppConfirm from '@/components/app/AppConfirm.vue'
import AppDataTable from '@/components/app/AppDataTable.vue'
import AppDialog from '@/components/app/AppDialog.vue'
import AppErrorState from '@/components/app/AppErrorState.vue'
import AppField from '@/components/app/AppField.vue'
import AppFilterBar from '@/components/app/AppFilterBar.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppPanel from '@/components/app/AppPanel.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import AppStatusTag from '@/components/app/AppStatusTag.vue'
import DepartmentSelect from '@/components/app/DepartmentSelect.vue'
import PageHeader from '@/components/app/PageHeader.vue'

const toast = useToastStore()
const nodes = ref<DepartmentNode[]>([]),
  loading = ref(false),
  error = ref('')
const keyword = ref(''),
  appliedKeyword = ref(''),
  filterStatus = ref(''),
  appliedStatus = ref('')
const collapsed = ref(new Set<number>())
function asOptions(list: DepartmentNode[], enabled = true): DepartmentOption[] {
  return list.map((node) => ({
    id: node.id,
    parentId: node.parentId,
    name: node.name,
    status: node.status,
    selectable: enabled && node.status === 'ENABLED',
    children: asOptions(node.children, enabled && node.status === 'ENABLED'),
  }))
}
const options = computed(() => asOptions(nodes.value))
type Row = DepartmentNode & { depth: number; context?: boolean }
const allRows = computed(() => {
  const visit = (list: DepartmentNode[], depth = 0): Row[] =>
    list.flatMap((node) => [{ ...node, depth }, ...visit(node.children, depth + 1)])
  return visit(nodes.value)
})
const rows = computed(() => {
  const term = appliedKeyword.value.toLowerCase(),
    status = appliedStatus.value
  if (term || status) {
    const included = new Set<number>()
    const matches = new Set<number>()
    for (const row of allRows.value.filter(
      (row) =>
        (!term || row.name.toLowerCase().includes(term) || row.code.toLowerCase().includes(term)) &&
        (!status || row.status === status),
    )) {
      matches.add(row.id)
      let item: Row | undefined = row
      while (item) {
        included.add(item.id)
        item = allRows.value.find((parent) => parent.id === item?.parentId)
      }
    }
    return allRows.value
      .filter((row) => included.has(row.id))
      .map((row) => ({ ...row, context: !matches.has(row.id) }))
  }
  return allRows.value.filter((row) => {
    let parent = allRows.value.find((item) => item.id === row.parentId)
    while (parent) {
      if (collapsed.value.has(parent.id)) return false
      parent = allRows.value.find((item) => item.id === parent?.parentId)
    }
    return true
  })
})
const columns = [
  { key: 'name', label: '部门名称' },
  { key: 'code', label: '部门编码' },
  { key: 'sort', label: '排序' },
  { key: 'status', label: '状态' },
  { key: 'directUserCount', label: '直属用户数' },
  { key: 'actions', label: '操作', align: 'right' as const, sticky: true, width: '176px' },
]
let generation = 0
async function load() {
  const version = ++generation
  loading.value = true
  error.value = ''
  try {
    const data = await fetchDepartmentTree()
    if (version === generation) nodes.value = data
  } catch (e) {
    if (version === generation) error.value = e instanceof ApiError ? e.message : '部门加载失败'
  } finally {
    if (version === generation) loading.value = false
  }
}
function search() {
  appliedKeyword.value = keyword.value.trim()
  appliedStatus.value = filterStatus.value
}
function reset() {
  keyword.value = ''
  filterStatus.value = ''
  search()
}
function toggle(id: number) {
  const next = new Set(collapsed.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  collapsed.value = next
}
const open = ref(false),
  editing = ref<DepartmentDetail | null>(null),
  parentId = ref<number>(),
  saving = ref(false),
  formError = ref('')
const schema = z.object({
  code: z
    .string()
    .trim()
    .regex(/^[A-Z][A-Z0-9_-]{1,63}$/, '编码需2–64位大写字母、数字、下划线或连字符，以字母开头'),
  name: z.string().trim().min(1, '请输入部门名称').max(64, '部门名称最多64个字符'),
  sort: z.string().regex(/^\d{1,4}$/, '排序需为0–9999的整数'),
  status: z.enum(['ENABLED', 'DISABLED']),
})
const form = useForm({
  validationSchema: toTypedSchema(schema),
  initialValues: { code: '', name: '', sort: '0', status: 'ENABLED' as 'ENABLED' | 'DISABLED' },
})
const { errors, defineField, handleSubmit, resetForm } = form
const [code, codeAttrs] = defineField('code'),
  [name, nameAttrs] = defineField('name'),
  [sort, sortAttrs] = defineField('sort'),
  [status] = defineField('status')
const excluded = computed(() =>
  editing.value ? departmentSubtreeIds(options.value, editing.value.id) : [],
)
const root = computed(() => editing.value?.code === 'ROOT')
function openCreate(parent?: number) {
  editing.value = null
  parentId.value = parent ?? nodes.value[0]?.id
  resetForm({ values: { code: '', name: '', sort: '0', status: 'ENABLED' } })
  formError.value = ''
  open.value = true
}
async function openEdit(row: DepartmentNode) {
  try {
    const detail = await fetchDepartment(row.id)
    editing.value = detail
    parentId.value = detail.parentId
    resetForm({
      values: {
        code: detail.code,
        name: detail.name,
        sort: String(detail.sort),
        status: detail.status,
      },
    })
    formError.value = ''
    open.value = true
  } catch (e) {
    toast.show(e instanceof ApiError ? e.message : '部门详情加载失败', 'error')
  }
}
const save = handleSubmit(async (values) => {
  if (parentId.value === undefined) {
    formError.value = '请选择上级部门'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    if (editing.value)
      await updateDepartment(editing.value.id, {
        parentId: parentId.value,
        name: values.name,
        sort: Number(values.sort),
        version: editing.value.version,
      })
    else
      await createDepartment({
        parentId: parentId.value,
        code: values.code,
        name: values.name,
        sort: Number(values.sort),
        status: values.status,
      })
    toast.show(editing.value ? '部门已更新' : '部门已创建', 'success')
    open.value = false
    await load()
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : '保存失败，请重试'
  } finally {
    saving.value = false
  }
})
const confirm = ref<{ row: DepartmentNode; action: 'delete' | 'status' }>(),
  confirmLoading = ref(false),
  confirmError = ref('')
function requestConfirm(row: DepartmentNode, action: 'delete' | 'status') {
  confirm.value = { row, action }
  confirmError.value = ''
}
async function execute() {
  if (!confirm.value) return
  confirmLoading.value = true
  confirmError.value = ''
  try {
    const { row, action } = confirm.value
    if (action === 'delete') await deleteDepartment(row.id, row.version)
    else
      await changeDepartmentStatus(
        row.id,
        row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED',
        row.version,
      )
    toast.show(action === 'delete' ? '部门已删除' : '部门状态已更新', 'success')
    confirm.value = undefined
    await load()
  } catch (e) {
    confirmError.value = e instanceof ApiError ? e.message : '操作失败，请重试'
  } finally {
    confirmLoading.value = false
  }
}
onMounted(() => void load())
</script>
<template>
  <PageHeader title="部门管理" description="维护组织层级与部门状态，用户通过部门归属进行筛选。"
    ><template #actions
      ><AppButton
        v-permission="'iam:department:create'"
        :disabled="loading || !nodes.length"
        :icon="Plus"
        @click="openCreate()"
        >新增部门</AppButton
      ></template
    ></PageHeader
  >
  <div class="mt-5 space-y-4">
    <AppFilterBar @submit="search"
      ><AppField label="关键字" for="department-keyword" class="w-[260px]"
        ><AppInput
          id="department-keyword"
          v-model="keyword"
          placeholder="部门名称 / 编码"
          maxlength="64" /></AppField
      ><AppField label="状态" for="department-status" class="w-[150px]"
        ><AppSelect
          id="department-status"
          v-model="filterStatus"
          :options="[
            { label: '全部状态', value: '' },
            { label: '启用', value: 'ENABLED' },
            { label: '停用', value: 'DISABLED' },
          ]" /></AppField
      ><template #actions
        ><AppButton :icon="RefreshCw" variant="ghost" @click="reset">重置</AppButton
        ><AppButton type="submit" :icon="Search">查询</AppButton></template
      ></AppFilterBar
    >
    <AppPanel
      ><template #header
        ><div class="flex items-center gap-3">
          <span class="section-label">部门列表</span
          ><span class="text-xs text-ink-soft"
            >共 {{ allRows.length }} 个部门<span v-if="appliedKeyword || appliedStatus">
              · 匹配 {{ rows.filter((row) => !row.context).length }} 个</span
            ></span
          >
        </div></template
      ><template #tools
        ><AppButton
          variant="ghost"
          size="icon-sm"
          aria-label="展开全部"
          :disabled="!!(appliedKeyword || appliedStatus)"
          title="展开全部"
          :icon="ChevronsDown"
          @click="collapsed = new Set()" /><AppButton
          variant="ghost"
          size="icon-sm"
          aria-label="折叠全部"
          :disabled="!!(appliedKeyword || appliedStatus)"
          title="折叠全部"
          :icon="ChevronsUp"
          @click="collapsed = new Set(allRows.map((row) => row.id))" /><AppButton
          variant="outline"
          size="icon-sm"
          aria-label="刷新部门"
          title="刷新部门"
          :loading="loading"
          :icon="RefreshCw"
          @click="load"
      /></template>
      <AppErrorState v-if="error" :message="error" @retry="load" />
      <AppDataTable
        :columns="columns"
        :rows="rows"
        :loading="loading"
        :min-width="760"
        empty-title="暂无匹配部门"
        empty-description="调整关键字或状态筛选条件。"
      >
        <template #cell-name="{ row }"
          ><div class="flex items-center gap-2" :style="{ paddingLeft: `${row.depth * 18}px` }">
            <button
              v-if="row.children.length"
              type="button"
              class="grid h-7 w-5 place-items-center rounded text-ink-soft focus-visible:outline-2 focus-visible:outline-brand"
              :disabled="!!(appliedKeyword || appliedStatus)"
              :aria-expanded="!!(appliedKeyword || appliedStatus) || !collapsed.has(row.id)"
              :aria-label="`${collapsed.has(row.id) ? '展开' : '折叠'}${row.name}`"
              @click="toggle(row.id)"
            >
              <ChevronRight
                :size="13"
                :class="(appliedKeyword || appliedStatus || !collapsed.has(row.id)) && 'rotate-90'"
                aria-hidden="true"
              /></button
            ><span v-else class="w-5" /><span class="font-semibold text-ink">{{ row.name }}</span
            ><span v-if="row.context" class="text-[11px] text-ink-soft">上级路径</span
            ><span v-else-if="row.code === 'ROOT'" class="text-[11px] text-ink-soft">根部门</span>
          </div></template
        >
        <template #cell-code="{ row }"
          ><span class="code-chip">{{ row.code }}</span></template
        ><template #cell-status="{ row }"
          ><AppStatusTag
            :label="row.status === 'ENABLED' ? '启用' : '停用'"
            :tone="row.status === 'ENABLED' ? 'success' : 'neutral'"
        /></template>
        <template #cell-actions="{ row }"
          ><div class="flex justify-end gap-0.5">
            <AppButton
              v-permission="'iam:department:create'"
              variant="ghost"
              size="icon-sm"
              :disabled="row.status !== 'ENABLED'"
              :icon="Plus"
              aria-label="新增下级部门"
              title="新增下级部门"
              @click="openCreate(row.id)"
            /><AppButton
              v-permission="'iam:department:update'"
              variant="ghost"
              size="icon-sm"
              :icon="Pencil"
              aria-label="编辑部门"
              title="编辑部门"
              @click="openEdit(row)"
            /><AppButton
              v-if="row.code !== 'ROOT'"
              v-permission="'iam:department:status'"
              variant="ghost"
              size="icon-sm"
              :icon="row.status === 'ENABLED' ? Ban : CircleCheck"
              :aria-label="row.status === 'ENABLED' ? '停用部门' : '启用部门'"
              :title="row.status === 'ENABLED' ? '停用部门' : '启用部门'"
              @click="requestConfirm(row, 'status')"
            /><AppButton
              v-if="row.code !== 'ROOT'"
              v-permission="'iam:department:delete'"
              variant="danger-ghost"
              size="icon-sm"
              :disabled="row.children.length > 0 || row.directUserCount > 0"
              :icon="Trash2"
              aria-label="删除部门"
              :title="
                row.children.length
                  ? '存在下级部门'
                  : row.directUserCount
                    ? '仍有关联用户'
                    : '删除部门'
              "
              @click="requestConfirm(row, 'delete')"
            /></div
        ></template> </AppDataTable
    ></AppPanel>
  </div>
  <AppDialog
    :open="open"
    :title="editing ? '编辑部门' : '新增部门'"
    @close="!saving && (open = false)"
    ><form id="department-form" class="space-y-4" @submit="save">
      <AppField label="上级部门" for="department-parent" required
        ><AppInput
          v-if="root"
          id="department-parent"
          model-value="组织根节点"
          disabled /><DepartmentSelect
          v-else
          id="department-parent"
          v-model="parentId"
          :options="options"
          :exclude-ids="excluded"
          :preserve-id="editing?.parentId"
      /></AppField>
      <div class="grid gap-4 sm:grid-cols-2">
        <AppField label="部门编码" for="department-code" :error="errors.code" required
          ><AppInput
            id="department-code"
            v-model="code"
            v-bind="codeAttrs"
            :disabled="!!editing"
            :invalid="!!errors.code"
            maxlength="64"
            placeholder="例如 RD" /></AppField
        ><AppField label="部门名称" for="department-name" :error="errors.name" required
          ><AppInput
            id="department-name"
            v-model="name"
            v-bind="nameAttrs"
            :invalid="!!errors.name"
            maxlength="64" /></AppField
        ><AppField label="排序" for="department-sort" :error="errors.sort" required
          ><AppInput
            id="department-sort"
            v-model="sort"
            v-bind="sortAttrs"
            :invalid="!!errors.sort"
            type="number"
            min="0"
            max="9999" /></AppField
        ><AppField v-if="!editing" label="状态" for="department-form-status" required
          ><AppSelect
            id="department-form-status"
            v-model="status"
            :options="[
              { label: '启用', value: 'ENABLED' },
              { label: '停用', value: 'DISABLED' },
            ]"
        /></AppField>
      </div>
      <p
        v-if="formError"
        role="alert"
        class="rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive"
      >
        {{ formError }}
      </p>
    </form>
    <template #footer
      ><AppButton variant="ghost" :disabled="saving" @click="open = false">取消</AppButton
      ><AppButton type="submit" form="department-form" :loading="saving">保存</AppButton></template
    ></AppDialog
  >
  <AppConfirm
    :open="!!confirm"
    :title="confirm?.action === 'delete' ? '删除部门' : '调整部门状态'"
    :message="`${confirm?.action === 'delete' ? '确定删除空部门' : '确定' + (confirm?.row.status === 'ENABLED' ? '停用' : '启用') + '部门'}「${confirm?.row.name ?? ''}」？${confirm?.action === 'delete' ? '删除后不可恢复。' : '此操作不会自动修改用户账号状态。'}${confirmError ? '\n' + confirmError : ''}`"
    :loading="confirmLoading"
    @close="!confirmLoading && (confirm = undefined)"
    @confirm="execute"
  />
</template>

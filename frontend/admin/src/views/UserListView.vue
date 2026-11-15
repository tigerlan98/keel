<script setup lang="ts">
import {
  Ban,
  Eye,
  EyeOff,
  CircleCheck,
  KeyRound,
  LogOut,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  ShieldCheck,
} from '@lucide/vue'
import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import { computed, onMounted, reactive, ref } from 'vue'
import { z } from 'zod'

import { ApiError } from '@/api/http'
import { fetchRoles } from '@/api/roles'
import type { RoleSummary, UserDetail, UserStatus, UserSummary } from '@/api/types'
import {
  assignUserRoles,
  createUser,
  fetchUser,
  fetchUsers,
  forceLogout,
  forceLogoutDevice,
  resetUserPassword,
  updateUserProfile,
  updateUserStatus,
} from '@/api/users'
import AppButton from '@/components/app/AppButton.vue'
import AppConfirm from '@/components/app/AppConfirm.vue'
import AppDataTable from '@/components/app/AppDataTable.vue'
import AppDialog from '@/components/app/AppDialog.vue'
import AppErrorState from '@/components/app/AppErrorState.vue'
import AppField from '@/components/app/AppField.vue'
import AppFilterBar from '@/components/app/AppFilterBar.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppPagination from '@/components/app/AppPagination.vue'
import AppPanel from '@/components/app/AppPanel.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import AppStatusTag from '@/components/app/AppStatusTag.vue'
import PageHeader from '@/components/app/PageHeader.vue'
import RolePicker from '@/components/app/RolePicker.vue'
import RoleSelect from '@/components/app/RoleSelect.vue'
import DepartmentTree from '@/components/app/DepartmentTree.vue'
import DepartmentFilter from '@/components/app/DepartmentFilter.vue'
import DepartmentSelect from '@/components/app/DepartmentSelect.vue'
import { useDepartments } from '@/composables/useDepartments'
import { flattenDepartments } from '@/lib/departments'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/lib/format'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()
const auth = useAuthStore()
const { departments, departmentsLoading, departmentsError, loadDepartments } = useDepartments()
const selectedDepartment = ref<number | 'unassigned'>()
const initialStatus = ref<UserStatus>('ENABLED')
const passwordVisible = ref(false)
const roleOptionsError = ref('')
const canReadRoles = computed(() => auth.hasPermission('iam:role:list'))
async function chooseDepartment(value: number | 'unassigned' | undefined) {
  selectedDepartment.value = value
  page.value = 1
  await load()
}
async function refreshDepartments() {
  await loadDepartments()
  if (
    typeof selectedDepartment.value === 'number' &&
    !departmentsError.value &&
    !flattenDepartments(departments.value).some((node) => node.id === selectedDepartment.value)
  ) {
    selectedDepartment.value = undefined
    toast.show('所选部门已删除，已切换至全部部门', 'info')
    page.value = 1
    await load()
  }
}

const filters = reactive({
  username: '',
  status: '',
})

const page = ref(1)
const size = ref(20)
const sort = ref('createdAt,desc')
const items = ref<UserSummary[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')

const columns = [
  { key: 'username', label: '用户' },
  { key: 'departmentName', label: '部门' },
  { key: 'roles', label: '角色' },
  { key: 'phone', label: '手机号' },
  { key: 'status', label: '状态' },
  { key: 'createdAt', label: '创建时间' },
  { key: 'actions', label: '操作', align: 'right' as const, sticky: true, width: '208px' },
]

const roleOptions = ref<RoleSummary[]>([])
const roleOptionsLoading = ref(false)

const formOpen = ref(false)
const editing = ref<UserDetail | null>(null)
const saving = ref(false)
const formError = ref('')
const roleIds = ref<number[]>([])

const userSchema = z
  .object({
    username: z.string(),
    password: z.string(),
    nickname: z.string().trim().min(1, '请输入昵称').max(64, '昵称最多 64 个字符'),
    email: z.string().trim(),
    phone: z.string().trim(),
    departmentId: z.number({ required_error: '请选择所属部门' }).int().positive('请选择所属部门'),
  })
  .superRefine((values, context) => {
    if (!editing.value) {
      if (!/^[A-Za-z0-9_.-]{3,64}$/.test(values.username)) {
        context.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['username'],
          message: '用户名需 3-64 位，只允许字母、数字、下划线、点和连字符',
        })
      }
      if (!/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/.test(values.password)) {
        context.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['password'],
          message: '初始密码需 8-72 位并同时包含字母和数字',
        })
      }
    }
    if (values.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email)) {
      context.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['email'],
        message: '邮箱格式不正确',
      })
    }
    if (values.phone && !/^[0-9+-]{6,32}$/.test(values.phone)) {
      context.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['phone'],
        message: '手机号格式不正确',
      })
    }
  })

type UserFormValues = z.infer<typeof userSchema>
type UserFormField = keyof UserFormValues

const userForm = useForm({
  validationSchema: toTypedSchema(userSchema),
  initialValues: {
    username: '',
    password: '',
    nickname: '',
    email: '',
    phone: '',
    departmentId: undefined,
  },
})

const {
  defineField,
  errors: validationErrors,
  handleSubmit,
  resetForm,
  setFieldError,
  submitCount,
} = userForm
const errors = computed(() =>
  Object.fromEntries(
    Object.entries(validationErrors.value).filter(
      ([key]) => submitCount.value > 0 || userForm.isFieldTouched(key as UserFormField),
    ),
  ),
)
const [username, usernameAttrs] = defineField('username')
const [password, passwordAttrs] = defineField('password')
const [nickname, nicknameAttrs] = defineField('nickname')
const [email, emailAttrs] = defineField('email')
const [phone, phoneAttrs] = defineField('phone')
const [departmentId] = defineField('departmentId')

const USER_FORM_FIELDS: UserFormField[] = [
  'username',
  'password',
  'nickname',
  'email',
  'phone',
  'departmentId',
]

function applyFieldErrors(caught: ApiError): boolean {
  let applied = false
  for (const item of caught.fieldErrors) {
    if ((USER_FORM_FIELDS as string[]).includes(item.field)) {
      setFieldError(item.field as UserFormField, item.message)
      applied = true
    }
  }
  return applied
}

const roleDialogOpen = ref(false)
const roleTarget = ref<UserDetail | null>(null)
const selectedRoleIds = ref<number[]>([])
const roleSaving = ref(false)
const roleError = ref('')

const passwordDialogOpen = ref(false)
const passwordTarget = ref<UserDetail | null>(null)
const passwordSaving = ref(false)
const passwordError = ref('')

const passwordSchema = z.object({
  newPassword: z
    .string()
    .regex(/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/, '新密码需 8-72 位并同时包含字母和数字'),
})

const passwordForm = useForm({
  validationSchema: toTypedSchema(passwordSchema),
  initialValues: { newPassword: '' },
})

const {
  defineField: definePasswordField,
  errors: passwordErrors,
  handleSubmit: handlePasswordSubmit,
  resetForm: resetPasswordForm,
} = passwordForm
const [newPassword, newPasswordAttrs] = definePasswordField('newPassword')

const statusTarget = ref<UserSummary | null>(null)
const statusSaving = ref(false)

const statusConfirmTitle = computed(() =>
  statusTarget.value?.status === 'ENABLED' ? '禁用用户' : '启用用户',
)
const statusConfirmAction = computed(() =>
  statusTarget.value?.status === 'ENABLED' ? '禁用' : '启用',
)
const statusConfirmMessage = computed(() => {
  const user = statusTarget.value
  if (!user) {
    return ''
  }
  return user.status === 'ENABLED'
    ? `确定禁用用户「${user.username}」吗？禁用后该用户将无法登录。`
    : `确定启用用户「${user.username}」吗？启用后该用户可以重新登录。`
})

const logoutTarget = ref<UserSummary | null>(null)
const loggingOut = ref(false)

let requestGeneration = 0
async function load(): Promise<void> {
  const generation = ++requestGeneration
  loading.value = true
  error.value = ''
  try {
    const result = await fetchUsers({
      page: page.value,
      size: size.value,
      sort: sort.value,
      keyword: filters.username.trim() || undefined,
      departmentId:
        typeof selectedDepartment.value === 'number' ? selectedDepartment.value : undefined,
      unassignedDepartment: selectedDepartment.value === 'unassigned' || undefined,
      status: filters.status ? (filters.status as UserStatus) : undefined,
    })
    if (generation !== requestGeneration) return
    items.value = result.items
    total.value = result.total
  } catch (caught) {
    if (generation !== requestGeneration) return
    items.value = []
    total.value = 0
    error.value = caught instanceof ApiError ? caught.message : '加载失败，请稍后重试'
  } finally {
    if (generation === requestGeneration) loading.value = false
  }
}

function search(): void {
  page.value = 1
  void load()
}

function resetFilters(): void {
  selectedDepartment.value = undefined
  filters.username = ''
  filters.status = ''
  sort.value = 'createdAt,desc'
  page.value = 1
  void load()
}

function changePage(next: number): void {
  page.value = next
  void load()
}

function changeSize(next: number): void {
  size.value = next
  page.value = 1
  void load()
}

async function loadRoleOptions(): Promise<void> {
  if (!canReadRoles.value) return
  roleOptionsError.value = ''
  roleOptionsLoading.value = true
  try {
    const result = await fetchRoles({ page: 1, size: 100, sort: 'sort,asc', status: 'ENABLED' })
    roleOptions.value = result.items
  } catch {
    roleOptionsError.value = '角色加载失败，可稍后重试或创建后再分配'
    roleOptions.value = []
  } finally {
    roleOptionsLoading.value = false
  }
}

function openCreate(): void {
  editing.value = null
  resetForm({
    values: {
      username: '',
      password: '',
      nickname: '',
      email: '',
      phone: '',
      departmentId:
        typeof selectedDepartment.value === 'number' &&
        flattenDepartments(departments.value).some(
          (row) => row.id === selectedDepartment.value && row.selectable,
        )
          ? selectedDepartment.value
          : undefined,
    },
  })
  initialStatus.value = 'ENABLED'
  passwordVisible.value = false
  void loadDepartments()
  roleIds.value = []
  formError.value = ''
  formOpen.value = true
  if (roleOptions.value.length === 0) {
    void loadRoleOptions()
  }
}

async function openEdit(user: UserSummary): Promise<void> {
  formError.value = ''
  try {
    const detail = await fetchUser(user.id)
    editing.value = detail
    resetForm({
      values: {
        username: detail.username,
        password: '',
        nickname: detail.nickname ?? '',
        email: detail.email ?? '',
        phone: detail.phone ?? '',
        departmentId: detail.departmentId,
      },
    })
    roleIds.value = []
    formOpen.value = true
    void loadDepartments()
  } catch (caught) {
    toast.show(caught instanceof ApiError ? caught.message : '加载用户失败', 'error')
  }
}

const saveUser = handleSubmit(async (values) => {
  formError.value = ''
  if (departmentsLoading.value || departmentsError.value) {
    formError.value = '请先加载部门后再保存'
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await updateUserProfile(editing.value.id, {
        nickname: values.nickname.trim(),
        email: values.email.trim() || null,
        phone: values.phone.trim() || null,
        version: editing.value.version,
        departmentId: values.departmentId,
      })
      toast.show('用户资料已更新', 'success')
    } else {
      await createUser({
        username: values.username.trim(),
        password: values.password,
        nickname: values.nickname.trim(),
        email: values.email.trim() || undefined,
        phone: values.phone.trim() || undefined,
        roleIds: roleIds.value,
        departmentId: values.departmentId,
        status: initialStatus.value,
      })
      toast.show('用户已创建', 'success')
    }
    formOpen.value = false
    await load()
  } catch (caught) {
    if (caught instanceof ApiError) {
      if (!applyFieldErrors(caught)) {
        formError.value = caught.message
      }
    } else {
      formError.value = '保存失败，请稍后重试'
    }
  } finally {
    saving.value = false
  }
})

function requestToggleStatus(user: UserSummary): void {
  statusTarget.value = user
}

async function confirmToggleStatus(): Promise<void> {
  const target = statusTarget.value
  if (!target) {
    return
  }
  statusSaving.value = true
  try {
    const detail = await fetchUser(target.id)
    const next: UserStatus = target.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
    await updateUserStatus(target.id, next, detail.version)
    toast.show(next === 'ENABLED' ? '用户已启用' : '用户已禁用', 'success')
    statusTarget.value = null
    await load()
  } catch (caught) {
    toast.show(caught instanceof ApiError ? caught.message : '状态更新失败', 'error')
  } finally {
    statusSaving.value = false
  }
}

async function openRoles(user: UserSummary): Promise<void> {
  roleError.value = ''
  selectedRoleIds.value = []
  try {
    const detail = await fetchUser(user.id)
    roleTarget.value = detail
    selectedRoleIds.value = detail.roles.map((role) => role.id)
    roleDialogOpen.value = true
    if (roleOptions.value.length === 0) {
      await loadRoleOptions()
    }
  } catch (caught) {
    toast.show(caught instanceof ApiError ? caught.message : '加载用户角色失败', 'error')
  }
}

async function saveRoles(): Promise<void> {
  if (!roleTarget.value) {
    return
  }
  roleSaving.value = true
  roleError.value = ''
  try {
    await assignUserRoles(roleTarget.value.id, selectedRoleIds.value)
    toast.show('用户角色已更新', 'success')
    roleDialogOpen.value = false
    await load()
  } catch (caught) {
    roleError.value = caught instanceof ApiError ? caught.message : '保存失败，请稍后重试'
  } finally {
    roleSaving.value = false
  }
}

async function openPassword(user: UserSummary): Promise<void> {
  passwordError.value = ''
  resetPasswordForm()
  try {
    passwordTarget.value = await fetchUser(user.id)
    passwordDialogOpen.value = true
  } catch (caught) {
    toast.show(caught instanceof ApiError ? caught.message : '加载用户失败', 'error')
  }
}

const savePassword = handlePasswordSubmit(async (values) => {
  if (!passwordTarget.value) {
    return
  }
  passwordSaving.value = true
  passwordError.value = ''
  try {
    await resetUserPassword(passwordTarget.value.id, {
      newPassword: values.newPassword,
      version: passwordTarget.value.version,
    })
    toast.show('密码已重置，用户下次登录需修改密码', 'success')
    passwordDialogOpen.value = false
    await load()
  } catch (caught) {
    passwordError.value = caught instanceof ApiError ? caught.message : '重置失败，请稍后重试'
  } finally {
    passwordSaving.value = false
  }
})

async function doForceLogout(device?: string): Promise<void> {
  if (!logoutTarget.value) {
    return
  }
  loggingOut.value = true
  try {
    if (device) {
      await forceLogoutDevice(logoutTarget.value.id, device)
    } else {
      await forceLogout(logoutTarget.value.id)
    }
    toast.show('已强制下线', 'success')
    logoutTarget.value = null
  } catch (caught) {
    toast.show(caught instanceof ApiError ? caught.message : '操作失败', 'error')
  } finally {
    loggingOut.value = false
  }
}

function statusText(status: UserStatus): string {
  return status === 'ENABLED' ? '启用' : '禁用'
}

onMounted(() => {
  void load()
  void loadDepartments()
})
</script>

<template>
  <PageHeader title="用户管理" description="维护后台账号、所属部门与角色；禁用的账号无法登录。">
    <template #actions>
      <AppButton v-permission="'iam:user:create'" @click="openCreate">
        <Plus :size="14" aria-hidden="true" />
        新增用户
      </AppButton>
    </template>
  </PageHeader>

  <div class="mt-5 grid min-w-0 gap-4 lg:grid-cols-[240px_minmax(0,1fr)]">
    <aside class="app-panel hidden self-start p-4 lg:block">
      <div class="mb-3 flex items-center justify-between">
        <span class="section-label">组织部门</span
        ><AppButton
          variant="ghost"
          size="icon-sm"
          :icon="RefreshCw"
          :loading="departmentsLoading"
          aria-label="刷新部门树"
          title="刷新部门树"
          @click="refreshDepartments"
        />
      </div>
      <p v-if="departmentsLoading && !departments.length" class="py-4 text-xs text-ink-soft">
        正在加载部门…
      </p>
      <AppErrorState
        v-if="departmentsError"
        :message="departmentsError"
        @retry="refreshDepartments"
      />
      <DepartmentTree
        :model-value="selectedDepartment"
        :nodes="departments"
        @update:model-value="chooseDepartment"
      />
    </aside>
    <div class="min-w-0 space-y-4">
      <div class="app-panel p-4 lg:hidden">
        <DepartmentFilter
          :nodes="departments"
          :model-value="selectedDepartment"
          @update:model-value="chooseDepartment"
        /><AppErrorState
          v-if="departmentsError"
          :message="departmentsError"
          @retry="refreshDepartments"
        />
      </div>
      <AppFilterBar @submit="search">
        <AppField label="关键字" for="user-username" class="w-[220px]">
          <AppInput
            id="user-username"
            v-model="filters.username"
            type="text"
            maxlength="64"
            placeholder="账号 / 显示名称 / 手机号"
          />
        </AppField>

        <AppField label="状态" for="user-status" class="w-[150px]">
          <AppSelect
            id="user-status"
            v-model="filters.status"
            :options="[
              { label: '全部', value: '' },
              { label: '启用', value: 'ENABLED' },
              { label: '禁用', value: 'DISABLED' },
            ]"
          />
        </AppField>

        <AppField label="排序" for="user-sort" class="w-[180px]">
          <AppSelect
            id="user-sort"
            v-model="sort"
            :options="[
              { label: '创建时间 ↓', value: 'createdAt,desc' },
              { label: '创建时间 ↑', value: 'createdAt,asc' },
              { label: '用户名 ↑', value: 'username,asc' },
              { label: '用户名 ↓', value: 'username,desc' },
              { label: '最近登录 ↓', value: 'lastLoginAt,desc' },
            ]"
          />
        </AppField>

        <template #actions>
          <AppButton type="submit" :loading="loading" :icon="Search">查询</AppButton>
          <AppButton variant="outline" :disabled="loading" :icon="RefreshCw" @click="resetFilters"
            >重置</AppButton
          >
        </template>
      </AppFilterBar>

      <AppPanel :show-footer="!error && total > 0">
        <template #header>
          <div class="flex items-center gap-2.5">
            <span class="section-label">用户列表</span>
            <span v-if="!error" class="text-[11.5px] text-ink-soft">共 {{ total }} 条</span>
          </div>
        </template>

        <template #tools>
          <AppButton variant="ghost" size="sm" :loading="loading" @click="load">
            <RefreshCw :size="14" aria-hidden="true" />
            刷新
          </AppButton>
        </template>

        <AppErrorState v-if="error" :message="error" @retry="load" />

        <AppDataTable
          v-else
          :columns="columns"
          :rows="items"
          :loading="loading"
          :min-width="980"
          empty-title="暂无用户"
          empty-description="调整筛选条件，或新增第一个用户。"
        >
          <template #cell-username="{ row }"
            ><div class="flex items-center gap-2.5">
              <span class="user-avatar" aria-hidden="true">{{
                (row.nickname || row.username).slice(0, 1)
              }}</span>
              <div class="min-w-0">
                <div class="font-semibold text-ink">{{ row.nickname || row.username }}</div>
                <div class="text-xs text-ink-soft">@{{ row.username }}</div>
              </div>
            </div></template
          >
          <template #cell-departmentName="{ row }"
            ><span class="text-ink-muted">{{ row.departmentName || '未分配部门' }}</span></template
          >
          <template #cell-roles="{ row }"
            ><div class="flex max-w-60 flex-wrap gap-1">
              <span
                v-for="role in row.roles"
                :key="role.id"
                class="rounded-full bg-brand/10 px-2 py-0.5 text-[11px] font-semibold text-brand"
                >{{ role.name }}{{ role.status === 'DISABLED' ? '（停用）' : '' }}</span
              ><span v-if="!row.roles?.length" class="text-ink-soft">未分配角色</span>
            </div></template
          >
          <template #cell-phone="{ row }"
            ><span class="tnum text-ink-muted">{{ row.phone || '—' }}</span></template
          >
          <template #cell-status="{ row }">
            <AppStatusTag
              :tone="row.status === 'ENABLED' ? 'success' : 'danger'"
              :label="statusText(row.status)"
            />
          </template>

          <template #cell-lastLoginAt="{ row }">
            <span class="tnum text-[12px] text-ink-soft">
              {{ formatDateTime(row.lastLoginAt) }}
            </span>
          </template>

          <template #cell-createdAt="{ row }">
            <span class="tnum text-[12px] text-ink-soft">
              {{ formatDateTime(row.createdAt) }}
            </span>
          </template>

          <template #cell-actions="{ row }">
            <div class="flex items-center justify-end gap-0.5">
              <AppButton
                v-permission="'iam:user:update'"
                variant="ghost"
                size="icon-sm"
                aria-label="编辑"
                title="编辑"
                @click="openEdit(row)"
              >
                <Pencil :size="14" aria-hidden="true" />
              </AppButton>
              <AppButton
                v-permission="'iam:user:assign-role'"
                variant="ghost"
                size="icon-sm"
                aria-label="分配角色"
                title="分配角色"
                @click="openRoles(row)"
              >
                <ShieldCheck :size="14" aria-hidden="true" />
              </AppButton>
              <AppButton
                v-permission="'iam:user:reset-password'"
                variant="ghost"
                size="icon-sm"
                aria-label="重置密码"
                title="重置密码"
                @click="openPassword(row)"
              >
                <KeyRound :size="14" aria-hidden="true" />
              </AppButton>
              <AppButton
                v-permission="'iam:user:status'"
                :variant="row.status === 'ENABLED' ? 'danger-ghost' : 'ghost'"
                size="icon-sm"
                :aria-label="row.status === 'ENABLED' ? '禁用' : '启用'"
                :title="row.status === 'ENABLED' ? '禁用' : '启用'"
                @click="requestToggleStatus(row)"
              >
                <Ban v-if="row.status === 'ENABLED'" :size="14" aria-hidden="true" />
                <CircleCheck v-else :size="14" aria-hidden="true" />
              </AppButton>
              <AppButton
                v-permission="'iam:user:force-logout'"
                variant="ghost"
                size="icon-sm"
                aria-label="强制下线"
                title="强制下线"
                @click="logoutTarget = row"
              >
                <LogOut :size="14" aria-hidden="true" />
              </AppButton>
            </div>
          </template>
        </AppDataTable>

        <template #footer>
          <AppPagination
            :page="page"
            :page-size="size"
            :total="total"
            :loading="loading"
            @update:page="changePage"
            @update:page-size="changeSize"
          />
        </template>
      </AppPanel>
    </div>
  </div>

  <AppDialog
    :open="formOpen"
    :title="editing ? '编辑用户' : '新增用户'"
    size="lg"
    @close="formOpen = false"
  >
    <form id="user-form" class="space-y-4" @submit="saveUser">
      <div class="grid gap-4 sm:grid-cols-2">
        <AppField
          label="登录账号"
          for="user-form-username"
          :error="errors.username"
          error-id="user-form-username-error"
          required
          :hint="editing ? '用户名不可修改' : undefined"
        >
          <AppInput
            id="user-form-username"
            v-model="username"
            v-bind="usernameAttrs"
            placeholder="字母、数字组合"
            maxlength="64"
            :disabled="!!editing"
            :invalid="!!errors.username"
            :aria-describedby="errors.username ? 'user-form-username-error' : undefined"
          />
        </AppField>
        <AppField
          label="显示名称"
          for="user-form-nickname"
          :error="errors.nickname"
          error-id="user-form-nickname-error"
          required
        >
          <AppInput
            id="user-form-nickname"
            v-model="nickname"
            v-bind="nicknameAttrs"
            placeholder="请输入显示名称"
            maxlength="64"
            :invalid="!!errors.nickname"
            :aria-describedby="errors.nickname ? 'user-form-nickname-error' : undefined"
          />
        </AppField>
        <AppField
          v-if="!editing"
          class="sm:col-span-2"
          label="初始密码"
          for="user-form-password"
          :error="errors.password"
          error-id="user-form-password-error"
          required
          hint="创建后用户首次登录必须修改密码"
        >
          <AppInput
            id="user-form-password"
            v-model="password"
            v-bind="passwordAttrs"
            :type="passwordVisible ? 'text' : 'password'"
            autocomplete="new-password"
            :invalid="!!errors.password"
            :aria-describedby="errors.password ? 'user-form-password-error' : undefined"
            ><template #trailing
              ><button
                type="button"
                class="grid h-7 w-7 place-items-center rounded text-ink-soft focus-visible:outline-2 focus-visible:outline-brand"
                :aria-label="passwordVisible ? '隐藏初始密码' : '显示初始密码'"
                :aria-pressed="passwordVisible"
                @click="passwordVisible = !passwordVisible"
              >
                <EyeOff v-if="passwordVisible" :size="15" aria-hidden="true" /><Eye
                  v-else
                  :size="15"
                  aria-hidden="true"
                /></button></template
          ></AppInput>
        </AppField>
        <AppField
          label="手机号"
          for="user-form-phone"
          :error="errors.phone"
          error-id="user-form-phone-error"
        >
          <AppInput
            id="user-form-phone"
            v-model="phone"
            v-bind="phoneAttrs"
            maxlength="32"
            autocomplete="tel"
            :invalid="!!errors.phone"
            :aria-describedby="errors.phone ? 'user-form-phone-error' : undefined"
          />
        </AppField>
        <AppField
          label="电子邮箱"
          for="user-form-email"
          :error="errors.email"
          error-id="user-form-email-error"
        >
          <AppInput
            id="user-form-email"
            v-model="email"
            v-bind="emailAttrs"
            type="email"
            maxlength="128"
            autocomplete="email"
            :invalid="!!errors.email"
            :aria-describedby="errors.email ? 'user-form-email-error' : undefined"
          />
        </AppField>
        <AppField
          label="所属部门"
          for="user-form-department"
          required
          :error="errors.departmentId"
          :hint="
            editing?.departmentStatus === 'DISABLED'
              ? '当前部门已停用，可保留或调整归属'
              : undefined
          "
          ><DepartmentSelect
            id="user-form-department"
            v-model="departmentId"
            :options="departments"
            :invalid="!!errors.departmentId"
            :disabled="departmentsLoading"
            :preserve-id="editing?.departmentId"
        /></AppField>
        <AppField v-if="!editing" label="角色" hint="选填；未分配角色不具备角色授予的业务权限"
          ><RoleSelect
            v-if="canReadRoles"
            v-model="roleIds"
            :roles="roleOptions"
            :disabled="roleOptionsLoading"
          />
          <p v-else class="py-2 text-xs text-ink-soft">可在创建后由有权限的管理员分配角色</p>
          <p v-if="roleOptionsError" role="alert" class="text-xs text-destructive">
            {{ roleOptionsError
            }}<AppButton variant="ghost" size="sm" @click="loadRoleOptions">重试</AppButton>
          </p></AppField
        >
        <AppField v-if="!editing" label="账号状态"
          ><div class="flex h-9 items-center gap-3">
            <button
              type="button"
              role="switch"
              :aria-checked="initialStatus === 'ENABLED'"
              aria-label="账号启用状态"
              class="relative h-5 w-9 rounded-full transition-colors focus-visible:outline-2 focus-visible:outline-brand focus-visible:outline-offset-2"
              :class="initialStatus === 'ENABLED' ? 'bg-brand-bright' : 'bg-ink/20'"
              @click="initialStatus = initialStatus === 'ENABLED' ? 'DISABLED' : 'ENABLED'"
            >
              <span
                class="absolute top-0.5 h-4 w-4 rounded-full bg-white shadow-sm transition-[left]"
                :class="initialStatus === 'ENABLED' ? 'left-[18px]' : 'left-0.5'"
              /></button
            ><span class="text-xs text-ink-muted">{{
              initialStatus === 'ENABLED' ? '启用，允许登录' : '禁用，暂不允许登录'
            }}</span>
          </div></AppField
        >
      </div>
      <p v-if="departmentsLoading" class="text-xs text-ink-soft">正在加载部门…</p>
      <p v-if="departmentsError" role="alert" class="text-xs text-destructive">
        {{ departmentsError
        }}<AppButton variant="ghost" size="sm" @click="loadDepartments">重试</AppButton>
      </p>

      <p
        v-if="formError"
        class="rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-[12px] leading-5 text-destructive"
        role="alert"
      >
        {{ formError }}
      </p>
    </form>

    <template #footer>
      <AppButton variant="outline" @click="formOpen = false">取消</AppButton>
      <AppButton
        type="submit"
        form="user-form"
        :loading="saving"
        :disabled="departmentsLoading || !!departmentsError"
        >保存</AppButton
      >
    </template>
  </AppDialog>

  <AppDialog
    :open="roleDialogOpen"
    :title="`分配角色 - ${roleTarget?.username ?? ''}`"
    @close="roleDialogOpen = false"
  >
    <RolePicker v-model="selectedRoleIds" :roles="roleOptions" />
    <p
      v-if="roleError"
      class="mt-3 rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-[12px] leading-5 text-destructive"
      role="alert"
    >
      {{ roleError }}
    </p>
    <template #footer>
      <AppButton variant="outline" @click="roleDialogOpen = false">取消</AppButton>
      <AppButton :loading="roleSaving" @click="saveRoles">保存</AppButton>
    </template>
  </AppDialog>

  <AppDialog
    :open="passwordDialogOpen"
    :title="`重置密码 - ${passwordTarget?.username ?? ''}`"
    @close="passwordDialogOpen = false"
  >
    <form id="user-password-form" class="space-y-4" @submit="savePassword">
      <AppField
        label="新密码"
        for="user-new-password"
        :error="passwordErrors.newPassword"
        error-id="user-new-password-error"
        required
        hint="重置后该用户全部会话失效，下次登录必须修改密码。"
      >
        <AppInput
          id="user-new-password"
          v-model="newPassword"
          v-bind="newPasswordAttrs"
          type="password"
          autocomplete="new-password"
          :invalid="!!passwordErrors.newPassword"
          :aria-describedby="passwordErrors.newPassword ? 'user-new-password-error' : undefined"
        />
      </AppField>

      <p
        v-if="passwordError"
        class="rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-[12px] leading-5 text-destructive"
        role="alert"
      >
        {{ passwordError }}
      </p>
    </form>

    <template #footer>
      <AppButton variant="outline" @click="passwordDialogOpen = false">取消</AppButton>
      <AppButton type="submit" form="user-password-form" :loading="passwordSaving">
        重置密码
      </AppButton>
    </template>
  </AppDialog>

  <AppDialog
    :open="!!logoutTarget"
    :title="`强制下线 - ${logoutTarget?.username ?? ''}`"
    size="sm"
    @close="logoutTarget = null"
  >
    <p class="text-[13px] leading-6 text-ink-muted">选择要下线的设备，操作立即生效。</p>
    <div class="mt-4 grid grid-cols-2 gap-2">
      <AppButton variant="danger" :disabled="loggingOut" @click="doForceLogout()">
        全部设备
      </AppButton>
      <AppButton
        v-for="device in ['WEB', 'APP', 'MINI']"
        :key="device"
        variant="outline"
        :disabled="loggingOut"
        @click="doForceLogout(device)"
      >
        {{ device }}
      </AppButton>
    </div>
  </AppDialog>

  <AppConfirm
    :open="!!statusTarget"
    :title="statusConfirmTitle"
    :message="statusConfirmMessage"
    :confirm-text="statusConfirmAction"
    :tone="statusTarget?.status === 'ENABLED' ? 'danger' : 'primary'"
    :loading="statusSaving"
    @close="statusTarget = null"
    @confirm="confirmToggleStatus"
  />
</template>

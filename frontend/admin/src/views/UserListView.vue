<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

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
  updateUser,
  updateUserStatus,
} from '@/api/users'
import AppDialog from '@/components/app/AppDialog.vue'
import PageHeader from '@/components/app/PageHeader.vue'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()

const filters = reactive({
  username: '',
  status: '' as '' | UserStatus,
})
const page = ref(1)
const size = ref(20)
const sort = ref('createdAt,desc')
const items = ref<UserSummary[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

const roleOptions = ref<RoleSummary[]>([])

const formOpen = ref(false)
const editing = ref<UserDetail | null>(null)
const saving = ref(false)
const formError = ref('')
const form = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  roleIds: [] as number[],
  version: 0,
})

const roleDialogOpen = ref(false)
const roleTarget = ref<UserDetail | null>(null)
const selectedRoleIds = ref<number[]>([])
const roleSaving = ref(false)
const roleError = ref('')

const passwordDialogOpen = ref(false)
const passwordTarget = ref<UserDetail | null>(null)
const newPassword = ref('')
const passwordSaving = ref(false)
const passwordError = ref('')

const logoutTarget = ref<UserSummary | null>(null)
const loggingOut = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchUsers({
      page: page.value,
      size: size.value,
      sort: sort.value,
      username: filters.username.trim() || undefined,
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
  filters.username = ''
  filters.status = ''
  sort.value = 'createdAt,desc'
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

async function loadRoleOptions() {
  try {
    const result = await fetchRoles({ page: 1, size: 100, sort: 'sort,asc', status: 'ENABLED' })
    roleOptions.value = result.items
  } catch {
    roleOptions.value = []
  }
}

function openCreate() {
  editing.value = null
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.email = ''
  form.phone = ''
  form.roleIds = []
  form.version = 0
  formError.value = ''
  formOpen.value = true
  if (roleOptions.value.length === 0) {
    void loadRoleOptions()
  }
}

async function openEdit(user: UserSummary) {
  formError.value = ''
  try {
    const detail = await fetchUser(user.id)
    editing.value = detail
    form.username = detail.username
    form.password = ''
    form.nickname = detail.nickname ?? ''
    form.email = detail.email ?? ''
    form.phone = detail.phone ?? ''
    form.version = detail.version
    formOpen.value = true
  } catch (caught) {
    toast.show(caught instanceof ApiError ? caught.message : '加载用户失败', 'error')
  }
}

async function save() {
  formError.value = ''
  if (!form.nickname.trim()) {
    formError.value = '请输入昵称'
    return
  }
  if (!editing.value) {
    if (!/^[A-Za-z0-9_.-]{3,64}$/.test(form.username)) {
      formError.value = '用户名需 3-64 位，只允许字母、数字、下划线、点和连字符'
      return
    }
    if (!/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/.test(form.password)) {
      formError.value = '初始密码需 8-72 位并同时包含字母和数字'
      return
    }
  }
  if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    formError.value = '邮箱格式不正确'
    return
  }
  if (form.phone && !/^[0-9+-]{6,32}$/.test(form.phone)) {
    formError.value = '手机号格式不正确'
    return
  }

  saving.value = true
  try {
    if (editing.value) {
      await updateUser(editing.value.id, {
        nickname: form.nickname.trim(),
        email: form.email.trim() || null,
        phone: form.phone.trim() || null,
        version: form.version,
      })
      toast.show('用户资料已更新', 'success')
    } else {
      await createUser({
        username: form.username.trim(),
        password: form.password,
        nickname: form.nickname.trim(),
        email: form.email.trim() || undefined,
        phone: form.phone.trim() || undefined,
        roleIds: form.roleIds,
      })
      toast.show('用户已创建', 'success')
    }
    formOpen.value = false
    await load()
  } catch (caught) {
    formError.value = caught instanceof ApiError ? caught.message : '保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

async function toggleStatus(user: UserSummary) {
  const next: UserStatus = user.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    const detail = await fetchUser(user.id)
    await updateUserStatus(user.id, next, detail.version)
    toast.show(next === 'ENABLED' ? '用户已启用' : '用户已禁用', 'success')
    await load()
  } catch (caught) {
    toast.show(caught instanceof ApiError ? caught.message : '状态更新失败', 'error')
  }
}

async function openRoles(user: UserSummary) {
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

function toggleRole(id: number) {
  const index = selectedRoleIds.value.indexOf(id)
  if (index >= 0) {
    selectedRoleIds.value.splice(index, 1)
  } else {
    selectedRoleIds.value.push(id)
  }
}

async function saveRoles() {
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

async function openPassword(user: UserSummary) {
  passwordError.value = ''
  newPassword.value = ''
  try {
    passwordTarget.value = await fetchUser(user.id)
    passwordDialogOpen.value = true
  } catch (caught) {
    toast.show(caught instanceof ApiError ? caught.message : '加载用户失败', 'error')
  }
}

async function savePassword() {
  if (!passwordTarget.value) {
    return
  }
  if (!/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/.test(newPassword.value)) {
    passwordError.value = '新密码需 8-72 位并同时包含字母和数字'
    return
  }
  passwordSaving.value = true
  passwordError.value = ''
  try {
    await resetUserPassword(passwordTarget.value.id, {
      newPassword: newPassword.value,
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
}

async function doForceLogout(device?: string) {
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

function statusText(status: UserStatus) {
  return status === 'ENABLED' ? '启用' : '禁用'
}

function statusClass(status: UserStatus) {
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
  <PageHeader title="用户管理" description="维护用户资料、角色与登录状态。" />

  <div class="mt-6 rounded-xl border bg-card">
    <form class="flex flex-wrap items-end gap-3 border-b p-4" @submit.prevent="search">
      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="user-username">用户名</label>
        <input
          id="user-username"
          v-model="filters.username"
          type="text"
          maxlength="64"
          placeholder="用户名前缀"
          class="flex h-9 w-52 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
        />
      </div>

      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="user-status">状态</label>
        <select
          id="user-status"
          v-model="filters.status"
          class="flex h-9 w-32 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
        >
          <option value="">全部</option>
          <option value="ENABLED">启用</option>
          <option value="DISABLED">禁用</option>
        </select>
      </div>

      <div class="space-y-1">
        <label class="text-xs text-muted-foreground" for="user-sort">排序</label>
        <select
          id="user-sort"
          v-model="sort"
          class="flex h-9 w-40 rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
        >
          <option value="createdAt,desc">创建时间 ↓</option>
          <option value="createdAt,asc">创建时间 ↑</option>
          <option value="username,asc">用户名 ↑</option>
          <option value="username,desc">用户名 ↓</option>
          <option value="lastLoginAt,desc">最近登录 ↓</option>
        </select>
      </div>

      <button
        type="submit"
        class="inline-flex h-9 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90 disabled:pointer-events-none disabled:opacity-50"
        :disabled="loading"
      >
        查询
      </button>
      <button
        type="button"
        class="inline-flex h-9 items-center justify-center rounded-md border border-input px-4 text-sm font-medium transition-colors hover:bg-accent disabled:pointer-events-none disabled:opacity-50"
        :disabled="loading"
        @click="resetFilters"
      >
        重置
      </button>
      <button
        v-permission="'iam:user:create'"
        type="button"
        class="ml-auto inline-flex h-9 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        @click="openCreate"
      >
        新增用户
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
      <table class="w-full min-w-[980px] text-sm">
        <thead class="border-b bg-muted/40 text-left text-xs text-muted-foreground">
          <tr>
            <th class="px-4 py-3 font-medium">用户名</th>
            <th class="px-4 py-3 font-medium">昵称</th>
            <th class="px-4 py-3 font-medium">状态</th>
            <th class="px-4 py-3 font-medium">最近登录</th>
            <th class="px-4 py-3 font-medium">创建时间</th>
            <th class="px-4 py-3 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="6" class="px-4 py-10 text-center text-muted-foreground">加载中...</td>
          </tr>
          <tr v-else-if="items.length === 0">
            <td colspan="6" class="px-4 py-10 text-center text-muted-foreground">暂无数据</td>
          </tr>
          <template v-else>
            <tr v-for="user in items" :key="user.id" class="border-b last:border-0">
              <td class="px-4 py-3 font-medium">{{ user.username }}</td>
              <td class="px-4 py-3 text-muted-foreground">{{ user.nickname || '-' }}</td>
              <td class="px-4 py-3">
                <span
                  class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                  :class="statusClass(user.status)"
                >
                  {{ statusText(user.status) }}
                </span>
              </td>
              <td class="px-4 py-3 text-muted-foreground">
                {{ formatDateTime(user.lastLoginAt) }}
              </td>
              <td class="px-4 py-3 text-muted-foreground">{{ formatDateTime(user.createdAt) }}</td>
              <td class="px-4 py-3">
                <div class="flex flex-wrap justify-end gap-3 text-primary">
                  <button
                    v-permission="'iam:user:update'"
                    type="button"
                    class="hover:underline"
                    @click="openEdit(user)"
                  >
                    编辑
                  </button>
                  <button
                    v-permission="'iam:user:assign-role'"
                    type="button"
                    class="hover:underline"
                    @click="openRoles(user)"
                  >
                    角色
                  </button>
                  <button
                    v-permission="'iam:user:reset-password'"
                    type="button"
                    class="hover:underline"
                    @click="openPassword(user)"
                  >
                    重置密码
                  </button>
                  <button
                    v-permission="'iam:user:status'"
                    type="button"
                    class="hover:underline"
                    :class="user.status === 'ENABLED' ? 'text-destructive' : 'text-primary'"
                    @click="toggleStatus(user)"
                  >
                    {{ user.status === 'ENABLED' ? '禁用' : '启用' }}
                  </button>
                  <button
                    v-permission="'iam:user:force-logout'"
                    type="button"
                    class="hover:underline"
                    @click="logoutTarget = user"
                  >
                    下线
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
            class="h-8 rounded-md border border-input bg-transparent px-2 text-sm outline-none focus-visible:border-ring"
            @change="changeSize"
          >
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
          </select>
        </label>
        <button
          type="button"
          class="inline-flex h-8 items-center rounded-md border border-input px-3 text-sm disabled:pointer-events-none disabled:opacity-40"
          :disabled="page <= 1 || loading"
          @click="previousPage"
        >
          上一页
        </button>
        <span class="min-w-16 text-center text-muted-foreground">{{ page }} / {{ totalPages }}</span>
        <button
          type="button"
          class="inline-flex h-8 items-center rounded-md border border-input px-3 text-sm disabled:pointer-events-none disabled:opacity-40"
          :disabled="page >= totalPages || loading"
          @click="nextPage"
        >
          下一页
        </button>
      </div>
    </div>
  </div>

  <AppDialog
    :open="formOpen"
    :title="editing ? '编辑用户' : '新增用户'"
    @close="formOpen = false"
  >
    <form id="user-form" class="space-y-4" @submit.prevent="save">
      <div class="space-y-1">
        <label class="text-sm font-medium" for="user-form-username">用户名</label>
        <input
          id="user-form-username"
          v-model="form.username"
          type="text"
          maxlength="64"
          :disabled="!!editing"
          class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm outline-none focus-visible:border-ring disabled:bg-muted"
        />
        <p v-if="editing" class="text-xs text-muted-foreground">用户名不可修改</p>
      </div>

      <div v-if="!editing" class="space-y-1">
        <label class="text-sm font-medium" for="user-form-password">初始密码</label>
        <input
          id="user-form-password"
          v-model="form.password"
          type="password"
          autocomplete="new-password"
          class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm outline-none focus-visible:border-ring"
        />
        <p class="text-xs text-muted-foreground">创建后用户首次登录必须修改密码</p>
      </div>

      <div class="space-y-1">
        <label class="text-sm font-medium" for="user-form-nickname">昵称</label>
        <input
          id="user-form-nickname"
          v-model="form.nickname"
          type="text"
          maxlength="64"
          class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm outline-none focus-visible:border-ring"
        />
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div class="space-y-1">
          <label class="text-sm font-medium" for="user-form-email">邮箱</label>
          <input
            id="user-form-email"
            v-model="form.email"
            type="email"
            maxlength="128"
            class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm outline-none focus-visible:border-ring"
          />
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium" for="user-form-phone">手机号</label>
          <input
            id="user-form-phone"
            v-model="form.phone"
            type="text"
            maxlength="32"
            class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm outline-none focus-visible:border-ring"
          />
        </div>
      </div>

      <div v-if="!editing" class="space-y-1">
        <span class="text-sm font-medium">初始角色</span>
        <div class="max-h-40 space-y-1 overflow-y-auto rounded-md border p-2">
          <label
            v-for="role in roleOptions"
            :key="role.id"
            class="flex cursor-pointer items-center gap-2 rounded px-1 py-1 text-sm hover:bg-accent"
          >
            <input
              type="checkbox"
              :checked="form.roleIds.includes(role.id)"
              @change="
                form.roleIds.includes(role.id)
                  ? form.roleIds.splice(form.roleIds.indexOf(role.id), 1)
                  : form.roleIds.push(role.id)
              "
            />
            {{ role.name }}
            <span class="font-mono text-xs text-muted-foreground">{{ role.code }}</span>
          </label>
          <p v-if="roleOptions.length === 0" class="px-1 py-2 text-xs text-muted-foreground">
            暂无可用角色
          </p>
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
        @click="formOpen = false"
      >
        取消
      </button>
      <button
        type="submit"
        form="user-form"
        :disabled="saving"
        class="inline-flex h-9 items-center rounded-md bg-primary px-4 text-sm text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
      >
        {{ saving ? '保存中...' : '保存' }}
      </button>
    </template>
  </AppDialog>

  <AppDialog
    :open="roleDialogOpen"
    :title="`分配角色 - ${roleTarget?.username ?? ''}`"
    @close="roleDialogOpen = false"
  >
    <div class="max-h-64 space-y-1 overflow-y-auto">
      <label
        v-for="role in roleOptions"
        :key="role.id"
        class="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-accent"
      >
        <input
          type="checkbox"
          :checked="selectedRoleIds.includes(role.id)"
          @change="toggleRole(role.id)"
        />
        <span class="font-medium">{{ role.name }}</span>
        <span class="font-mono text-xs text-muted-foreground">{{ role.code }}</span>
      </label>
      <p v-if="roleOptions.length === 0" class="px-2 py-3 text-sm text-muted-foreground">
        暂无可用角色
      </p>
    </div>
    <p v-if="roleError" class="mt-3 rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">
      {{ roleError }}
    </p>
    <template #footer>
      <button
        type="button"
        class="inline-flex h-9 items-center rounded-md border border-input px-4 text-sm hover:bg-accent"
        @click="roleDialogOpen = false"
      >
        取消
      </button>
      <button
        type="button"
        :disabled="roleSaving"
        class="inline-flex h-9 items-center rounded-md bg-primary px-4 text-sm text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
        @click="saveRoles"
      >
        {{ roleSaving ? '保存中...' : '保存' }}
      </button>
    </template>
  </AppDialog>

  <AppDialog
    :open="passwordDialogOpen"
    :title="`重置密码 - ${passwordTarget?.username ?? ''}`"
    @close="passwordDialogOpen = false"
  >
    <div class="space-y-2">
      <label class="text-sm font-medium" for="user-new-password">新密码</label>
      <input
        id="user-new-password"
        v-model="newPassword"
        type="password"
        autocomplete="new-password"
        class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm outline-none focus-visible:border-ring"
      />
      <p class="text-xs text-muted-foreground">
        重置后该用户全部会话失效，下次登录必须修改密码。
      </p>
    </div>
    <p
      v-if="passwordError"
      class="mt-3 rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive"
    >
      {{ passwordError }}
    </p>
    <template #footer>
      <button
        type="button"
        class="inline-flex h-9 items-center rounded-md border border-input px-4 text-sm hover:bg-accent"
        @click="passwordDialogOpen = false"
      >
        取消
      </button>
      <button
        type="button"
        :disabled="passwordSaving"
        class="inline-flex h-9 items-center rounded-md bg-primary px-4 text-sm text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
        @click="savePassword"
      >
        {{ passwordSaving ? '提交中...' : '重置密码' }}
      </button>
    </template>
  </AppDialog>

  <AppDialog
    :open="!!logoutTarget"
    :title="`强制下线 - ${logoutTarget?.username ?? ''}`"
    @close="logoutTarget = null"
  >
    <p class="text-sm text-muted-foreground">选择要下线的设备，操作立即生效。</p>
    <div class="mt-4 grid grid-cols-2 gap-2">
      <button
        type="button"
        class="inline-flex h-9 items-center justify-center rounded-md bg-destructive px-3 text-sm text-white hover:bg-destructive/90 disabled:opacity-50"
        :disabled="loggingOut"
        @click="doForceLogout()"
      >
        全部设备
      </button>
      <button
        v-for="device in ['WEB', 'APP', 'MINI']"
        :key="device"
        type="button"
        class="inline-flex h-9 items-center justify-center rounded-md border border-input px-3 text-sm hover:bg-accent disabled:opacity-50"
        :disabled="loggingOut"
        @click="doForceLogout(device)"
      >
        {{ device }}
      </button>
    </div>
  </AppDialog>
</template>

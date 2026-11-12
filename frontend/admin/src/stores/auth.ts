import { defineStore } from 'pinia'
import { ref } from 'vue'

import { fetchMe, login as loginApi, logout as logoutApi } from '@/api/auth'
import type { MenuNode, UserBrief } from '@/api/types'

interface SessionPayload {
  user: UserBrief
  superAdmin: boolean
  permissions: string[]
  menus: MenuNode[]
  mustChangePassword: boolean
}

/**
 * 认证状态只保存在内存中，页面刷新后由 /auth/me 恢复。
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserBrief | null>(null)
  const superAdmin = ref(false)
  const permissions = ref<string[]>([])
  const menus = ref<MenuNode[]>([])
  const mustChangePassword = ref(false)
  const initialized = ref(false)

  function setSession(payload: SessionPayload): void {
    user.value = payload.user
    superAdmin.value = payload.superAdmin
    permissions.value = payload.permissions
    menus.value = payload.menus
    mustChangePassword.value = payload.mustChangePassword
    initialized.value = true
  }

  async function initialize(): Promise<void> {
    if (initialized.value) {
      return
    }
    try {
      setSession(await fetchMe())
    } catch {
      clearSession()
    } finally {
      // 即使未登录也视为已初始化，避免路由守卫重复请求 /me
      initialized.value = true
    }
  }

  async function login(username: string, password: string): Promise<void> {
    const session = await loginApi(username, password)
    setSession(session)
  }

  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } catch {
      // 网络或服务端异常时仍需清理本地登录态
    } finally {
      reset()
    }
  }

  function hasPermission(permission: string): boolean {
    return superAdmin.value || permissions.value.includes(permission)
  }

  function clearSession(): void {
    user.value = null
    superAdmin.value = false
    permissions.value = []
    menus.value = []
    mustChangePassword.value = false
  }

  function reset(): void {
    clearSession()
    initialized.value = false
  }

  return {
    user,
    superAdmin,
    permissions,
    menus,
    mustChangePassword,
    initialized,
    setSession,
    initialize,
    login,
    logout,
    hasPermission,
    reset,
  }
})

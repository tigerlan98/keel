import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface AuthUser {
  id: number
  username: string
  nickname: string
}

interface SessionPayload {
  user: AuthUser
  superAdmin: boolean
  permissions: string[]
}

/**
 * 认证状态只保存在内存中，页面刷新后由 /auth/me 恢复。
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(null)
  const superAdmin = ref(false)
  const permissions = ref<string[]>([])
  const initialized = ref(false)

  function setSession(payload: SessionPayload): void {
    user.value = payload.user
    superAdmin.value = payload.superAdmin
    permissions.value = payload.permissions
    initialized.value = true
  }

  function hasPermission(permission: string): boolean {
    return superAdmin.value || permissions.value.includes(permission)
  }

  function reset(): void {
    user.value = null
    superAdmin.value = false
    permissions.value = []
    initialized.value = false
  }

  return { user, superAdmin, permissions, initialized, setSession, hasPermission, reset }
})

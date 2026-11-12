import type { Directive, DirectiveBinding } from 'vue'

import { useAuthStore } from '@/stores/auth'

type PermissionValue = string | string[]

function allowed(value: PermissionValue): boolean {
  const auth = useAuthStore()
  if (auth.superAdmin) {
    return true
  }
  const required = Array.isArray(value) ? value : [value]
  return required.some((permission) => auth.permissions.includes(permission))
}

function apply(el: HTMLElement, binding: DirectiveBinding<PermissionValue>): void {
  el.style.display = allowed(binding.value) ? '' : 'none'
}

/**
 * 按钮级权限：`v-permission="'iam:user:create'"`。超级管理员始终可见。
 */
export const permissionDirective: Directive<HTMLElement, PermissionValue> = {
  mounted: apply,
  updated: apply,
}

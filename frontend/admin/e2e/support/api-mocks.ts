import type { Page } from '@playwright/test'
import type { MenuNode } from '../../src/api/types'

export const session = {
  user: { id: 1, username: 'admin', nickname: '超级管理员' },
  superAdmin: true,
  permissions: ['iam:user:list'],
  menus: [],
  mustChangePassword: false,
}

export function ok(data: unknown) {
  return {
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 'OK', message: 'success', data }),
  }
}

export function unauthorized() {
  return {
    status: 401,
    contentType: 'application/json',
    body: JSON.stringify({
      code: 'COMMON_UNAUTHORIZED',
      message: '未登录或会话已失效',
      data: null,
    }),
  }
}

/** 已登录：/auth/me 返回有效会话；可通过 overrides 覆盖权限、菜单等字段。 */
export async function mockSession(
  page: Page,
  overrides: Partial<Omit<typeof session, 'menus'>> & { menus?: MenuNode[] } = {},
): Promise<void> {
  await page.route('**/api/v1/admin/audit/**', (route) =>
    route.fulfill(ok({ items: [], total: 0, page: 1, size: 100 })),
  )
  await page.route('**/api/v1/admin/iam/departments/options', (route) =>
    route.fulfill(
      ok([
        { id: 1, parentId: 0, name: '总公司', status: 'ENABLED', selectable: true, children: [] },
      ]),
    ),
  )
  await page.route('**/api/v1/admin/auth/me', (route) =>
    route.fulfill(ok({ ...session, ...overrides })),
  )
}

/** 未登录：/auth/me 返回 401。 */
export async function mockUnauthenticated(page: Page): Promise<void> {
  await page.route('**/api/v1/admin/auth/me', (route) => route.fulfill(unauthorized()))
}

/** 登录与写请求前的 CSRF token。 */
export async function mockCsrf(page: Page): Promise<void> {
  await page.route('**/api/v1/admin/auth/csrf', (route) =>
    route.fulfill(ok({ token: 'csrf-token' })),
  )
}

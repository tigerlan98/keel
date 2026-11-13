import type { Page } from '@playwright/test'

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
  overrides: Partial<typeof session> = {},
): Promise<void> {
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
  await page.route('**/api/v1/admin/auth/csrf', (route) => route.fulfill(ok({ token: 'csrf-token' })))
}

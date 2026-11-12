import { expect, test } from '@playwright/test'

import { mockCsrf, mockSession, mockUnauthenticated, ok, unauthorized } from './support/api-mocks'

test.describe('认证与会话', () => {
  test('未登录访问受保护页面跳转登录页', async ({ page }) => {
    await mockUnauthenticated(page)

    await page.goto('/iam/users')

    await expect(page).toHaveURL('/login?redirect=/iam/users')
    await expect(page.getByRole('heading', { name: 'Keel 管理端' })).toBeVisible()
  })

  test('登录成功后进入首页并提交 WEB 设备', async ({ page }) => {
    await mockCsrf(page)
    let loginBody: unknown
    await page.route('**/api/v1/admin/auth/login', async (route) => {
      loginBody = route.request().postDataJSON()
      await route.fulfill(ok(sessionPayload()))
    })

    await page.goto('/login')
    await page.getByLabel('用户名').fill('admin')
    await page.getByLabel('密码').fill('secret')
    await page.getByRole('button', { name: '登录' }).click()

    await expect(page).toHaveURL('/')
    await expect(page.getByText('超级管理员')).toBeVisible()
    expect(loginBody).toMatchObject({ username: 'admin', password: 'secret', device: 'WEB' })
  })

  test('登录失败时展示错误并停留在登录页', async ({ page }) => {
    await mockCsrf(page)
    await page.route('**/api/v1/admin/auth/login', (route) =>
      route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'AUTH_INVALID_CREDENTIALS',
          message: '用户名或密码错误',
          data: null,
        }),
      }),
    )

    await page.goto('/login')
    await page.getByLabel('用户名').fill('admin')
    await page.getByLabel('密码').fill('wrong')
    await page.getByRole('button', { name: '登录' }).click()

    await expect(page.getByText('用户名或密码错误')).toBeVisible()
    await expect(page).toHaveURL('/login')
    await expect(page.getByRole('button', { name: '登录' })).toBeEnabled()
  })

  test('退出登录后返回登录页', async ({ page }) => {
    await mockSession(page)
    await mockCsrf(page)
    await page.route('**/api/v1/admin/auth/logout', (route) =>
      route.fulfill({ status: 204, body: '' }),
    )

    await page.goto('/')
    await expect(page.getByText('超级管理员')).toBeVisible()
    await page.getByRole('button', { name: '退出登录' }).click()

    await expect(page).toHaveURL('/login')
  })

  test('会话失效的 401 由应用统一清理并跳转', async ({ page }) => {
    await mockSession(page)
    await page.route('**/api/v1/admin/iam/users*', (route) => route.fulfill(unauthorized()))

    await page.goto('/iam/users')

    await expect(page).toHaveURL('/login?redirect=/iam/users')
  })
})

function sessionPayload() {
  return {
    user: { id: 1, username: 'admin', nickname: '超级管理员' },
    superAdmin: true,
    permissions: ['iam:user:list'],
    menus: [],
    mustChangePassword: false,
  }
}

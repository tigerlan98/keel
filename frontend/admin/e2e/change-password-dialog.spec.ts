import { expect, test, type Page } from '@playwright/test'
import { mockCsrf, mockSession, ok } from './support/api-mocks'

test.beforeEach(async ({ page }) => {
  await mockSession(page)
  await mockCsrf(page)
  await page.route('**/api/v1/admin/iam/users*', (route) =>
    route.fulfill(ok({ items: [], total: 0, page: 1, size: 10 })),
  )
  await page.route('**/api/v1/admin/iam/departments/options', (route) => route.fulfill(ok([])))
})

async function openDialog(page: Page) {
  await page.getByRole('button', { name: '用户菜单：超级管理员' }).click()
  await page.getByRole('menuitem', { name: '修改密码', exact: true }).click()
  const dialog = page.getByRole('dialog', { name: '修改密码', exact: true })
  await expect(dialog).toBeVisible()
  return dialog
}

test('菜单打开改密弹窗，取消清空密码且留在当前页面', async ({ page }, testInfo) => {
  let requests = 0
  await page.route('**/api/v1/admin/auth/change-password', (route) => {
    requests += 1
    return route.fulfill({ status: 204, body: '' })
  })
  await page.goto('/iam/users')
  const dialog = await openDialog(page)
  await expect(dialog.getByRole('alert')).toHaveCount(0)
  await expect(dialog.locator('#change-password-current')).toBeFocused()
  await expect(dialog.getByLabel('当前密码')).toBeFocused()
  await page.screenshot({
    path: testInfo.outputPath('change-password-dialog.png'),
    animations: 'disabled',
  })
  await dialog.getByLabel('当前密码').fill('oldPassw0rd')
  await dialog.getByRole('button', { name: '取消', exact: true }).click()
  await expect(dialog).not.toBeVisible()
  await expect(page).toHaveURL('/iam/users')
  await openDialog(page)
  await expect(dialog.getByLabel('当前密码')).toHaveValue('')
  await page.keyboard.press('Escape')
  await expect(dialog).not.toBeVisible()
  expect(requests).toBe(0)
})

test('改密校验和服务端错误保留弹窗，成功后重新登录', async ({ page }) => {
  const currentPassword = ['Example', '2026', 'Old'].join('')
  const nextPassword = ['Example', '2026', 'New'].join('')
  const payloads: unknown[] = []
  await page.route('**/api/v1/admin/auth/change-password', (route) => {
    payloads.push(route.request().postDataJSON())
    return payloads.length === 1
      ? route.fulfill({
          status: 400,
          json: { code: 'IAM_INVALID_PASSWORD', message: '当前密码不正确', data: null },
        })
      : route.fulfill({ status: 204, body: '' })
  })
  await page.goto('/iam/users')
  const dialog = await openDialog(page)
  await dialog.getByRole('button', { name: '确认修改' }).click()
  await expect(dialog.getByText('请输入当前密码', { exact: true })).toBeVisible()
  await dialog.getByLabel('当前密码').fill(currentPassword)
  await dialog.locator('#change-password-new').fill('short1')
  await dialog.getByLabel('确认新密码').fill('different1')
  await dialog.getByRole('button', { name: '确认修改' }).click()
  await expect(dialog.getByText('密码至少 8 位', { exact: true })).toBeVisible()
  expect(payloads).toHaveLength(0)
  await dialog.locator('#change-password-new').fill(nextPassword)
  await dialog.getByLabel('确认新密码').fill(nextPassword)
  await dialog.getByRole('button', { name: '确认修改' }).click()
  await expect(dialog.getByRole('alert')).toHaveText('当前密码不正确')
  await expect(page).toHaveURL('/iam/users')
  await dialog.getByRole('button', { name: '确认修改' }).focus()
  await page.keyboard.press('Enter')
  await expect(page).toHaveURL('/login')
  expect(payloads).toEqual([
    { oldPassword: currentPassword, newPassword: nextPassword },
    { oldPassword: currentPassword, newPassword: nextPassword },
  ])
  await expect(page.locator('.toast-msg')).toHaveText('密码已修改，请重新登录')
})

test('窄屏修改密码弹窗完整显示', async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 375, height: 800 })
  await page.goto('/iam/users')
  const dialog = await openDialog(page)
  await expect(dialog.getByRole('button', { name: '确认修改' })).toBeInViewport()
  await expect(dialog.getByLabel('当前密码')).toBeInViewport()
  await page.screenshot({
    path: testInfo.outputPath('change-password-mobile.png'),
    animations: 'disabled',
  })
})

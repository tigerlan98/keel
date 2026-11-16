import { expect, test, type Page } from '@playwright/test'

import { mockCsrf, mockSession, ok } from './support/api-mocks'

const target = {
  id: 2,
  username: 'operator',
  nickname: '运营账号',
  departmentName: '研发中心',
  roles: [],
  status: 'ENABLED',
  createdAt: '2026-11-16T10:00:00.000',
  updatedAt: '2026-11-16T10:00:00.000',
  version: 3,
  mustChangePassword: false,
}

async function mockDeletion(page: Page, fail = false) {
  await mockSession(page)
  await mockCsrf(page)
  const requests: URL[] = []
  let deleted = false
  let deleteCount = 0
  await page.route('**/api/v1/admin/iam/users*', (route) => {
    const url = new URL(route.request().url())
    requests.push(url)
    const current = Number(url.searchParams.get('page') ?? 1)
    return route.fulfill(
      ok({
        items: current === 2 && !deleted ? [target] : [],
        total: deleted ? 10 : 11,
        page: current,
        size: 10,
      }),
    )
  })
  await page.route('**/api/v1/admin/iam/users/2*', (route) => {
    if (route.request().method() !== 'DELETE') return route.fulfill(ok(target))
    deleteCount += 1
    expect(new URL(route.request().url()).searchParams.get('version')).toBe('3')
    if (fail)
      return route.fulfill({
        status: 409,
        json: { code: 'COMMON_OPTIMISTIC_LOCK', message: '数据已变更，请刷新后重试', data: null },
      })
    deleted = true
    return route.fulfill({ status: 204, body: '' })
  })
  return { requests, deleteCount: () => deleteCount }
}

test('取消不删除；确认永久删除后空末页回退', async ({ page }, testInfo) => {
  const mock = await mockDeletion(page)
  await page.goto('/iam/users')
  await page.getByRole('button', { name: '下一页' }).click()
  await page.getByRole('button', { name: '删除', exact: true }).click()
  const dialog = page.getByRole('dialog', { name: '删除用户', exact: true })
  await expect(dialog).toContainText('此操作不可恢复')
  await dialog.getByRole('button', { name: '取消', exact: true }).click()
  await expect(dialog).not.toBeVisible()
  expect(mock.deleteCount()).toBe(0)
  await page.getByRole('button', { name: '删除', exact: true }).click()
  await expect(dialog).toBeVisible()
  await page.screenshot({
    path: testInfo.outputPath('user-delete-confirm.png'),
    animations: 'disabled',
  })
  await dialog.getByRole('button', { name: '删除', exact: true }).click()
  await expect(dialog).not.toBeVisible()
  await expect.poll(() => mock.requests.at(-1)?.searchParams.get('page')).toBe('1')
  expect(mock.deleteCount()).toBe(1)
  await expect(page.getByRole('button', { name: '强制下线', exact: true })).toHaveCount(0)
  await expect(page.getByRole('button', { name: '禁用', exact: true })).toHaveCount(0)
})

test('删除失败保留确认框与错误反馈', async ({ page }) => {
  const mock = await mockDeletion(page, true)
  await page.goto('/iam/users')
  await page.getByRole('button', { name: '下一页' }).click()
  await page.getByRole('button', { name: '删除', exact: true }).click()
  const dialog = page.getByRole('dialog', { name: '删除用户', exact: true })
  await dialog.getByRole('button', { name: '删除', exact: true }).click()
  await expect(dialog.getByRole('alert')).toHaveText('数据已变更，请刷新后重试')
  await expect(dialog.getByRole('button', { name: '删除', exact: true })).toBeEnabled()
  expect(mock.deleteCount()).toBe(1)
  await dialog.getByRole('button', { name: '取消', exact: true }).click()
  await expect(dialog).not.toBeVisible()
  await expect(page.getByRole('cell', { name: '运营账号 @operator', exact: true })).toBeVisible()
})

test('没有删除权限时不显示删除入口', async ({ page }) => {
  await mockDeletion(page)
  await mockSession(page, { superAdmin: false, permissions: ['iam:user:list'] })
  await page.goto('/iam/users')
  await page.getByRole('button', { name: '下一页' }).click()
  await expect(page.getByRole('cell', { name: '运营账号 @operator', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '删除', exact: true })).toHaveCount(0)
})

test('当前账号与超级管理员的删除按钮禁用', async ({ page }) => {
  await mockSession(page)
  await page.route('**/api/v1/admin/iam/users*', (route) =>
    route.fulfill(
      ok({
        items: [
          { ...target, id: 1, username: 'admin', nickname: '当前账号' },
          {
            ...target,
            id: 3,
            username: 'super',
            roles: [{ id: 1, code: 'SUPER_ADMIN', name: '超级管理员', status: 'ENABLED' }],
          },
          target,
        ],
        total: 3,
        page: 1,
        size: 10,
      }),
    ),
  )
  await page.goto('/iam/users')
  const buttons = page.getByRole('button', { name: '删除', exact: true })
  await expect(buttons).toHaveCount(3)
  await expect(buttons.nth(0)).toBeDisabled()
  await expect(buttons.nth(1)).toBeDisabled()
  await expect(buttons.nth(2)).toBeEnabled()
})

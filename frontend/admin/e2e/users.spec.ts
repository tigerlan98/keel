import { expect, test, type Page } from '@playwright/test'

import { mockSession, ok } from './support/api-mocks'

const users = [
  {
    id: 1,
    username: 'admin',
    nickname: '超级管理员',
    status: 'ENABLED',
    lastLoginAt: '2026-11-12T10:00:00.000',
    createdAt: '2026-11-01T09:00:00.000',
  },
  {
    id: 2,
    username: 'operator',
    nickname: '运营',
    status: 'DISABLED',
    lastLoginAt: null,
    createdAt: '2026-11-02T09:00:00.000',
  },
]

test.describe('用户列表', () => {
  test.beforeEach(async ({ page }) => {
    await mockSession(page)
  })

  test('渲染用户、状态与分页信息', async ({ page }) => {
    await mockUsers(page)

    await page.goto('/iam/users')

    const table = page.locator('tbody')
    await expect(table.getByRole('cell', { name: 'admin', exact: true })).toBeVisible()
    await expect(table.getByRole('cell', { name: 'operator', exact: true })).toBeVisible()
    await expect(table.locator('span').getByText('启用', { exact: true })).toBeVisible()
    await expect(table.locator('span').getByText('禁用', { exact: true })).toBeVisible()
    await expect(page.getByText('共 25 条')).toBeVisible()
    await expect(page.getByText('1 / 2')).toBeVisible()
  })

  test('查询时携带用户名前缀、状态与排序参数', async ({ page }) => {
    const requests: URL[] = []
    await mockUsers(page, requests)

    await page.goto('/iam/users')
    await page.getByLabel('用户名').fill('ali')
    await page.getByLabel('状态').selectOption('ENABLED')
    await page.getByRole('button', { name: '查询' }).click()

    await expect.poll(() => requests.length).toBeGreaterThan(1)
    const last = requests.at(-1)!
    expect(last.searchParams.get('page')).toBe('1')
    expect(last.searchParams.get('sort')).toBe('createdAt,desc')
    expect(last.searchParams.get('username')).toBe('ali')
    expect(last.searchParams.get('status')).toBe('ENABLED')
  })

  test('翻页后请求下一页并更新页码', async ({ page }) => {
    const requests: URL[] = []
    await mockUsers(page, requests)

    await page.goto('/iam/users')
    await expect(page.getByText('1 / 2')).toBeVisible()

    await page.getByRole('button', { name: '下一页' }).click()

    await expect(page.getByText('2 / 2')).toBeVisible()
    expect(requests.at(-1)?.searchParams.get('page')).toBe('2')
  })

  test('加载失败时展示错误并可重试', async ({ page }) => {
    let attempts = 0
    await page.route('**/api/v1/admin/iam/users*', (route) => {
      attempts += 1
      if (attempts === 1) {
        return route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'COMMON_INTERNAL_ERROR',
            message: '系统繁忙，请稍后重试',
            data: null,
          }),
        })
      }
      return route.fulfill(ok({ items: users, total: users.length, page: 1, size: 20 }))
    })

    await page.goto('/iam/users')
    await expect(page.getByText('系统繁忙，请稍后重试')).toBeVisible()

    await page.getByRole('button', { name: '重试' }).click()
    await expect(page.getByRole('cell', { name: 'admin', exact: true })).toBeVisible()
  })
})

async function mockUsers(page: Page, requests: URL[] = []) {
  await page.route('**/api/v1/admin/iam/users*', (route) => {
    const url = new URL(route.request().url())
    requests.push(url)
    const pageNumber = Number(url.searchParams.get('page') ?? '1')
    return route.fulfill(
      ok({
        items: pageNumber === 1 ? users : [users[1]],
        total: 25,
        page: pageNumber,
        size: 20,
      }),
    )
  })
}

import { expect, test, type Page } from '@playwright/test'

import { mockSession, ok } from './support/api-mocks'

const phase3Permissions = [
  'sys:config:list',
  'sys:config:update',
  'audit:operation:list',
  'audit:login:list',
]

const configs = [
  {
    id: 1,
    configKey: 'system.site.name',
    configValue: 'Keel',
    configGroup: 'SYSTEM',
    publicFlag: true,
    description: '站点名称',
    updatedAt: '2026-11-13T09:00:00.000',
    version: 0,
  },
  {
    id: 2,
    configKey: 'auth.access-token.ttl-minutes',
    configValue: '30',
    configGroup: 'AUTH',
    publicFlag: false,
    description: 'Web 会话空闲超时',
    updatedAt: '2026-11-12T09:00:00.000',
    version: 2,
  },
]

const configDetail = {
  id: 1,
  configKey: 'system.site.name',
  configValue: 'Keel',
  configGroup: 'SYSTEM',
  publicFlag: true,
  description: '站点名称',
  createdAt: '2026-11-01T09:00:00.000',
  createdBy: 1,
  updatedAt: '2026-11-13T09:00:00.000',
  updatedBy: 1,
  version: 0,
}

const operationLogs = [
  {
    id: 1,
    traceId: 'trace-1',
    userId: 1,
    username: 'admin',
    module: 'system',
    action: 'UPDATE',
    requestMethod: 'PUT',
    requestUri: '/api/v1/admin/system/configs/1',
    success: true,
    ip: '127.0.0.1',
    durationMs: 23,
    createdAt: '2026-11-13T10:00:00.000',
  },
  {
    id: 2,
    traceId: 'trace-2',
    userId: 2,
    username: 'operator',
    module: 'iam',
    action: 'STATUS_CHANGE',
    requestMethod: 'PUT',
    requestUri: '/api/v1/admin/iam/users/2/status',
    success: false,
    ip: '10.0.0.8',
    durationMs: 41,
    createdAt: '2026-11-13T09:30:00.000',
  },
]

const loginLogs = [
  {
    id: 1,
    traceId: 'login-1',
    userId: 1,
    username: 'admin',
    action: 'LOGIN',
    loginType: 'WEB',
    device: 'Chrome on Windows',
    ip: '127.0.0.1',
    success: true,
    createdAt: '2026-11-13T10:00:00.000',
  },
  {
    id: 2,
    traceId: 'login-2',
    userId: null,
    username: 'admin',
    action: 'LOGIN_FAILED',
    loginType: 'WEB',
    device: 'Chrome on Windows',
    ip: '10.0.0.8',
    success: false,
    failureReason: 'BAD_CREDENTIALS',
    createdAt: '2026-11-13T09:30:00.000',
  },
]

test.describe('Phase 3 系统参数与审计日志', () => {
  test.beforeEach(async ({ page }) => {
    await mockSession(page, { superAdmin: false, permissions: phase3Permissions })
  })

  test.describe('系统参数', () => {
    test('渲染参数列表并携带筛选与排序参数', async ({ page }) => {
      const { requests } = await mockConfigApi(page)

      await page.goto('/system/config')

      const table = page.locator('tbody')
      await expect(table.getByRole('cell', { name: 'system.site.name' })).toBeVisible()
      await expect(table.getByRole('cell', { name: 'auth.access-token.ttl-minutes' })).toBeVisible()
      await expect(table.getByText('公开', { exact: true })).toBeVisible()
      await expect(table.getByText('私有', { exact: true })).toBeVisible()
      await expect(page.getByText('共 2 条')).toBeVisible()

      await page.getByLabel('分组').selectOption('SYSTEM')
      await page.getByLabel('参数 Key').fill('system.')
      await page.getByRole('button', { name: '查询' }).click()

      await expect.poll(() => requests.length).toBeGreaterThan(1)
      const last = requests.at(-1)!
      expect(last.searchParams.get('page')).toBe('1')
      expect(last.searchParams.get('sort')).toBe('updatedAt,desc')
      expect(last.searchParams.get('group')).toBe('SYSTEM')
      expect(last.searchParams.get('keyword')).toBe('system.')
    })

    test('编辑参数时加载详情并提交更新', async ({ page }) => {
      const { updates } = await mockConfigApi(page)

      await page.goto('/system/config')
      await page
        .locator('tbody tr')
        .filter({ hasText: 'system.site.name' })
        .getByRole('button', { name: '编辑' })
        .click()

      const dialog = page.getByRole('dialog')
      await expect(dialog.getByText('编辑系统参数')).toBeVisible()
      await expect(dialog.getByLabel('参数 Key')).toHaveValue('system.site.name')

      await dialog.getByLabel('参数值').fill('Keel Portal')
      await dialog.getByLabel('说明').fill('门户标题')
      await dialog.getByRole('button', { name: '保存' }).click()

      await expect(page.getByText('参数已更新')).toBeVisible()
      expect(updates).toHaveLength(1)
      expect(updates[0]).toMatchObject({
        configValue: 'Keel Portal',
        description: '门户标题',
        publicFlag: true,
        version: 0,
      })
    })

    test('参数值为空时前端拦截提交', async ({ page }) => {
      const { updates } = await mockConfigApi(page)

      await page.goto('/system/config')
      await page
        .locator('tbody tr')
        .filter({ hasText: 'system.site.name' })
        .getByRole('button', { name: '编辑' })
        .click()

      const dialog = page.getByRole('dialog')
      await expect(dialog.getByText('编辑系统参数')).toBeVisible()
      await dialog.getByLabel('参数值').fill('')
      await dialog.getByRole('button', { name: '保存' }).click()

      await expect(dialog.getByText('请输入参数值')).toBeVisible()
      expect(updates).toHaveLength(0)
    })

    test('加载失败时展示错误并可重试', async ({ page }) => {
      await mockConfigApi(page, { listStatuses: [500] })

      await page.goto('/system/config')
      await expect(page.getByText('系统繁忙，请稍后重试')).toBeVisible()

      await page.getByRole('button', { name: '重试' }).click()
      await expect(page.getByRole('cell', { name: 'system.site.name' })).toBeVisible()
    })
  })

  test.describe('操作日志', () => {
    test('渲染日志并携带筛选参数', async ({ page }) => {
      const requests = await mockOperationLogApi(page)

      await page.goto('/audit/operation-logs')

      const table = page.locator('tbody')
      await expect(table.getByRole('cell', { name: 'admin', exact: true })).toBeVisible()
      await expect(table.getByText('PUT /api/v1/admin/system/configs/1')).toBeVisible()
      await expect(table.getByText('成功', { exact: true })).toBeVisible()
      await expect(table.getByText('失败', { exact: true })).toBeVisible()
      await expect(page.getByText('共 2 条')).toBeVisible()

      await page.getByLabel('操作人').fill('admin')
      await page.getByLabel('模块').selectOption('system')
      await page.getByLabel('动作').selectOption('UPDATE')
      await page.getByLabel('结果').selectOption('false')
      await page.getByRole('button', { name: '查询' }).click()

      await expect.poll(() => requests.length).toBeGreaterThan(1)
      const last = requests.at(-1)!
      expect(last.searchParams.get('page')).toBe('1')
      expect(last.searchParams.get('sort')).toBe('createdAt,desc')
      expect(last.searchParams.get('username')).toBe('admin')
      expect(last.searchParams.get('module')).toBe('system')
      expect(last.searchParams.get('action')).toBe('UPDATE')
      expect(last.searchParams.get('success')).toBe('false')
    })

    test('查看操作日志详情', async ({ page }) => {
      await mockOperationLogApi(page)

      await page.goto('/audit/operation-logs')
      await page
        .locator('tbody tr')
        .filter({ hasText: 'system/configs/1' })
        .getByRole('button', { name: '详情' })
        .click()

      const dialog = page.getByRole('dialog')
      await expect(dialog.getByText('操作日志详情')).toBeVisible()
      await expect(dialog.getByText('PUT /api/v1/admin/system/configs/1')).toBeVisible()
      await expect(dialog.getByText('trace-1')).toBeVisible()
      await expect(dialog.getByText('{"configValue":"Keel Portal"}')).toBeVisible()
    })
  })

  test.describe('登录日志', () => {
    test('渲染日志并携带筛选参数', async ({ page }) => {
      const requests = await mockLoginLogApi(page)

      await page.goto('/audit/login-logs')

      const table = page.locator('tbody')
      await expect(table.getByText('LOGIN_FAILED', { exact: true })).toBeVisible()
      await expect(table.getByText('BAD_CREDENTIALS', { exact: true })).toBeVisible()
      await expect(table.getByText('失败', { exact: true })).toBeVisible()
      await expect(page.getByText('共 2 条')).toBeVisible()

      await page.getByLabel('动作').selectOption('LOGIN_FAILED')
      await page.getByLabel('结果').selectOption('false')
      await page.getByLabel('IP').fill('10.0')
      await page.getByRole('button', { name: '查询' }).click()

      await expect.poll(() => requests.length).toBeGreaterThan(1)
      const last = requests.at(-1)!
      expect(last.searchParams.get('page')).toBe('1')
      expect(last.searchParams.get('sort')).toBe('createdAt,desc')
      expect(last.searchParams.get('action')).toBe('LOGIN_FAILED')
      expect(last.searchParams.get('success')).toBe('false')
      expect(last.searchParams.get('ip')).toBe('10.0')
    })

    test('查看登录日志详情', async ({ page }) => {
      await mockLoginLogApi(page)

      await page.goto('/audit/login-logs')
      await page
        .locator('tbody tr')
        .filter({ hasText: 'BAD_CREDENTIALS' })
        .getByRole('button', { name: '详情' })
        .click()

      const dialog = page.getByRole('dialog')
      await expect(dialog.getByText('登录日志详情')).toBeVisible()
      await expect(dialog.getByText('失败：BAD_CREDENTIALS')).toBeVisible()
      await expect(dialog.getByText(/Mozilla\/5\.0/)).toBeVisible()
    })
  })
})

interface ConfigApiMockOptions {
  listStatuses?: number[]
}

async function mockConfigApi(page: Page, options: ConfigApiMockOptions = {}) {
  const requests: URL[] = []
  const updates: unknown[] = []
  const listStatuses = [...(options.listStatuses ?? [])]

  await page.route('**/api/v1/admin/system/configs**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const isDetail = /\/configs\/\d+$/.test(url.pathname)

    if (request.method() === 'PUT' && isDetail) {
      const body = request.postDataJSON() as Record<string, unknown>
      updates.push(body)
      return route.fulfill(ok({ ...configDetail, ...body, updatedAt: '2026-11-13T10:00:00.000' }))
    }
    if (isDetail) {
      return route.fulfill(ok(configDetail))
    }

    requests.push(url)
    const status = listStatuses.shift()
    if (status) {
      return route.fulfill({
        status,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'COMMON_INTERNAL_ERROR',
          message: '系统繁忙，请稍后重试',
          data: null,
        }),
      })
    }
    return route.fulfill(ok({ items: configs, total: 2, page: 1, size: 20 }))
  })

  return { requests, updates }
}

async function mockOperationLogApi(page: Page) {
  const requests: URL[] = []

  await page.route('**/api/v1/admin/audit/operation-logs**', (route) => {
    const url = new URL(route.request().url())
    if (/\/operation-logs\/\d+$/.test(url.pathname)) {
      return route.fulfill(
        ok({
          ...operationLogs[0],
          method: 'ConfigController.update',
          requestParams: '{"configValue":"Keel Portal"}',
          userAgent: 'Mozilla/5.0',
        }),
      )
    }
    requests.push(url)
    return route.fulfill(ok({ items: operationLogs, total: 2, page: 1, size: 20 }))
  })

  return requests
}

async function mockLoginLogApi(page: Page) {
  const requests: URL[] = []

  await page.route('**/api/v1/admin/audit/login-logs**', (route) => {
    const url = new URL(route.request().url())
    if (/\/login-logs\/\d+$/.test(url.pathname)) {
      return route.fulfill(
        ok({ ...loginLogs[1], userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' }),
      )
    }
    requests.push(url)
    return route.fulfill(ok({ items: loginLogs, total: 2, page: 1, size: 20 }))
  })

  return requests
}

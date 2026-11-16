import { expect, test, type Page } from '@playwright/test'
import { mockCsrf, mockSession, ok } from './support/api-mocks'
import { chooseOption } from './support/controls'

const stamp = '2026-11-13T10:00:00.000'
const users = [
  {
    id: 1,
    username: 'admin',
    nickname: '超级管理员',
    status: 'ENABLED',
    createdAt: stamp,
    lastLoginAt: stamp,
  },
  {
    id: 2,
    username: 'demo',
    nickname: '测试账号',
    status: 'DISABLED',
    createdAt: stamp,
    lastLoginAt: null,
  },
]
const configs = [
  {
    id: 1,
    configKey: 'system.name',
    configValue: 'Keel',
    configGroup: 'SYSTEM',
    publicFlag: true,
    description: '系统名称',
    updatedAt: stamp,
    version: 0,
  },
  {
    id: 2,
    configKey: 'audit.login.retention-days',
    configValue: '365',
    configGroup: 'AUDIT',
    publicFlag: false,
    description: '登录日志保留天数',
    updatedAt: stamp,
    version: 0,
  },
  {
    id: 3,
    configKey: 'auth.max-login-failures',
    configValue: '5',
    configGroup: 'AUTH',
    publicFlag: false,
    description: '连续登录失败上限',
    updatedAt: stamp,
    version: 0,
  },
]
const operations = [
  {
    id: 1,
    username: 'admin',
    module: 'system',
    action: 'UPDATE',
    requestMethod: 'PUT',
    requestUri: '/api/v1/admin/system/configs/1',
    success: true,
    durationMs: 23,
    ip: '127.0.0.1',
    createdAt: stamp,
  },
  {
    id: 2,
    username: 'demo',
    module: 'iam',
    action: 'STATUS_CHANGE',
    requestMethod: 'PUT',
    requestUri: '/api/v1/admin/iam/users/2/status',
    success: false,
    durationMs: 41,
    ip: '10.0.0.8',
    createdAt: stamp,
  },
]
const logins = [
  {
    id: 1,
    username: 'admin',
    action: 'LOGIN',
    loginType: 'WEB',
    device: 'Chrome on Windows',
    ip: '127.0.0.1',
    success: true,
    createdAt: stamp,
  },
  {
    id: 2,
    username: 'demo',
    action: 'LOGIN_FAILED',
    loginType: 'WEB',
    device: 'Chrome on Windows',
    ip: '10.0.0.8',
    success: false,
    failureReason: 'BAD_CREDENTIALS',
    createdAt: stamp,
  },
]
const paths = [
  ['用户管理', '/iam/users'],
  ['角色管理', '/iam/roles'],
  ['系统参数', '/system/config'],
  ['操作日志', '/audit/operation-logs'],
  ['登录日志', '/audit/login-logs'],
] as const
async function mockControls(page: Page): Promise<URL[]> {
  const requests: URL[] = []
  await mockCsrf(page)
  await mockSession(page, {
    menus: [
      {
        id: 100,
        parentId: 0,
        type: 'DIRECTORY',
        code: 'ui:group',
        name: '系统管理',
        icon: 'settings',
        sort: 0,
        visible: true,
        children: paths.map(([name, path], index) => ({
          id: index + 101,
          parentId: 100,
          type: 'MENU',
          code: `ui:menu:${index}`,
          name,
          path,
          icon: index < 2 ? 'users' : 'file-clock',
          sort: index,
          visible: true,
          children: [],
        })),
      },
    ],
  })
  await page.route('**/api/v1/admin/iam/users*', (route) => {
    requests.push(new URL(route.request().url()))
    return route.fulfill(ok({ items: users, total: 40, page: 1, size: 20 }))
  })
  await page.route('**/api/v1/admin/iam/roles*', (route) =>
    route.fulfill(
      ok({
        items: [
          {
            id: 1,
            code: 'OPS_ADMIN',
            name: '运维管理员',
            status: 'ENABLED',
            sort: 1,
            createdAt: stamp,
          },
        ],
        total: 1,
        page: 1,
        size: 20,
      }),
    ),
  )
  await page.route('**/api/v1/admin/system/configs*', (route) =>
    route.fulfill(ok({ items: configs, total: configs.length, page: 1, size: 20 })),
  )
  await page.route('**/api/v1/admin/audit/operation-logs*', (route) =>
    route.fulfill(ok({ items: operations, total: operations.length, page: 1, size: 20 })),
  )
  await page.route('**/api/v1/admin/audit/login-logs*', (route) =>
    route.fulfill(ok({ items: logins, total: logins.length, page: 1, size: 20 })),
  )
  return requests
}

test('下拉框支持键盘、空值选项和数字分页值', async ({ page }) => {
  const requests = await mockControls(page)
  await page.goto('/iam/users')
  const status = page.getByRole('combobox', { name: '状态', exact: true })
  await status.focus()
  await page.keyboard.press('ArrowDown')
  await expect(page.getByRole('listbox')).toBeVisible()
  await expect(page.getByRole('option', { name: '全部', exact: true })).toBeFocused()
  await page.keyboard.press('End')
  await expect(page.getByRole('option', { name: '禁用', exact: true })).toBeFocused()
  await page.keyboard.press('Enter')
  await expect(status).toContainText('禁用')
  await expect(status).toBeFocused()
  await chooseOption(page, '状态', '全部')
  await expect(status).toContainText('全部')
  await page.getByRole('button', { name: '查询', exact: true }).click()
  await expect.poll(() => requests.length).toBeGreaterThan(1)
  expect(requests.at(-1)!.searchParams.has('status')).toBe(false)
  await chooseOption(page, '每页条数', '50 条/页')
  await expect.poll(() => requests.at(-1)!.searchParams.get('size')).toBe('50')
  expect(requests.at(-1)!.searchParams.get('page')).toBe('1')
})

test('角色弹窗状态开关可操作，Escape 关闭弹窗', async ({ page }, testInfo) => {
  await page.addInitScript(() => localStorage.setItem('keel.appearance.theme', 'dark'))
  await mockControls(page)
  await page.route('**/api/v1/admin/iam/resources/tree*', (route) => route.fulfill(ok([])))
  await page.route('**/api/v1/admin/iam/users*', (route) =>
    route.fulfill(ok({ items: users, total: users.length, page: 1, size: 100 })),
  )
  await page.route('**/api/v1/admin/iam/roles/*/resources', (route) =>
    route.fulfill(ok({ resourceIds: [] })),
  )
  await page.goto('/iam/roles')
  await page.getByRole('button', { name: '新增角色', exact: true }).click()
  const dialog = page.getByRole('dialog', { name: '新增角色' })
  const status = dialog.getByRole('switch', { name: '角色启用状态', exact: true })
  await expect(status).toBeChecked()
  await status.click()
  await expect(status).not.toBeChecked()
  await page.screenshot({
    path: testInfo.outputPath('modal-select-dark.png'),
    animations: 'disabled',
  })
  await page.keyboard.press('Escape')
  await expect(dialog).not.toBeVisible()
})

for (const theme of ['light', 'dark'] as const) {
  test(`下拉框视觉 ${theme} 桌面与窄屏`, async ({ page }, testInfo) => {
    await page.addInitScript((mode) => localStorage.setItem('keel.appearance.theme', mode), theme)
    await mockControls(page)
    await page.setViewportSize({ width: 1600, height: 1000 })
    await page.goto('/iam/users')
    await expect(page.locator('tbody')).toContainText('admin')
    await page.getByRole('combobox', { name: '状态', exact: true }).click()
    await expect(page.getByRole('listbox')).toHaveCSS('z-index', '120')
    await expect(page.getByRole('listbox')).not.toHaveCSS('background-color', 'rgba(0, 0, 0, 0)')
    await expect(page.getByRole('option', { name: '全部', exact: true })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await page.getByRole('option', { name: '禁用', exact: true }).hover()
    await page.screenshot({
      path: testInfo.outputPath(`select-${theme}-desktop.png`),
      animations: 'disabled',
    })
    await page.keyboard.press('Escape')
    await page.setViewportSize({ width: 390, height: 844 })
    await page.getByRole('combobox', { name: '每页条数', exact: true }).click()
    await expect(page.getByRole('option', { name: '100 条/页', exact: true })).toBeVisible()
    const box = await page.getByRole('listbox').boundingBox()
    expect(box!.x).toBeGreaterThanOrEqual(0)
    expect(box!.x + box!.width).toBeLessThanOrEqual(390)
    expect(box!.y + box!.height).toBeLessThanOrEqual(844)
    await page.screenshot({
      path: testInfo.outputPath(`select-${theme}-mobile.png`),
      animations: 'disabled',
    })
  })
  for (const [name, path] of paths.slice(2)) {
    test(`统一表格视觉 ${theme} ${name} 桌面与窄屏`, async ({ page }, testInfo) => {
      await page.addInitScript((mode) => localStorage.setItem('keel.appearance.theme', mode), theme)
      await mockControls(page)
      await page.setViewportSize({ width: 1600, height: 1000 })
      await page.goto(path)
      await expect(page.getByRole('heading', { name, exact: true })).toBeVisible()
      await expect(page.locator('tbody')).toContainText(
        name === '系统参数' ? 'system.name' : 'admin',
      )
      await expect(page.locator('.app-panel .data-table')).toHaveCount(1)
      await page.screenshot({
        path: testInfo.outputPath(`table-${theme}-desktop.png`),
        animations: 'disabled',
      })
      await page.setViewportSize({ width: 390, height: 844 })
      await expect
        .poll(() => page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
        .toBe(true)
      await page.screenshot({
        path: testInfo.outputPath(`table-${theme}-mobile.png`),
        animations: 'disabled',
      })
    })
  }
}

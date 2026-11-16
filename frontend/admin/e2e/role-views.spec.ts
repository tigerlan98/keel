import { expect, test, type Page } from '@playwright/test'
import { mockCsrf, mockSession, ok } from './support/api-mocks'

const role = (id: number, name: string, code: string, status = 'ENABLED') => ({
  id,
  name,
  code,
  status,
  sort: id,
  description: `${name}职责`,
  createdAt: '2026-11-16T10:00:00.000',
  version: 0,
})
const roles = [
  role(1, '超级管理员', 'SUPER_ADMIN'),
  role(2, '运维管理员', 'OPS'),
  role(3, '审计员', 'AUDITOR'),
  role(4, '闲置角色', 'IDLE'),
  role(5, '停用角色', 'OFF', 'DISABLED'),
]
const resource = (id: number, name: string, code: string, type: string, parentId = 0) => ({
  id,
  name,
  code,
  type,
  parentId,
  status: 'ENABLED',
  sort: id,
  visible: true,
  children: [] as unknown[],
})
const resources = [resource(1, '系统管理', 'iam:system', 'DIRECTORY')]
resources[0]!.children = [
  resource(2, '用户管理', 'iam:user:list', 'MENU', 1),
  resource(4, '角色管理', 'iam:role:list', 'MENU', 1),
  resource(5, '部门管理', 'iam:department:list', 'MENU', 1),
]
;(resources[0]!.children[0] as ReturnType<typeof resource>).children = [
  resource(3, '删除用户', 'iam:user:delete', 'BUTTON', 2),
]
const user = (id: number, name: string, roleIds: number[], status = 'ENABLED') => ({
  id,
  username: `user${id}`,
  nickname: name,
  status,
  departmentName: '总公司',
  createdAt: '2026-11-16T10:00:00.000',
  roles: roles.filter((item) => roleIds.includes(item.id)),
})
const users = [
  user(1, '系统管理员', [1]),
  user(2, '运维用户', [2]),
  user(3, '审计用户', [3]),
  user(4, '待授权用户', []),
  user(5, '停用用户', [2], 'DISABLED'),
]

async function mockGraph(
  page: Page,
  options: { partialFailure?: boolean; userCount?: number } = {},
) {
  await mockSession(page)
  await mockCsrf(page)
  const grants: Record<number, number[]> = { 1: [], 2: [1, 2, 3], 3: [1, 2], 4: [], 5: [3] }
  const writes: { roleId: number; ids: number[] }[] = []
  const userPages: number[] = []
  const rows = options.userCount
    ? Array.from({ length: options.userCount }, (_, i) => user(i + 1, `测试用户${i + 1}`, [2]))
    : users
  let rejected = false
  await page.route('**/api/v1/admin/iam/resources/tree*', (route) => route.fulfill(ok(resources)))
  await page.route('**/api/v1/admin/iam/roles**', (route) => {
    const url = new URL(route.request().url())
    const match = url.pathname.match(/roles\/(\d+)\/resources$/)
    if (match) {
      const roleId = Number(match[1])
      if (route.request().method() === 'PUT') {
        const ids = route.request().postDataJSON().resourceIds as number[]
        writes.push({ roleId, ids })
        if (options.partialFailure && roleId === 3 && !rejected) {
          rejected = true
          return route.fulfill({
            status: 409,
            json: { code: 'CONFLICT', message: '授权发生冲突，请重试', data: null },
          })
        }
        grants[roleId] = ids
        return route.fulfill({ status: 204, body: '' })
      }
      return route.fulfill(ok({ resourceIds: grants[roleId] ?? [] }))
    }
    const id = Number(url.pathname.split('/').at(-1))
    if (id) return route.fulfill(ok(roles.find((item) => item.id === id)))
    return route.fulfill(ok({ items: roles, total: roles.length, page: 1, size: 100 }))
  })
  await page.route('**/api/v1/admin/iam/users*', (route) => {
    const url = new URL(route.request().url())
    const current = Number(url.searchParams.get('page') ?? 1)
    const size = Number(url.searchParams.get('size') ?? 100)
    userPages.push(current)
    return route.fulfill(
      ok({
        items: rows.slice((current - 1) * size, current * size),
        total: rows.length,
        page: current,
        size,
      }),
    )
  })
  return { writes, userPages }
}

test('三种角色视图、真实计数、下钻与本地权限查询', async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 1600, height: 1000 })
  await mockGraph(page)
  await page.goto('/iam/roles')
  await expect(page.getByRole('cell', { name: '运维管理员 OPS' })).toBeVisible()
  await page.screenshot({ path: testInfo.outputPath('roles-list.png'), animations: 'disabled' })
  await page.getByRole('button', { name: '溯源', exact: true }).click()
  await expect(page.locator('.permission-trace')).toBeVisible()
  await expect(page.locator('.trace-legend')).toContainText('无角色用户')
  await expect(page.getByRole('button', { name: '角色，5，点击查看' })).toBeVisible()
  await page.screenshot({ path: testInfo.outputPath('roles-trace.png'), animations: 'disabled' })
  await page.getByRole('button', { name: '谁能删除用户？', exact: true }).click()
  await expect(page.locator('.trace-answer')).toContainText('2 个启用角色、2 个启用用户')
  await expect(page.locator('.trace-answer')).not.toContainText('停用用户')
  await page.getByRole('button', { name: '关闭查询结果' }).click()
  await page.getByRole('button', { name: '哪些用户还没分配角色？', exact: true }).click()
  await expect(page.locator('.trace-answer')).toContainText('1 个用户尚未分配角色')
  await page
    .locator('.trace-answer')
    .getByRole('button', { name: '待授权用户', exact: true })
    .click()
  await expect(page).toHaveURL(/trace=user(?::|%3A)4/)
  await expect(page.locator('.trace-insight')).toContainText('待授权用户')
  await page.screenshot({ path: testInfo.outputPath('roles-detail.png'), animations: 'disabled' })
  await page.getByRole('button', { name: '矩阵', exact: true }).click()
  await expect(
    page.getByRole('checkbox', { name: '超级管理员：删除用户', exact: true }),
  ).toBeDisabled()
  await page.screenshot({ path: testInfo.outputPath('roles-matrix.png'), animations: 'disabled' })
})

test('矩阵未保存保护、分角色部分成功及失败后重试', async ({ page }, testInfo) => {
  const state = await mockGraph(page, { partialFailure: true })
  await page.goto('/iam/roles')
  await page.getByRole('button', { name: '矩阵', exact: true }).click()
  await page.getByRole('checkbox', { name: '运维管理员：角色管理', exact: true }).click()
  await page.getByRole('checkbox', { name: '审计员：角色管理', exact: true }).click()
  await page.screenshot({
    path: testInfo.outputPath('roles-matrix-unsaved.png'),
    animations: 'disabled',
  })
  await page.getByRole('button', { name: '列表', exact: true }).click()
  const confirm = page.getByRole('dialog', { name: '放弃未保存的改动' })
  await expect(confirm).toBeVisible()
  await confirm.getByRole('button', { name: '取消', exact: true }).click()
  await expect(confirm).not.toBeVisible()
  await expect(
    page.getByRole('checkbox', { name: '运维管理员：角色管理', exact: true }),
  ).toBeChecked()
  await page.getByRole('button', { name: '保存权限', exact: true }).click()
  await expect(page.locator('.toast-msg')).toContainText('已保存 1 个角色')
  await expect(page.locator('.matrix-save-bar')).toContainText('未保存 1 项改动')
  await page.getByRole('button', { name: '保存权限', exact: true }).click()
  await expect(page.locator('.matrix-save-bar')).toHaveCount(0)
  expect(state.writes.map((item) => item.roleId)).toEqual([2, 3, 3])
})

test('没有用户权限不读取用户，矩阵无写权限时只读', async ({ page }) => {
  const state = await mockGraph(page)
  await mockSession(page, {
    superAdmin: false,
    permissions: ['iam:role:list', 'iam:resource:list'],
  })
  await page.goto('/iam/roles')
  await page.getByRole('button', { name: '溯源', exact: true }).click()
  await expect(page.locator('.trace-visibility')).toContainText('无读取权限')
  await page.getByRole('button', { name: '哪些用户还没分配角色？', exact: true }).click()
  await expect(page.locator('.trace-answer')).toContainText('没有用户列表权限')
  expect(state.userPages).toEqual([])
  await page.getByRole('button', { name: '矩阵', exact: true }).click()
  await expect(
    page.getByRole('checkbox', { name: '运维管理员：删除用户', exact: true }),
  ).toBeDisabled()
  expect(state.writes).toEqual([])
})

test('统计读取完整分页，窄屏图谱与深色矩阵可用', async ({ page }, testInfo) => {
  const state = await mockGraph(page, { userCount: 101 })
  await page.setViewportSize({ width: 375, height: 900 })
  await page.goto('/iam/roles')
  await page.getByRole('button', { name: '溯源', exact: true }).click()
  await expect(page.getByRole('button', { name: '用户，101，点击查看' })).toBeVisible()
  expect(state.userPages).toEqual([1, 2])
  await page.screenshot({
    path: testInfo.outputPath('roles-trace-mobile.png'),
    animations: 'disabled',
    fullPage: true,
  })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(
    true,
  )
  await page.getByRole('button', { name: '矩阵', exact: true }).click()
  await page.evaluate(() => document.documentElement.classList.add('dark'))
  await page.screenshot({
    path: testInfo.outputPath('roles-matrix-dark-mobile.png'),
    animations: 'disabled',
    fullPage: true,
  })
})

test('关系读取失败支持重试，无资源权限不请求资源树', async ({ page }) => {
  await mockGraph(page)
  let fail = true
  let treeReads = 0
  await page.route('**/api/v1/admin/iam/resources/tree*', (route) => {
    treeReads += 1
    return fail
      ? route.fulfill({ status: 500, json: { code: 'ERROR', message: '资源加载失败', data: null } })
      : route.fulfill(ok(resources))
  })
  await page.goto('/iam/roles')
  await page.getByRole('button', { name: '溯源', exact: true }).click()
  await expect(page.getByText('资源加载失败', { exact: true })).toBeVisible()
  fail = false
  await page.getByRole('button', { name: '重试', exact: true }).click()
  await expect(page.locator('.permission-trace')).toBeVisible()
  await mockSession(page, { superAdmin: false, permissions: ['iam:role:list'] })
  await page.reload()
  const before = treeReads
  await page.getByRole('button', { name: '矩阵', exact: true }).click()
  await expect(page.getByText('查看矩阵和溯源需要资源列表权限', { exact: true })).toBeVisible()
  expect(treeReads).toBe(before)
})

test('角色编辑、授权侧栏和受保护删除保持原规则', async ({ page }, testInfo) => {
  const state = await mockGraph(page)
  let updated: Record<string, unknown> | undefined
  await page.route('**/api/v1/admin/iam/roles/2', (route) => {
    if (route.request().method() === 'PUT') {
      updated = route.request().postDataJSON()
      return route.fulfill(ok({ ...roles[1], ...updated, version: 1 }))
    }
    if (route.request().method() === 'DELETE')
      return route.fulfill({
        status: 409,
        json: { code: 'IAM_ROLE_IN_USE', message: '角色已被用户引用，不能删除', data: null },
      })
    return route.fulfill(ok(roles[1]))
  })
  await page.goto('/iam/roles')
  const builtin = page.getByRole('row').filter({ hasText: 'SUPER_ADMIN' })
  await expect(builtin.getByText('全部权限', { exact: true })).toBeVisible()
  await expect(builtin.getByRole('button')).toHaveCount(0)
  const row = page.getByRole('row').filter({ hasText: 'OPS' })
  await row.getByRole('button', { name: '编辑', exact: true }).click()
  const edit = page.getByRole('dialog', { name: '编辑角色', exact: true })
  await expect(edit.locator('#role-code')).toBeDisabled()
  await edit.locator('#role-name').fill('运维角色')
  await page.screenshot({ path: testInfo.outputPath('role-edit.png'), animations: 'disabled' })
  await edit.getByRole('button', { name: '保存', exact: true }).click()
  await expect(edit).not.toBeVisible()
  expect(updated).toMatchObject({ name: '运维角色', version: 0, status: 'ENABLED' })
  await row.getByRole('button', { name: '授权', exact: true }).click()
  const drawer = page.getByRole('dialog', { name: '分配权限 · 运维管理员', exact: true })
  await expect(drawer.getByRole('checkbox').first()).toBeVisible()
  await page.screenshot({
    path: testInfo.outputPath('role-permission-drawer.png'),
    animations: 'disabled',
  })
  await drawer.getByRole('checkbox', { name: /角色管理/ }).check()
  await drawer.getByRole('button', { name: '保存权限', exact: true }).click()
  await expect(drawer).not.toBeVisible()
  expect(state.writes[0]).toMatchObject({ roleId: 2, ids: expect.arrayContaining([1, 2, 3, 4]) })
  await row.getByRole('button', { name: '删除', exact: true }).click()
  const remove = page.getByRole('dialog', { name: '删除角色', exact: true })
  await remove.getByRole('button', { name: '删除', exact: true }).click()
  await expect(remove.getByRole('alert')).toHaveText('角色已被用户引用，不能删除')
})

test('空关系、键盘下钻和图谱暂停重播可用', async ({ page }) => {
  await mockGraph(page)
  await page.goto('/iam/roles')
  await page.getByRole('button', { name: '溯源', exact: true }).click()
  await page.getByRole('button', { name: '暂停', exact: true }).click()
  expect(
    await page
      .locator('.trace-stage')
      .evaluate((element) => (element as SVGSVGElement).animationsPaused()),
  ).toBe(true)
  await page.getByRole('button', { name: '继续', exact: true }).click()
  expect(
    await page
      .locator('.trace-stage')
      .evaluate((element) => (element as SVGSVGElement).animationsPaused()),
  ).toBe(false)
  await page.getByRole('button', { name: '重播', exact: true }).click()
  await page.getByRole('button', { name: '角色，5，点击查看' }).focus()
  await page.keyboard.press('Enter')
  await expect(page.getByRole('button', { name: /^运维管理员，/ })).toBeVisible()
  await page.getByRole('button', { name: /^运维管理员，/ }).focus()
  await page.keyboard.press('Escape')
  await expect(page.locator('.trace-stage')).toBeVisible()
  await page.route('**/api/v1/admin/iam/roles*', (route) =>
    route.fulfill(ok({ items: [], total: 0, page: 1, size: 100 })),
  )
  await page.route('**/api/v1/admin/iam/users*', (route) =>
    route.fulfill(ok({ items: [], total: 0, page: 1, size: 100 })),
  )
  await page.route('**/api/v1/admin/iam/resources/tree*', (route) => route.fulfill(ok([])))
  await page.reload()
  await page.getByRole('button', { name: '矩阵', exact: true }).click()
  await expect(page.getByText('暂无角色，请先新增角色', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '溯源', exact: true }).click()
  await expect(page.getByRole('button', { name: '角色，0，点击查看' })).toBeVisible()
})

test('溯源分类采用表盘与扇形胶囊，返回与窄屏可用', async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 1800, height: 1000 })
  await mockGraph(page)
  await page.goto('/iam/roles')
  await page.getByRole('button', { name: '溯源', exact: true }).click()
  await page.getByRole('button', { name: '角色，5，点击查看' }).click()
  await expect(page.locator('.module-capsule')).toHaveCount(5)
  await expect(
    page.getByRole('button', { name: '运维管理员，2 人持有 · 1 个页面', exact: true }),
  ).toBeVisible()
  await expect(page.locator('.trace-controls')).toBeVisible()
  await page.screenshot({
    path: testInfo.outputPath('roles-trace-role-module.png'),
    animations: 'disabled',
  })
  await page.getByRole('button', { name: '暂停', exact: true }).click()
  expect(
    await page
      .locator('.module-stage')
      .evaluate((element) => (element as SVGSVGElement).animationsPaused()),
  ).toBe(true)
  await page.getByRole('button', { name: '重播', exact: true }).click()
  await page.getByRole('button', { name: '运维管理员，2 人持有 · 1 个页面', exact: true }).click()
  await expect(page.locator('.trace-insight')).toContainText('运维管理员')
  await page.getByRole('button', { name: '返回上一层' }).click()
  await expect(page.locator('.module-capsule')).toHaveCount(5)
  await page.setViewportSize({ width: 375, height: 800 })
  await expect(
    page.getByRole('button', { name: '运维管理员，2 人持有 · 1 个页面', exact: true }),
  ).toBeVisible()
  await page.screenshot({
    path: testInfo.outputPath('roles-trace-role-module-mobile.png'),
    animations: 'disabled',
    fullPage: true,
  })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(
    true,
  )
  await page.getByRole('button', { name: '返回权限全景', exact: true }).click()
  await expect(page.locator('.trace-stage')).toBeVisible()
})

test('溯源分类超过显示容量仍可搜索全部用户', async ({ page }) => {
  await mockGraph(page, { userCount: 101 })
  await page.goto('/iam/roles')
  await page.getByRole('button', { name: '溯源', exact: true }).click()
  await page.getByRole('button', { name: '用户，101，点击查看' }).click()
  await page.getByRole('button', { name: /查看全部/ }).click()
  await page.getByRole('textbox', { name: '搜索用户', exact: true }).fill('测试用户101')
  await page
    .locator('.module-all')
    .getByRole('button', { name: /测试用户101/ })
    .click()
  await expect(page.locator('.trace-insight')).toContainText('测试用户101')
})

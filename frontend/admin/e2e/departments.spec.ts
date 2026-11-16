import { expect, test } from '@playwright/test'
import { mockSession, mockCsrf, ok } from './support/api-mocks'
import type { DepartmentNode } from '../src/api/departments'

function tree(): DepartmentNode[] {
  return [
    {
      id: 1,
      parentId: 0,
      code: 'ROOT',
      name: '总公司',
      sort: 0,
      status: 'ENABLED',
      version: 0,
      directUserCount: 0,
      children: [
        {
          id: 2,
          parentId: 1,
          code: 'RD',
          name: '研发中心',
          sort: 10,
          status: 'ENABLED',
          version: 0,
          directUserCount: 1,
          children: [],
        },
        {
          id: 3,
          parentId: 1,
          code: 'OLD',
          name: '停用部门',
          sort: 20,
          status: 'DISABLED',
          version: 0,
          directUserCount: 0,
          children: [],
        },
      ],
    },
  ]
}
const user = {
  id: 2,
  username: 'zhangsan',
  nickname: '张三',
  status: 'ENABLED',
  departmentId: 2,
  departmentName: '研发中心',
  phone: '13800000002',
  roles: [],
  createdAt: '2026-11-15T10:00:00.000',
  version: 0,
  mustChangePassword: false,
  updatedAt: '2026-11-15T10:00:00.000',
}
test.beforeEach(async ({ page }) => {
  await mockSession(page)
  await mockCsrf(page)
  await page.route('**/api/v1/admin/iam/departments/options', (route) =>
    route.fulfill(
      ok(
        tree().map((root) => ({
          ...root,
          selectable: true,
          children: root.children.map((child) => ({
            ...child,
            selectable: child.status === 'ENABLED',
          })),
        })),
      ),
    ),
  )
  await page.route('**/api/v1/admin/iam/roles*', (route) =>
    route.fulfill(ok({ items: [], total: 0, page: 1, size: 100 })),
  )
})
test('部门树筛选、角色展示与移动端筛选可用', async ({ page }, testInfo) => {
  const requests: URL[] = []
  await page.route('**/api/v1/admin/iam/users*', (route) => {
    requests.push(new URL(route.request().url()))
    return route.fulfill(ok({ items: [user], total: 1, page: 1, size: 20 }))
  })
  await page.goto('/iam/users')
  await expect(page.getByRole('cell', { name: '张三 @zhangsan' })).toBeVisible()
  await page
    .getByRole('navigation', { name: '部门筛选' })
    .getByRole('button', { name: '研发中心', exact: true })
    .click()
  await expect.poll(() => requests.at(-1)?.searchParams.get('departmentId')).toBe('2')
  await expect(page.getByRole('button', { name: '未分配部门', exact: true })).toHaveCount(0)
  await page
    .getByRole('navigation', { name: '部门筛选' })
    .getByRole('button', { name: '全部部门', exact: true })
    .click()
  await expect.poll(() => requests.at(-1)?.searchParams.has('departmentId')).toBe(false)
  expect(requests.at(-1)?.searchParams.has('unassignedDepartment')).toBe(false)
  await page.screenshot({ path: testInfo.outputPath('users-desktop.png'), fullPage: true })
  await page.setViewportSize({ width: 375, height: 812 })
  await page.getByRole('button', { name: '按部门筛选', exact: true }).click()
  await expect(
    page
      .getByRole('dialog', { name: '按部门筛选' })
      .getByRole('button', { name: '未分配部门', exact: true }),
  ).toHaveCount(0)
  await page
    .getByRole('dialog', { name: '按部门筛选' })
    .getByRole('button', { name: '研发中心', exact: true })
    .click()
  await expect.poll(() => requests.at(-1)?.searchParams.get('departmentId')).toBe('2')
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await expect(page.getByRole('dialog', { name: '按部门筛选' })).not.toBeVisible()
  await page.screenshot({ path: testInfo.outputPath('users-mobile.png'), fullPage: true })
})
test('新增用户必须选部门，角色可空，密码可显隐', async ({ page }, testInfo) => {
  let payload: Record<string, unknown> | undefined
  await page.route('**/api/v1/admin/iam/users*', (route) => {
    if (route.request().method() === 'POST') {
      payload = route.request().postDataJSON()
      return route.fulfill({ ...ok(user), status: 201 })
    }
    return route.fulfill(ok({ items: [user], total: 1, page: 1, size: 20 }))
  })
  await page.goto('/iam/users')
  await page.getByRole('button', { name: '新增用户', exact: true }).click()
  const dialog = page.getByRole('dialog', { name: '新增用户', exact: true })
  await expect(dialog.getByRole('alert')).toHaveCount(0)
  await dialog.getByLabel('登录账号', { exact: false }).fill('newuser')
  await dialog.getByLabel('显示名称', { exact: false }).fill('新用户')
  await dialog.locator('#user-form-password').fill('NewPass123!')
  await dialog.getByRole('button', { name: '显示初始密码' }).click()
  await expect(dialog.locator('#user-form-password')).toHaveAttribute('type', 'text')
  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  await expect(dialog.getByText('请选择所属部门', { exact: true })).toBeVisible()
  expect(payload).toBeUndefined()
  await dialog.locator('#user-form-department').click()
  await page
    .getByRole('dialog', { name: '选择部门', exact: true })
    .getByRole('button', { name: '研发中心', exact: true })
    .click()
  await expect(page.getByRole('dialog', { name: '选择部门', exact: true })).not.toBeVisible()
  await page.screenshot({ path: testInfo.outputPath('user-create.png'), fullPage: true })
  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  await expect.poll(() => payload?.departmentId).toBe(2)
  expect(payload?.roleIds).toEqual([])
  expect(payload?.status).toBe('ENABLED')
  await expect(dialog).not.toBeVisible()
})
test('用户资料使用profile原子保存，旧账号需补齐部门', async ({ page }) => {
  let payload: Record<string, unknown> | undefined
  await page.route('**/api/v1/admin/iam/users**', (route) => {
    const path = new URL(route.request().url()).pathname
    if (path.endsWith('/profile')) {
      payload = route.request().postDataJSON()
      return route.fulfill(ok(user))
    }
    if (path.endsWith('/2'))
      return route.fulfill(ok({ ...user, departmentId: undefined, departmentName: undefined }))
    return route.fulfill(ok({ items: [user], total: 1, page: 1, size: 20 }))
  })
  await page.goto('/iam/users')
  await page.getByRole('button', { name: '编辑', exact: true }).click()
  const dialog = page.getByRole('dialog', { name: '编辑用户' })
  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  await expect(dialog.getByText('请选择所属部门', { exact: true })).toBeVisible()
  await dialog.locator('#user-form-department').click()
  await page
    .getByRole('dialog', { name: '选择部门', exact: true })
    .getByRole('button', { name: '研发中心', exact: true })
    .click()
  await dialog.getByLabel('电子邮箱').fill('')
  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  await expect.poll(() => payload?.departmentId).toBe(2)
  expect(payload?.email).toBeNull()
  expect(payload?.version).toBe(0)
})
test('部门页面新增、编辑和版本冲突保留输入', async ({ page }, testInfo) => {
  const nodes = tree()
  let writes = 0
  await page.route('**/api/v1/admin/iam/departments**', (route) => {
    const path = new URL(route.request().url()).pathname,
      method = route.request().method()
    if (path.endsWith('/tree')) return route.fulfill(ok(nodes))
    if (method === 'POST') {
      const body = route.request().postDataJSON()
      nodes[0]!.children.push({ ...body, id: 4, version: 0, directUserCount: 0, children: [] })
      writes++
      return route.fulfill({ ...ok({ ...body, id: 4, version: 0 }), status: 201 })
    }
    if (method === 'PUT')
      return route.fulfill({
        status: 409,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'COMMON_OPTIMISTIC_LOCK',
          message: '数据已被修改，请刷新后重试',
          data: null,
        }),
      })
    const node =
      nodes[0]!.children.find((row) => String(row.id) === path.split('/').at(-1)) ?? nodes[0]!
    return route.fulfill(
      ok({ ...node, createdAt: '2026-11-15T10:00:00.000', updatedAt: '2026-11-15T10:00:00.000' }),
    )
  })
  await page.goto('/iam/departments')
  await expect(page.getByRole('heading', { name: '部门管理', exact: true })).toBeVisible()
  await page.getByLabel('关键字', { exact: true }).fill('RD')
  await page.getByRole('button', { name: '查询', exact: true }).click()
  await expect(page.locator('tbody tr')).toHaveCount(2)
  await expect(page.getByText('上级路径', { exact: true })).toBeVisible()
  await expect(page.getByText('匹配 1 个', { exact: false })).toBeVisible()
  await page.getByRole('button', { name: '重置', exact: true }).click()
  await expect(
    page
      .locator('tbody tr')
      .filter({ hasText: '总公司' })
      .getByRole('button', { name: '删除部门', exact: true }),
  ).toHaveCount(0)
  await page.getByRole('button', { name: '新增部门', exact: true }).click()
  const create = page.getByRole('dialog', { name: '新增部门', exact: true })
  await create.getByLabel('部门编码').fill('FIN')
  await create.getByLabel('部门名称').fill('财务部')
  await create.getByRole('button', { name: '保存', exact: true }).click()
  await expect.poll(() => writes).toBe(1)
  await expect(page.getByRole('cell', { name: '财务部' })).toBeVisible()
  await page.screenshot({ path: testInfo.outputPath('departments-desktop.png'), fullPage: true })
  await page
    .locator('tbody tr')
    .filter({ hasText: '财务部' })
    .getByRole('button', { name: '编辑部门', exact: true })
    .click()
  const edit = page.getByRole('dialog', { name: '编辑部门', exact: true })
  await edit.getByLabel('部门名称').fill('新财务部')
  await edit.getByRole('button', { name: '保存', exact: true }).click()
  await expect(edit.getByRole('alert')).toContainText('数据已被修改')
  await expect(edit.getByLabel('部门名称')).toHaveValue('新财务部')
})

import { expect, test, type Page } from '@playwright/test'

import { mockCsrf, ok, session } from './support/api-mocks'

const userMenu = {
  id: 10,
  parentId: 0,
  type: 'DIRECTORY',
  code: 'iam:directory',
  name: '身份与权限',
  path: '/iam',
  component: null,
  icon: 'shield',
  sort: 20,
  visible: true,
  children: [
    {
      id: 11,
      parentId: 10,
      type: 'MENU',
      code: 'iam:user:list',
      name: '用户管理',
      path: '/iam/users',
      component: 'iam/users/index',
      icon: 'users',
      sort: 201,
      visible: true,
      children: [],
    },
  ],
}

test.describe('RBAC 与菜单', () => {
  test('登录后落在第一个有权限菜单，侧边栏由后端菜单驱动', async ({ page }) => {
    await mockSessionWithMenus(page, [userMenu], ['iam:user:list'])
    await mockUsers(page)

    await page.goto('/')

    await expect(page).toHaveURL('/iam/users')
    await expect(page.getByRole('link', { name: '用户管理' })).toBeVisible()
    await expect(page.getByRole('link', { name: '角色管理' })).toHaveCount(0)
  })

  test('403 只提示无权限，不退出登录', async ({ page }) => {
    await mockSessionWithMenus(page, [userMenu], ['iam:user:list'])
    await page.route('**/api/v1/admin/iam/users*', (route) =>
      route.fulfill({
        status: 403,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'COMMON_FORBIDDEN',
          message: '无权限执行该操作',
          data: null,
        }),
      }),
    )

    await page.goto('/iam/users')

    await expect(page.getByText('无权限执行该操作').first()).toBeVisible()
    await expect(page).toHaveURL('/iam/users')
    await expect(page.getByRole('button', { name: '退出登录' })).toBeVisible()
  })

  test('角色页面可新增角色并提交编码', async ({ page }) => {
    await mockSessionWithMenus(page, [
      userMenu,
      {
        ...userMenu,
        id: 20,
        code: 'iam:role:list',
        name: '角色管理',
        path: '/iam/roles',
        children: [],
      },
    ], ['iam:user:list', 'iam:role:list'])

    let createdBody: unknown
    await page.route('**/api/v1/admin/iam/roles', async (route) => {
      if (route.request().method() === 'POST') {
        createdBody = route.request().postDataJSON()
        await route.fulfill(
          ok({
            id: 1,
            code: 'OPS_ADMIN',
            name: '运维管理员',
            description: null,
            status: 'ENABLED',
            sort: 0,
            createdAt: '2026-11-12T10:00:00.000',
            updatedAt: '2026-11-12T10:00:00.000',
            version: 0,
          }),
        )
        return
      }
      await route.fulfill(ok({ items: [], total: 0, page: 1, size: 20 }))
    })

    await page.goto('/iam/roles')
    await page.getByRole('button', { name: '新增角色' }).click()
    await page.getByLabel('编码').fill('OPS_ADMIN')
    await page.getByLabel('名称').fill('运维管理员')
    await page.getByRole('button', { name: '保存' }).click()

    await expect(page.getByText('角色已创建')).toBeVisible()
    expect(createdBody).toMatchObject({ code: 'OPS_ADMIN', name: '运维管理员' })
  })

  test('资源停用冲突展示后端错误', async ({ page }) => {
    await mockSessionWithMenus(page, [
      userMenu,
      {
        ...userMenu,
        id: 21,
        code: 'iam:resource:list',
        name: '资源管理',
        path: '/iam/resources',
        children: [],
      },
    ], ['iam:user:list', 'iam:resource:list'])

    await page.route('**/api/v1/admin/iam/resources/tree', (route) =>
      route.fulfill(
        ok([
          {
            id: 1,
            parentId: 0,
            type: 'DIRECTORY',
            code: 'iam:directory',
            name: '身份与权限',
            path: '/iam',
            component: null,
            icon: 'shield',
            sort: 20,
            visible: true,
            status: 'ENABLED',
            children: [
              {
                id: 2,
                parentId: 1,
                type: 'MENU',
                code: 'iam:user:list',
                name: '用户管理',
                path: '/iam/users',
                component: 'iam/users/index',
                icon: 'users',
                sort: 201,
                visible: true,
                status: 'ENABLED',
                children: [],
              },
            ],
          },
        ]),
      ),
    )
    await page.route('**/api/v1/admin/iam/resources/1', (route) =>
      route.fulfill(
        ok({
          id: 1,
          parentId: 0,
          type: 'DIRECTORY',
          code: 'iam:directory',
          name: '身份与权限',
          path: '/iam',
          component: null,
          icon: 'shield',
          sort: 20,
          visible: true,
          status: 'ENABLED',
          createdAt: '2026-11-01T09:00:00.000',
          updatedAt: '2026-11-01T09:00:00.000',
          version: 0,
        }),
      ),
    )
    await page.route('**/api/v1/admin/iam/resources/1/status', (route) =>
      route.fulfill({
        status: 409,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'IAM_RESOURCE_HAS_ENABLED_CHILDREN',
          message: '资源存在启用子资源，不能停用',
          data: null,
        }),
      }),
    )

    await page.goto('/iam/resources')
    await page.getByRole('button', { name: '禁用' }).first().click()

    await expect(page.getByText('资源存在启用子资源，不能停用')).toBeVisible()
  })

  test('必须改密时强制跳转并允许修改', async ({ page }) => {
    await page.route('**/api/v1/admin/auth/me', (route) =>
      route.fulfill(ok({ ...session, mustChangePassword: true })),
    )
    await mockCsrf(page)
    await page.route('**/api/v1/admin/auth/change-password', (route) =>
      route.fulfill({ status: 204, body: '' }),
    )

    await page.goto('/iam/users')
    await expect(page).toHaveURL('/change-password')

    await page.getByLabel('当前密码').fill('oldPassw0rd')
    await page.getByLabel('新密码', { exact: true }).fill('newPassw0rd')
    await page.getByLabel('确认新密码').fill('newPassw0rd')
    await page.getByRole('button', { name: '确认修改' }).click()

    await expect(page).toHaveURL('/login')
  })
})

async function mockSessionWithMenus(
  page: Page,
  menus: unknown[],
  permissions: string[] = ['iam:user:list'],
): Promise<void> {
  await page.route('**/api/v1/admin/auth/me', (route) =>
    route.fulfill(ok({ ...session, menus, permissions })),
  )
}

async function mockUsers(page: Page): Promise<void> {
  await page.route('**/api/v1/admin/iam/users*', (route) =>
    route.fulfill(ok({ items: [], total: 0, page: 1, size: 20 })),
  )
}

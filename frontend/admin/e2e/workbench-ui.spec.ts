import { expect, test, type Page } from '@playwright/test'
import { format, subHours } from 'date-fns'
import { mockCsrf, mockSession, ok } from './support/api-mocks'

const menus = [
  {
    id: 1,
    parentId: 0,
    type: 'DIRECTORY',
    code: 'iam:directory',
    name: '系统管理',
    icon: 'shield',
    sort: 1,
    visible: true,
    children: [
      {
        id: 2,
        parentId: 1,
        type: 'MENU',
        code: 'iam:user:list',
        name: '用户管理',
        path: '/iam/users',
        icon: 'users',
        sort: 1,
        visible: true,
        children: [],
      },
      {
        id: 3,
        parentId: 1,
        type: 'MENU',
        code: 'iam:role:list',
        name: '角色管理',
        path: '/iam/roles',
        icon: 'shield',
        sort: 2,
        visible: true,
        children: [],
      },
      {
        id: 4,
        parentId: 1,
        type: 'MENU',
        code: 'iam:resource:list',
        name: '菜单管理',
        path: '/iam/resources',
        icon: 'menu',
        sort: 3,
        visible: true,
        children: [],
      },
    ],
  },
]
const stamp = (hours: number) => format(subHours(new Date(), hours), "yyyy-MM-dd'T'HH:mm:ss")
const logins = Array.from({ length: 60 }, (_, index) => ({
  id: index + 1,
  username: ['admin', 'zhangsan', 'lisi'][index % 3],
  action: 'LOGIN',
  loginType: 'WEB',
  device: 'WEB',
  success: index % 9 !== 0,
  createdAt: stamp(index * 0.6),
}))
const operations = Array.from({ length: 90 }, (_, index) => ({
  id: index + 1,
  username: 'admin',
  module: ['用户管理', '角色管理', '菜单管理'][index % 3]!,
  action: 'UPDATE',
  success: index % 13 !== 0,
  durationMs: 12,
  createdAt: stamp(index * 1.2),
}))
const users = Array.from({ length: 8 }, (_, index) => ({
  id: index + 1,
  username: ['admin', 'zhangsan', 'lisi', 'wangwu', 'zhaoliu', 'operator', 'editor', 'auditor'][
    index
  ],
  nickname: ['系统管理员', '张三', '李四', '王五', '赵六', '运营', '编辑', '审计'][index],
  status: index === 5 ? 'DISABLED' : 'ENABLED',
  createdAt: stamp(300),
  lastLoginAt: stamp(index),
}))
const roles = [
  {
    id: 1,
    code: 'SUPER_ADMIN',
    name: '超级管理员',
    status: 'ENABLED',
    sort: 0,
    description: '系统内置角色',
    createdAt: stamp(300),
  },
  {
    id: 2,
    code: 'OPS_ADMIN',
    name: '运维管理员',
    status: 'ENABLED',
    sort: 10,
    description: '日常运维与审计',
    createdAt: stamp(200),
  },
]
const resources = [
  {
    ...menus[0]!,
    status: 'ENABLED',
    children: menus[0]!.children.map((menu) => ({
      ...menu,
      status: 'ENABLED',
      children: [
        {
          id: menu.id + 10,
          parentId: menu.id,
          type: 'BUTTON',
          code: `${menu.code}:update`,
          name: '编辑',
          sort: 1,
          visible: true,
          status: 'ENABLED',
          children: [],
        },
      ],
    })),
  },
]

async function mockUi(page: Page): Promise<void> {
  await mockCsrf(page)
  await mockSession(page, { menus })
  await page.route('**/api/v1/admin/audit/login-logs*', (route) =>
    route.fulfill(ok({ items: logins, total: 160, page: 1, size: 100 })),
  )
  await page.route('**/api/v1/admin/audit/operation-logs*', (route) =>
    route.fulfill(ok({ items: operations, total: 190, page: 1, size: 100 })),
  )
  await page.route('**/api/v1/admin/iam/users*', (route) =>
    route.fulfill(ok({ items: users, total: users.length, page: 1, size: 20 })),
  )
  await page.route('**/api/v1/admin/iam/roles*', (route) =>
    route.fulfill(ok({ items: roles, total: roles.length, page: 1, size: 20 })),
  )
  await page.route('**/api/v1/admin/iam/resources/tree', (route) => route.fulfill(ok(resources)))
}

test('工作台默认路由、真实样本与时间范围切换', async ({ page }) => {
  await mockUi(page)
  await page.goto('/')
  await expect(page).toHaveURL('/')
  await expect(page.getByRole('heading', { name: /超级管理员/ })).toBeVisible()
  await page.getByRole('button', { name: '7 天', exact: true }).click()
  await expect(page.getByRole('button', { name: '7 天', exact: true })).toHaveAttribute(
    'aria-pressed',
    'true',
  )
  await expect(page.locator('.pulse-figure .animated-number')).toHaveAttribute('aria-label', '53')
})

test('没有日志权限时不请求受保护日志', async ({ page }) => {
  let requests = 0
  await mockSession(page, { superAdmin: false, permissions: [], menus: [] })
  await page.route('**/api/v1/admin/audit/**', (route) => {
    requests++
    return route.fulfill(ok(null))
  })
  await page.goto('/')
  await expect(page.locator('.pulse-card')).toContainText('暂无登录日志查看权限')
  await expect(page.locator('.side-card').last()).toContainText('暂无操作日志查看权限')
  expect(requests).toBe(0)
})

test('工作台局部请求失败可重试，成功的面板仍可使用', async ({ page }) => {
  await mockUi(page)
  let attempts = 0
  await page.route('**/api/v1/admin/audit/login-logs*', (route) => {
    attempts++
    return route.fulfill(
      attempts === 1
        ? {
            status: 500,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'COMMON_INTERNAL_ERROR',
              message: '登录记录暂不可用',
              data: null,
            }),
          }
        : ok({ items: logins, total: logins.length, page: 1, size: 100 }),
    )
  })
  await page.goto('/')
  await expect(page.getByText('登录记录暂不可用')).toBeVisible()
  await expect(page.locator('.module-list')).toContainText('用户管理')
  await page.getByRole('button', { name: '重试', exact: true }).click()
  await expect(page.getByText('登录记录暂不可用')).toHaveCount(0)
  await expect(page.locator('.pulse-figure .metric')).not.toContainText('—')
  expect(attempts).toBe(2)
})

test('角色表格加载骨架、编辑弹窗与减少动画偏好', async ({ page }, testInfo) => {
  await mockUi(page)
  let release: () => void = () => {}
  const pending = new Promise<void>((resolve) => {
    release = resolve
  })
  await page.route('**/api/v1/admin/iam/roles*', async (route) => {
    await pending
    await route.fulfill(ok({ items: roles, total: roles.length, page: 1, size: 20 }))
  })
  await page.setViewportSize({ width: 1600, height: 1000 })
  await page.goto('/iam/roles')
  await expect(page.locator('.app-data-table')).toHaveAttribute('aria-busy', 'true')
  await expect(page.locator('.table-skeleton').first()).toBeVisible()
  release()
  await expect(page.locator('.app-data-table')).toHaveAttribute('aria-busy', 'false')
  await expect(page.locator('tbody')).toContainText('OPS_ADMIN')
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await page.getByRole('button', { name: '新增角色' }).click()
  const dialog = page.getByRole('dialog', { name: '新增角色' })
  await expect(dialog).toBeVisible()
  const duration = await dialog.evaluate((element) =>
    Number.parseFloat(getComputedStyle(element).animationDuration),
  )
  expect(duration).toBeLessThan(0.01)
  await page.screenshot({ path: testInfo.outputPath('role-dialog.png'), animations: 'disabled' })
})

test('菜单弹窗多次开合保持居中，移动侧栏不会遮住搜索', async ({ page }) => {
  await mockUi(page)
  await page.setViewportSize({ width: 1600, height: 1000 })
  await page.goto('/')
  for (let index = 0; index < 3; index++) {
    await page.getByRole('button', { name: '搜索菜单', exact: true }).click()
    const dialog = page.getByRole('dialog', { name: '搜索菜单' })
    await expect(dialog).toBeVisible()
    await expect(page.getByLabel('菜单名称')).toBeFocused()
    await expect
      .poll(async () => {
        const box = await dialog.boundingBox()
        return box
          ? Math.abs(box.x + box.width / 2 - 800) + Math.abs(box.y + box.height / 2 - 500)
          : 999
      })
      .toBeLessThan(2)
    await page.keyboard.press('Escape')
    await expect(dialog).toHaveCount(0)
  }
  await page.setViewportSize({ width: 390, height: 844 })
  await page.getByRole('button', { name: '打开导航' }).click()
  await expect(page.getByRole('dialog', { name: '导航菜单' })).toBeVisible()
  await page.keyboard.press('Control+k')
  await expect(page.getByRole('dialog', { name: '导航菜单' })).toHaveCount(0)
  await expect(page.getByRole('dialog', { name: '搜索菜单' })).toBeVisible()
})

test('菜单树查询保留祖先，展开收起与表格保持一致', async ({ page }) => {
  await mockUi(page)
  await page.goto('/iam/resources')
  await expect(page.getByRole('heading', { name: '菜单管理' })).toBeVisible()
  await expect(page.locator('tbody tr')).toHaveCount(7)
  await page.getByRole('button', { name: '全部收起' }).click()
  await expect(page.locator('tbody tr')).toHaveCount(1)
  await page.getByLabel('关键字').fill('用户管理')
  await expect(page.locator('tbody')).toContainText('系统管理')
  await expect(page.locator('tbody')).toContainText('用户管理')
  await page.getByRole('button', { name: '重置', exact: true }).click()
  await page.getByRole('button', { name: '全部展开' }).click()
  await expect(page.locator('tbody tr')).toHaveCount(7)
})

for (const [name, path] of [
  ['workbench', '/'],
  ['users', '/iam/users'],
  ['roles', '/iam/roles'],
  ['menus', '/iam/resources'],
] as const) {
  test(`视觉检查 ${name} 桌面与移动布局`, async ({ page }, testInfo) => {
    await mockUi(page)
    await page.setViewportSize({ width: 1600, height: 1000 })
    await page.goto(path)
    if (name === 'workbench') {
      await expect(page.locator('.pulse-figure .metric')).not.toContainText('—')
      await expect
        .poll(() =>
          page
            .locator('.animated-number')
            .evaluateAll((elements) =>
              elements.every(
                (element) =>
                  element.getAttribute('data-ready') === 'true' &&
                  element
                    .getAnimations({ subtree: true })
                    .every((animation) => animation.playState !== 'running'),
              ),
            ),
        )
        .toBe(true)
    } else
      await expect(page.locator('tbody tr').first()).toContainText(
        name === 'users' ? 'admin' : name === 'roles' ? 'SUPER_ADMIN' : '系统管理',
      )
    await page.screenshot({
      path: testInfo.outputPath(`${name}-desktop.png`),
      animations: 'disabled',
    })
    await page.setViewportSize({ width: 390, height: 844 })
    await expect
      .poll(() => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
      .toBe(true)
    await page.screenshot({
      path: testInfo.outputPath(`${name}-mobile.png`),
      animations: 'disabled',
    })
  })
}

test('顶栏外观设置支持深色持久化与跟随系统', async ({ page }) => {
  await mockUi(page)
  await page.goto('/')
  await page.getByRole('button', { name: '外观设置', exact: true }).click()
  const dialog = page.getByRole('dialog', { name: '外观设置' })
  await dialog.getByRole('button', { name: '深色', exact: true }).click()
  await expect(page.locator('html')).toHaveClass('dark')
  await expect(dialog.getByRole('button', { name: '深色', exact: true })).toHaveAttribute(
    'aria-pressed',
    'true',
  )
  await dialog.getByRole('button', { name: '完成' }).click()
  await page.reload()
  await expect(page.locator('html')).toHaveClass('dark')
  await page.getByRole('button', { name: '外观设置', exact: true }).click()
  await dialog.getByRole('button', { name: '跟随系统', exact: true }).click()
  await page.emulateMedia({ colorScheme: 'light' })
  await expect(page.locator('html')).not.toHaveClass('dark')
  await page.emulateMedia({ colorScheme: 'dark' })
  await expect(page.locator('html')).toHaveClass('dark')
  await dialog.getByRole('button', { name: '浅色', exact: true }).click()
  await expect(page.locator('html')).not.toHaveClass('dark')
})

test('同一侧栏高亮连续跨工作台与业务菜单滑动，轨迹对齐参考项目', async ({ page }) => {
  await mockUi(page)
  await page.setViewportSize({ width: 1600, height: 1000 })
  await page.goto('/')
  const thumb = page.locator('.app-rail .nav-selection')
  await expect(thumb).toHaveCount(1)
  await expect(thumb).toHaveCSS('opacity', '1')
  await thumb.evaluate((element) => element.setAttribute('data-continuous', 'true'))
  for (const label of ['用户管理', '角色管理', '工作台']) {
    const start = await thumb.boundingBox()
    await page.getByRole('link', { name: label, exact: true }).focus()
    await page.keyboard.press('Enter')
    await expect
      .poll(() =>
        thumb.evaluate((element) =>
          element.getAnimations().some((animation) => animation.id === 'navigation-slide'),
        ),
      )
      .toBe(true)
    const sample = await thumb.evaluate((element, startY) => {
      const animation = element.getAnimations().find((item) => item.id === 'navigation-slide')!
      animation.pause()
      animation.currentTime = 130
      const actual = element.getBoundingClientRect().top
      const target = element
        .parentElement!.querySelector('.nav-item[aria-current="page"]')!
        .getBoundingClientRect().top
      const duration = animation.effect!.getTiming().duration
      animation.finish()
      return { actual, expected: startY + (target - startY) * (1 - 0.75 ** 5), duration }
    }, start!.y)
    expect(sample.duration).toBe(520)
    expect(Math.abs(sample.actual - sample.expected)).toBeLessThan(1)
    await expect(thumb).toHaveAttribute('data-continuous', 'true')
  }
})

test('列表刷新保留旧行，同一批记录不重播，换页数据按参考节奏显现', async ({ page }) => {
  await mockUi(page)
  await page.goto('/iam/users')
  await expect(page.locator('tbody')).toContainText('admin')
  await page
    .locator('tbody tr')
    .first()
    .evaluate((element) => element.setAttribute('data-retained', 'true'))
  let release: () => void = () => {}
  const pending = new Promise<void>((resolve) => {
    release = resolve
  })
  await page.route('**/api/v1/admin/iam/users*', async (route) => {
    await pending
    const fresh = new URL(route.request().url()).searchParams.has('keyword')
    const items = fresh
      ? Array.from({ length: 18 }, (_, index) => ({
          ...users[0],
          id: index + 101,
          username: `fresh-${index}`,
        }))
      : users
    await route.fulfill(ok({ items, total: items.length, page: 1, size: 20 }))
  })
  await page.getByRole('button', { name: '刷新', exact: true }).click()
  await expect(page.locator('.app-data-table')).toHaveAttribute('aria-busy', 'true')
  await expect(page.locator('.table-skeleton')).toHaveCount(0)
  await expect(page.locator('tbody tr').first()).toHaveAttribute('data-retained', 'true')
  await expect(page.locator('tbody tr').first()).toHaveCSS('opacity', '0.55')
  release()
  await expect(page.locator('.app-data-table')).toHaveAttribute('aria-busy', 'false')
  expect(
    await page
      .locator('tbody tr')
      .first()
      .evaluate((element) => element.getAnimations().length),
  ).toBe(0)
  await page.getByLabel('关键字', { exact: true }).fill('fresh')
  await page.getByRole('button', { name: '查询', exact: true }).click()
  await expect(page.locator('tbody tr').first()).toContainText('fresh-0')
  const timings = await page.locator('tbody tr').evaluateAll((elements) =>
    elements.flatMap((element) =>
      element
        .getAnimations()
        .filter((animation) => animation.id === 'data-row-enter')
        .map((animation) => animation.effect!.getTiming()),
    ),
  )
  expect(timings).toHaveLength(14)
  expect(timings.every((timing) => timing.duration === 500)).toBe(true)
  expect(timings.map((timing) => timing.delay)).toEqual(
    Array.from({ length: 14 }, (_, index) => index * 25),
  )
})

test('页面按标题、筛选、表格顺序进入，连续切换后复位滚动和样式', async ({ page }) => {
  await mockUi(page)
  await page.setViewportSize({ width: 1600, height: 600 })
  await page.goto('/iam/users')
  await expect(page.getByRole('heading', { name: '用户管理' })).toBeVisible()
  const timings = await page.locator('.route-page').evaluate((element) => ({
    page: element
      .getAnimations()
      .find((animation) => animation.id === 'page-enter')
      ?.effect?.getTiming(),
    sections: [...element.querySelectorAll('.page-reveal')].map((section) =>
      section
        .getAnimations()
        .find((animation) => animation.id === 'page-section-enter')
        ?.effect?.getTiming(),
    ),
  }))
  expect(timings.page?.duration).toBe(620)
  expect(timings.sections.map((timing) => timing?.delay)).toEqual([40, 90, 140])
  await page.locator('.app-main').evaluate((element) => {
    element.scrollTop = 200
  })
  await page.getByRole('link', { name: '角色管理', exact: true }).click()
  await page.getByRole('link', { name: '菜单管理', exact: true }).click()
  await expect(page.getByRole('heading', { name: '菜单管理' })).toBeVisible()
  await expect
    .poll(() => page.locator('.app-main').evaluate((element) => element.scrollTop))
    .toBe(0)
  await page.locator('.route-page').evaluate(async (element) => {
    await Promise.allSettled(
      element.getAnimations({ subtree: true }).map((animation) => animation.finished),
    )
  })
  await expect(page.locator('.route-page')).toHaveCSS('transform', 'none')
  await expect(page.locator('.route-page')).toHaveCSS('opacity', '1')
})

test('工作台首次回弹入场只播一次，返回与刷新保持图表节点', async ({ page }) => {
  await mockUi(page)
  await page.goto('/')
  await expect
    .poll(() =>
      page
        .locator('.workbench-card')
        .first()
        .evaluate((element) =>
          element.getAnimations().some((animation) => animation.id === 'workbench-intro'),
        ),
    )
    .toBe(true)
  await page.getByRole('link', { name: '用户管理', exact: true }).click()
  await expect(page.getByRole('heading', { name: '用户管理' })).toBeVisible()
  await page.getByRole('link', { name: '工作台', exact: true }).click()
  await expect
    .poll(() =>
      page
        .locator('.workbench-card')
        .first()
        .evaluate((element) =>
          element.getAnimations().some((animation) => animation.id === 'workbench-return'),
        ),
    )
    .toBe(true)
  await page
    .locator('.trend-chart')
    .evaluate((element) => element.setAttribute('data-retained', 'true'))
  await page.getByRole('button', { name: '7 天', exact: true }).click()
  await expect(page.locator('.trend-chart')).toHaveAttribute('data-retained', 'true')
  let release: () => void = () => {}
  const pending = new Promise<void>((resolve) => {
    release = resolve
  })
  await page.route('**/api/v1/admin/audit/login-logs*', async (route) => {
    await pending
    await route.fulfill(ok({ items: logins, total: logins.length, page: 1, size: 100 }))
  })
  await page.getByRole('button', { name: '刷新', exact: true }).click()
  await expect(page.locator('.workbench')).toHaveAttribute('aria-busy', 'true')
  await expect(page.locator('.pulse-figure .animated-number')).toHaveAttribute('aria-label', '53')
  release()
  await expect(page.locator('.workbench')).toHaveAttribute('aria-busy', 'false')
  await expect(page.locator('.trend-chart')).toHaveAttribute('data-retained', 'true')
})

for (const [name, path] of [
  ['workbench', '/'],
  ['users', '/iam/users'],
  ['roles', '/iam/roles'],
  ['menus', '/iam/resources'],
] as const) {
  test(`深色视觉检查 ${name} 桌面与移动布局`, async ({ page }, testInfo) => {
    await page.addInitScript(() => localStorage.setItem('keel.appearance.theme', 'dark'))
    await mockUi(page)
    await page.setViewportSize({ width: 1600, height: 1000 })
    await page.goto(path)
    await expect(page.locator('html')).toHaveClass('dark')
    if (name === 'workbench') {
      await expect(page.locator('.pulse-figure .animated-number')).not.toHaveAttribute(
        'aria-label',
        '暂无数据',
      )
      await expect
        .poll(() =>
          page
            .locator('.animated-number')
            .evaluateAll((elements) =>
              elements.every(
                (element) =>
                  element.getAttribute('data-ready') === 'true' &&
                  element
                    .getAnimations({ subtree: true })
                    .every((animation) => animation.playState !== 'running'),
              ),
            ),
        )
        .toBe(true)
    } else
      await expect(page.locator('tbody tr').first()).toContainText(
        name === 'users' ? 'admin' : name === 'roles' ? 'SUPER_ADMIN' : '系统管理',
      )
    await page.screenshot({
      path: testInfo.outputPath(`${name}-dark-desktop.png`),
      animations: 'disabled',
    })
    await page.setViewportSize({ width: 390, height: 844 })
    await expect
      .poll(() => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
      .toBe(true)
    await page.screenshot({
      path: testInfo.outputPath(`${name}-dark-mobile.png`),
      animations: 'disabled',
    })
    await page.getByRole('button', { name: '外观设置', exact: true }).click()
    await expect(page.getByRole('dialog', { name: '外观设置' })).toBeVisible()
    if (name === 'workbench')
      await page.screenshot({
        path: testInfo.outputPath('appearance-dark-mobile.png'),
        animations: 'disabled',
      })
  })
}

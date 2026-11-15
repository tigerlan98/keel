import { expect, test } from '@playwright/test'
import { mockSession, ok } from './support/api-mocks'

test.beforeEach(async ({ page }) => {
  await mockSession(page)
  await page.goto('/')
})

test('写请求缺少CSRF Cookie时先获取token再提交', async ({ page }) => {
  const order: string[] = []
  await page.route('**/api/v1/admin/auth/csrf', (route) => {
    order.push('csrf')
    return route.fulfill({
      ...ok({ token: 'fresh-csrf' }),
      headers: { 'set-cookie': 'XSRF-TOKEN=fresh-csrf; Path=/; SameSite=Lax' },
    })
  })
  await page.route('**/api/v1/admin/iam/users/2/profile', (route) => {
    order.push('save')
    expect(route.request().headers()['x-xsrf-token']).toBe('fresh-csrf')
    expect(route.request().headers().cookie).toContain('XSRF-TOKEN=fresh-csrf')
    return route.fulfill(ok({ saved: true }))
  })
  const result = await page.evaluate(async () => {
    const path = '/src/api/http.ts'
    const { default: http } = await import(path)
    return (await http.put('/admin/iam/users/2/profile', { nickname: '测试' })).data
  })
  expect(result.data.saved).toBe(true)
  expect(order).toEqual(['csrf', 'save'])
})

test('已有Cookie时直接回传，不额外获取token', async ({ page, context }) => {
  await context.addCookies([
    { name: 'XSRF-TOKEN', value: 'existing-csrf', url: 'http://127.0.0.1:5173' },
  ])
  let fetches = 0
  await page.route('**/api/v1/admin/auth/csrf', (route) => {
    fetches++
    return route.fulfill(ok({ token: 'unexpected' }))
  })
  await page.route('**/api/v1/admin/iam/users/2/profile', (route) => {
    expect(route.request().headers()['x-xsrf-token']).toBe('existing-csrf')
    return route.fulfill(ok({ saved: true }))
  })
  await page.evaluate(async () => {
    const path = '/src/api/http.ts'
    const { default: http } = await import(path)
    await http.put('/admin/iam/users/2/profile', {})
  })
  expect(fetches).toBe(0)
})

test('并发写请求共用一次CSRF获取', async ({ page }) => {
  let fetches = 0,
    saves = 0
  let release: () => void = () => {}
  const gate = new Promise<void>((resolve) => {
    release = resolve
  })
  await page.route('**/api/v1/admin/auth/csrf', async (route) => {
    fetches++
    await gate
    await route.fulfill({
      ...ok({ token: 'shared-csrf' }),
      headers: { 'set-cookie': 'XSRF-TOKEN=shared-csrf; Path=/' },
    })
  })
  await page.route('**/api/v1/admin/iam/users/*/profile', (route) => {
    saves++
    expect(route.request().headers()['x-xsrf-token']).toBe('shared-csrf')
    return route.fulfill(ok({ saved: true }))
  })
  const writes = page.evaluate(async () => {
    const path = '/src/api/http.ts'
    const { default: http } = await import(path)
    await Promise.all([
      http.put('/admin/iam/users/2/profile', {}),
      http.put('/admin/iam/users/3/profile', {}),
    ])
  })
  await expect.poll(() => fetches).toBe(1)
  release()
  await writes
  expect(fetches).toBe(1)
  expect(saves).toBe(2)
})

test('明确的CSRF错误刷新后重试一次，原始请求体不丢失', async ({ page, context }) => {
  await context.addCookies([
    { name: 'XSRF-TOKEN', value: 'old-csrf', url: 'http://127.0.0.1:5173' },
  ])
  let fetches = 0,
    saves = 0
  await page.route('**/api/v1/admin/auth/csrf', (route) => {
    fetches++
    return route.fulfill({
      ...ok({ token: 'new-csrf' }),
      headers: { 'set-cookie': 'XSRF-TOKEN=new-csrf; Path=/' },
    })
  })
  await page.route('**/api/v1/admin/iam/users/2/profile', (route) => {
    saves++
    expect(route.request().postDataJSON()).toEqual({
      nickname: '张三',
      departmentId: 1,
      version: 2,
    })
    if (saves === 1)
      return route.fulfill({
        status: 403,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'AUTH_CSRF_INVALID', message: 'CSRF 校验失败', data: null }),
      })
    expect(route.request().headers()['x-xsrf-token']).toBe('new-csrf')
    return route.fulfill(ok({ saved: true }))
  })
  const saved = await page.evaluate(async () => {
    const path = '/src/api/http.ts'
    const { default: http } = await import(path)
    return (
      await http.put('/admin/iam/users/2/profile', {
        nickname: '张三',
        departmentId: 1,
        version: 2,
      })
    ).data.data.saved
  })
  expect(saved).toBe(true)
  expect(saves).toBe(2)
  expect(fetches).toBe(1)
  await expect(page.getByText('CSRF 校验失败', { exact: true })).toHaveCount(0)
})

test('CSRF持续失败不会无限重试', async ({ page, context }) => {
  await context.addCookies([{ name: 'XSRF-TOKEN', value: 'old', url: 'http://127.0.0.1:5173' }])
  let saves = 0,
    fetches = 0
  await page.route('**/api/v1/admin/auth/csrf', (route) => {
    fetches++
    return route.fulfill(ok({ token: 'fresh' }))
  })
  await page.route('**/api/v1/admin/iam/users/2/profile', (route) => {
    saves++
    return route.fulfill({
      status: 403,
      contentType: 'application/json',
      body: JSON.stringify({ code: 'AUTH_CSRF_INVALID', message: 'CSRF 校验失败', data: null }),
    })
  })
  const code = await page.evaluate(async () => {
    const path = '/src/api/http.ts'
    const { default: http } = await import(path)
    try {
      await http.put('/admin/iam/users/2/profile', {})
    } catch (error) {
      return (error as { code: string }).code
    }
  })
  expect(code).toBe('AUTH_CSRF_INVALID')
  expect(saves).toBe(2)
  expect(fetches).toBe(1)
})

test('普通403不刷新token、不重复提交、不清理登录态', async ({ page, context }) => {
  await context.addCookies([{ name: 'XSRF-TOKEN', value: 'valid', url: 'http://127.0.0.1:5173' }])
  let saves = 0,
    fetches = 0
  await page.route('**/api/v1/admin/auth/csrf', (route) => {
    fetches++
    return route.fulfill(ok({ token: 'unexpected' }))
  })
  await page.route('**/api/v1/admin/iam/users/2/profile', (route) => {
    saves++
    return route.fulfill({
      status: 403,
      contentType: 'application/json',
      body: JSON.stringify({ code: 'COMMON_FORBIDDEN', message: '无权限执行该操作', data: null }),
    })
  })
  await page.evaluate(async () => {
    const path = '/src/api/http.ts'
    const { default: http } = await import(path)
    try {
      await http.put('/admin/iam/users/2/profile', {})
    } catch {
      /* Expected authorization failure. */
    }
  })
  expect(saves).toBe(1)
  expect(fetches).toBe(0)
  await expect(page).toHaveURL('/')
})

import { createRouter, createWebHistory } from 'vue-router'

import { firstMenuPath, flattenMenuPaths } from '@/lib/menus'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/change-password',
      name: 'change-password',
      component: () => import('@/views/ChangePasswordView.vue'),
      meta: { title: '修改密码', allowPasswordChangeRequired: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
          meta: { title: '首页' },
        },
        {
          path: 'iam/users',
          name: 'users',
          component: () => import('@/views/UserListView.vue'),
          meta: { title: '用户管理', permission: 'iam:user:list' },
        },
        {
          path: 'iam/roles',
          name: 'roles',
          component: () => import('@/views/RoleListView.vue'),
          meta: { title: '角色管理', permission: 'iam:role:list' },
        },
        {
          path: 'iam/resources',
          name: 'resources',
          component: () => import('@/views/ResourceTreeView.vue'),
          meta: { title: '资源管理', permission: 'iam:resource:list' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

let validatedUserId: number | null = null

function isRoutableMenuPath(path: string): boolean {
  return router.resolve(path).matched.some((record) => record.name !== undefined)
}

function validateMenus(): void {
  const auth = useAuthStore()
  if (!auth.user || validatedUserId === auth.user.id) {
    return
  }
  validatedUserId = auth.user.id
  for (const path of flattenMenuPaths(auth.menus)) {
    if (path && router.resolve(path).matched.length === 0) {
      console.warn(`[menu] 后端菜单路径未匹配静态路由: ${path}`)
    }
  }
}

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${String(to.meta.title)} - Keel 管理端` : 'Keel 管理端'

  const auth = useAuthStore()
  if (to.meta.public) {
    if (to.name === 'login' && auth.initialized && auth.user) {
      return { name: 'home' }
    }
    return true
  }

  if (!auth.initialized) {
    await auth.initialize()
  }
  if (!auth.user) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  validateMenus()

  if (auth.mustChangePassword && !to.meta.allowPasswordChangeRequired) {
    return { name: 'change-password' }
  }
  if (!auth.mustChangePassword && to.name === 'change-password') {
    return { name: 'home' }
  }

  if (to.name === 'home') {
    const target = firstMenuPath(auth.menus, isRoutableMenuPath)
    if (target) {
      return target
    }
  }

  if (to.meta.permission && !auth.hasPermission(String(to.meta.permission))) {
    const target = firstMenuPath(auth.menus, isRoutableMenuPath)
    if (target && target !== to.fullPath) {
      return target
    }
    return { name: 'home' }
  }

  return true
})

export default router

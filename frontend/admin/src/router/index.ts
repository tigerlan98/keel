import { createRouter, createWebHistory } from 'vue-router'

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
          meta: { title: '用户管理' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

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
  return true
})

export default router

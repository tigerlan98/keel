import { createRouter, createWebHistory } from 'vue-router'

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
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

// 当前只维护页面标题；认证跳转在 Phase 1B 接入 /auth/me 后补充。
router.beforeEach((to) => {
  document.title = to.meta.title ? `${String(to.meta.title)} - Keel 管理端` : 'Keel 管理端'
})

export default router

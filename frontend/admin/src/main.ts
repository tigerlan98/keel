import { createPinia } from 'pinia'
import { createApp } from 'vue'

import App from './App.vue'
import { onForbidden, onUnauthorized } from './api/http'
import { permissionDirective } from './directives/permission'
import router from './router'
import { useAuthStore } from './stores/auth'
import { useToastStore } from './stores/toast'

import './assets/index.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.directive('permission', permissionDirective)

onUnauthorized(() => {
  useAuthStore(pinia).reset()
  const current = router.currentRoute.value
  if (current.name && current.name !== 'login') {
    void router.push({ name: 'login', query: { redirect: current.fullPath } })
  }
})

onForbidden((code, message) => {
  if (code === 'AUTH_PASSWORD_CHANGE_REQUIRED') {
    useAuthStore(pinia).mustChangePassword = true
    void router.push({ name: 'change-password' })
    return
  }
  useToastStore(pinia).show(message || '无权限执行该操作', 'error')
})

app.use(router)
app.mount('#app')

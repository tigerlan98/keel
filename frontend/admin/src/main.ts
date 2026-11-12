import { createPinia } from 'pinia'
import { createApp } from 'vue'

import App from './App.vue'
import { onUnauthorized } from './api/http'
import router from './router'
import { useAuthStore } from './stores/auth'

import './assets/index.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)

onUnauthorized(() => {
  useAuthStore(pinia).reset()
  const current = router.currentRoute.value
  if (current.name && current.name !== 'login') {
    void router.push({ name: 'login', query: { redirect: current.fullPath } })
  }
})

app.use(router)
app.mount('#app')

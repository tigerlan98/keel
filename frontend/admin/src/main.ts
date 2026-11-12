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
app.use(router)

onUnauthorized(() => {
  useAuthStore(pinia).reset()
  if (router.currentRoute.value.name !== 'login') {
    void router.push({ name: 'login' })
  }
})

app.mount('#app')

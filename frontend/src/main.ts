/** 前端入口：安装共享插件并恢复本地登录会话。 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import '@/styles/index.css'
import App from './App.vue'
import router from './router'
import { useAuthStore } from '@/stores/auth'

const pinia = createPinia()
const app = createApp(App)

app.use(pinia)
app.use(router)

void useAuthStore(pinia).initialize()
app.mount('#app')

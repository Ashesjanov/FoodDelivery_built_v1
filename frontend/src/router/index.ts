/** 第一阶段用户端路由表。 */
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      // 懒加载页面，减少 Vite 首屏包体积。
      component: () => import('../views/home/HomeView.vue'),
    },
  ],
})

export default router

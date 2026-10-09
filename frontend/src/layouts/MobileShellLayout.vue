<script setup lang="ts">
/** 移动端应用外壳：顶部品牌栏 + 底部 Tab 导航，按角色展示对应入口。 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import House from 'lucide-vue-next/dist/esm/icons/house.js'
import Receipt from 'lucide-vue-next/dist/esm/icons/receipt.js'
import ShoppingCart from 'lucide-vue-next/dist/esm/icons/shopping-cart.js'
import UserRound from 'lucide-vue-next/dist/esm/icons/user-round.js'
import ClipboardList from 'lucide-vue-next/dist/esm/icons/clipboard-list.js'
import Utensils from 'lucide-vue-next/dist/esm/icons/utensils.js'
import Gauge from 'lucide-vue-next/dist/esm/icons/gauge.js'
import History from 'lucide-vue-next/dist/esm/icons/history.js'
import LogOut from 'lucide-vue-next/dist/esm/icons/log-out.js'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import type { UserRole } from '@/types/domain'
import '@/styles/mobile.css'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

interface TabItem {
  label: string
  to: string
  icon: typeof House
  badge?: number
}

const tabs = computed<TabItem[]>(() => {
  const role = auth.user?.role as UserRole | undefined
  if (role === 'MERCHANT') {
    return [
      { label: '订单', to: '/m/merchant/orders', icon: ClipboardList },
      { label: '菜单', to: '/m/merchant/menu', icon: Utensils },
      { label: '我的', to: '/m/profile', icon: UserRound },
    ]
  }
  if (role === 'RIDER') {
    return [
      { label: '工作台', to: '/m/rider', icon: Gauge },
      { label: '抢单', to: '/m/rider/available', icon: Receipt },
      { label: '历史', to: '/m/rider/history', icon: History },
      { label: '我的', to: '/m/profile', icon: UserRound },
    ]
  }
  return [
    { label: '首页', to: '/m/home', icon: House },
    { label: '订单', to: '/m/orders', icon: Receipt },
    { label: '购物车', to: '/m/cart', icon: ShoppingCart, badge: cart.cart.itemCount || 0 },
    { label: '我的', to: '/m/profile', icon: UserRound },
  ]
})

function isActive(to: string): boolean {
  if (to === '/m/home') return route.path === '/m' || route.path === '/m/home' || route.path.startsWith('/m/merchants')
  if (to === '/m/orders') return route.path.startsWith('/m/orders')
  if (to === '/m/profile') return route.path.startsWith('/m/profile') || route.path.startsWith('/m/addresses')
  return route.path === to || route.path.startsWith(`${to}/`)
}

function logout(): void {
  auth.logout()
  void router.push('/m/login')
}
</script>

<template>
  <div class="m-shell">
    <header class="m-topbar">
      <RouterLink to="/m/home" class="m-brand">食刻送达</RouterLink>
      <div class="m-topbar-actions">
        <RouterLink
          v-if="auth.isAuthenticated && (!auth.user || auth.user.role === 'CUSTOMER' || auth.user.role === 'ADMIN')"
          to="/m/cart"
          class="m-cart-link"
          aria-label="购物车"
        >
          <ShoppingCart :size="20" aria-hidden="true" />
          <span v-if="cart.cart.itemCount" class="m-cart-badge">{{ cart.cart.itemCount }}</span>
        </RouterLink>
        <button v-if="auth.isAuthenticated" type="button" class="m-cart-link" aria-label="退出登录" @click="logout">
          <LogOut :size="20" aria-hidden="true" />
        </button>
        <RouterLink v-else to="/m/login" class="m-brand" style="font-size: 14px">登录</RouterLink>
      </div>
    </header>

    <main id="main-content">
      <RouterView />
    </main>

    <nav class="m-tabbar" aria-label="底部导航">
      <RouterLink v-for="tab in tabs" :key="tab.to" :to="tab.to" class="m-tab" :class="{ 'is-active': isActive(tab.to) }">
        <component :is="tab.icon" :size="20" aria-hidden="true" />
        <span>{{ tab.label }}</span>
        <span v-if="tab.badge" class="m-tab-badge">{{ tab.badge }}</span>
      </RouterLink>
    </nav>
  </div>
</template>

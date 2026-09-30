<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Menu from 'lucide-vue-next/dist/esm/icons/menu.js'
import ShoppingCart from 'lucide-vue-next/dist/esm/icons/shopping-cart.js'
import UserRound from 'lucide-vue-next/dist/esm/icons/user-round.js'
import X from 'lucide-vue-next/dist/esm/icons/x.js'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import type { UserRole } from '@/types/domain'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()
const menuOpen = ref(false)

const navItems = computed(() => {
  const role = auth.user?.role as UserRole | undefined
  const items: Array<{ label: string; to: string }> = []
  if (role === 'MERCHANT') {
    items.push({ label: '订单', to: '/merchant/orders' }, { label: '菜单', to: '/merchant/menu' }, { label: '评价', to: '/merchant/reviews' }, { label: '优惠券', to: '/merchant/coupons' })
  } else if (role === 'RIDER') {
    items.push({ label: '工作台', to: '/rider' }, { label: '可接订单', to: '/rider/available' }, { label: '历史', to: '/rider/history' })
  } else if (role === 'ADMIN') {
    items.push({ label: '用户', to: '/admin/users' }, { label: '商户', to: '/admin/merchants' }, { label: '订单', to: '/admin/orders' }, { label: '评价', to: '/admin/reviews' })
  } else {
    items.push({ label: '发现美食', to: '/home' }, { label: '购物车', to: '/cart' }, { label: '订单', to: '/orders' })
  }
  return items
})

function logout(): void {
  auth.logout()
  void router.push('/login')
}
</script>

<template>
  <div class="min-h-screen bg-zinc-50 text-zinc-950">
    <header class="sticky top-0 z-40 border-b border-zinc-200 bg-white/95">
      <div class="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-4 md:px-8">
        <RouterLink to="/home" class="rounded-xl px-2 py-1 text-lg font-semibold text-zinc-950 focus:outline-none focus-visible:ring-2 focus-visible:ring-orange-500">食刻送达</RouterLink>
        <nav class="hidden flex-wrap items-center gap-4 md:flex" aria-label="主导航">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="rounded-xl px-3 py-2 text-sm text-zinc-600 transition-colors duration-200 ease-out hover:bg-zinc-100 hover:text-zinc-950 focus:outline-none focus-visible:ring-2 focus-visible:ring-orange-500"
            :class="route.path.startsWith(item.to) ? 'bg-orange-50 text-orange-700' : ''"
          >
            {{ item.label }}
          </RouterLink>
        </nav>
        <div class="flex items-center gap-4">
          <RouterLink v-if="auth.isAuthenticated" to="/cart" class="relative rounded-xl p-2 text-zinc-700 hover:bg-zinc-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-orange-500" aria-label="购物车">
            <ShoppingCart :size="20" aria-hidden="true" />
            <span v-if="cart.cart.itemCount" class="absolute -right-1 -top-1 rounded-xl bg-orange-500 px-2 py-0.5 text-xs text-white">{{ cart.cart.itemCount }}</span>
          </RouterLink>
          <RouterLink v-if="!auth.isAuthenticated" to="/login" class="rounded-xl bg-orange-500 px-4 py-2 text-sm font-medium text-white hover:bg-orange-600 focus:outline-none focus-visible:ring-2 focus-visible:ring-orange-500">登录</RouterLink>
          <button v-else type="button" class="hidden items-center gap-4 rounded-xl px-3 py-2 text-sm text-zinc-700 hover:bg-zinc-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-orange-500 md:flex" @click="logout">
            <UserRound :size="18" aria-hidden="true" />{{ auth.user?.nickname || auth.user?.username }} · 退出
          </button>
          <button type="button" class="rounded-xl p-2 text-zinc-700 hover:bg-zinc-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-orange-500 md:hidden" :aria-expanded="menuOpen" aria-label="打开菜单" @click="menuOpen = !menuOpen">
            <X v-if="menuOpen" :size="22" aria-hidden="true" />
            <Menu v-else :size="22" aria-hidden="true" />
          </button>
        </div>
      </div>
      <nav v-if="menuOpen" class="grid gap-4 border-t border-zinc-200 px-4 py-4 md:hidden" aria-label="移动导航">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" class="rounded-xl px-3 py-3 text-sm text-zinc-700 hover:bg-zinc-100" @click="menuOpen = false">{{ item.label }}</RouterLink>
        <button v-if="auth.isAuthenticated" type="button" class="rounded-xl px-3 py-3 text-left text-sm text-zinc-700 hover:bg-zinc-100" @click="logout">退出登录</button>
      </nav>
    </header>
    <main id="main-content" class="mx-auto w-full max-w-7xl px-4 py-8 md:px-8 md:py-12">
      <RouterView />
    </main>
  </div>
</template>

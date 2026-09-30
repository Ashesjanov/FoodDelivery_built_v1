import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { UserRole } from '@/types/domain'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    public?: boolean
    guestOnly?: boolean
    roles?: UserRole[]
    layout?: 'app' | 'auth'
  }
}

const routes: RouteRecordRaw[] = [
  { path: '/', name: 'landing', component: () => import('@/views/home/HomeView.vue'), meta: { title: '发现美食', public: true } },
  { path: '/home', name: 'home', component: () => import('@/views/home/HomeView.vue'), meta: { title: '发现美食', public: true } },
  { path: '/login', name: 'login', component: () => import('@/views/auth/LoginView.vue'), meta: { title: '登录', guestOnly: true, layout: 'auth' } },
  { path: '/register', name: 'register', component: () => import('@/views/auth/RegisterView.vue'), meta: { title: '注册', guestOnly: true, layout: 'auth' } },
  { path: '/merchants/:id', name: 'merchant-detail', component: () => import('@/views/merchant/PublicMerchantView.vue'), meta: { title: '商户详情', public: true } },
  { path: '/cart', name: 'cart', component: () => import('@/views/cart/CartView.vue'), meta: { title: '购物车', roles: ['CUSTOMER', 'ADMIN'] } },
  { path: '/checkout', name: 'checkout', component: () => import('@/views/checkout/CheckoutView.vue'), meta: { title: '确认订单', roles: ['CUSTOMER', 'ADMIN'] } },
  { path: '/orders', name: 'orders', component: () => import('@/views/orders/OrderListView.vue'), meta: { title: '我的订单', roles: ['CUSTOMER', 'ADMIN'] } },
  { path: '/orders/:id', name: 'order-detail', component: () => import('@/views/orders/OrderDetailView.vue'), meta: { title: '订单详情', roles: ['CUSTOMER', 'ADMIN'] } },
  { path: '/orders/:id/pay', name: 'order-pay', component: () => import('@/views/orders/PaymentView.vue'), meta: { title: '订单支付', roles: ['CUSTOMER', 'ADMIN'] } },
  { path: '/orders/:id/review', name: 'order-review', component: () => import('@/views/orders/ReviewView.vue'), meta: { title: '评价订单', roles: ['CUSTOMER', 'ADMIN'] } },
  { path: '/addresses', name: 'addresses', component: () => import('@/views/addresses/AddressView.vue'), meta: { title: '收货地址', roles: ['CUSTOMER', 'ADMIN'] } },
  { path: '/profile', name: 'profile', component: () => import('@/views/profile/ProfileView.vue'), meta: { title: '个人资料', roles: ['CUSTOMER', 'ADMIN', 'MERCHANT', 'RIDER'] } },

  { path: '/merchant/orders', name: 'merchant-orders', component: () => import('@/views/merchant/manage/MerchantOrdersView.vue'), meta: { title: '商家订单', roles: ['MERCHANT', 'ADMIN'] } },
  { path: '/merchant/menu', name: 'merchant-menu', component: () => import('@/views/merchant/manage/MerchantMenuManageView.vue'), meta: { title: '菜单管理', roles: ['MERCHANT', 'ADMIN'] } },
  { path: '/merchant/profile', name: 'merchant-profile', component: () => import('@/views/merchant/manage/MerchantProfileView.vue'), meta: { title: '门店资料', roles: ['MERCHANT', 'ADMIN'] } },
  { path: '/merchant/reviews', name: 'merchant-reviews', component: () => import('@/views/merchant/manage/MerchantReviewsView.vue'), meta: { title: '商家评价', roles: ['MERCHANT', 'ADMIN'] } },
  { path: '/merchant/coupons', name: 'merchant-coupons', component: () => import('@/views/merchant/manage/MerchantCouponsView.vue'), meta: { title: '优惠券', roles: ['MERCHANT', 'ADMIN'] } },

  { path: '/rider', name: 'rider-dashboard', component: () => import('@/views/rider/RiderDashboardView.vue'), meta: { title: '骑手工作台', roles: ['RIDER', 'ADMIN'] } },
  { path: '/rider/available', name: 'rider-available', component: () => import('@/views/rider/RiderAvailableView.vue'), meta: { title: '可接订单', roles: ['RIDER', 'ADMIN'] } },
  { path: '/rider/orders/:id', name: 'rider-order', component: () => import('@/views/rider/RiderOrderView.vue'), meta: { title: '配送详情', roles: ['RIDER', 'ADMIN'] } },
  { path: '/rider/history', name: 'rider-history', component: () => import('@/views/rider/RiderHistoryView.vue'), meta: { title: '配送历史', roles: ['RIDER', 'ADMIN'] } },
  { path: '/rider/profile', name: 'rider-profile', component: () => import('@/views/rider/RiderProfileView.vue'), meta: { title: '骑手资料', roles: ['RIDER', 'ADMIN'] } },

  { path: '/admin/users', name: 'admin-users', component: () => import('@/views/admin/AdminUsersView.vue'), meta: { title: '用户管理', roles: ['ADMIN'] } },
  { path: '/admin/categories', name: 'admin-categories', component: () => import('@/views/admin/AdminCategoriesView.vue'), meta: { title: '分类管理', roles: ['ADMIN'] } },
  { path: '/admin/merchants', name: 'admin-merchants', component: () => import('@/views/admin/AdminMerchantsView.vue'), meta: { title: '商户管理', roles: ['ADMIN'] } },
  { path: '/admin/orders', name: 'admin-orders', component: () => import('@/views/admin/AdminOrdersView.vue'), meta: { title: '订单管理', roles: ['ADMIN'] } },
  { path: '/admin/coupons', name: 'admin-coupons', component: () => import('@/views/admin/AdminCouponsView.vue'), meta: { title: '优惠券管理', roles: ['ADMIN'] } },
  { path: '/admin/reviews', name: 'admin-reviews', component: () => import('@/views/admin/AdminReviewsView.vue'), meta: { title: '评价管理', roles: ['ADMIN'] } },

  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue'), meta: { title: '页面不存在', public: true } },
]

function roleFromToken(token: string): UserRole | null {
  try {
    const payload = JSON.parse(atob(token.split('.')[1] ?? '')) as { role?: string }
    return (payload.role as UserRole | undefined) ?? null
  } catch {
    return null
  }
}

function homeForRole(role: UserRole | null): string {
  if (role === 'MERCHANT') return '/merchant/orders'
  if (role === 'RIDER') return '/rider'
  if (role === 'ADMIN') return '/admin/orders'
  return '/home'
}

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  const token = getToken()
  const role = token ? roleFromToken(token) : null

  if (to.meta.guestOnly) {
    if (token && role) return homeForRole(role)
    return true
  }
  if (to.meta.public) return true
  if (!token) return { path: '/login', query: { redirect: to.fullPath } }

  const auth = useAuthStore()
  void auth.initialize()
  const effectiveRole = auth.role ?? role
  const allowedRoles = to.meta.roles

  if (allowedRoles && (!effectiveRole || !allowedRoles.includes(effectiveRole))) return homeForRole(effectiveRole)
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 食刻送达` : '食刻送达'
})

export default router

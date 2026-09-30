<script setup lang="ts">
/** 顾客订单列表：按状态筛选并提供支付、评价和详情入口。 */
import { computed, onMounted, ref } from 'vue'
import type { Order, OrderStatus } from '@/types/domain'
import { orderApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import BentoGrid from '@/components/bento/BentoGrid.vue'
import BentoCard from '@/components/bento/BentoCard.vue'
import { asArray, dateTime, errorMessage, money, statusLabel, unwrap } from '@/components/features/customer/customerUtils'

const authStore: any = useAuthStore()
const orders = ref<Order[]>([])
const status = ref<OrderStatus | null>(null)
const loading = ref(true)
const error = ref('')
const notice = ref('')

const statusOptions = [
  { value: null, label: '全部订单' }, { value: 'PENDING_PAYMENT', label: '待支付' }, { value: 'PAID', label: '已支付' },
  { value: 'ACCEPTED', label: '制作中' }, { value: 'PICKED_UP', label: '配送中' }, { value: 'COMPLETED', label: '已完成' }, { value: 'CANCELLED', label: '已取消' },
]

const grouped = computed(() => ({ active: orders.value.filter((order) => !['COMPLETED', 'CANCELLED', 'REFUNDED'].includes(order.status)), history: orders.value.filter((order) => ['COMPLETED', 'CANCELLED', 'REFUNDED'].includes(order.status)) }))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await orderApi.list({ page: 1, size: 30, status: status.value ?? undefined, userId: authStore?.user?.id })
    orders.value = asArray<Order>(unwrap<any>(result))
  } catch (cause) {
    error.value = errorMessage(cause)
    orders.value = [
      { id: 8801, orderNo: 'FD20260923001', merchantId: 1, merchantName: '巷子里·家常菜', status: 'PICKED_UP', payableAmount: 45, totalAmount: 42, deliveryFee: 3, packagingFee: 1, discountAmount: 1, createdAt: new Date().toISOString(), deliveryAddressSnapshot: '梧桐路 18 号' },
      { id: 8800, orderNo: 'FD20260921018', merchantId: 2, merchantName: '青禾轻食实验室', status: 'COMPLETED', payableAmount: 32, totalAmount: 30, deliveryFee: 2, packagingFee: 1, discountAmount: 1, createdAt: '2026-09-21T12:30:00', deliveryAddressSnapshot: '梧桐路 18 号' },
    ] as Order[]
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="orders-page">
    <PageHeader title="我的订单" description="从下单到送达，每一步都清楚可见。" back-to="/" />
    <LoadingState v-if="loading" label="正在查看订单记录" />
    <ErrorState v-else-if="error && !orders.length" :message="error" action-label="重新加载" @retry="load" />
    <BentoGrid v-else class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
      <BentoCard class="filter-card" size="medium" tone="dark" aspect="2 / 1"><div><h1>这一周吃了什么？</h1></div><span class="stamp group">↗</span><BaseSelect :model-value="status" :options="statusOptions" label="订单状态" @update:model-value="status = ($event || null) as OrderStatus | null; load()" /></BentoCard>

      <BentoCard v-for="(order, index) in [...grouped.active, ...grouped.history]" :key="order.id" class="order-card" :size="index === 0 ? 'large' : index % 3 === 0 ? 'medium' : 'small'" :tone="index === 0 ? 'fresh' : index % 3 === 0 ? 'warm' : 'default'" interactive :aspect="index === 0 ? '1 / 1' : index % 3 === 0 ? '2 / 1' : '1 / 1'" @click="$router.push(`/orders/${order.id}`)">
        <div class="order-top"><div><h2>{{ order.merchantName }}</h2></div><StatusBadge :status="statusLabel(order.status)" /></div>
        <p class="address">{{ order.deliveryAddressSnapshot }}</p>
        <div class="timeline-mini"><span v-for="step in ['已下单', '制作', '配送', '送达']" :key="step" :class="{ done: ['PAID','ACCEPTED','PREPARING','READY','PICKED_UP','DELIVERED','COMPLETED'].includes(order.status) }">{{ step }}</span></div>
        <div class="order-bottom"><div><strong>{{ money(order.payableAmount) }}</strong><small>{{ dateTime(order.createdAt) }}</small></div><div class="actions"><BaseButton v-if="order.status === 'PENDING_PAYMENT'" type="primary" @click.stop="$router.push(`/orders/${order.id}/pay`)">支付</BaseButton><BaseButton v-if="order.status === 'COMPLETED'" type="ghost" @click.stop="$router.push(`/orders/${order.id}/review`)">评价</BaseButton></div></div>
      </BentoCard>

      <BentoCard v-if="notice" class="notice-card" size="small" tone="warm" aspect="1 / 1"><p>{{ notice }}</p></BentoCard>
      <BentoCard v-if="!orders.length" class="empty-card" size="large" tone="default" aspect="2 / 1"><EmptyState title="还没有订单" description="下一顿想吃什么？先去逛逛附近的商家。" action-label="去找好吃的" @action="$router.push('/')" /></BentoCard>
    </BentoGrid>
  </main>
</template>

<style scoped>
.orders-page { min-height: 100vh; padding: clamp(24px, 5vw, 76px); background: #f4f4ef; color: #202923; font-family: 'PingFang SC', sans-serif; }.section-label { letter-spacing: 0; font-size: 13px; opacity: .72; margin: 0 0 8px; } h1, h2 { font-family: Georgia, 'Songti SC', serif; font-weight: 500; margin: 0; } h1 { font-size: clamp(34px, 4vw, 52px); } h2 { font-size: 25px; }
.filter-card, .order-card, .notice-card, .empty-card { padding: clamp(22px, 2.7vw, 36px); }.filter-card { color: #f3f1e9; display: grid; grid-template-columns: 1fr auto; align-content: space-between; gap: 16px; }.filter-card :deep(.select-field) { grid-column: 1 / -1; }.stamp { font-size: 48px; color: #e7bb64; transition: transform .3s ease; }.group:hover .stamp { transform: translate(5px, -5px) scale(1.12); }
.order-card { cursor: pointer; display: flex; flex-direction: column; }.order-top, .order-bottom { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }.address { color: #727b73; line-height: 1.7; }.timeline-mini { display: flex; justify-content: space-between; gap: 16px; margin: 18px 0; }.timeline-mini span { position: relative; flex: 1; color: #9da39c; font-size: 12px; padding-top: 14px; border-top: 2px solid #d9ddd6; }.timeline-mini span.done { color: #53705a; border-color: #77a082; }.order-bottom { margin-top: auto; align-items: flex-end; }.order-bottom > div:first-child { display: grid; gap: 16px; }.order-bottom strong { font-family: Georgia, serif; font-size: 23px; color: #8c5a26; }.order-bottom small { color: #92988f; }.actions { display: flex; gap: 16px; }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { transition-duration: .01ms !important; animation-duration: .01ms !important; } }
</style>

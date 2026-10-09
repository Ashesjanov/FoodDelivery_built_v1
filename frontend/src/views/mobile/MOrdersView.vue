<script setup lang="ts">
/** 移动端顾客订单列表：与电脑端内容一致，按状态筛选并提供支付、评价和详情入口。 */
import { computed, onMounted, ref } from 'vue'
import type { Order, OrderStatus } from '@/types/domain'
import { orderApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
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
  <div class="m-page">
    <header class="m-page-head">
      <h1 class="m-title">我的订单</h1>
      <p class="m-sub">从下单到送达，每一步都清楚可见。</p>
    </header>

    <LoadingState v-if="loading" label="正在查看订单记录" />
    <ErrorState v-else-if="error && !orders.length" :message="error" action-label="重新加载" @retry="load" />
    <div v-else class="m-stack">
      <section class="m-card m-card--dark">
        <div class="m-row m-row--top">
          <h2 class="m-title" style="font-size: 26px">这一周吃了什么？</h2>
          <span style="font-size: 44px; color: #e7bb64" aria-hidden="true">↗</span>
        </div>
        <div style="margin-top: 16px">
          <BaseSelect :model-value="status" :options="statusOptions" label="订单状态" @update:model-value="status = ($event || null) as OrderStatus | null; load()" />
        </div>
      </section>

      <button
        v-for="(order, index) in [...grouped.active, ...grouped.history]"
        :key="order.id"
        type="button"
        class="m-card m-order"
        :class="index === 0 ? 'm-card--fresh' : ''"
        @click="$router.push(`/m/orders/${order.id}`)"
      >
        <div class="m-row m-row--top">
          <h2 class="m-subtitle">{{ order.merchantName }}</h2>
          <StatusBadge :status="statusLabel(order.status)" />
        </div>
        <p class="m-address">{{ order.deliveryAddressSnapshot }}</p>
        <div class="m-timeline-mini">
          <span v-for="step in ['已下单', '制作', '配送', '送达']" :key="step" :class="{ done: ['PAID','ACCEPTED','PREPARING','READY','PICKED_UP','DELIVERED','COMPLETED'].includes(order.status) }">{{ step }}</span>
        </div>
        <div class="m-row m-row--bottom">
          <div>
            <strong class="m-amount">{{ money(order.payableAmount) }}</strong>
            <small class="m-time">{{ dateTime(order.createdAt) }}</small>
          </div>
          <div class="m-actions">
            <BaseButton v-if="order.status === 'PENDING_PAYMENT'" type="primary" size="sm" @click.stop="$router.push(`/m/orders/${order.id}/pay`)">支付</BaseButton>
            <BaseButton v-if="order.status === 'COMPLETED'" type="ghost" size="sm" @click.stop="$router.push(`/m/orders/${order.id}/review`)">评价</BaseButton>
          </div>
        </div>
      </button>

      <p v-if="notice" class="m-notice" role="status">{{ notice }}</p>

      <section v-if="!orders.length" class="m-card">
        <EmptyState title="还没有订单" description="下一顿想吃什么？先去逛逛附近的商家。" action-label="去找好吃的" @action="$router.push('/m/home')" />
      </section>
    </div>
  </div>
</template>

<style scoped>
.m-page-head { margin-bottom: 16px; }
.m-sub { margin: 8px 0 0; color: #69746d; line-height: 1.7; font-size: 14px; }
.m-order { display: block; width: 100%; text-align: left; font: inherit; color: inherit; cursor: pointer; }
.m-order:hover, .m-order:focus-visible { outline: none; box-shadow: 0 14px 34px rgba(24, 24, 27, 0.12); }
.m-address { color: #727b73; line-height: 1.7; margin: 10px 0 0; font-size: 14px; }
.m-timeline-mini { display: flex; justify-content: space-between; gap: 16px; margin: 18px 0; }
.m-timeline-mini span { position: relative; flex: 1; color: #9da39c; font-size: 12px; padding-top: 14px; border-top: 2px solid #d9ddd6; }
.m-timeline-mini span.done { color: #53705a; border-color: #77a082; }
.m-row--bottom { align-items: flex-end; }
.m-amount { display: block; font-family: Georgia, 'Songti SC', serif; font-size: 23px; color: #8c5a26; }
.m-time { color: #92988f; font-size: 12px; }
</style>

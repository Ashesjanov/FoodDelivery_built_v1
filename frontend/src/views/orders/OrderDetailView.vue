<script setup lang="ts">
/** 订单详情页：展示履约时间线、费用、餐品快照和可用操作。 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { Order, OrderDetail, OrderItem } from '@/types/domain'
import { orderApi } from '@/api'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import BentoGrid from '@/components/bento/BentoGrid.vue'
import BentoCard from '@/components/bento/BentoCard.vue'
import { dateTime, errorMessage, money, orderIdFromRoute, statusLabel, unwrap } from '@/components/features/customer/customerUtils'

const route = useRoute()
const router = useRouter()
const orderId = orderIdFromRoute(route)
const detail = ref<OrderDetail | null>(null)
const loading = ref(true)
const error = ref('')
const notice = ref('')
const action = ref('')
const canceling = ref(false)
const cancelReason = ref('')

const order = computed<Order | null>(() => detail.value?.order ?? null)
const items = computed<OrderItem[]>(() => detail.value?.items ?? [])
const timeline = computed(() => {
  const value = order.value
  if (!value) return []
  return [
    { label: '订单提交', time: value.createdAt, done: true },
    { label: '商家接单', time: value.acceptedAt, done: !!value.acceptedAt },
    { label: '骑手取餐', time: value.pickedAt, done: !!value.pickedAt },
    { label: '订单送达', time: value.deliveredAt, done: !!value.deliveredAt },
  ]
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    detail.value = unwrap<OrderDetail>(await orderApi.detail(orderId))
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function cancelOrder() {
  if (action.value || canceling.value) return
  canceling.value = true
  action.value = 'cancel'
  try {
    detail.value = unwrap<OrderDetail>(await orderApi.userCancel(orderId, cancelReason.value.trim() || undefined))
    notice.value = '订单已取消'
    cancelReason.value = ''
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    action.value = ''
    canceling.value = false
  }
}

async function completeOrder() {
  if (action.value) return
  action.value = 'complete'
  try {
    detail.value = unwrap<OrderDetail>(await orderApi.complete(orderId))
    notice.value = '已确认收货，祝你用餐愉快'
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    action.value = ''
  }
}

onMounted(load)
</script>

<template>
  <main class="detail-page">
    <PageHeader title="订单详情" description="状态、时间和餐品快照都记录在这里。" back-to="/orders" />
    <LoadingState v-if="loading" label="正在追踪这笔订单" />
    <ErrorState v-else-if="error && !order" :message="error" action-label="重新加载" @retry="load" />
    <BentoGrid v-else-if="order" class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
      <BentoCard class="status-card" size="large" tone="dark" aspect="1 / 1">
        <div class="status-top"><div><h1>{{ statusLabel(order.status) }}</h1></div><StatusBadge :status="statusLabel(order.status)" /></div>
        <p class="lead">{{ order.status === 'PICKED_UP' ? '骑手正在路上，请保持电话畅通。' : order.status === 'PENDING_PAYMENT' ? '完成支付后，商家会立即开始准备。' : '感谢你的耐心，我们会在每个环节保持透明。' }}</p>
        <div class="actions"><BaseButton v-if="order.status === 'PENDING_PAYMENT'" type="primary" @click="router.push(`/orders/${order.id}/pay`)">立即支付</BaseButton><BaseButton v-if="order.status === 'DELIVERED'" type="primary" :loading="action === 'complete'" @click="completeOrder">确认完成</BaseButton><BaseButton v-if="order.status === 'COMPLETED'" type="ghost" @click="router.push(`/orders/${order.id}/review`)">评价本单</BaseButton></div>
        <p v-if="notice" class="notice">{{ notice }}</p>
      </BentoCard>

      <BentoCard class="timeline-card" size="medium" tone="fresh" aspect="2 / 1"><h2>配送进度</h2><ol class="timeline"><li v-for="step in timeline" :key="step.label" :class="{ done: step.done }"><span class="dot"></span><div><strong>{{ step.label }}</strong><small>{{ dateTime(step.time) }}</small></div></li></ol></BentoCard>

      <BentoCard class="items-card" size="medium" tone="default" aspect="2 / 1"><h2>餐品</h2><div v-if="items.length" class="item-list"><div v-for="item in items" :key="item.id" class="item-row"><div><strong>{{ item.dishName }}</strong><small>{{ item.specName || '标准份' }} × {{ item.quantity }}</small><small v-if="item.note">备注：{{ item.note }}</small></div><span>{{ money(item.subtotal) }}</span></div></div><EmptyState v-else title="没有餐品记录" description="这笔订单没有可展示的餐品快照。" /></BentoCard>

      <BentoCard class="fee-card" size="small" tone="warm" aspect="1 / 1"><h2>实付</h2><p class="total">{{ money(order.payableAmount) }}</p><dl><div><dt>餐品</dt><dd>{{ money(order.totalAmount) }}</dd></div><div><dt>配送与包装</dt><dd>{{ money(order.deliveryFee + order.packagingFee) }}</dd></div><div><dt>优惠</dt><dd>-{{ money(order.discountAmount) }}</dd></div></dl></BentoCard>

      <BentoCard class="address-card" size="small" tone="accent" aspect="1 / 1"><h2>收货信息</h2><p>{{ order.deliveryAddressSnapshot }}</p><p>{{ order.contactPhone || '未填写电话' }}</p><p v-if="order.userNote">备注：{{ order.userNote }}</p></BentoCard>

      <BentoCard v-if="['PENDING_PAYMENT', 'PAID', 'ACCEPTED'].includes(order.status)" class="cancel-card" size="small" tone="default" aspect="1 / 1"><h2>需要取消？</h2><BaseInput v-model="cancelReason" label="取消原因" placeholder="告诉我们原因" /><BaseButton type="danger" :loading="canceling" :disabled="!cancelReason.trim()" @click="cancelOrder">取消订单</BaseButton></BentoCard>
    </BentoGrid>
    <BentoGrid v-else class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6"><BentoCard class="empty-card" size="large" tone="default" aspect="2 / 1"><EmptyState title="找不到这笔订单" description="订单可能已被删除，或链接已经失效。" action-label="返回订单列表" @action="router.push('/orders')" /></BentoCard></BentoGrid>
  </main>
</template>

<style scoped>
.detail-page { min-height: 100vh; padding: clamp(24px, 5vw, 76px); background: #f4f3ee; color: #202923; font-family: 'PingFang SC', sans-serif; }.section-label { letter-spacing: 0; font-size: 13px; opacity: .72; margin: 0 0 8px; } h1, h2 { font-family: Georgia, 'Songti SC', serif; font-weight: 500; margin: 0; } h1 { font-size: clamp(38px, 5vw, 64px); } h2 { font-size: 25px; }
.status-card, .timeline-card, .items-card, .fee-card, .address-card, .cancel-card, .empty-card { padding: clamp(22px, 2.7vw, 36px); }.status-card { color: #f5f2e9; display: flex; flex-direction: column; justify-content: space-between; }.status-top { display: flex; justify-content: space-between; gap: 16px; }.lead { color: #c7cec6; line-height: 1.8; }.actions { display: flex; flex-wrap: wrap; gap: 16px; }.notice { color: #f0c47b; margin-bottom: 0; }
.timeline { list-style: none; padding: 0; margin: 24px 0 0; display: grid; gap: 16px; }.timeline li { display: flex; gap: 16px; color: #8a928a; }.timeline li.done { color: #31553a; }.dot { width: 14px; height: 14px; border: 3px solid #bfc9bf; border-radius: 50%; margin-top: 3px; }.timeline li.done .dot { background: #5f8a68; border-color: #5f8a68; box-shadow: 0 0 0 5px rgba(95,138,104,.15); }.timeline div { display: grid; gap: 16px; }.timeline small { color: #969d95; }
.item-list { display: grid; gap: 16px; margin-top: 20px; }.item-row { display: flex; justify-content: space-between; gap: 16px; padding-bottom: 12px; border-bottom: 1px solid #e1e2dc; }.item-row div { display: grid; gap: 16px; }.item-row small { color: #858b83; }.total { font-family: Georgia, serif; font-size: 44px; color: #8d5827; margin: 14px 0; }.fee-card dl { display: grid; gap: 16px; }.fee-card dl div, .fee-card dt, .fee-card dd { display: flex; justify-content: space-between; margin: 0; color: #766a5b; }.address-card p { color: #6f6b64; line-height: 1.7; }.cancel-card > * { margin-top: 14px; }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { transition-duration: .01ms !important; animation-duration: .01ms !important; } }
</style>

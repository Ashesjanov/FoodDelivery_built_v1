<script setup lang="ts">
/** 移动端订单详情页：与电脑端内容一致，展示履约时间线、费用、餐品快照和可用操作。 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { Order, OrderDetail, OrderItem } from '@/types/domain'
import { orderApi } from '@/api'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
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
  <div class="m-page">
    <header class="m-page-head">
      <RouterLink class="m-back" to="/m/orders"><span aria-hidden="true">←</span> 返回订单列表</RouterLink>
      <h1 class="m-title" style="margin-top: 8px">订单详情</h1>
      <p class="m-sub">状态、时间和餐品快照都记录在这里。</p>
    </header>

    <LoadingState v-if="loading" label="正在追踪这笔订单" />
    <ErrorState v-else-if="error && !order" :message="error" action-label="重新加载" @retry="load" />
    <div v-else-if="order" class="m-stack">
      <section class="m-card m-card--dark">
        <div class="m-row m-row--top">
          <h2 class="m-title" style="font-size: 28px">{{ statusLabel(order.status) }}</h2>
          <StatusBadge :status="statusLabel(order.status)" />
        </div>
        <p class="m-lead">{{ order.status === 'PICKED_UP' ? '骑手正在路上，请保持电话畅通。' : order.status === 'PENDING_PAYMENT' ? '完成支付后，商家会立即开始准备。' : '感谢你的耐心，我们会在每个环节保持透明。' }}</p>
        <div class="m-actions" style="margin-top: 16px">
          <BaseButton v-if="order.status === 'PENDING_PAYMENT'" type="primary" @click="router.push(`/m/orders/${order.id}/pay`)">立即支付</BaseButton>
          <BaseButton v-if="order.status === 'DELIVERED'" type="primary" :loading="action === 'complete'" @click="completeOrder">确认完成</BaseButton>
          <BaseButton v-if="order.status === 'COMPLETED'" type="ghost" @click="router.push(`/m/orders/${order.id}/review`)">评价本单</BaseButton>
        </div>
        <p v-if="notice" class="m-notice" role="status">{{ notice }}</p>
      </section>

      <section class="m-card m-card--fresh">
        <h2 class="m-subtitle">配送进度</h2>
        <ol class="m-timeline">
          <li v-for="step in timeline" :key="step.label" :class="{ done: step.done }">
            <span class="m-dot" aria-hidden="true"></span>
            <div><strong>{{ step.label }}</strong><small>{{ dateTime(step.time) }}</small></div>
          </li>
        </ol>
      </section>

      <section class="m-card">
        <h2 class="m-subtitle">餐品</h2>
        <div v-if="items.length" class="m-item-list">
          <div v-for="item in items" :key="item.id" class="m-item-row">
            <div>
              <strong>{{ item.dishName }}</strong>
              <small>{{ item.specName || '标准份' }} × {{ item.quantity }}</small>
              <small v-if="item.note">备注：{{ item.note }}</small>
            </div>
            <span>{{ money(item.subtotal) }}</span>
          </div>
        </div>
        <EmptyState v-else title="没有餐品记录" description="这笔订单没有可展示的餐品快照。" />
      </section>

      <section class="m-card m-card--warm">
        <h2 class="m-subtitle">实付</h2>
        <p class="m-total">{{ money(order.payableAmount) }}</p>
        <dl class="m-fee-list">
          <div><dt>餐品</dt><dd>{{ money(order.totalAmount) }}</dd></div>
          <div><dt>配送与包装</dt><dd>{{ money(order.deliveryFee + order.packagingFee) }}</dd></div>
          <div><dt>优惠</dt><dd>-{{ money(order.discountAmount) }}</dd></div>
        </dl>
      </section>

      <section class="m-card m-card--accent">
        <h2 class="m-subtitle">收货信息</h2>
        <p class="m-info">{{ order.deliveryAddressSnapshot }}</p>
        <p class="m-info">{{ order.contactPhone || '未填写电话' }}</p>
        <p v-if="order.userNote" class="m-info">备注：{{ order.userNote }}</p>
      </section>

      <section v-if="['PENDING_PAYMENT', 'PAID', 'ACCEPTED'].includes(order.status)" class="m-card">
        <h2 class="m-subtitle">需要取消？</h2>
        <div style="margin-top: 16px">
          <BaseInput v-model="cancelReason" label="取消原因" placeholder="告诉我们原因" />
        </div>
        <div style="margin-top: 16px">
          <BaseButton type="danger" :loading="canceling" :disabled="!cancelReason.trim()" @click="cancelOrder">取消订单</BaseButton>
        </div>
      </section>
    </div>
    <div v-else class="m-stack">
      <section class="m-card">
        <EmptyState title="找不到这笔订单" description="订单可能已被删除，或链接已经失效。" action-label="返回订单列表" @action="router.push('/m/orders')" />
      </section>
    </div>
  </div>
</template>

<style scoped>
.m-page-head { margin-bottom: 16px; }
.m-back { display: inline-flex; align-items: center; gap: 16px; color: #69746d; font-size: 14px; text-decoration: none; min-height: 44px; }
.m-back:hover, .m-back:focus-visible { color: #1d2722; outline: none; }
.m-sub { margin: 8px 0 0; color: #69746d; line-height: 1.7; font-size: 14px; }
.m-timeline { list-style: none; padding: 0; margin: 20px 0 0; display: flex; flex-direction: column; gap: 16px; }
.m-timeline li { display: flex; gap: 16px; color: #8a928a; }
.m-timeline li.done { color: #31553a; }
.m-dot { width: 14px; height: 14px; flex-shrink: 0; border: 3px solid #bfc9bf; border-radius: 50%; margin-top: 3px; }
.m-timeline li.done .m-dot { background: #5f8a68; border-color: #5f8a68; box-shadow: 0 0 0 5px rgba(95, 138, 104, 0.15); }
.m-timeline div { display: flex; flex-direction: column; gap: 16px; }
.m-timeline small { color: #969d95; }
.m-item-list { display: flex; flex-direction: column; gap: 16px; margin-top: 16px; }
.m-item-row { display: flex; justify-content: space-between; gap: 16px; padding-bottom: 12px; border-bottom: 1px solid #e1e2dc; }
.m-item-row div { display: flex; flex-direction: column; gap: 16px; }
.m-item-row small { color: #858b83; }
.m-total { font-family: Georgia, 'Songti SC', serif; font-size: 40px; color: #8d5827; margin: 12px 0; }
.m-fee-list { display: flex; flex-direction: column; gap: 16px; margin: 0; }
.m-fee-list div { display: flex; justify-content: space-between; color: #766a5b; }
.m-fee-list dd { margin: 0; }
.m-info { color: #6f6b64; line-height: 1.7; margin: 8px 0 0; }
</style>

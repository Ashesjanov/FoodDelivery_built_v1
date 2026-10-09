<script setup lang="ts">
/** 移动端模拟支付页：与电脑端内容一致，确认订单金额后调用安全的演示支付接口。 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { Order, OrderDetail, Payment, PaymentMethod } from '@/types/domain'
import { orderApi, paymentApi } from '@/api'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { errorMessage, money, orderIdFromRoute, statusLabel, unwrap } from '@/components/features/customer/customerUtils'

const route = useRoute()
const router = useRouter()
const orderId = orderIdFromRoute(route)
const detail = ref<OrderDetail | null>(null)
const payment = ref<Payment | null>(null)
const method = ref<PaymentMethod>('MOCK_BALANCE')
const transactionNo = ref('')
const loading = ref(true)
const error = ref('')
const paying = ref(false)
const notice = ref('')

const order = computed<Order | null>(() => detail.value?.order ?? null)
const methodOptions = [
  { value: 'MOCK_BALANCE', label: '模拟余额支付' }, { value: 'ALIPAY', label: '支付宝（模拟）' }, { value: 'WECHAT', label: '微信支付（模拟）' },
]

async function load() {
  loading.value = true
  error.value = ''
  try {
    detail.value = unwrap<OrderDetail>(await orderApi.detail(orderId))
    payment.value = await paymentApi.byOrder(orderId).catch(() => null)
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function pay() {
  if (paying.value || !order.value || order.value.status !== 'PENDING_PAYMENT') return
  paying.value = true
  notice.value = ''
  try {
    payment.value = unwrap<Payment>(await paymentApi.mockPay(orderId, { transactionNo: transactionNo.value.trim() || undefined }))
    notice.value = '支付成功，商家正在确认订单'
    await router.replace(`/m/orders/${orderId}`)
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    paying.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="m-page">
    <header class="m-page-head">
      <RouterLink class="m-back" :to="`/m/orders/${orderId}`"><span aria-hidden="true">←</span> 返回订单详情</RouterLink>
      <h1 class="m-title" style="margin-top: 8px">模拟支付</h1>
      <p class="m-sub">使用安全的演示支付完成订单，不会产生真实扣款。</p>
    </header>

    <LoadingState v-if="loading" label="正在确认支付信息" />
    <ErrorState v-else-if="error && !order" :message="error" action-label="重新加载" @retry="load" />
    <div v-else-if="order" class="m-stack">
      <section class="m-card m-card--dark">
        <div class="m-row m-row--top">
          <h2 class="m-title" style="font-size: 28px">完成支付</h2>
          <span style="font-size: 44px; color: #e6bb64" aria-hidden="true">◇</span>
        </div>
        <div class="m-amount">
          <span>应付金额</span>
          <strong>{{ money(order.payableAmount) }}</strong>
        </div>
        <div class="m-row" style="margin-top: 16px; color: #c1c7c0; font-size: 13px">
          <span>{{ order.merchantName }}</span>
          <span>{{ order.orderNo }}</span>
        </div>
        <div style="margin-top: 12px">
          <StatusBadge :status="statusLabel(order.status)" />
        </div>
      </section>

      <section class="m-card m-card--fresh">
        <h2 class="m-subtitle">支付方式</h2>
        <div class="m-form" style="margin-top: 16px">
          <BaseSelect :model-value="method" :options="methodOptions" label="选择支付方式" @update:model-value="method = $event as PaymentMethod" />
          <BaseInput v-model="transactionNo" label="交易号（选填）" placeholder="留空时自动生成演示交易号" />
        </div>
        <div style="margin-top: 16px">
          <BaseButton type="primary" :loading="paying" :disabled="order.status !== 'PENDING_PAYMENT'" block @click="pay">确认支付 {{ money(order.payableAmount) }}</BaseButton>
        </div>
        <p v-if="notice" class="m-notice" role="alert">{{ notice }}</p>
      </section>

      <section class="m-card m-card--warm">
        <h2 class="m-subtitle">订单摘要</h2>
        <dl class="m-fee-list">
          <div><dt>餐品小计</dt><dd>{{ money(order.totalAmount) }}</dd></div>
          <div><dt>配送费</dt><dd>{{ money(order.deliveryFee) }}</dd></div>
          <div><dt>包装费</dt><dd>{{ money(order.packagingFee) }}</dd></div>
          <div><dt>优惠</dt><dd>-{{ money(order.discountAmount) }}</dd></div>
        </dl>
      </section>

      <section class="m-card m-card--accent">
        <h2 class="m-subtitle">支付状态</h2>
        <div style="margin-top: 12px">
          <StatusBadge :status="payment ? statusLabel(payment.status) : 'PENDING'" />
        </div>
        <p v-if="payment" class="m-info">{{ payment.transactionNo || payment.paymentNo }}</p>
        <p v-else class="m-muted" style="margin-top: 12px">等待发起支付</p>
      </section>

      <section class="m-card">
        <h2 class="m-subtitle">遇到问题？</h2>
        <p class="m-info">支付失败不会重复扣款。请返回订单详情重新发起支付。</p>
        <div style="margin-top: 16px">
          <BaseButton type="ghost" @click="router.push(`/m/orders/${orderId}`)">返回订单</BaseButton>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.m-page-head { margin-bottom: 16px; }
.m-back { display: inline-flex; align-items: center; gap: 16px; color: #69746d; font-size: 14px; text-decoration: none; min-height: 44px; }
.m-back:hover, .m-back:focus-visible { color: #1d2722; outline: none; }
.m-sub { margin: 8px 0 0; color: #69746d; line-height: 1.7; font-size: 14px; }
.m-amount { display: flex; flex-direction: column; gap: 16px; margin-top: 16px; color: #bfc7bf; }
.m-amount strong { font-family: Georgia, 'Songti SC', serif; font-size: 52px; color: #efc46f; font-weight: 500; }
.m-fee-list { display: flex; flex-direction: column; gap: 16px; margin: 16px 0 0; }
.m-fee-list div { display: flex; justify-content: space-between; color: #766858; }
.m-fee-list dd { margin: 0; color: #3c433c; }
.m-info { color: #6e736d; line-height: 1.7; margin: 10px 0 0; }
</style>

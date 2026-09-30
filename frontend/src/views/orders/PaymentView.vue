<script setup lang="ts">
/** 模拟支付页：确认订单金额后调用安全的演示支付接口。 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { Order, OrderDetail, Payment, PaymentMethod } from '@/types/domain'
import { orderApi, paymentApi } from '@/api'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import BentoGrid from '@/components/bento/BentoGrid.vue'
import BentoCard from '@/components/bento/BentoCard.vue'
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
    await router.replace(`/orders/${orderId}`)
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    paying.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="payment-page">
    <PageHeader title="模拟支付" description="使用安全的演示支付完成订单，不会产生真实扣款。" :back-to="`/orders/${orderId}`" />
    <LoadingState v-if="loading" label="正在确认支付信息" />
    <ErrorState v-else-if="error && !order" :message="error" action-label="重新加载" @retry="load" />
    <BentoGrid v-else-if="order" class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
      <BentoCard class="amount-card" size="large" tone="dark" aspect="1 / 1">
        <div class="amount-top"><div><h1>完成支付</h1></div><span class="lock-icon group">◇</span></div>
        <div class="amount"><span>应付金额</span><strong>{{ money(order.payableAmount) }}</strong></div>
        <div class="order-ref"><span>{{ order.merchantName }}</span><span>{{ order.orderNo }}</span></div>
        <StatusBadge :status="statusLabel(order.status)" />
      </BentoCard>

      <BentoCard class="method-card" size="medium" tone="fresh" aspect="2 / 1">
        <h2>支付方式</h2>
        <BaseSelect :model-value="method" :options="methodOptions" label="选择支付方式" @update:model-value="method = $event as PaymentMethod" />
        <BaseInput v-model="transactionNo" label="交易号（选填）" placeholder="留空时自动生成演示交易号" />
        <BaseButton type="primary" :loading="paying" :disabled="order.status !== 'PENDING_PAYMENT'" @click="pay">确认支付 {{ money(order.payableAmount) }}</BaseButton>
        <p v-if="notice" class="notice" role="alert">{{ notice }}</p>
      </BentoCard>

      <BentoCard class="summary-card" size="medium" tone="warm" aspect="2 / 1"><h2>订单摘要</h2><dl><div><dt>餐品小计</dt><dd>{{ money(order.totalAmount) }}</dd></div><div><dt>配送费</dt><dd>{{ money(order.deliveryFee) }}</dd></div><div><dt>包装费</dt><dd>{{ money(order.packagingFee) }}</dd></div><div><dt>优惠</dt><dd>-{{ money(order.discountAmount) }}</dd></div></dl></BentoCard>

      <BentoCard class="result-card" size="small" tone="accent" aspect="1 / 1"><h2>支付状态</h2><StatusBadge :status="payment ? statusLabel(payment.status) : 'PENDING'" /><p v-if="payment" class="result-copy">{{ payment.transactionNo || payment.paymentNo }}</p><p v-else class="muted">等待发起支付</p></BentoCard>

      <BentoCard class="help-card" size="small" tone="default" aspect="1 / 1"><h2>遇到问题？</h2><p>支付失败不会重复扣款。请返回订单详情重新发起支付。</p><BaseButton type="ghost" @click="router.push(`/orders/${orderId}`)">返回订单</BaseButton></BentoCard>
    </BentoGrid>
  </main>
</template>

<style scoped>
.payment-page { min-height: 100vh; padding: clamp(24px, 5vw, 76px); background: #f3f3ee; color: #202923; font-family: 'PingFang SC', sans-serif; }.section-label { letter-spacing: 0; font-size: 13px; opacity: .72; margin: 0 0 8px; } h1, h2 { font-family: Georgia, 'Songti SC', serif; font-weight: 500; margin: 0; } h1 { font-size: clamp(38px, 5vw, 58px); } h2 { font-size: 25px; }
.amount-card, .method-card, .summary-card, .result-card, .help-card { padding: clamp(22px, 2.8vw, 38px); }.amount-card { color: #f4f1e8; display: flex; flex-direction: column; justify-content: space-between; }.amount-top { display: flex; justify-content: space-between; gap: 16px; }.lock-icon { font-size: 52px; color: #e6bb64; transition: transform .3s ease; }.group:hover .lock-icon { transform: rotate(45deg) scale(1.1); }.amount { display: grid; gap: 16px; color: #bfc7bf; }.amount strong { font-family: Georgia, serif; font-size: clamp(44px, 6vw, 78px); color: #efc46f; font-weight: 500; }.order-ref { display: flex; justify-content: space-between; gap: 16px; color: #c1c7c0; }.method-card > *, .help-card > * { margin-top: 16px; }.notice { color: #9a5e23; margin-bottom: 0; }
.summary-card dl { display: grid; gap: 16px; margin: 22px 0 0; }.summary-card dl div { display: flex; justify-content: space-between; color: #766858; }.summary-card dd { margin: 0; color: #3c433c; }.result-copy, .muted { color: #6e736d; }.result-card > *, .help-card > * { margin-top: 15px; }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { transition-duration: .01ms !important; animation-duration: .01ms !important; } }
</style>

<script setup lang="ts">
/** 骑手活动单详情：核对订单并完成取餐、送达。 */
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { Delivery, OrderDetail } from '@/types/domain'
import { deliveryApi, orderApi } from '@/api'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import BentoGrid from '@/components/bento/BentoGrid.vue'
import BentoCard from '@/components/bento/BentoCard.vue'
import { callApi, dateTime, errorMessage, money } from '@/components/features/ops/opsUtils'
import '@/components/features/ops/opsStyles.css'

const route = useRoute()
const orderId = computed(() => Number(Array.isArray(route.params.id) ? route.params.id[0] : route.params.id))
const delivery = ref<Delivery | null>(null)
const order = ref<OrderDetail | null>(null)
const note = ref('')
const loading = ref(false)
const saving = ref('')
const error = ref('')
const notice = ref('')

async function load() {
  if (!orderId.value) return
  loading.value = true
  error.value = ''
  try {
    const [deliveryResult, orderResult] = await Promise.all([
      callApi<Delivery>(deliveryApi, ['delivery'], [orderId.value]),
      callApi<OrderDetail>(orderApi, ['detail', 'get'], [orderId.value]),
    ])
    delivery.value = deliveryResult
    order.value = orderResult
    note.value = deliveryResult.deliveryNote ?? ''
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function act(action: 'pickup' | 'deliver') {
  if (!orderId.value || saving.value) return
  saving.value = action
  notice.value = ''
  try {
    delivery.value = await callApi<Delivery>(deliveryApi, [action], [orderId.value, note.value.trim() || undefined])
    notice.value = action === 'pickup' ? '已确认取餐，开始配送' : '已确认送达，感谢配送'
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = ''
  }
}

onMounted(load)
</script>

<template>
  <main class="ops-page">
    <PageHeader title="活动单详情" description="核对取餐码和收货信息，按顺序完成取餐、送达。" backTo="/rider" back-label="返回工作台">
      <template #actions><BaseButton variant="outline" :loading="loading" @click="load">刷新详情</BaseButton></template>
    </PageHeader>

    <BentoGrid class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6 ops-grid">
      <BentoCard class="ops-card-lg ops-card" size="standard" tone="cyan" :interactive="false" title="配送任务" :description="order ? `订单 #${order.order.orderNo}` : '载入活动单详情'">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">⌁</span></template>
        <LoadingState v-if="loading" label="正在载入活动单" />
        <ErrorState v-else-if="error" :message="error" @retry="load" />
        <EmptyState v-else-if="!delivery" title="暂无活动单" description="请从工作台或抢单列表选择任务。" />
        <template v-else-if="order">
          <div class="ops-row-title"><span>订单 #{{ order.order.orderNo }}</span><StatusBadge :status="delivery.status" /></div>
          <div class="ops-list">
            <div v-for="item in order.items" :key="item.id" class="ops-row">
              <div class="ops-row-main"><strong>{{ item.dishName }}</strong><div class="ops-row-meta">{{ item.specName || '默认规格' }} × {{ item.quantity }}</div></div>
              <strong>¥{{ money(item.subtotal) }}</strong>
            </div>
          </div>
          <div class="ops-kpis">
            <div class="ops-kpi"><span>取餐码</span><strong class="ops-code">{{ delivery.pickupCode || '待生成' }}</strong></div>
            <div class="ops-kpi"><span>距离</span><strong>{{ delivery.distanceKm ? `${delivery.distanceKm.toFixed(1)} km` : '—' }}</strong></div>
            <div class="ops-kpi"><span>下单时间</span><strong style="font-size: 15px">{{ dateTime(order.order.createdAt) }}</strong></div>
            <div class="ops-kpi"><span>应付金额</span><strong>¥{{ money(order.order.payableAmount) }}</strong></div>
          </div>
          <p class="ops-muted">收货地址：{{ order.order.deliveryAddressSnapshot }}<br />联系电话：{{ order.order.contactPhone || '订单未留电话' }}</p>
          <div class="ops-form">
            <BaseInput v-model="note" label="配送备注" placeholder="例如已放置前台" />
            <div class="ops-actions">
              <BaseButton v-if="delivery.status === 'ASSIGNED' || delivery.status === 'WAITING_RIDER'" :loading="saving === 'pickup'" @click="act('pickup')">确认取餐</BaseButton>
              <BaseButton v-if="delivery.status === 'PICKED_UP'" :loading="saving === 'deliver'" @click="act('deliver')">确认送达</BaseButton>
            </div>
          </div>
          <p v-if="notice" class="ops-notice" :class="{ 'ops-success': notice.includes('已确认') }" role="status">{{ notice }}</p>
        </template>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="orange" :interactive="false" title="订单金额">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">¥</span></template>
        <strong style="font-size: 32px">{{ order ? `¥${money(order.order.payableAmount)}` : '—' }}</strong>
        <p class="ops-muted">金额按两位小数展示</p>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="green" :interactive="false" title="订单时间">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">◷</span></template>
        <strong style="font-size: 15px">{{ order ? dateTime(order.order.createdAt) : '—' }}</strong>
        <p class="ops-muted">请在承诺时间内送达</p>
      </BentoCard>

      <BentoCard class="ops-card-wide ops-card" size="compact" tone="neutral" :interactive="false" title="配送备注">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">文</span></template>
        <p class="ops-muted">备注会在取餐、送达动作中同步给订单记录。完成后请保持电话畅通。</p>
      </BentoCard>
    </BentoGrid>
  </main>
</template>

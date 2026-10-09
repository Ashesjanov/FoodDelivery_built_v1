<script setup lang="ts">
/** 移动端骑手活动单详情：与电脑端内容一致，核对订单并完成取餐、送达。 */
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { Delivery, OrderDetail } from '@/types/domain'
import { deliveryApi, orderApi } from '@/api'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { callApi, dateTime, errorMessage, money } from '@/components/features/ops/opsUtils'
import '@/components/features/ops/opsStyles.css'

const route = useRoute()
const orderId = Number(Array.isArray(route.params.id) ? route.params.id[0] : route.params.id)
const delivery = ref<Delivery | null>(null)
const order = ref<OrderDetail | null>(null)
const note = ref('')
const loading = ref(false)
const saving = ref('')
const error = ref('')
const notice = ref('')

async function load() {
  if (!orderId) return
  loading.value = true
  error.value = ''
  try {
    const [deliveryResult, orderResult] = await Promise.all([
      callApi<Delivery>(deliveryApi, ['delivery'], [orderId]),
      callApi<OrderDetail>(orderApi, ['detail', 'get'], [orderId]),
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
  if (!orderId || saving.value) return
  saving.value = action
  notice.value = ''
  try {
    delivery.value = await callApi<Delivery>(deliveryApi, [action], [orderId, note.value.trim() || undefined])
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
  <div class="m-page">
    <header class="m-page-head">
      <div class="m-row m-row--top">
        <div>
          <RouterLink class="m-back" to="/m/rider"><span aria-hidden="true">←</span> 返回工作台</RouterLink>
          <h1 class="m-title" style="margin-top: 8px">活动单详情</h1>
          <p class="m-sub">核对取餐码和收货信息，按顺序完成取餐、送达。</p>
        </div>
        <BaseButton variant="outline" size="sm" :loading="loading" @click="load">刷新</BaseButton>
      </div>
    </header>

    <div class="m-stack">
      <section class="m-card m-card--accent">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">配送任务</h2>
            <p class="m-muted" style="margin-top: 6px">{{ order ? `订单 #${order.order.orderNo}` : '载入活动单详情' }}</p>
          </div>
          <span class="m-icon" aria-hidden="true">⌁</span>
        </div>
        <LoadingState v-if="loading" label="正在载入活动单" />
        <ErrorState v-else-if="error" :message="error" @retry="load" />
        <EmptyState v-else-if="!delivery" title="暂无活动单" description="请从工作台或抢单列表选择任务。" />
        <template v-else-if="order">
          <div class="ops-row-title" style="margin-top: 16px"><span>订单 #{{ order.order.orderNo }}</span><StatusBadge :status="delivery.status" /></div>
          <div class="ops-list">
            <div v-for="item in order.items" :key="item.id" class="ops-row">
              <div class="ops-row-main"><strong>{{ item.dishName }}</strong><div class="ops-row-meta">{{ item.specName || '默认规格' }} × {{ item.quantity }}</div></div>
              <strong>¥{{ money(item.subtotal) }}</strong>
            </div>
          </div>
          <div class="m-kpis">
            <div class="m-kpi"><span>取餐码</span><strong class="m-kpi-code">{{ delivery.pickupCode || '待生成' }}</strong></div>
            <div class="m-kpi"><span>距离</span><strong>{{ delivery.distanceKm ? `${delivery.distanceKm.toFixed(1)} km` : '—' }}</strong></div>
            <div class="m-kpi"><span>下单时间</span><strong class="m-kpi-time">{{ dateTime(order.order.createdAt) }}</strong></div>
            <div class="m-kpi"><span>应付金额</span><strong>¥{{ money(order.order.payableAmount) }}</strong></div>
          </div>
          <p class="m-muted" style="margin-top: 16px">收货地址：{{ order.order.deliveryAddressSnapshot }}<br />联系电话：{{ order.order.contactPhone || '订单未留电话' }}</p>
          <div class="m-form" style="margin-top: 16px">
            <BaseInput v-model="note" label="配送备注" placeholder="例如已放置前台" />
            <div class="m-actions">
              <BaseButton v-if="delivery.status === 'ASSIGNED' || delivery.status === 'WAITING_RIDER'" :loading="saving === 'pickup'" @click="act('pickup')">确认取餐</BaseButton>
              <BaseButton v-if="delivery.status === 'PICKED_UP'" :loading="saving === 'deliver'" @click="act('deliver')">确认送达</BaseButton>
            </div>
          </div>
          <p v-if="notice" class="ops-notice" :class="{ 'ops-success': notice.includes('已确认') }" role="status">{{ notice }}</p>
        </template>
      </section>

      <section class="m-card m-card--warm">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">订单金额</h2>
            <p class="m-count">{{ order ? `¥${money(order.order.payableAmount)}` : '—' }}</p>
            <p class="m-muted">金额按两位小数展示</p>
          </div>
          <span class="m-icon" aria-hidden="true">¥</span>
        </div>
      </section>

      <section class="m-card m-card--fresh">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">订单时间</h2>
            <p class="m-kpi-inline">{{ order ? dateTime(order.order.createdAt) : '—' }}</p>
            <p class="m-muted">请在承诺时间内送达</p>
          </div>
          <span class="m-icon" aria-hidden="true">◷</span>
        </div>
      </section>

      <section class="m-card m-card--gray">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">配送备注</h2>
            <p class="m-muted" style="margin-top: 8px">备注会在取餐、送达动作中同步给订单记录。完成后请保持电话畅通。</p>
          </div>
          <span class="m-icon" aria-hidden="true">文</span>
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
.m-icon { font-size: 28px; color: #c65d1e; }
.m-kpis { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; margin-top: 16px; }
.m-kpi { padding-top: 12px; border-top: 1px solid rgba(24, 32, 29, 0.12); }
.m-kpi span { display: block; color: #69736e; font-size: 13px; }
.m-kpi strong { display: block; margin-top: 4px; font-size: 22px; font-variant-numeric: tabular-nums; }
.m-kpi-code { font-family: ui-monospace, monospace; font-size: 18px; padding: 4px 8px; border: 1px solid #d8ddd9; border-radius: var(--radius-control); background: #f7f8f6; }
.m-kpi-time { font-size: 15px !important; }
.m-count { font-family: Georgia, 'Songti SC', serif; font-size: 30px; color: #8c5a26; margin: 8px 0; }
.m-kpi-inline { font-size: 15px; color: #1d2722; margin: 8px 0; }
</style>

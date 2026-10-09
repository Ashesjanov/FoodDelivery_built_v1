<script setup lang="ts">
/** 移动端商家订单队列：与电脑端内容一致，筛选、查看明细并推进备餐或取消。 */
import { computed, onMounted, ref } from 'vue'
import type { Merchant, Order, OrderDetail, Payment } from '@/types/domain'
import { merchantApi, orderApi, paymentApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { callApi, currentUserId, dateTime, errorMessage, money, pageRecords, pageTotal, statusLabel } from '@/components/features/ops/opsUtils'
import '@/components/features/ops/opsStyles.css'

const auth = useAuthStore() as any
const merchant = ref<Merchant | null>(null)
const orders = ref<Order[]>([])
const detail = ref<OrderDetail | null>(null)
const payment = ref<Payment | null>(null)
const status = ref('')
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const detailLoading = ref(false)
const saving = ref('')
const error = ref('')
const notice = ref('')
const cancelReason = ref('')
const showCancel = ref(false)

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待接单', value: 'PAID' },
  { label: '已接单', value: 'ACCEPTED' },
  { label: '备餐中', value: 'PREPARING' },
  { label: '待取餐', value: 'READY' },
  { label: '配送中', value: 'PICKED_UP' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
]

const counts = computed(() => ({
  waiting: orders.value.filter((item) => ['PAID', 'ACCEPTED'].includes(item.status)).length,
  preparing: orders.value.filter((item) => item.status === 'PREPARING').length,
  ready: orders.value.filter((item) => item.status === 'READY').length,
  revenue: orders.value.reduce((sum, item) => sum + Number(item.payableAmount ?? 0), 0),
}))

async function resolveMerchant() {
  if (merchant.value) return merchant.value
  const result = await callApi<any>(merchantApi, ['list', 'page'], [{ page: 1, size: 100 }])
  const list = pageRecords<Merchant>(result)
  const userId = currentUserId(auth)
  merchant.value = list.find((item) => item.ownerId === userId) ?? list[0] ?? null
  return merchant.value
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const store = await resolveMerchant()
    if (!store) throw new Error('当前账号尚未关联商家')
    const result = await callApi<any>(orderApi, ['list', 'page'], [{ merchantId: store.id, status: status.value || undefined, page: page.value, size: 8 }])
    orders.value = pageRecords<Order>(result)
    total.value = pageTotal(result)
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function openDetail(order: Order) {
  detailLoading.value = true
  notice.value = ''
  try {
    detail.value = await callApi<OrderDetail>(orderApi, ['detail', 'get'], [order.id])
    try {
      payment.value = await callApi<Payment>(paymentApi, ['byOrder', 'latestForOrder'], [order.id])
    } catch {
      payment.value = null
    }
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    detailLoading.value = false
  }
}

async function advance(action: 'accept' | 'ready') {
  const id = detail.value?.order.id
  if (!id || saving.value) return
  saving.value = action
  notice.value = ''
  try {
    detail.value = await callApi<OrderDetail>(orderApi, [action], [id])
    notice.value = action === 'accept' ? '订单已接单' : '订单已出餐，等待骑手取餐'
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = ''
  }
}

async function submitCancel() {
  const id = detail.value?.order.id
  if (!id || saving.value) return
  saving.value = 'cancel'
  notice.value = ''
  try {
    detail.value = await callApi<OrderDetail>(orderApi, ['merchantCancel'], [id, cancelReason.value.trim() || undefined])
    showCancel.value = false
    cancelReason.value = ''
    notice.value = '订单已取消'
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = ''
  }
}

function changePage(next: number) {
  page.value = next
  void load()
}

onMounted(load)
</script>

<template>
  <div class="m-page">
    <header class="m-page-head">
      <div class="m-row m-row--top">
        <div>
          <h1 class="m-title">商家订单</h1>
          <p class="m-sub">按状态处理接单、备餐、出餐和异常取消。</p>
        </div>
        <BaseButton variant="outline" size="sm" :loading="loading" @click="load">刷新</BaseButton>
      </div>
    </header>

    <div class="m-stack">
      <section class="m-card m-card--accent">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">今日订单队列</h2>
            <p class="m-muted" style="margin-top: 6px">优先显示待接单和备餐中的订单。</p>
          </div>
          <span class="m-icon" aria-hidden="true">▤</span>
        </div>
        <div style="margin-top: 16px">
          <BaseSelect v-model="status" label="订单状态" :options="statusOptions" @update:model-value="changePage(1)" />
        </div>
        <LoadingState v-if="loading" label="正在载入订单" />
        <ErrorState v-else-if="error" :message="error" @retry="load" />
        <EmptyState v-else-if="orders.length === 0" title="暂无订单" description="当前筛选条件下没有需要处理的订单。" />
        <div v-else class="ops-list">
          <article v-for="order in orders" :key="order.id" class="ops-row">
            <div class="ops-row-main">
              <div class="ops-row-title"><span>#{{ order.orderNo }}</span><StatusBadge :status="order.status" /></div>
              <div class="ops-row-meta">{{ dateTime(order.createdAt) }} · {{ order.deliveryAddressSnapshot }} · 应付 ¥{{ money(order.payableAmount) }}</div>
            </div>
            <BaseButton variant="outline" size="sm" @click="openDetail(order)">明细</BaseButton>
          </article>
        </div>
        <nav v-if="total > 8" class="ops-pager" aria-label="分页">
          <span class="ops-muted">共 {{ total }} 条，第 {{ page }} / {{ Math.max(Math.ceil(total / 8), 1) }} 页</span>
          <div class="ops-actions">
            <BaseButton variant="outline" size="sm" :disabled="loading || page <= 1" @click="changePage(page - 1)">上一页</BaseButton>
            <BaseButton variant="outline" size="sm" :disabled="loading || page >= Math.ceil(total / 8)" @click="changePage(page + 1)">下一页</BaseButton>
          </div>
        </nav>
      </section>

      <section class="m-card m-card--fresh">
        <div class="m-row m-row--top">
          <h2 class="m-subtitle">队列概览</h2>
          <span class="m-icon" aria-hidden="true">◷</span>
        </div>
        <div class="m-kpis">
          <div class="m-kpi"><span>待处理</span><strong>{{ counts.waiting }}</strong></div>
          <div class="m-kpi"><span>备餐中</span><strong>{{ counts.preparing }}</strong></div>
          <div class="m-kpi"><span>待取餐</span><strong>{{ counts.ready }}</strong></div>
          <div class="m-kpi"><span>本页金额</span><strong>¥{{ money(counts.revenue) }}</strong></div>
        </div>
      </section>

      <section class="m-card">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">商家</h2>
            <p class="m-muted" style="margin-top: 6px">{{ merchant?.name || '待关联' }}</p>
          </div>
          <StatusBadge v-if="merchant" :status="merchant.businessStatus" />
        </div>
      </section>

      <section class="m-card m-card--warm">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">订单金额</h2>
            <p class="m-revenue">¥{{ money(counts.revenue) }}</p>
            <p class="m-muted">金额均按两位小数展示</p>
          </div>
          <span class="m-icon" aria-hidden="true">¥</span>
        </div>
      </section>
    </div>

    <p v-if="notice && !detail" class="m-notice" :class="{ 'ops-success': notice.includes('已') }" role="status">{{ notice }}</p>

    <div v-if="detail || detailLoading" class="m-sheet-backdrop" role="presentation" @click.self="detail = null">
      <section class="m-sheet" role="dialog" aria-modal="true" aria-label="订单明细">
        <div class="m-sheet-head">
          <h2 class="m-sheet-title">订单明细</h2>
          <button class="m-sheet-close" type="button" aria-label="关闭" @click="detail = null">×</button>
        </div>
        <LoadingState v-if="detailLoading" label="正在载入明细" />
        <template v-else-if="detail">
          <div class="ops-row-title"><span>#{{ detail.order.orderNo }}</span><StatusBadge :status="detail.order.status" /></div>
          <p class="ops-muted" style="margin-top: 8px">{{ detail.order.deliveryAddressSnapshot }} · {{ detail.order.contactPhone || '未留电话' }}</p>
          <div class="ops-list">
            <div v-for="item in detail.items" :key="item.id" class="ops-row">
              <div class="ops-row-main"><strong>{{ item.dishName }}</strong><div class="ops-row-meta">{{ item.specName || '默认规格' }} × {{ item.quantity }} · ¥{{ money(item.unitPrice) }}</div></div>
              <strong>¥{{ money(item.subtotal) }}</strong>
            </div>
          </div>
          <div class="m-kpis">
            <div class="m-kpi"><span>商品金额</span><strong>¥{{ money(detail.order.totalAmount) }}</strong></div>
            <div class="m-kpi"><span>应付金额</span><strong>¥{{ money(detail.order.payableAmount) }}</strong></div>
            <div class="m-kpi"><span>支付状态</span><strong>{{ payment ? statusLabel(payment.status) : '暂无记录' }}</strong></div>
            <div class="m-kpi"><span>下单时间</span><strong class="m-kpi-time">{{ dateTime(detail.order.createdAt) }}</strong></div>
          </div>
          <p v-if="notice" class="ops-notice" :class="{ 'ops-success': notice.includes('已') }" role="status">{{ notice }}</p>
          <div v-if="showCancel" class="m-form" style="margin-top: 16px">
            <BaseInput v-model="cancelReason" label="取消原因" placeholder="请输入给顾客的说明" />
            <div class="m-actions">
              <BaseButton variant="danger" :loading="saving === 'cancel'" @click="submitCancel">确认取消</BaseButton>
              <BaseButton variant="ghost" @click="showCancel = false">返回</BaseButton>
            </div>
          </div>
          <footer class="m-actions" style="margin-top: 18px">
            <BaseButton v-if="detail.order.status === 'PAID'" :loading="saving === 'accept'" @click="advance('accept')">接单</BaseButton>
            <BaseButton v-if="['ACCEPTED', 'PREPARING'].includes(detail.order.status)" :loading="saving === 'ready'" @click="advance('ready')">出餐</BaseButton>
            <BaseButton v-if="['PAID', 'ACCEPTED', 'PREPARING'].includes(detail.order.status)" variant="danger" @click="showCancel = true">取消订单</BaseButton>
          </footer>
        </template>
      </section>
    </div>
  </div>
</template>

<style scoped>
.m-page-head { margin-bottom: 16px; }
.m-sub { margin: 8px 0 0; color: #69746d; line-height: 1.7; font-size: 14px; }
.m-icon { font-size: 28px; color: #c65d1e; }
.m-kpis { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; margin-top: 16px; }
.m-kpi { padding-top: 12px; border-top: 1px solid rgba(24, 32, 29, 0.12); }
.m-kpi span { display: block; color: #69736e; font-size: 13px; }
.m-kpi strong { display: block; margin-top: 4px; font-size: 22px; font-variant-numeric: tabular-nums; }
.m-kpi-time { font-size: 15px !important; }
.m-revenue { font-family: Georgia, 'Songti SC', serif; font-size: 30px; color: #8c5a26; margin: 8px 0; }
</style>

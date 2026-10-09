<script setup lang="ts">
/** 管理员订单管理：筛选订单、查看明细、推进履约状态或取消。 */
import { computed, onMounted, ref } from 'vue'
import type { Order, OrderDetail, OrderStatus, Payment } from '@/types/domain'
import { orderApi, paymentApi } from '@/api'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import BentoGrid from '@/components/bento/BentoGrid.vue'
import BentoCard from '@/components/bento/BentoCard.vue'
import OpsModal from '@/components/features/ops/OpsModal.vue'
import OpsPager from '@/components/features/ops/OpsPager.vue'
import { callApi, dateTime, errorMessage, money, pageRecords, pageTotal, statusLabel } from '@/components/features/ops/opsUtils'
import '@/components/features/ops/opsStyles.css'

const orders = ref<Order[]>([])
const detail = ref<OrderDetail | null>(null)
const payment = ref<Payment | null>(null)
const filterStatus = ref<OrderStatus | ''>('')
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
  { label: '全部状态', value: '' }, { label: '待支付', value: 'PENDING_PAYMENT' },
  { label: '待接单', value: 'PAID' }, { label: '已接单', value: 'ACCEPTED' }, { label: '备餐中', value: 'PREPARING' },
  { label: '待取餐', value: 'READY' }, { label: '配送中', value: 'PICKED_UP' }, { label: '已送达', value: 'DELIVERED' },
  { label: '已完成', value: 'COMPLETED' }, { label: '已取消', value: 'CANCELLED' }, { label: '已退款', value: 'REFUNDED' },
]

const summary = computed(() => ({
  active: orders.value.filter((item) => !['COMPLETED', 'CANCELLED', 'REFUNDED'].includes(item.status)).length,
  completed: orders.value.filter((item) => item.status === 'COMPLETED').length,
  cancelled: orders.value.filter((item) => ['CANCELLED', 'REFUNDED'].includes(item.status)).length,
  amount: orders.value.reduce((sum, item) => sum + Number(item.payableAmount ?? 0), 0),
}))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await callApi<any>(orderApi, ['list', 'page'], [{ status: filterStatus.value || undefined, page: page.value, size: 8 }])
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

async function advance(action: 'accept' | 'ready' | 'pickup' | 'deliver' | 'complete') {
  const id = detail.value?.order.id
  if (!id || saving.value) return
  saving.value = action
  notice.value = ''
  try {
    detail.value = await callApi<OrderDetail>(orderApi, [action], [id])
    notice.value = `订单已${statusLabel(detail.value.order.status)}`
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
    detail.value = await callApi<OrderDetail>(orderApi, ['cancel'], [id, cancelReason.value.trim() || undefined])
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
  <main class="ops-page">
    <PageHeader title="订单管理" description="查看平台订单，按状态推进履约并处理取消。">
      <template #actions><BaseButton variant="outline" :loading="loading" @click="load">刷新订单</BaseButton></template>
    </PageHeader>

    <BentoGrid class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6 ops-grid">
      <BentoCard class="ops-card-lg ops-card" size="standard" tone="orange" :interactive="false" title="平台订单" description="状态推进会同步到商家和骑手工作台。">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">▤</span></template>
        <div class="ops-toolbar"><BaseSelect v-model="filterStatus" label="订单状态" :options="statusOptions" @update:model-value="changePage(1)" /></div>
        <LoadingState v-if="loading" label="正在载入订单" />
        <ErrorState v-else-if="error" :message="error" @retry="load" />
        <EmptyState v-else-if="orders.length === 0" title="暂无订单" description="当前筛选条件下没有订单。" />
        <div v-else class="ops-list">
          <article v-for="order in orders" :key="order.id" class="ops-row">
            <div class="ops-row-main">
              <div class="ops-row-title"><span>#{{ order.orderNo }}</span><StatusBadge :status="order.status" /></div>
              <div class="ops-row-meta">{{ order.merchantName }} · {{ dateTime(order.createdAt) }} · 应付 ¥{{ money(order.payableAmount) }}</div>
            </div>
            <BaseButton size="sm" variant="outline" @click="openDetail(order)">明细</BaseButton>
          </article>
        </div>
        <OpsPager :page="page" :pages="Math.ceil(total / 8)" :total="total" :busy="loading" @prev="changePage(page - 1)" @next="changePage(page + 1)" />
      </BentoCard>

      <BentoCard class="ops-card-wide ops-card" size="compact" tone="green" :interactive="false" title="订单概览">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">◷</span></template>
        <div class="ops-kpis">
          <div class="ops-kpi"><span>履约中</span><strong>{{ summary.active }}</strong></div>
          <div class="ops-kpi"><span>已完成</span><strong>{{ summary.completed }}</strong></div>
          <div class="ops-kpi"><span>取消/退款</span><strong>{{ summary.cancelled }}</strong></div>
          <div class="ops-kpi"><span>本页金额</span><strong>¥{{ money(summary.amount) }}</strong></div>
        </div>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="cyan" :interactive="false" title="订单总量">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">#</span></template>
        <strong style="font-size: 34px">{{ total }}</strong>
        <p class="ops-muted">笔平台订单</p>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="pink" :interactive="false" title="金额概览">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">¥</span></template>
        <strong style="font-size: 26px">¥{{ money(summary.amount) }}</strong>
        <p class="ops-muted">本页订单应收</p>
      </BentoCard>
    </BentoGrid>

    <p v-if="notice" class="ops-notice" :class="{ 'ops-success': notice.includes('已') }" role="status">{{ notice }}</p>

    <OpsModal :open="Boolean(detail) || detailLoading" title="订单明细" @close="detail = null">
      <LoadingState v-if="detailLoading" label="正在载入明细" />
      <template v-else-if="detail">
        <div class="ops-row-title"><span>#{{ detail.order.orderNo }}</span><StatusBadge :status="detail.order.status" /></div>
        <p class="ops-muted">{{ detail.order.merchantName }} · {{ detail.order.deliveryAddressSnapshot }} · {{ detail.order.contactPhone || '未留电话' }}</p>
        <div class="ops-list">
          <div v-for="item in detail.items" :key="item.id" class="ops-row">
            <div class="ops-row-main"><strong>{{ item.dishName }}</strong><div class="ops-row-meta">{{ item.specName || '默认规格' }} × {{ item.quantity }} · ¥{{ money(item.unitPrice) }}</div></div>
            <strong>¥{{ money(item.subtotal) }}</strong>
          </div>
        </div>
        <div class="ops-kpis">
          <div class="ops-kpi"><span>商品金额</span><strong>¥{{ money(detail.order.totalAmount) }}</strong></div>
          <div class="ops-kpi"><span>优惠金额</span><strong>¥{{ money(detail.order.discountAmount) }}</strong></div>
          <div class="ops-kpi"><span>应付金额</span><strong>¥{{ money(detail.order.payableAmount) }}</strong></div>
          <div class="ops-kpi"><span>支付状态</span><strong>{{ payment ? statusLabel(payment.status) : '暂无记录' }}</strong></div>
        </div>
        <p v-if="detail.order.cancelReason" class="ops-notice">取消原因：{{ detail.order.cancelReason }}</p>
        <p v-if="notice" class="ops-notice" :class="{ 'ops-success': notice.includes('已') }" role="status">{{ notice }}</p>
        <div v-if="showCancel" class="ops-form">
          <BaseInput v-model="cancelReason" label="取消原因" placeholder="请输入处理说明" />
          <div class="ops-actions"><BaseButton variant="danger" :loading="saving === 'cancel'" @click="submitCancel">确认取消</BaseButton><BaseButton variant="ghost" @click="showCancel = false">返回</BaseButton></div>
        </div>
        <footer class="ops-actions" style="margin-top: 18px">
          <BaseButton v-if="detail.order.status === 'PAID'" :loading="saving === 'accept'" @click="advance('accept')">接单</BaseButton>
          <BaseButton v-if="['ACCEPTED', 'PREPARING'].includes(detail.order.status)" :loading="saving === 'ready'" @click="advance('ready')">出餐</BaseButton>
          <BaseButton v-if="detail.order.status === 'READY'" :loading="saving === 'pickup'" @click="advance('pickup')">取餐</BaseButton>
          <BaseButton v-if="detail.order.status === 'PICKED_UP'" :loading="saving === 'deliver'" @click="advance('deliver')">送达</BaseButton>
          <BaseButton v-if="detail.order.status === 'DELIVERED'" :loading="saving === 'complete'" @click="advance('complete')">完成</BaseButton>
          <BaseButton v-if="!['COMPLETED', 'CANCELLED', 'REFUNDED'].includes(detail.order.status)" variant="danger" @click="showCancel = true">取消订单</BaseButton>
        </footer>
      </template>
    </OpsModal>
  </main>
</template>

<script setup lang="ts">
/** 移动端骑手工作台：与电脑端内容一致，维护在线状态与位置，查看当前活动单。 */
import { onMounted, ref } from 'vue'
import type { Delivery, Rider, RiderStatus } from '@/types/domain'
import { deliveryApi } from '@/api'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { callApi, errorMessage, money, statusLabel } from '@/components/features/ops/opsUtils'
import '@/components/features/ops/opsStyles.css'

const rider = ref<Rider | null>(null)
const active = ref<Delivery[]>([])
const loading = ref(false)
const saving = ref('')
const error = ref('')
const notice = ref('')
const status = ref<RiderStatus>('OFFLINE')
const longitude = ref<string | number>('')
const latitude = ref<string | number>('')

const statusOptions = [
  { label: '离线', value: 'OFFLINE' }, { label: '在线接单', value: 'ONLINE' },
  { label: '配送中', value: 'BUSY' }, { label: '已停用', value: 'SUSPENDED' },
]

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [profile, records] = await Promise.all([
      callApi<Rider>(deliveryApi, ['me', 'rider'], []),
      callApi<Delivery[]>(deliveryApi, ['active'], []),
    ])
    rider.value = profile
    active.value = records ?? []
    status.value = profile.status
    longitude.value = profile.currentLongitude ?? ''
    latitude.value = profile.currentLatitude ?? ''
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function updateStatus() {
  if (!rider.value || saving.value) return
  saving.value = 'status'
  notice.value = ''
  try {
    rider.value = await callApi<Rider>(deliveryApi, ['setStatus', 'updateStatus'], [{ status: status.value }])
    notice.value = `状态已切换为${statusLabel(status.value)}`
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = ''
  }
}

async function updateLocation() {
  if (!rider.value || saving.value || longitude.value === '' || latitude.value === '') return
  saving.value = 'location'
  notice.value = ''
  try {
    rider.value = await callApi<Rider>(deliveryApi, ['updateLocation'], [{ longitude: Number(longitude.value), latitude: Number(latitude.value) }])
    notice.value = '当前位置已更新'
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
          <h1 class="m-title">骑手工作台</h1>
          <p class="m-sub">保持在线、同步位置并处理当前配送任务。</p>
        </div>
        <BaseButton variant="outline" size="sm" :loading="loading" @click="load">刷新</BaseButton>
      </div>
    </header>

    <div class="m-stack">
      <section class="m-card m-card--accent">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">今日配送</h2>
            <p class="m-muted" style="margin-top: 6px">当前账号的活动配送单会实时显示。</p>
          </div>
          <span class="m-icon" aria-hidden="true">⌁</span>
        </div>
        <LoadingState v-if="loading" label="正在载入工作台" />
        <ErrorState v-else-if="error" :message="error" @retry="load" />
        <EmptyState v-else-if="active.length === 0" title="暂无活动单" description="进入抢单列表寻找新的配送任务。" />
        <div v-else class="ops-list">
          <RouterLink v-for="delivery in active" :key="delivery.id" class="ops-row" :to="`/m/rider/orders/${delivery.orderId}`">
            <div class="ops-row-main">
              <div class="ops-row-title"><span>订单 #{{ delivery.orderId }}</span><StatusBadge :status="delivery.status" /></div>
              <div class="ops-row-meta">{{ delivery.distanceKm ? `${delivery.distanceKm.toFixed(1)} km` : '距离待更新' }} · {{ delivery.deliveryNote || '无配送备注' }}</div>
            </div>
            <strong>处理</strong>
          </RouterLink>
        </div>
      </section>

      <section class="m-card m-card--fresh">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">在线状态</h2>
            <div v-if="rider" class="ops-row-title" style="margin-top: 10px"><span>{{ rider.name }}</span><StatusBadge :status="rider.status" /></div>
          </div>
          <span class="m-icon" aria-hidden="true">◉</span>
        </div>
        <div v-if="rider" class="m-form" style="margin-top: 16px">
          <BaseSelect v-model="status" label="接单状态" :options="statusOptions" />
          <BaseButton :loading="saving === 'status'" @click="updateStatus">更新状态</BaseButton>
        </div>
      </section>

      <section class="m-card">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">当前位置</h2>
            <p class="m-muted" style="margin-top: 6px">{{ rider?.currentLongitude ?? '—' }}, {{ rider?.currentLatitude ?? '—' }}</p>
          </div>
          <span class="m-icon" aria-hidden="true">⌖</span>
        </div>
        <div class="m-form" style="margin-top: 16px">
          <BaseInput v-model="longitude" label="经度" type="number" step="any" />
          <BaseInput v-model="latitude" label="纬度" type="number" step="any" />
          <BaseButton size="sm" variant="outline" :loading="saving === 'location'" @click="updateLocation">同步位置</BaseButton>
        </div>
      </section>

      <section class="m-card m-card--warm">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">配送统计</h2>
            <div class="m-kpis">
              <div class="m-kpi"><span>完成单量</span><strong>{{ rider?.completedCount ?? 0 }}</strong></div>
              <div class="m-kpi"><span>服务评分</span><strong>{{ Number(rider?.rating ?? 0).toFixed(1) }}</strong></div>
              <div class="m-kpi"><span>活动单</span><strong>{{ active.length }}</strong></div>
              <div class="m-kpi"><span>车辆</span><strong class="m-kpi-text">{{ rider?.vehicleType || '—' }}</strong></div>
            </div>
          </div>
          <span class="m-icon" aria-hidden="true">✓</span>
        </div>
      </section>

      <section class="m-card m-card--gray">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">配送提醒</h2>
            <p class="m-muted" style="margin-top: 8px">取餐前核对取餐码，送达后在订单页确认。距离和金额均以平台最终数据为准，当前活动单金额为 {{ money(0) }}。</p>
          </div>
          <span class="m-icon" aria-hidden="true">!</span>
        </div>
      </section>
    </div>

    <p v-if="notice" class="m-notice" :class="{ 'ops-success': notice.includes('已') }" role="status">{{ notice }}</p>
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
.m-kpi-text { font-size: 15px !important; }
</style>

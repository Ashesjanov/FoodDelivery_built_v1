<script setup lang="ts">
/** 骑手工作台：维护在线状态与位置，查看当前活动单。 */
import { onMounted, ref } from 'vue'
import type { Delivery, Rider, RiderStatus } from '@/types/domain'
import { deliveryApi } from '@/api'
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
  <main class="ops-page">
    <PageHeader title="骑手工作台" description="保持在线、同步位置并处理当前配送任务。">
      <template #actions><BaseButton variant="outline" :loading="loading" @click="load">刷新工作台</BaseButton></template>
    </PageHeader>

    <BentoGrid class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6 ops-grid">
      <BentoCard class="ops-card-lg ops-card" size="standard" tone="cyan" :interactive="false" title="今日配送" description="当前账号的活动配送单会实时显示。">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">⌁</span></template>
        <LoadingState v-if="loading" label="正在载入工作台" />
        <ErrorState v-else-if="error" :message="error" @retry="load" />
        <EmptyState v-else-if="active.length === 0" title="暂无活动单" description="进入抢单列表寻找新的配送任务。" />
        <div v-else class="ops-list">
          <RouterLink v-for="delivery in active" :key="delivery.id" class="ops-row" :to="`/rider/orders/${delivery.orderId}`">
            <div class="ops-row-main">
              <div class="ops-row-title"><span>订单 #{{ delivery.orderId }}</span><StatusBadge :status="delivery.status" /></div>
              <div class="ops-row-meta">{{ delivery.distanceKm ? `${delivery.distanceKm.toFixed(1)} km` : '距离待更新' }} · {{ delivery.deliveryNote || '无配送备注' }}</div>
            </div>
            <strong>处理</strong>
          </RouterLink>
        </div>
      </BentoCard>

      <BentoCard class="ops-card-wide ops-card" size="compact" tone="green" :interactive="false" title="在线状态">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">◉</span></template>
        <div v-if="rider" class="ops-row-title"><span>{{ rider.name }}</span><StatusBadge :status="rider.status" /></div>
        <div v-if="rider" class="ops-inline-form">
          <BaseSelect v-model="status" label="接单状态" :options="statusOptions" />
          <BaseButton :loading="saving === 'status'" @click="updateStatus">更新状态</BaseButton>
        </div>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="orange" :interactive="false" title="当前位置">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">⌖</span></template>
        <p class="ops-muted">{{ rider?.currentLongitude ?? '—' }}, {{ rider?.currentLatitude ?? '—' }}</p>
        <div class="ops-form">
          <BaseInput v-model="longitude" label="经度" type="number" step="any" />
          <BaseInput v-model="latitude" label="纬度" type="number" step="any" />
          <BaseButton size="sm" variant="outline" :loading="saving === 'location'" @click="updateLocation">同步位置</BaseButton>
        </div>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="pink" :interactive="false" title="配送统计">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">✓</span></template>
        <div class="ops-kpis">
          <div class="ops-kpi"><span>完成单量</span><strong>{{ rider?.completedCount ?? 0 }}</strong></div>
          <div class="ops-kpi"><span>服务评分</span><strong>{{ Number(rider?.rating ?? 0).toFixed(1) }}</strong></div>
          <div class="ops-kpi"><span>活动单</span><strong>{{ active.length }}</strong></div>
          <div class="ops-kpi"><span>车辆</span><strong style="font-size: 15px">{{ rider?.vehicleType || '—' }}</strong></div>
        </div>
      </BentoCard>

      <BentoCard class="ops-card-wide ops-card" size="compact" tone="neutral" :interactive="false" title="配送提醒">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">!</span></template>
        <p class="ops-muted">取餐前核对取餐码，送达后在订单页确认。距离和金额均以平台最终数据为准，当前活动单金额为 {{ money(0) }}。</p>
      </BentoCard>
    </BentoGrid>

    <p v-if="notice" class="ops-notice" :class="{ 'ops-success': notice.includes('已') }" role="status">{{ notice }}</p>
  </main>
</template>

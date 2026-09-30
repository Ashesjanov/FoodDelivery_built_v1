<script setup lang="ts">
/** 骑手资料页：维护姓名、电话、车辆和位置信息。 */
import { onMounted, ref } from 'vue'
import type { Rider, RiderStatus } from '@/types/domain'
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
import { callApi, errorMessage, statusLabel } from '@/components/features/ops/opsUtils'
import '@/components/features/ops/opsStyles.css'

const rider = ref<Rider | null>(null)
const name = ref('')
const phone = ref('')
const vehicleType = ref('')
const status = ref<RiderStatus>('OFFLINE')
const longitude = ref<string | number>('')
const latitude = ref<string | number>('')
const loading = ref(false)
const saving = ref('')
const error = ref('')
const notice = ref('')

const statusOptions = [
  { label: '离线', value: 'OFFLINE' }, { label: '在线接单', value: 'ONLINE' },
  { label: '配送中', value: 'BUSY' }, { label: '已停用', value: 'SUSPENDED' },
]

async function load() {
  loading.value = true
  error.value = ''
  try {
    rider.value = await callApi<Rider>(deliveryApi, ['me', 'rider'], [])
    name.value = rider.value.name
    phone.value = rider.value.phone
    vehicleType.value = rider.value.vehicleType
    status.value = rider.value.status
    longitude.value = rider.value.currentLongitude ?? ''
    latitude.value = rider.value.currentLatitude ?? ''
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (!rider.value || saving.value || !name.value.trim() || !phone.value.trim() || !vehicleType.value.trim()) return
  saving.value = 'profile'
  notice.value = ''
  try {
    rider.value = await callApi<Rider>(deliveryApi, ['updateProfile', 'updateRider'], [{ name: name.value.trim(), phone: phone.value.trim(), vehicleType: vehicleType.value.trim() }])
    notice.value = '骑手资料已保存'
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = ''
  }
}

async function saveStatus() {
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

async function saveLocation() {
  if (!rider.value || saving.value || longitude.value === '' || latitude.value === '') return
  saving.value = 'location'
  notice.value = ''
  try {
    rider.value = await callApi<Rider>(deliveryApi, ['updateLocation'], [{ longitude: Number(longitude.value), latitude: Number(latitude.value) }])
    notice.value = '位置已更新'
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
    <PageHeader title="骑手资料" description="保持资料准确，方便顾客和平台联系你。">
      <template #actions><BaseButton variant="outline" :loading="loading" @click="load">重新载入</BaseButton></template>
    </PageHeader>

    <BentoGrid class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6 ops-grid">
      <BentoCard class="ops-card-lg ops-card" size="standard" tone="cyan" :interactive="false" title="基本资料" description="资料变更会同步到配送记录。">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">人</span></template>
        <LoadingState v-if="loading" label="正在载入骑手资料" />
        <ErrorState v-else-if="error" :message="error" @retry="load" />
        <form v-else-if="rider" class="ops-form" @submit.prevent="saveProfile">
          <BaseInput v-model="name" label="姓名" required />
          <BaseInput v-model="phone" label="联系电话" type="tel" required />
          <BaseInput v-model="vehicleType" label="车辆类型" required placeholder="电动车、摩托车或自行车" />
          <div class="ops-actions"><BaseButton native-type="submit" :loading="saving === 'profile'">保存资料</BaseButton></div>
        </form>
        <EmptyState v-else title="暂无骑手资料" description="请联系管理员开通骑手账号。" />
      </BentoCard>

      <BentoCard class="ops-card-wide ops-card" size="compact" tone="green" :interactive="false" title="接单状态">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">◉</span></template>
        <div v-if="rider" class="ops-row-title"><span>{{ rider.name }}</span><StatusBadge :status="rider.status" /></div>
        <div v-if="rider" class="ops-inline-form">
          <BaseSelect v-model="status" label="状态" :options="statusOptions" />
          <BaseButton :loading="saving === 'status'" @click="saveStatus">更新状态</BaseButton>
        </div>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="orange" :interactive="false" title="当前位置">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">⌖</span></template>
        <div class="ops-form">
          <BaseInput v-model="longitude" label="经度" type="number" step="any" />
          <BaseInput v-model="latitude" label="纬度" type="number" step="any" />
          <BaseButton size="sm" variant="outline" :loading="saving === 'location'" @click="saveLocation">同步位置</BaseButton>
        </div>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="pink" :interactive="false" title="服务表现">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">★</span></template>
        <div class="ops-kpis">
          <div class="ops-kpi"><span>评分</span><strong>{{ Number(rider?.rating ?? 0).toFixed(1) }}</strong></div>
          <div class="ops-kpi"><span>完成单量</span><strong>{{ rider?.completedCount ?? 0 }}</strong></div>
          <div class="ops-kpi"><span>活动单</span><strong>{{ rider?.activeOrderCount ?? 0 }}</strong></div>
          <div class="ops-kpi"><span>车辆</span><strong style="font-size: 15px">{{ rider?.vehicleType || '—' }}</strong></div>
        </div>
      </BentoCard>
    </BentoGrid>

    <p v-if="notice" class="ops-notice" :class="{ 'ops-success': notice.includes('已') }" role="status">{{ notice }}</p>
  </main>
</template>

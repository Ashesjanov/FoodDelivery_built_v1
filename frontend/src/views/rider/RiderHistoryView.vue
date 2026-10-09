<script setup lang="ts">
/** 骑手历史记录：分页查看已完成配送。 */
import { onMounted, ref } from 'vue'
import type { Delivery } from '@/types/domain'
import { deliveryApi } from '@/api'
import BaseButton from '@/components/base/BaseButton.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import BentoGrid from '@/components/bento/BentoGrid.vue'
import BentoCard from '@/components/bento/BentoCard.vue'
import OpsPager from '@/components/features/ops/OpsPager.vue'
import { callApi, dateTime, errorMessage, pageRecords, pageTotal } from '@/components/features/ops/opsUtils'
import '@/components/features/ops/opsStyles.css'

const records = ref<Delivery[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await callApi<any>(deliveryApi, ['records'], [{ page: page.value, size: 10 }])
    records.value = pageRecords<Delivery>(result)
    total.value = pageTotal(result)
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
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
    <PageHeader title="配送历史" description="查看已完成、已取消的配送记录。">
      <template #actions><BaseButton variant="outline" :loading="loading" @click="load">刷新历史</BaseButton></template>
    </PageHeader>

    <BentoGrid class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6 ops-grid">
      <BentoCard class="ops-card-lg ops-card" size="standard" tone="neutral" :interactive="false" title="历史配送" description="记录保留每笔配送的完成时间和备注。">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">▤</span></template>
        <LoadingState v-if="loading" label="正在载入历史" />
        <ErrorState v-else-if="error" :message="error" @retry="load" />
        <EmptyState v-else-if="records.length === 0" title="暂无历史记录" description="完成配送后记录会显示在这里。" />
        <div v-else class="ops-list">
          <article v-for="record in records" :key="record.id" class="ops-row">
            <div class="ops-row-main">
              <div class="ops-row-title"><span>订单 #{{ record.orderId }}</span><StatusBadge :status="record.status" /></div>
              <div class="ops-row-meta">完成于 {{ dateTime(record.deliveredAt || record.updatedAt) }} · {{ record.distanceKm ? `${record.distanceKm.toFixed(1)} km` : '距离未记录' }}</div>
              <div v-if="record.deliveryNote" class="ops-row-meta">备注：{{ record.deliveryNote }}</div>
            </div>
            <span class="ops-muted">{{ record.pickupCode ? '已完成' : '已归档' }}</span>
          </article>
        </div>
        <OpsPager :page="page" :pages="Math.ceil(total / 10)" :total="total" :busy="loading" @prev="changePage(page - 1)" @next="changePage(page + 1)" />
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="green" :interactive="false" title="累计记录">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">#</span></template>
        <strong style="font-size: 34px">{{ total }}</strong>
        <p class="ops-muted">条配送记录</p>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="cyan" :interactive="false" title="本页记录">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">▤</span></template>
        <strong style="font-size: 34px">{{ records.length }}</strong>
        <p class="ops-muted">条已归档</p>
      </BentoCard>

      <BentoCard class="ops-card-wide ops-card" size="compact" tone="orange" :interactive="false" title="服务建议">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">↗</span></template>
        <p class="ops-muted">按时取餐、礼貌沟通和准确送达会帮助你获得更高评分。</p>
      </BentoCard>
    </BentoGrid>
  </main>
</template>

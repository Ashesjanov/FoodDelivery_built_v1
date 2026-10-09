<script setup lang="ts">
/** 骑手抢单列表：查看待接配送并防止重复抢单。 */
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

const deliveries = ref<Delivery[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const saving = ref(0)
const error = ref('')
const notice = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await callApi<any>(deliveryApi, ['available', 'availableOrders'], [{ page: page.value, size: 8 }])
    deliveries.value = pageRecords<Delivery>(result)
    total.value = pageTotal(result)
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function accept(delivery: Delivery) {
  if (saving.value) return
  saving.value = delivery.id
  notice.value = ''
  try {
    await callApi(deliveryApi, ['accept'], [delivery.orderId])
    notice.value = `订单 #${delivery.orderId} 已接单`
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = 0
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
    <PageHeader title="抢单列表" description="选择距离合适的配送单，接单后进入活动单详情。">
      <template #actions><BaseButton variant="outline" :loading="loading" @click="load">刷新列表</BaseButton></template>
    </PageHeader>

    <BentoGrid class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6 ops-grid">
      <BentoCard class="ops-card-lg ops-card" size="standard" tone="cyan" :interactive="false" title="待接配送" description="接单成功后请尽快前往商家取餐。">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">↗</span></template>
        <LoadingState v-if="loading" label="正在寻找配送单" />
        <ErrorState v-else-if="error" :message="error" @retry="load" />
        <EmptyState v-else-if="deliveries.length === 0" title="暂无可接订单" description="稍后刷新，或先确认已切换为在线状态。" />
        <div v-else class="ops-list">
          <article v-for="delivery in deliveries" :key="delivery.id" class="ops-row">
            <div class="ops-row-main">
              <div class="ops-row-title"><span>订单 #{{ delivery.orderId }}</span><StatusBadge :status="delivery.status" /></div>
              <div class="ops-row-meta">发布于 {{ dateTime(delivery.createdAt) }} · {{ delivery.distanceKm ? `${delivery.distanceKm.toFixed(1)} km` : '距离待更新' }}</div>
            </div>
            <BaseButton size="sm" :loading="saving === delivery.id" @click="accept(delivery)">抢单</BaseButton>
          </article>
        </div>
        <OpsPager :page="page" :pages="Math.ceil(total / 8)" :total="total" :busy="loading" @prev="changePage(page - 1)" @next="changePage(page + 1)" />
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="orange" :interactive="false" title="当前可接">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">#</span></template>
        <strong style="font-size: 34px">{{ total }}</strong>
        <p class="ops-muted">条配送需求</p>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="green" :interactive="false" title="本页任务">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">▤</span></template>
        <strong style="font-size: 34px">{{ deliveries.length }}</strong>
        <p class="ops-muted">条待处理</p>
      </BentoCard>

      <BentoCard class="ops-card-wide ops-card" size="compact" tone="neutral" :interactive="false" title="抢单提示">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">!</span></template>
        <p class="ops-muted">一次只能处理一个抢单动作。若提示操作冲突，说明订单已被其他骑手接走，请刷新列表。</p>
      </BentoCard>
    </BentoGrid>

    <p v-if="notice" class="ops-notice" :class="{ 'ops-success': notice.includes('已接单') }" role="status">{{ notice }}</p>
  </main>
</template>

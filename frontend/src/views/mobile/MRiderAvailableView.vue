<script setup lang="ts">
/** 移动端骑手抢单列表：与电脑端内容一致，查看待接配送并防止重复抢单。 */
import { onMounted, ref } from 'vue'
import type { Delivery } from '@/types/domain'
import { deliveryApi } from '@/api'
import BaseButton from '@/components/base/BaseButton.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
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
  <div class="m-page">
    <header class="m-page-head">
      <div class="m-row m-row--top">
        <div>
          <h1 class="m-title">抢单列表</h1>
          <p class="m-sub">选择距离合适的配送单，接单后进入活动单详情。</p>
        </div>
        <BaseButton variant="outline" size="sm" :loading="loading" @click="load">刷新</BaseButton>
      </div>
    </header>

    <div class="m-stack">
      <section class="m-card m-card--accent">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">待接配送</h2>
            <p class="m-muted" style="margin-top: 6px">接单成功后请尽快前往商家取餐。</p>
          </div>
          <span class="m-icon" aria-hidden="true">↗</span>
        </div>
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
          <div>
            <h2 class="m-subtitle">当前可接</h2>
            <p class="m-count">{{ total }}</p>
            <p class="m-muted">条配送需求</p>
          </div>
          <span class="m-icon" aria-hidden="true">#</span>
        </div>
      </section>

      <section class="m-card m-card--warm">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">本页任务</h2>
            <p class="m-count">{{ deliveries.length }}</p>
            <p class="m-muted">条待处理</p>
          </div>
          <span class="m-icon" aria-hidden="true">▤</span>
        </div>
      </section>

      <section class="m-card m-card--gray">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">抢单提示</h2>
            <p class="m-muted" style="margin-top: 8px">一次只能处理一个抢单动作。若提示操作冲突，说明订单已被其他骑手接走，请刷新列表。</p>
          </div>
          <span class="m-icon" aria-hidden="true">!</span>
        </div>
      </section>
    </div>

    <p v-if="notice" class="m-notice" :class="{ 'ops-success': notice.includes('已接单') }" role="status">{{ notice }}</p>
  </div>
</template>

<style scoped>
.m-page-head { margin-bottom: 16px; }
.m-sub { margin: 8px 0 0; color: #69746d; line-height: 1.7; font-size: 14px; }
.m-icon { font-size: 28px; color: #c65d1e; }
.m-count { font-family: Georgia, 'Songti SC', serif; font-size: 34px; color: #8c5a26; margin: 8px 0; }
</style>

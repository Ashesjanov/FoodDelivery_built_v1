<script setup lang="ts">
/** 移动端骑手历史记录：与电脑端内容一致，分页查看已完成配送。 */
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
  <div class="m-page">
    <header class="m-page-head">
      <div class="m-row m-row--top">
        <div>
          <h1 class="m-title">配送历史</h1>
          <p class="m-sub">查看已完成、已取消的配送记录。</p>
        </div>
        <BaseButton variant="outline" size="sm" :loading="loading" @click="load">刷新</BaseButton>
      </div>
    </header>

    <div class="m-stack">
      <section class="m-card m-card--gray">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">历史配送</h2>
            <p class="m-muted" style="margin-top: 6px">记录保留每笔配送的完成时间和备注。</p>
          </div>
          <span class="m-icon" aria-hidden="true">▤</span>
        </div>
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
        <nav v-if="total > 10" class="ops-pager" aria-label="分页">
          <span class="ops-muted">共 {{ total }} 条，第 {{ page }} / {{ Math.max(Math.ceil(total / 10), 1) }} 页</span>
          <div class="ops-actions">
            <BaseButton variant="outline" size="sm" :disabled="loading || page <= 1" @click="changePage(page - 1)">上一页</BaseButton>
            <BaseButton variant="outline" size="sm" :disabled="loading || page >= Math.ceil(total / 10)" @click="changePage(page + 1)">下一页</BaseButton>
          </div>
        </nav>
      </section>

      <section class="m-card m-card--fresh">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">累计记录</h2>
            <p class="m-count">{{ total }}</p>
            <p class="m-muted">条配送记录</p>
          </div>
          <span class="m-icon" aria-hidden="true">#</span>
        </div>
      </section>

      <section class="m-card m-card--accent">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">本页记录</h2>
            <p class="m-count">{{ records.length }}</p>
            <p class="m-muted">条已归档</p>
          </div>
          <span class="m-icon" aria-hidden="true">▤</span>
        </div>
      </section>

      <section class="m-card m-card--warm">
        <div class="m-row m-row--top">
          <div>
            <h2 class="m-subtitle">服务建议</h2>
            <p class="m-muted" style="margin-top: 8px">按时取餐、礼貌沟通和准确送达会帮助你获得更高评分。</p>
          </div>
          <span class="m-icon" aria-hidden="true">↗</span>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.m-page-head { margin-bottom: 16px; }
.m-sub { margin: 8px 0 0; color: #69746d; line-height: 1.7; font-size: 14px; }
.m-icon { font-size: 28px; color: #c65d1e; }
.m-count { font-family: Georgia, 'Songti SC', serif; font-size: 34px; color: #8c5a26; margin: 8px 0; }
</style>

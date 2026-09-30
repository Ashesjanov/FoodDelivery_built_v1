<script setup lang="ts">
/** 管理员评价管理：按商家查询公开评价，并代表平台回复顾客反馈。 */
import { computed, ref } from 'vue'
import type { Review } from '@/types/domain'
import { reviewApi } from '@/api'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import BentoGrid from '@/components/bento/BentoGrid.vue'
import BentoCard from '@/components/bento/BentoCard.vue'
import OpsPager from '@/components/features/ops/OpsPager.vue'
import { dateTime, errorMessage } from '@/components/features/ops/opsUtils'
import '@/components/features/ops/opsStyles.css'

const pageSize = 8
const merchantIdInput = ref<string | number>('')
const submittedMerchantId = ref<number | null>(null)
const reviews = ref<Review[]>([])
const replyDrafts = ref<Record<number, string>>({})
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const savingReviewId = ref(0)
const error = ref('')
const queryError = ref('')
const notice = ref('')
const noticeKind = ref<'success' | 'error'>('success')

const averageRating = computed(() => {
  if (!reviews.value.length) return 0
  return reviews.value.reduce((sum, review) => sum + review.rating, 0) / reviews.value.length
})

const replyRate = computed(() => {
  if (!reviews.value.length) return 0
  return Math.round((reviews.value.filter((review) => review.replyContent).length / reviews.value.length) * 100)
})

const pendingReplyCount = computed(() => reviews.value.filter((review) => !review.replyContent).length)

function ratingStars(rating: number): string {
  const score = Math.max(0, Math.min(5, Math.round(rating)))
  return `${'★'.repeat(score)}${'☆'.repeat(5 - score)}`
}

function feedback(message: string, kind: 'success' | 'error' = 'success') {
  notice.value = message
  noticeKind.value = kind
}

async function loadReviews(resetPage = false) {
  if (loading.value) return
  const merchantId = Number(resetPage ? merchantIdInput.value : submittedMerchantId.value ?? merchantIdInput.value)
  if (!Number.isInteger(merchantId) || merchantId <= 0) {
    queryError.value = '请输入大于 0 的整数商家 ID'
    return
  }

  queryError.value = ''
  if (resetPage) {
    page.value = 1
    replyDrafts.value = {}
    feedback('')
  }
  submittedMerchantId.value = merchantId
  loading.value = true
  error.value = ''
  try {
    const result = await reviewApi.merchantReviews(merchantId, { page: page.value, size: pageSize })
    reviews.value = result.records ?? []
    total.value = Number(result.total || 0)
    reviews.value.forEach((review) => {
      if (replyDrafts.value[review.id] === undefined) replyDrafts.value[review.id] = review.replyContent ?? ''
    })
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function saveReply(review: Review) {
  const content = (replyDrafts.value[review.id] ?? '').trim()
  if (!content || savingReviewId.value) return

  const hadReply = Boolean(review.replyContent)
  savingReviewId.value = review.id
  feedback('')
  try {
    const updated = await reviewApi.reply(review.id, content)
    const index = reviews.value.findIndex((item) => item.id === updated.id)
    if (index >= 0) reviews.value.splice(index, 1, updated)
    replyDrafts.value[updated.id] = updated.replyContent ?? ''
    feedback(hadReply ? '回复已更新' : '回复已发布')
  } catch (cause) {
    feedback(errorMessage(cause), 'error')
  } finally {
    savingReviewId.value = 0
  }
}

function changePage(next: number) {
  if (loading.value || next < 1 || next > Math.max(Math.ceil(total.value / pageSize), 1)) return
  page.value = next
  void loadReviews()
}
</script>

<template>
  <main class="ops-page">
    <PageHeader title="评价管理" description="按商家查询公开评价，并及时回复顾客反馈。">
      <template #actions><BaseButton variant="outline" :loading="loading" :disabled="submittedMerchantId === null" @click="loadReviews()">刷新评价</BaseButton></template>
    </PageHeader>

    <BentoGrid class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6 ops-grid">
      <BentoCard class="ops-card-lg ops-card" size="standard" tone="pink" :interactive="false" title="顾客评价" description="评价查询以商家为范围，回复会展示在该商家的评价列表中。">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">★</span></template>
        <div class="ops-inline-form">
          <BaseInput v-model="merchantIdInput" label="商家 ID" type="number" :min="1" :step="1" :error="queryError" placeholder="输入商家 ID" @keyup.enter="loadReviews(true)" />
          <BaseButton variant="outline" :loading="loading" :disabled="loading" @click="loadReviews(true)">查询</BaseButton>
        </div>
        <LoadingState v-if="loading" label="正在载入评价" />
        <ErrorState v-else-if="error" :message="error" @retry="loadReviews()" />
        <EmptyState v-else-if="submittedMerchantId === null" title="请选择商家" description="输入商家 ID 后查询该商家的公开评价。" />
        <EmptyState v-else-if="reviews.length === 0" title="暂无评价" description="该商家当前没有可见评价。" />
        <div v-else class="ops-list">
          <article v-for="review in reviews" :key="review.id" class="ops-row">
            <div class="ops-row-main">
              <div class="ops-row-title">
                <span :aria-label="`${review.rating} 星评价`">{{ ratingStars(review.rating) }}</span>
                <span class="ops-muted">订单 #{{ review.orderId }}</span>
                <StatusBadge status="VISIBLE" />
              </div>
              <p class="ops-row-meta">{{ review.content || '顾客未填写文字评价' }} · {{ dateTime(review.createdAt) }}</p>
              <div v-if="review.replyContent" class="ops-row-meta"><strong>已回复：</strong>{{ review.replyContent }} · {{ dateTime(review.repliedAt) }}</div>
              <BaseInput v-model="replyDrafts[review.id]" :label="review.replyContent ? '修改回复' : '回复顾客'" placeholder="写下真诚的回应" />
            </div>
            <BaseButton
              size="sm"
              :loading="savingReviewId === review.id"
              :disabled="savingReviewId !== 0 || !replyDrafts[review.id]?.trim()"
              @click="saveReply(review)"
            >
              {{ review.replyContent ? '更新回复' : '发布回复' }}
            </BaseButton>
          </article>
        </div>
        <OpsPager
          v-if="submittedMerchantId !== null && !loading && !error"
          :page="page"
          :pages="Math.ceil(total / pageSize)"
          :total="total"
          :busy="loading"
          @prev="changePage(page - 1)"
          @next="changePage(page + 1)"
        />
      </BentoCard>

      <BentoCard class="ops-card-wide ops-card" size="compact" tone="cyan" :interactive="false" title="评价概览">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">▤</span></template>
        <div class="ops-kpis">
          <div class="ops-kpi"><span>评价总量</span><strong>{{ total }}</strong></div>
          <div class="ops-kpi"><span>本页评价</span><strong>{{ reviews.length }}</strong></div>
          <div class="ops-kpi"><span>已回复</span><strong>{{ reviews.length - pendingReplyCount }}</strong></div>
          <div class="ops-kpi"><span>待回复</span><strong>{{ pendingReplyCount }}</strong></div>
        </div>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="orange" :interactive="false" title="平均评分">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">★</span></template>
        <strong class="text-3xl">{{ Number(averageRating).toFixed(1) }}</strong>
        <p class="ops-muted">满分 5 分</p>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="green" :interactive="false" title="回复率">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">文</span></template>
        <strong class="text-3xl">{{ replyRate }}%</strong>
        <p class="ops-muted">本页评价已回复</p>
      </BentoCard>
    </BentoGrid>

    <p v-if="notice" class="ops-notice" :class="{ 'ops-success': noticeKind === 'success' }" role="status">{{ notice }}</p>
  </main>
</template>

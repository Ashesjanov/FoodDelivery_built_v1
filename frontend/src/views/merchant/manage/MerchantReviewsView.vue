<script setup lang="ts">
/** 商家评价回复页：查看评分分布并回复顾客评价。 */
import { computed, onMounted, ref } from 'vue'
import type { Merchant, Review } from '@/types/domain'
import { merchantApi, reviewApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import BentoGrid from '@/components/bento/BentoGrid.vue'
import BentoCard from '@/components/bento/BentoCard.vue'
import OpsPager from '@/components/features/ops/OpsPager.vue'
import { callApi, currentUserId, dateTime, errorMessage, pageRecords, pageTotal } from '@/components/features/ops/opsUtils'
import '@/components/features/ops/opsStyles.css'

const auth = useAuthStore() as any
const merchant = ref<Merchant | null>(null)
const reviews = ref<Review[]>([])
const replyDrafts = ref<Record<number, string>>({})
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const saving = ref(0)
const error = ref('')
const notice = ref('')

const average = computed(() => {
  if (!reviews.value.length) return merchant.value?.rating ?? 0
  return reviews.value.reduce((sum, item) => sum + item.rating, 0) / reviews.value.length
})

const positiveRate = computed(() => {
  if (!reviews.value.length) return 0
  return Math.round((reviews.value.filter((item) => item.rating >= 4).length / reviews.value.length) * 100)
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await callApi<any>(merchantApi, ['list', 'page'], [{ page: 1, size: 100 }])
    const list = pageRecords<Merchant>(result)
    merchant.value = list.find((item) => item.ownerId === currentUserId(auth)) ?? list[0] ?? null
    if (!merchant.value) throw new Error('当前账号尚未关联商家')
    const reviewResult = await callApi<any>(reviewApi, ['byMerchant', 'merchantReviews'], [merchant.value.id, { page: page.value, size: 8 }])
    reviews.value = pageRecords<Review>(reviewResult)
    total.value = pageTotal(reviewResult)
    reviews.value.forEach((item) => { if (replyDrafts.value[item.id] === undefined) replyDrafts.value[item.id] = item.replyContent ?? '' })
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function saveReply(review: Review) {
  const content = (replyDrafts.value[review.id] ?? '').trim()
  if (!content || saving.value) return
  saving.value = review.id
  notice.value = ''
  try {
    await callApi(reviewApi, ['reply'], [review.id, content])
    notice.value = '回复已发布'
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
    <PageHeader title="商家评价" description="查看顾客反馈，及时回复评价建立信任。">
      <template #actions><BaseButton variant="outline" :loading="loading" @click="load">刷新评价</BaseButton></template>
    </PageHeader>

    <BentoGrid class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6 ops-grid">
      <BentoCard class="ops-card-lg ops-card" size="standard" tone="pink" :interactive="false" title="顾客评价" description="回复会展示在商家评价列表中。">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">★</span></template>
        <LoadingState v-if="loading" label="正在载入评价" />
        <ErrorState v-else-if="error" :message="error" @retry="load" />
        <EmptyState v-else-if="reviews.length === 0" title="暂无评价" description="顾客完成订单后可以留下反馈。" />
        <div v-else class="ops-list">
          <article v-for="review in reviews" :key="review.id" class="ops-row">
            <div class="ops-row-main">
              <div class="ops-row-title"><span>{{ '★'.repeat(review.rating) }}{{ '☆'.repeat(5 - review.rating) }}</span><span class="ops-muted">订单 #{{ review.orderId }}</span></div>
              <p class="ops-row-meta">{{ review.content || '顾客未填写文字评价' }} · {{ dateTime(review.createdAt) }}</p>
              <div v-if="review.replyContent" class="ops-row-meta"><strong>已回复：</strong>{{ review.replyContent }}</div>
              <BaseInput v-model="replyDrafts[review.id]" :label="review.replyContent ? '修改回复' : '回复顾客'" placeholder="写下真诚的回应" />
            </div>
            <BaseButton size="sm" :loading="saving === review.id" :disabled="!replyDrafts[review.id]?.trim()" @click="saveReply(review)">{{ review.replyContent ? '更新回复' : '发布回复' }}</BaseButton>
          </article>
        </div>
        <OpsPager :page="page" :pages="Math.ceil(total / 8)" :total="total" :busy="loading" @prev="changePage(page - 1)" @next="changePage(page + 1)" />
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="orange" :interactive="false" title="平均评分">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">★</span></template>
        <strong style="font-size: 34px">{{ Number(average).toFixed(1) }}</strong>
        <p class="ops-muted">满分 5 分</p>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="green" :interactive="false" title="好评率">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">♡</span></template>
        <strong style="font-size: 34px">{{ positiveRate }}%</strong>
        <p class="ops-muted">四星及以上评价</p>
      </BentoCard>

      <BentoCard class="ops-card-wide ops-card" size="compact" tone="cyan" :interactive="false" title="回复建议">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">文</span></template>
        <p class="ops-muted">感谢顾客的具体反馈，说明改进安排，并欢迎再次光临。回复长度控制在 500 字以内。</p>
      </BentoCard>
    </BentoGrid>

    <p v-if="notice" class="ops-notice" :class="{ 'ops-success': notice.includes('已') }" role="status">{{ notice }}</p>
  </main>
</template>

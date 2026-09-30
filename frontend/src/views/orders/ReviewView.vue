<script setup lang="ts">
/** 订单评价页：提交评分、文字感受和可选图片链接。 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { OrderDetail, Review } from '@/types/domain'
import { orderApi, reviewApi } from '@/api'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import BentoGrid from '@/components/bento/BentoGrid.vue'
import BentoCard from '@/components/bento/BentoCard.vue'
import { errorMessage, orderIdFromRoute, unwrap } from '@/components/features/customer/customerUtils'

const route = useRoute()
const router = useRouter()
const orderId = orderIdFromRoute(route)
const detail = ref<OrderDetail | null>(null)
const rating = ref(5)
const content = ref('')
const imageUrl = ref('')
const loading = ref(true)
const error = ref('')
const submitting = ref(false)
const notice = ref('')
const submitted = ref<Review | null>(null)

const ratingText = computed(() => ['', '不太满意', '一般', '还不错', '很满意', '超出预期'][rating.value])

async function load() {
  loading.value = true
  error.value = ''
  try {
    detail.value = unwrap<OrderDetail>(await orderApi.detail(orderId))
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (submitting.value || !detail.value) return
  submitting.value = true
  notice.value = ''
  try {
    submitted.value = unwrap<Review>(await reviewApi.create({ orderId, rating: rating.value, content: content.value.trim(), imageUrl: imageUrl.value.trim() || undefined }))
    notice.value = '感谢你的评价，它会帮助更多人找到好味道。'
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="review-page">
    <PageHeader title="分享这次味道" description="真实感受会让商家做得更好。" :back-to="`/orders/${orderId}`" />
    <LoadingState v-if="loading" label="正在准备评价" />
    <ErrorState v-else-if="error" :message="error" action-label="重新加载" @retry="load" />
    <BentoGrid v-else class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
      <BentoCard class="rating-card" size="large" tone="warm" aspect="1 / 1">
        <h1>{{ detail?.order.merchantName }}</h1><p class="lead">这顿饭带给你怎样的感受？</p>
        <div class="stars" role="radiogroup" aria-label="评分"><button v-for="value in 5" :key="value" type="button" :class="['star group', { active: value <= rating, disabled: !!submitted }]" :disabled="!!submitted" :aria-label="`${value} 分`" @click="rating = value">★</button></div><p class="rating-text">{{ ratingText }}</p>
      </BentoCard>

      <BentoCard class="form-card" size="medium" tone="default" aspect="2 / 1">
        <h2>写下你的感受</h2>
        <BaseInput v-model="content" label="评价内容" placeholder="味道、份量、包装或配送体验" :disabled="!!submitted" />
        <BaseInput v-model="imageUrl" label="图片链接（选填）" placeholder="分享一张你拍下的美味" :disabled="!!submitted" />
        <BaseButton type="primary" :loading="submitting" :disabled="!!submitted" @click="submit">{{ submitted ? '评价已提交' : '提交评价' }}</BaseButton>
        <p v-if="notice" class="notice" role="alert">{{ notice }}</p>
      </BentoCard>

      <BentoCard class="preview-card" size="small" tone="fresh" aspect="1 / 1"><h2>公开预览</h2><div class="preview-stars">{{ '★'.repeat(rating) }}{{ '☆'.repeat(5 - rating) }}</div><p>{{ content || '你的评价会出现在商家页面。' }}</p></BentoCard>
      <BentoCard class="thanks-card" size="small" tone="accent" aspect="1 / 1"><h2>谢谢你认真吃饭</h2><p>每一次反馈都会成为下一顿饭更好的开始。</p><BaseButton type="ghost" @click="router.push('/orders')">返回订单</BaseButton></BentoCard>
    </BentoGrid>
  </main>
</template>

<style scoped>
.review-page { min-height: 100vh; padding: clamp(24px, 5vw, 76px); background: #f5f2ec; color: #25251f; font-family: 'PingFang SC', sans-serif; }.section-label { letter-spacing: 0; font-size: 13px; opacity: .72; margin: 0 0 8px; } h1, h2 { font-family: Georgia, 'Songti SC', serif; font-weight: 500; margin: 0; } h1 { font-size: clamp(38px, 5vw, 60px); } h2 { font-size: 25px; }
.rating-card, .form-card, .preview-card, .thanks-card { padding: clamp(22px, 2.8vw, 38px); }.rating-card { display: flex; flex-direction: column; justify-content: space-between; }.lead { color: #775f43; }.stars { display: flex; gap: 16px; }.star { border: 0; background: transparent; color: #d8c5aa; font-size: clamp(34px, 4vw, 52px); padding: 0; cursor: pointer; transition: transform .25s ease, color .25s ease; }.star.active { color: #b46a22; }.star:hover, .star:focus-visible { transform: scale(1.18) rotate(-6deg); outline: none; }.star.disabled { cursor: default; }.rating-text { font-family: Georgia, 'Songti SC', serif; font-size: 24px; color: #765332; }.form-card > *, .preview-card > *, .thanks-card > * { margin-top: 16px; }.notice { color: #9a5e23; margin-bottom: 0; }.preview-stars { color: #b46a22; font-size: 24px; }.preview-card p, .thanks-card p { color: #687369; line-height: 1.8; }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { transition-duration: .01ms !important; animation-duration: .01ms !important; } }
</style>

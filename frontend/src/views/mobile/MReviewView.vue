<script setup lang="ts">
/** 移动端订单评价页：与电脑端内容一致，提交评分、文字感受和可选图片链接。 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { OrderDetail, Review } from '@/types/domain'
import { orderApi, reviewApi } from '@/api'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
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
  <div class="m-page">
    <header class="m-page-head">
      <RouterLink class="m-back" :to="`/m/orders/${orderId}`"><span aria-hidden="true">←</span> 返回订单详情</RouterLink>
      <h1 class="m-title" style="margin-top: 8px">分享这次味道</h1>
      <p class="m-sub">真实感受会让商家做得更好。</p>
    </header>

    <LoadingState v-if="loading" label="正在准备评价" />
    <ErrorState v-else-if="error" :message="error" action-label="重新加载" @retry="load" />
    <div v-else class="m-stack">
      <section class="m-card m-card--warm">
        <h2 class="m-title" style="font-size: 26px">{{ detail?.order.merchantName }}</h2>
        <p class="m-lead" style="color: #775f43">这顿饭带给你怎样的感受？</p>
        <div class="m-stars" role="radiogroup" aria-label="评分">
          <button v-for="value in 5" :key="value" type="button" class="m-star" :class="{ active: value <= rating, disabled: !!submitted }" :disabled="!!submitted" :aria-label="`${value} 分`" @click="rating = value">★</button>
        </div>
        <p class="m-rating-text">{{ ratingText }}</p>
      </section>

      <section class="m-card">
        <h2 class="m-subtitle">写下你的感受</h2>
        <div class="m-form" style="margin-top: 16px">
          <BaseInput v-model="content" label="评价内容" placeholder="味道、份量、包装或配送体验" :disabled="!!submitted" />
          <BaseInput v-model="imageUrl" label="图片链接（选填）" placeholder="分享一张你拍下的美味" :disabled="!!submitted" />
        </div>
        <div style="margin-top: 16px">
          <BaseButton type="primary" :loading="submitting" :disabled="!!submitted" block @click="submit">{{ submitted ? '评价已提交' : '提交评价' }}</BaseButton>
        </div>
        <p v-if="notice" class="m-notice" role="alert">{{ notice }}</p>
      </section>

      <section class="m-card m-card--fresh">
        <h2 class="m-subtitle">公开预览</h2>
        <p class="m-preview-stars">{{ '★'.repeat(rating) }}{{ '☆'.repeat(5 - rating) }}</p>
        <p class="m-info">{{ content || '你的评价会出现在商家页面。' }}</p>
      </section>

      <section class="m-card m-card--accent">
        <h2 class="m-subtitle">谢谢你认真吃饭</h2>
        <p class="m-info">每一次反馈都会成为下一顿饭更好的开始。</p>
        <div style="margin-top: 16px">
          <BaseButton type="ghost" @click="router.push('/m/orders')">返回订单</BaseButton>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.m-page-head { margin-bottom: 16px; }
.m-back { display: inline-flex; align-items: center; gap: 16px; color: #69746d; font-size: 14px; text-decoration: none; min-height: 44px; }
.m-back:hover, .m-back:focus-visible { color: #1d2722; outline: none; }
.m-sub { margin: 8px 0 0; color: #69746d; line-height: 1.7; font-size: 14px; }
.m-stars { display: flex; gap: 16px; margin-top: 18px; }
.m-star { border: 0; background: transparent; color: #d8c5aa; font-size: 42px; line-height: 1; padding: 0; min-width: 44px; min-height: 44px; cursor: pointer; transition: transform 0.25s ease, color 0.25s ease; }
.m-star.active { color: #b46a22; }
.m-star:hover, .m-star:focus-visible { transform: scale(1.15) rotate(-6deg); outline: none; }
.m-star.disabled { cursor: default; }
.m-rating-text { font-family: Georgia, 'Songti SC', serif; font-size: 22px; color: #765332; margin: 16px 0 0; }
.m-preview-stars { color: #b46a22; font-size: 24px; margin: 12px 0 0; }
.m-info { color: #687369; line-height: 1.8; margin: 10px 0 0; }
</style>

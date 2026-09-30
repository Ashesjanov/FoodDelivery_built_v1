<script setup lang="ts">
/** 顾客首页：筛选营业商家、展示精选商家和可领取优惠券。 */
import { computed, onMounted, ref } from 'vue'
import type { Coupon, Merchant, MerchantCategory } from '@/types/domain'
import { couponApi, merchantApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import BentoGrid from '@/components/bento/BentoGrid.vue'
import BentoCard from '@/components/bento/BentoCard.vue'
import { asArray, errorMessage, money, statusLabel } from '@/components/features/customer/customerUtils'

const authStore: any = useAuthStore()
const loading = ref(true)
const error = ref('')
const keyword = ref('')
const categoryId = ref<number | null>(null)
const openOnly = ref(true)
const claiming = ref(0)
const notice = ref('')
const categories = ref<MerchantCategory[]>([])
const merchants = ref<Merchant[]>([])
const coupons = ref<Coupon[]>([])

const categoryOptions = computed(() => [{ id: null, name: '全部' }, ...categories.value])

function merchantOpen(merchant: Merchant): boolean {
  return ['OPEN', 'BUSINESS', 'OPENING'].includes(String((merchant as any).businessStatus))
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [categoryResult, merchantResult, couponResult] = await Promise.all([
      merchantApi.categories(),
      merchantApi.list({ page: 1, size: 24, categoryId: categoryId.value ?? undefined, keyword: keyword.value.trim() || undefined, openOnly: openOnly.value }),
      couponApi.available().catch(() => []),
    ])
    categories.value = asArray<MerchantCategory>(categoryResult)
    merchants.value = asArray<Merchant>(merchantResult)
    coupons.value = asArray<Coupon>(couponResult)
  } catch (cause) {
    error.value = errorMessage(cause)
    useDemoData()
  } finally {
    loading.value = false
  }
}

function useDemoData() {
  categories.value = [
    { id: 1, name: '家常菜' }, { id: 2, name: '轻食' }, { id: 3, name: '烘焙' },
  ] as MerchantCategory[]
  merchants.value = [
    { id: 1, name: '巷子里·家常菜', description: '锅气十足的每日现炒，米饭免费续', businessStatus: 'OPEN', rating: 4.9, monthlySales: 862, minOrderAmount: 20, deliveryFee: 3, logoUrl: '/images/dish-vegetable.jpg' },
    { id: 2, name: '青禾轻食实验室', description: '低负担谷物碗与当季蔬果', businessStatus: 'OPEN', rating: 4.8, monthlySales: 519, minOrderAmount: 25, deliveryFee: 2, logoUrl: '/images/dish-salad.jpg' },
    { id: 3, name: '柴窑披萨工坊', description: '手工面团，450°C 柴窑现烤', businessStatus: 'CLOSED', rating: 4.7, monthlySales: 731, minOrderAmount: 48, deliveryFee: 5, logoUrl: '/images/dish-pizza.jpg' },
  ] as Merchant[]
  coupons.value = [
    { id: 11, code: 'LUNCH8', name: '午后满减券', couponType: 'FIXED', status: 'ACTIVE', thresholdAmount: 40, discountAmount: 8, discountRate: null, totalQuantity: 100, claimedQuantity: 68, remainingQuantity: 32, perUserLimit: 1, startTime: '', endTime: '' },
    { id: 12, code: 'NEW90', name: '新客九折券', couponType: 'PERCENT', status: 'ACTIVE', thresholdAmount: 20, discountAmount: null, discountRate: 0.9, totalQuantity: 100, claimedQuantity: 12, remainingQuantity: 88, perUserLimit: 1, startTime: '', endTime: '' },
  ] as Coupon[]
}

async function claimCoupon(id: number) {
  if (claiming.value) return
  claiming.value = id
  notice.value = ''
  try {
    await couponApi.claim(id)
    notice.value = '优惠券已放入你的账户'
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    claiming.value = 0
  }
}

function resetFilters() {
  categoryId.value = null
  keyword.value = ''
  openOnly.value = true
  load()
}

onMounted(load)
</script>

<template>
  <main class="home-page">
    <PageHeader title="今天吃点什么？" :description="authStore?.user?.nickname ? `${authStore.user.nickname}，附近有新鲜出炉的好味道。` : '挑选附近正在营业的好味道，准时送到你手上。'" />
    <LoadingState v-if="loading" label="正在寻找附近好味" />
    <ErrorState v-else-if="error && !merchants.length" :message="error" action-label="重新加载" @retry="load" />
    <BentoGrid v-else class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
      <BentoCard class="search-card" size="large" tone="dark" aspect="1 / 1">
        <div class="search-top"><div><h1>把城市的<br />好味道端上桌</h1></div><span class="hero-icon group">◎</span></div>
        <div class="search-panel">
          <BaseInput v-model="keyword" label="搜索商家" placeholder="店名、菜系或关键词" @keyup.enter="load" />
          <BaseButton type="primary" @click="load">搜索</BaseButton>
        </div>
        <div class="filter-line"><span>{{ merchants.length }} 家推荐</span><label><input v-model="openOnly" type="checkbox" @change="load" /> 仅看营业中</label></div>
      </BentoCard>

      <BentoCard class="category-card" size="small" tone="fresh" aspect="1 / 1">
        <h2>按口味探索</h2>
        <div class="category-list">
          <button v-for="category in categoryOptions" :key="category.id ?? 'all'" type="button" :class="['category-chip group', { active: categoryId === category.id }]" @click="categoryId = category.id; load()"><span class="chip-icon">◇</span>{{ category.name }}</button>
        </div>
      </BentoCard>

      <BentoCard v-for="(coupon, index) in coupons.slice(0, 2)" :key="coupon.id" class="coupon-card" :size="index === 0 ? 'medium' : 'small'" :tone="index === 0 ? 'warm' : 'accent'" :aspect="index === 0 ? '2 / 1' : '1 / 1'">
        <div class="coupon-head"><span class="coupon-icon group">✦</span><StatusBadge :status="statusLabel(coupon.status ?? 'ACTIVE')" /></div>
        <h2>{{ coupon.name }}</h2>
        <p class="coupon-value">{{ coupon.couponType === 'PERCENT' ? `${Number(coupon.discountRate ?? 1) * 10} 折` : `减 ${money(coupon.discountAmount)}` }}</p>
        <div class="coupon-bottom"><span>满 {{ money(coupon.thresholdAmount) }} 可用</span><BaseButton type="ghost" :loading="claiming === coupon.id" @click="claimCoupon(Number(coupon.id))">领取</BaseButton></div>
      </BentoCard>

      <BentoCard v-for="(merchant, index) in merchants" :key="merchant.id" class="merchant-card" :size="index % 5 === 0 ? 'large' : index % 3 === 0 ? 'medium' : 'small'" tone="default" interactive :aspect="index % 5 === 0 ? '1 / 1' : index % 3 === 0 ? '2 / 1' : '1 / 1'" @click="$router.push(`/merchants/${merchant.id}`)">
        <div class="merchant-visual">
          <img :src="merchant.logoUrl || '/images/dish-vegetable.jpg'" :alt="merchant.name" />
          <StatusBadge :status="merchantOpen(merchant) ? 'OPEN' : 'CLOSED'" />
        </div>
        <div class="merchant-copy"><h2>{{ merchant.name }}</h2><p class="description">{{ merchant.description }}</p><div class="merchant-meta"><span>★ {{ Number(merchant.rating ?? 0).toFixed(1) }}</span><span>月售 {{ merchant.monthlySales ?? 0 }}</span><span>{{ money(merchant.deliveryFee) }} 配送</span></div></div>
      </BentoCard>

      <BentoCard v-if="notice" class="notice-card" size="small" tone="warm" aspect="2 / 1"><p class="notice-text">{{ notice }}</p></BentoCard>

      <BentoCard v-if="!merchants.length" class="empty-card" size="large" tone="default" aspect="2 / 1">
        <EmptyState title="附近暂时没有匹配商家" description="换个关键词或取消筛选，再看看别的口味。" action-label="重置筛选" @action="resetFilters" />
      </BentoCard>
    </BentoGrid>
  </main>
</template>

<style scoped>
.home-page { min-height: 100vh; padding: clamp(24px, 5vw, 76px); background: #f4f4ef; color: #1d2722; font-family: 'PingFang SC', sans-serif; }
.section-label { font-size: 13px; letter-spacing: 0; opacity: .72; margin: 0 0 8px; }
h1, h2 { font-family: Georgia, 'Songti SC', serif; font-weight: 500; margin: 0; } h1 { font-size: clamp(34px, 4vw, 58px); line-height: 1.08; } h2 { font-size: clamp(21px, 2vw, 28px); }
.search-card { padding: clamp(24px, 3vw, 42px); color: #f6f4ed; display: flex; flex-direction: column; justify-content: space-between; }
.search-top { display: flex; justify-content: space-between; align-items: flex-start; }.hero-icon { font-size: 48px; color: #e0b356; transition: transform .3s ease; }.group:hover .hero-icon, .hero-icon:hover { transform: rotate(18deg) scale(1.12); }
.search-panel { display: grid; grid-template-columns: 1fr auto; align-items: end; gap: 16px; }.filter-line { display: flex; justify-content: space-between; color: #b9c2bb; font-size: 13px; }.filter-line label { display: flex; align-items: center; gap: 16px; }.filter-line input { accent-color: #d7a34c; }
.category-card, .coupon-card, .merchant-card, .notice-card, .empty-card { padding: clamp(20px, 2.4vw, 32px); }
.category-list { display: flex; flex-wrap: wrap; gap: 16px; margin-top: 24px; }.category-chip { border: 1px solid #c6d8c9; background: rgba(255,255,255,.45); border-radius: 999px; padding: 8px 13px; color: #31583d; cursor: pointer; font: inherit; transition: transform .25s ease, background .25s ease; }.category-chip:hover, .category-chip:focus-visible, .category-chip.active { background: #31583d; color: #fff; transform: translateY(-3px); outline: none; }.chip-icon { margin-right: 6px; transition: transform .25s ease; }.group:hover .chip-icon { transform: scale(1.3) rotate(25deg); }
.coupon-card { display: flex; flex-direction: column; justify-content: space-between; }.coupon-head, .coupon-bottom { display: flex; justify-content: space-between; align-items: center; gap: 16px; }.coupon-icon { font-size: 25px; color: #9e5b22; transition: transform .3s ease; }.group:hover .coupon-icon { transform: scale(1.2) rotate(-12deg); }.coupon-value { font-family: Georgia, 'Songti SC', serif; font-size: 28px; margin: 8px 0 14px; }.coupon-bottom { font-size: 12px; color: #765634; }
.merchant-card { overflow: hidden; cursor: pointer; }.merchant-visual { position: relative; overflow: hidden; border-radius: 16px; aspect-ratio: 16 / 8; margin-bottom: 18px; }.merchant-visual img { width: 100%; height: 100%; object-fit: cover; transition: transform .5s ease; }.merchant-card:hover .merchant-visual img { transform: scale(1.06); }.merchant-visual :deep(.status-badge) { position: absolute; top: 12px; right: 12px; }.description { color: #69746d; line-height: 1.65; margin: 10px 0 18px; }.merchant-meta { display: flex; flex-wrap: wrap; gap: 16px; color: #a16429; font-size: 13px; }
.notice-card { align-self: start; }.notice-text { margin: 0; color: #73512b; line-height: 1.7; }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { transition-duration: .01ms !important; animation-duration: .01ms !important; } }
</style>

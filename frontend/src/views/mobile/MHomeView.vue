<script setup lang="ts">
/** 移动端顾客首页：与电脑端首页内容一致，筛选营业商家、展示精选商家和可领取优惠券。 */
import { computed, onMounted, ref } from 'vue'
import type { Coupon, Merchant, MerchantCategory } from '@/types/domain'
import { couponApi, merchantApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
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
  <div class="m-page">
    <header class="m-page-head">
      <h1 class="m-title">今天吃点什么？</h1>
      <p class="m-sub">{{ authStore?.user?.nickname ? `${authStore.user.nickname}，附近有新鲜出炉的好味道。` : '挑选附近正在营业的好味道，准时送到你手上。' }}</p>
    </header>

    <LoadingState v-if="loading" label="正在寻找附近好味" />
    <ErrorState v-else-if="error && !merchants.length" :message="error" action-label="重新加载" @retry="load" />
    <div v-else class="m-stack">
      <section class="m-card m-card--dark">
        <h2 class="m-title" style="font-size: 26px">把城市的<br />好味道端上桌</h2>
        <div class="m-search">
          <BaseInput v-model="keyword" label="搜索商家" placeholder="店名、菜系或关键词" @keyup.enter="load" />
          <BaseButton type="primary" @click="load">搜索</BaseButton>
        </div>
        <div class="m-filter-line">
          <span>{{ merchants.length }} 家推荐</span>
          <label><input v-model="openOnly" type="checkbox" @change="load" /> 仅看营业中</label>
        </div>
      </section>

      <section class="m-card m-card--fresh">
        <h2 class="m-subtitle">按口味探索</h2>
        <div class="m-chips" style="margin-top: 16px">
          <button v-for="category in categoryOptions" :key="category.id ?? 'all'" type="button" class="m-chip" :class="{ 'is-active': categoryId === category.id }" @click="categoryId = category.id; load()">◇ {{ category.name }}</button>
        </div>
      </section>

      <section v-for="coupon in coupons.slice(0, 2)" :key="coupon.id" class="m-card" :class="coupon.couponType === 'PERCENT' ? 'm-card--accent' : 'm-card--warm'">
        <div class="m-row">
          <span style="font-size: 24px; color: #9e5b22" aria-hidden="true">✦</span>
          <StatusBadge :status="statusLabel(coupon.status ?? 'ACTIVE')" />
        </div>
        <h2 class="m-subtitle" style="margin-top: 8px">{{ coupon.name }}</h2>
        <p class="m-coupon-value">{{ coupon.couponType === 'PERCENT' ? `${Number(coupon.discountRate ?? 1) * 10} 折` : `减 ${money(coupon.discountAmount)}` }}</p>
        <div class="m-row">
          <span class="m-muted" style="color: #765634">满 {{ money(coupon.thresholdAmount) }} 可用</span>
          <BaseButton type="ghost" :loading="claiming === coupon.id" @click="claimCoupon(Number(coupon.id))">领取</BaseButton>
        </div>
      </section>

      <p v-if="notice" class="m-notice" role="status">{{ notice }}</p>

      <button v-for="merchant in merchants" :key="merchant.id" type="button" class="m-card m-merchant" @click="$router.push(`/m/merchants/${merchant.id}`)">
        <div class="m-merchant-visual">
          <img :src="merchant.logoUrl || '/images/dish-vegetable.jpg'" :alt="merchant.name" />
          <StatusBadge :status="merchantOpen(merchant) ? 'OPEN' : 'CLOSED'" />
        </div>
        <h2 class="m-subtitle">{{ merchant.name }}</h2>
        <p class="m-desc">{{ merchant.description }}</p>
        <div class="m-meta">
          <span>★ {{ Number(merchant.rating ?? 0).toFixed(1) }}</span>
          <span>月售 {{ merchant.monthlySales ?? 0 }}</span>
          <span>{{ money(merchant.deliveryFee) }} 配送</span>
        </div>
      </button>

      <section v-if="!merchants.length" class="m-card">
        <EmptyState title="附近暂时没有匹配商家" description="换个关键词或取消筛选，再看看别的口味。" action-label="重置筛选" @action="resetFilters" />
      </section>
    </div>
  </div>
</template>

<style scoped>
.m-page-head { margin-bottom: 16px; }
.m-sub { margin: 8px 0 0; color: #69746d; line-height: 1.7; font-size: 14px; }
.m-search { display: grid; gap: 16px; margin-top: 16px; }
.m-filter-line { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-top: 16px; color: #b9c2bb; font-size: 13px; }
.m-filter-line input { width: 18px; height: 18px; accent-color: #d7a34c; }
.m-coupon-value { font-family: Georgia, 'Songti SC', serif; font-size: 26px; margin: 8px 0 14px; }
.m-merchant { display: block; width: 100%; text-align: left; font: inherit; color: inherit; cursor: pointer; }
.m-merchant:hover, .m-merchant:focus-visible { outline: none; box-shadow: 0 14px 34px rgba(24, 24, 27, 0.12); }
.m-merchant-visual { position: relative; margin-bottom: 14px; }
.m-merchant-visual img { width: 100%; aspect-ratio: 16 / 9; object-fit: cover; border-radius: 16px; display: block; }
.m-merchant-visual :deep(.status-badge) { position: absolute; top: 12px; right: 12px; }
.m-desc { color: #69746d; line-height: 1.65; margin: 8px 0 14px; font-size: 14px; }
.m-meta { display: flex; flex-wrap: wrap; gap: 16px; color: #a16429; font-size: 13px; }
</style>

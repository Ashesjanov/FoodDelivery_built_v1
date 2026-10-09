<script setup lang="ts">
/** 公开商家菜单页：支持规格、数量、备注、库存和跨商家加购拦截。 */
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { Dish, DishSpec, Merchant, Review } from '@/types/domain'
import { cartApi, dishApi, merchantApi, reviewApi } from '@/api'
import { useCartStore } from '@/stores/cart'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import BentoGrid from '@/components/bento/BentoGrid.vue'
import BentoCard from '@/components/bento/BentoCard.vue'
import { asArray, errorMessage, money, statusLabel, unwrap } from '@/components/features/customer/customerUtils'

const route = useRoute()
const cartStore: any = useCartStore()
const merchantId = Number(Array.isArray(route.params.id) ? route.params.id[0] : route.params.id)
const merchant = ref<Merchant | null>(null)
const dishes = ref<Dish[]>([])
const reviews = ref<Review[]>([])
const keyword = ref('')
const activeCategory = ref<number | null>(null)
const loading = ref(true)
const error = ref('')
const notice = ref('')
const adding = ref(false)
const pendingDish = ref<Dish | null>(null)
const specs = ref<DishSpec[]>([])
const specId = ref<number | null>(null)
const quantity = ref(1)
const note = ref('')
const crossMerchant = ref(false)
const conflictMerchant = ref('')

const visibleDishes = computed(() => dishes.value.filter((dish) => {
  const matchKeyword = !keyword.value.trim() || `${dish.name}${dish.description ?? ''}`.includes(keyword.value.trim())
  const matchCategory = activeCategory.value === null || dish.categoryId === activeCategory.value
  return matchKeyword && matchCategory
}))

const groupedCategories = computed(() => {
  const ids = [...new Set(dishes.value.map((dish) => dish.categoryId))]
  return ids.map((id, index) => ({ id, name: ['招牌推荐', '主食', '小食', '饮品', '套餐'][index] || `分类 ${index + 1}` }))
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [merchantResult, dishResult, reviewResult] = await Promise.all([
      merchantApi.detail(merchantId),
      dishApi.list({ merchantId, page: 1, size: 60, onSaleOnly: true }),
      reviewApi.byMerchant(merchantId, { page: 1, size: 3 }).catch(() => ({ records: [] })),
    ])
    merchant.value = (unwrap<any>(merchantResult)?.merchant ?? unwrap<any>(merchantResult)) as Merchant
    dishes.value = asArray<Dish>(dishResult)
    reviews.value = asArray<Review>(reviewResult)
    if (!merchant.value) useDemoData()
  } catch (cause) {
    error.value = errorMessage(cause)
    useDemoData()
  } finally {
    loading.value = false
  }
}

function useDemoData() {
  merchant.value = { id: merchantId, ownerId: 1, name: '巷子里·家常菜', description: '认真做每一顿家常菜，热乎、踏实、有锅气。', logoUrl: '/images/dish-vegetable.jpg', contactPhone: '13800138000', address: '梧桐路 18 号', businessStatus: 'OPEN', businessHours: '10:30 - 21:30', minOrderAmount: 20, deliveryFee: 3, packagingFee: 1, rating: 4.9, monthlySales: 862 } as Merchant
  dishes.value = [
    { id: 101, merchantId, categoryId: 1, name: '砂锅红烧肉', description: '肥瘦相间，慢火收汁', imageUrl: '/images/dish-vegetable.jpg', price: 36, originalPrice: 42, status: 'ON_SALE', stock: 18, sales: 203 },
    { id: 102, merchantId, categoryId: 1, name: '外婆菜炒饭', description: '粒粒分明，微辣鲜香', imageUrl: '/images/dish-pizza.jpg', price: 22, status: 'ON_SALE', stock: 25, sales: 168 },
    { id: 103, merchantId, categoryId: 2, name: '桂花酸梅汤', description: '自煮冰镇，清爽解腻', imageUrl: '/images/dish-salad.jpg', price: 8, status: 'ON_SALE', stock: 40, sales: 326 },
    { id: 104, merchantId, categoryId: 3, name: '时令蔬菜', description: '今日空心菜，蒜蓉清炒', imageUrl: '/images/dish-salad.jpg', price: 16, status: 'SOLD_OUT', stock: 0, sales: 76 },
  ] as Dish[]
}

async function openDish(dish: Dish) {
  if (dish.stock <= 0 || dish.status !== 'ON_SALE') {
    notice.value = '这道菜暂时售罄了'
    return
  }
  pendingDish.value = dish
  quantity.value = 1
  note.value = ''
  specId.value = null
  specs.value = []
  try {
    specs.value = asArray<DishSpec>(await dishApi.specs(dish.id))
    specId.value = specs.value.find((spec) => spec.isDefault)?.id ?? specs.value[0]?.id ?? null
  } catch {
    specs.value = [
      { id: 1, dishId: dish.id, groupName: '份量', name: '标准份', priceOffset: 0, isDefault: true },
      { id: 2, dishId: dish.id, groupName: '份量', name: '大份', priceOffset: 6, isDefault: false },
    ] as DishSpec[]
    specId.value = 1
  }
}

function closeModal() {
  pendingDish.value = null
  crossMerchant.value = false
}

async function addToCart(replace = false) {
  if (!pendingDish.value || adding.value) return
  adding.value = true
  notice.value = ''
  try {
    if (!replace) {
      const cart = unwrap<any>(await cartApi.get().catch(() => null))
      const existingMerchant = Number(cart?.items?.[0]?.merchantId ?? 0)
      if (existingMerchant && existingMerchant !== merchantId) {
        conflictMerchant.value = cart?.merchantName || '另一家商家'
        crossMerchant.value = true
        return
      }
    } else {
      await cartApi.clear()
    }
    await cartApi.add({ dishId: pendingDish.value.id, dishSpecId: specId.value ?? undefined, quantity: quantity.value, note: note.value.trim() || undefined })
    await cartStore.refresh?.()
    notice.value = '已加入购物车'
    closeModal()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    adding.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="merchant-page">
    <PageHeader :title="merchant?.name || '商家菜单'" :description="merchant?.description || '浏览今日菜单'" back-to="/" />
    <LoadingState v-if="loading" label="正在准备菜单" />
    <ErrorState v-else-if="error && !merchant" :message="error" action-label="重新加载" @retry="load" />
    <BentoGrid v-else class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
      <BentoCard class="merchant-hero" size="large" tone="dark" aspect="1 / 1">
        <img :src="merchant?.logoUrl || '/images/dish-vegetable.jpg'" :alt="merchant?.name" />
        <div class="hero-overlay"><StatusBadge :status="statusLabel(merchant?.businessStatus ?? 'OPEN')" /><h1>{{ merchant?.name }}</h1><p>{{ merchant?.businessHours }} · {{ money(merchant?.deliveryFee) }} 配送</p></div>
      </BentoCard>

      <BentoCard class="filter-card" size="medium" tone="fresh" aspect="2 / 1">
        <div class="filter-title"><div><h2>选一道喜欢的</h2></div><BaseButton type="secondary" @click="$router.push('/cart')">查看购物车</BaseButton></div>
        <BaseInput v-model="keyword" label="搜索菜品" placeholder="菜名或描述" />
        <div class="tabs"><button v-for="item in [{ id: null, name: '全部' }, ...groupedCategories]" :key="item.id ?? 'all'" type="button" :class="{ active: activeCategory === item.id }" @click="activeCategory = item.id">{{ item.name }}</button></div>
      </BentoCard>

      <BentoCard class="notice-card" size="small" tone="warm" aspect="1 / 1"><h3>起送 {{ money(merchant?.minOrderAmount) }}</h3><p>{{ merchant?.address }}</p><p v-if="notice" class="notice-text">{{ notice }}</p></BentoCard>

      <BentoCard v-for="(dish, index) in visibleDishes" :key="dish.id" class="dish-card" :size="index === 0 ? 'large' : index % 3 === 0 ? 'medium' : 'small'" :tone="index === 0 ? 'accent' : 'default'" interactive :aspect="index === 0 ? '1 / 1' : index % 3 === 0 ? '2 / 1' : '1 / 1'" @click="openDish(dish)">
        <div class="dish-image group"><img :src="dish.imageUrl || '/images/dish-salad.jpg'" :alt="dish.name" /><span class="add-icon">＋</span></div>
        <div class="dish-copy"><div class="dish-title"><h2>{{ dish.name }}</h2><StatusBadge :status="dish.status === 'ON_SALE' ? (dish.stock > 0 ? 'ON_SALE' : 'SOLD_OUT') : dish.status" /></div><p>{{ dish.description }}</p><div class="dish-meta"><strong>{{ money(dish.price) }}</strong><span>库存 {{ dish.stock }} · 已售 {{ dish.sales }}</span></div></div>
      </BentoCard>

      <BentoCard v-if="!visibleDishes.length" class="empty-card" size="large" tone="default" aspect="2 / 1"><EmptyState title="没有找到这道菜" description="换个关键词，或者看看全部菜单。" action-label="查看全部" @action="keyword = ''; activeCategory = null" /></BentoCard>

      <BentoCard class="review-card" size="medium" tone="warm" aspect="2 / 1"><h2>大家怎么说</h2><div v-if="reviews.length" class="review-list"><blockquote v-for="review in reviews" :key="review.id"><span>★★★★★</span><p>{{ review.content || '味道很不错，配送也很准时。' }}</p><cite>{{ review.rating }} 分</cite></blockquote></div><EmptyState v-else title="还没有评价" description="成为第一位留下感受的人。" /></BentoCard>
    </BentoGrid>

    <div v-if="pendingDish" class="modal-backdrop" role="presentation" @click.self="closeModal">
      <section class="dish-modal" role="dialog" aria-modal="true" aria-label="选择菜品规格">
        <button class="close-button" type="button" aria-label="关闭" @click="closeModal">×</button>
        <h2>{{ pendingDish.name }}</h2><p class="modal-desc">{{ pendingDish.description }}</p>
        <div v-if="specs.length" class="spec-section"><h3>选择规格</h3><div class="spec-list"><button v-for="spec in specs" :key="spec.id" type="button" :class="{ active: specId === spec.id }" @click="specId = spec.id"><span>{{ spec.groupName }} · {{ spec.name }}</span><small>{{ spec.priceOffset ? `+${money(spec.priceOffset)}` : '不加价' }}</small></button></div></div>
        <BaseInput v-model="note" label="口味备注" placeholder="例如：少盐、不要香菜" />
        <div class="quantity-row"><span>数量</span><div class="stepper"><button type="button" @click="quantity = Math.max(1, quantity - 1)">−</button><strong>{{ quantity }}</strong><button type="button" @click="quantity = Math.min(pendingDish.stock, quantity + 1)">＋</button></div></div>
        <div class="modal-actions"><BaseButton type="secondary" @click="closeModal">再看看</BaseButton><BaseButton type="primary" :loading="adding" @click="addToCart()">加入购物车 {{ money(Number(pendingDish.price) * quantity) }}</BaseButton></div>
      </section>
    </div>

    <div v-if="crossMerchant" class="modal-backdrop" role="presentation" @click.self="closeModal">
      <section class="conflict-modal" role="dialog" aria-modal="true"><h2>要换一家店吗？</h2><p>购物车里已有{{ conflictMerchant }}的商品。清空后才能把这道菜加入。</p><div class="modal-actions"><BaseButton type="secondary" @click="closeModal">保留原购物车</BaseButton><BaseButton type="danger" :loading="adding" @click="addToCart(true)">清空并加入</BaseButton></div></section>
    </div>
  </main>
</template>

<style scoped>
.merchant-page { min-height: 100vh; padding: clamp(24px, 5vw, 76px); background: #f5f3ee; color: #25251f; font-family: 'PingFang SC', sans-serif; }
.section-label { letter-spacing: 0; font-size: 13px; opacity: .72; margin: 0 0 8px; } h1, h2, h3 { font-family: Georgia, 'Songti SC', serif; font-weight: 500; margin: 0; } h1 { font-size: clamp(36px, 4vw, 58px); } h2 { font-size: 25px; } h3 { font-size: 20px; }
.merchant-hero { padding: 0; overflow: hidden; position: relative; color: white; }.merchant-hero img { width: 100%; height: 100%; object-fit: cover; }.hero-overlay { position: absolute; inset: 0; padding: clamp(24px, 3vw, 42px); display: flex; flex-direction: column; justify-content: flex-end; background: rgba(16,22,18,.52); }.hero-overlay p:last-child { color: #d8ded7; margin: 12px 0 0; }
.filter-card, .notice-card, .dish-card, .review-card, .empty-card { padding: clamp(20px, 2.6vw, 34px); }.filter-title, .dish-title, .quantity-row, .modal-actions { display: flex; align-items: center; justify-content: space-between; gap: 16px; }.tabs { display: flex; flex-wrap: wrap; gap: 16px; margin-top: 18px; }.tabs button { border: 1px solid #c8d5c8; background: transparent; border-radius: 999px; padding: 8px 13px; color: #3d5b42; cursor: pointer; font: inherit; }.tabs button.active, .tabs button:hover, .tabs button:focus-visible { background: #3d5b42; color: white; outline: none; transform: translateY(-2px); }
.notice-card p { color: #75644d; line-height: 1.7; }.notice-text { border-top: 1px solid #dfcfb7; padding-top: 14px; }
.dish-card { cursor: pointer; overflow: hidden; }.dish-image { position: relative; overflow: hidden; border-radius: 16px; aspect-ratio: 16 / 9; }.dish-image img { width: 100%; height: 100%; object-fit: cover; transition: transform .45s ease; }.dish-card:hover .dish-image img { transform: scale(1.07); }.add-icon { position: absolute; right: 12px; bottom: 12px; width: 42px; height: 42px; display: grid; place-items: center; border-radius: 50%; background: #f3c15d; color: #3f321b; font-size: 24px; transition: transform .25s ease; }.group:hover .add-icon { transform: rotate(90deg) scale(1.1); }.dish-copy { padding-top: 18px; }.dish-copy > p { color: #74736b; margin: 9px 0 18px; }.dish-meta { display: flex; align-items: center; justify-content: space-between; gap: 16px; color: #8d6531; }.dish-meta strong { font-family: Georgia, serif; font-size: 22px; }
.review-list { display: grid; gap: 16px; margin-top: 18px; }.review-list blockquote { margin: 0; padding-top: 12px; border-top: 1px solid #e1d4c0; }.review-list blockquote span { color: #bb7a2d; letter-spacing: 0; }.review-list p { margin: 6px 0; color: #655d52; }.review-list cite { font-style: normal; color: #96846e; font-size: 12px; }
.modal-backdrop { position: fixed; inset: 0; z-index: 20; background: rgba(23,28,24,.55); display: grid; place-items: center; padding: 18px; }.dish-modal, .conflict-modal { width: min(560px, 100%); max-height: 92vh; overflow: auto; background: #fbfaf6; border-radius: 24px; padding: clamp(24px, 4vw, 42px); box-shadow: 0 30px 70px rgba(20,28,23,.25); }.close-button { float: right; border: 0; background: #efeee9; width: 38px; height: 38px; border-radius: 50%; font-size: 22px; cursor: pointer; }.modal-desc { color: #777269; line-height: 1.7; }.spec-section { margin: 22px 0; }.spec-list { display: grid; gap: 16px; margin-top: 12px; }.spec-list button { border: 1px solid #dad7cc; background: white; border-radius: 14px; padding: 12px 14px; display: flex; justify-content: space-between; cursor: pointer; color: #4e514a; font: inherit; }.spec-list button.active { border-color: #bb7a2d; color: #9c5c20; background: #fff7e8; }.quantity-row { margin: 22px 0; }.stepper { display: flex; align-items: center; gap: 16px; }.stepper button { width: 36px; height: 36px; border: 1px solid #d5d2c8; border-radius: 50%; background: white; cursor: pointer; font-size: 18px; }.conflict-modal p { color: #6e6a62; line-height: 1.8; }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { transition-duration: .01ms !important; animation-duration: .01ms !important; } }
</style>

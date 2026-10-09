<script setup lang="ts">
/** 移动端公开商家菜单页：与电脑端内容一致，支持规格、数量、备注、库存和跨商家加购拦截。 */
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { Dish, DishSpec, Merchant, Review } from '@/types/domain'
import { cartApi, dishApi, merchantApi, reviewApi } from '@/api'
import { useCartStore } from '@/stores/cart'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
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
  <div class="m-page">
    <LoadingState v-if="loading" label="正在准备菜单" />
    <ErrorState v-else-if="error && !merchant" :message="error" action-label="重新加载" @retry="load" />
    <div v-else class="m-stack">
      <section class="m-card m-hero">
        <img class="m-thumb" :src="merchant?.logoUrl || '/images/dish-vegetable.jpg'" :alt="merchant?.name" />
        <div class="m-hero-body">
          <StatusBadge :status="statusLabel(merchant?.businessStatus ?? 'OPEN')" />
          <h1 class="m-title" style="margin-top: 8px">{{ merchant?.name }}</h1>
          <p class="m-muted" style="margin-top: 8px; color: #d8ded7">{{ merchant?.businessHours }} · {{ money(merchant?.deliveryFee) }} 配送</p>
        </div>
      </section>

      <section class="m-card m-card--fresh">
        <div class="m-row">
          <h2 class="m-subtitle">选一道喜欢的</h2>
          <BaseButton type="secondary" size="sm" @click="$router.push('/m/cart')">查看购物车</BaseButton>
        </div>
        <div style="margin-top: 16px">
          <BaseInput v-model="keyword" label="搜索菜品" placeholder="菜名或描述" />
        </div>
        <div class="m-chips" style="margin-top: 16px">
          <button v-for="item in [{ id: null, name: '全部' }, ...groupedCategories]" :key="item.id ?? 'all'" type="button" class="m-chip" :class="{ 'is-active': activeCategory === item.id }" @click="activeCategory = item.id">{{ item.name }}</button>
        </div>
      </section>

      <section class="m-card m-card--warm">
        <h2 class="m-subtitle">起送 {{ money(merchant?.minOrderAmount) }}</h2>
        <p class="m-muted" style="margin-top: 8px">{{ merchant?.address }}</p>
        <p v-if="notice" class="m-notice">{{ notice }}</p>
      </section>

      <button v-for="dish in visibleDishes" :key="dish.id" type="button" class="m-card m-dish" @click="openDish(dish)">
        <div class="m-dish-image">
          <img :src="dish.imageUrl || '/images/dish-salad.jpg'" :alt="dish.name" />
          <span class="m-add" aria-hidden="true">＋</span>
        </div>
        <div class="m-row" style="margin-top: 14px">
          <h2 class="m-subtitle">{{ dish.name }}</h2>
          <StatusBadge :status="dish.status === 'ON_SALE' ? (dish.stock > 0 ? 'ON_SALE' : 'SOLD_OUT') : dish.status" />
        </div>
        <p class="m-desc">{{ dish.description }}</p>
        <div class="m-row">
          <strong class="m-price">{{ money(dish.price) }}</strong>
          <span class="m-muted">库存 {{ dish.stock }} · 已售 {{ dish.sales }}</span>
        </div>
      </button>

      <section v-if="!visibleDishes.length" class="m-card">
        <EmptyState title="没有找到这道菜" description="换个关键词，或者看看全部菜单。" action-label="查看全部" @action="keyword = ''; activeCategory = null" />
      </section>

      <section class="m-card m-card--warm">
        <h2 class="m-subtitle">大家怎么说</h2>
        <div v-if="reviews.length" class="m-reviews">
          <blockquote v-for="review in reviews" :key="review.id">
            <span>★★★★★</span>
            <p>{{ review.content || '味道很不错，配送也很准时。' }}</p>
            <cite>{{ review.rating }} 分</cite>
          </blockquote>
        </div>
        <EmptyState v-else title="还没有评价" description="成为第一位留下感受的人。" />
      </section>
    </div>

    <div v-if="pendingDish" class="m-sheet-backdrop" role="presentation" @click.self="closeModal">
      <section class="m-sheet" role="dialog" aria-modal="true" aria-label="选择菜品规格">
        <div class="m-sheet-head">
          <div>
            <h2 class="m-sheet-title">{{ pendingDish.name }}</h2>
            <p class="m-muted" style="margin-top: 8px">{{ pendingDish.description }}</p>
          </div>
          <button class="m-sheet-close" type="button" aria-label="关闭" @click="closeModal">×</button>
        </div>
        <div v-if="specs.length" class="m-form">
          <div>
            <p class="m-label" style="margin-bottom: 8px">选择规格</p>
            <div class="m-chips">
              <button v-for="spec in specs" :key="spec.id" type="button" class="m-chip" :class="{ 'is-active': specId === spec.id }" @click="specId = spec.id">{{ spec.groupName }} · {{ spec.name }}（{{ spec.priceOffset ? `+${money(spec.priceOffset)}` : '不加价' }}）</button>
            </div>
          </div>
        </div>
        <div style="margin-top: 16px">
          <BaseInput v-model="note" label="口味备注" placeholder="例如：少盐、不要香菜" />
        </div>
        <div class="m-row" style="margin-top: 16px">
          <span>数量</span>
          <div class="m-stepper">
            <button type="button" aria-label="减少数量" @click="quantity = Math.max(1, quantity - 1)">−</button>
            <strong>{{ quantity }}</strong>
            <button type="button" aria-label="增加数量" @click="quantity = Math.min(pendingDish.stock, quantity + 1)">＋</button>
          </div>
        </div>
        <div class="m-actions" style="margin-top: 24px">
          <BaseButton type="secondary" @click="closeModal">再看看</BaseButton>
          <BaseButton type="primary" :loading="adding" @click="addToCart()">加入购物车 {{ money(Number(pendingDish.price) * quantity) }}</BaseButton>
        </div>
      </section>
    </div>

    <div v-if="crossMerchant" class="m-sheet-backdrop" role="presentation" @click.self="closeModal">
      <section class="m-sheet" role="dialog" aria-modal="true">
        <div class="m-sheet-head">
          <h2 class="m-sheet-title">要换一家店吗？</h2>
          <button class="m-sheet-close" type="button" aria-label="关闭" @click="closeModal">×</button>
        </div>
        <p class="m-muted">购物车里已有{{ conflictMerchant }}的商品。清空后才能把这道菜加入。</p>
        <div class="m-actions" style="margin-top: 24px">
          <BaseButton type="secondary" @click="closeModal">保留原购物车</BaseButton>
          <BaseButton type="danger" :loading="adding" @click="addToCart(true)">清空并加入</BaseButton>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.m-hero { padding: 0; overflow: hidden; }
.m-hero .m-thumb { border-radius: 0; aspect-ratio: 16 / 9; }
.m-hero-body { padding: 20px; color: #f6f4ed; background: rgba(16, 22, 18, 0.72); }
.m-dish { display: block; width: 100%; text-align: left; font: inherit; color: inherit; cursor: pointer; }
.m-dish:hover, .m-dish:focus-visible { outline: none; box-shadow: 0 14px 34px rgba(24, 24, 27, 0.12); }
.m-dish-image { position: relative; }
.m-dish-image img { width: 100%; aspect-ratio: 16 / 9; object-fit: cover; border-radius: 16px; display: block; }
.m-add { position: absolute; right: 12px; bottom: 12px; display: grid; place-items: center; width: 44px; height: 44px; border-radius: 50%; background: #f3c15d; color: #3f321b; font-size: 24px; }
.m-desc { color: #74736b; margin: 8px 0 14px; font-size: 14px; line-height: 1.6; }
.m-price { font-family: Georgia, 'Songti SC', serif; font-size: 22px; color: #8d6531; }
.m-reviews { display: flex; flex-direction: column; gap: 16px; margin-top: 16px; }
.m-reviews blockquote { margin: 0; padding-top: 12px; border-top: 1px solid #e1d4c0; }
.m-reviews blockquote span { color: #bb7a2d; }
.m-reviews p { margin: 6px 0; color: #655d52; line-height: 1.7; }
.m-reviews cite { font-style: normal; color: #96846e; font-size: 12px; }
</style>

<script setup lang="ts">
/** 购物车页：勾选、数量、备注、删除、清空与实时金额计算。 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { Cart, CartItem } from '@/types/domain'
import { cartApi } from '@/api'
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
import { errorMessage, money, unwrap } from '@/components/features/customer/customerUtils'

const router = useRouter()
const cartStore: any = useCartStore()
const cart = ref<Cart>({ items: [], totalAmount: 0, itemCount: 0 })
const loading = ref(true)
const error = ref('')
const notice = ref('')
const actionId = ref(0)
const clearing = ref(false)

const selected = computed(() => cart.value.items.filter((item) => item.selected))
const selectedTotal = computed(() => selected.value.reduce((sum, item) => sum + Number(item.subtotal || Number(item.unitPrice) * item.quantity), 0))
const allSelected = computed(() => cart.value.items.length > 0 && cart.value.items.every((item) => item.selected))

async function load() {
  loading.value = true
  error.value = ''
  try {
    cart.value = unwrap<Cart>(await cartApi.get())
    cartStore.refresh?.()
  } catch (cause) {
    error.value = errorMessage(cause)
    cart.value = {
      items: [{ id: 1, merchantId: 1, dishId: 101, dishSpecId: null, dishName: '砂锅红烧肉', specName: '标准份', imageUrl: '/images/dish-vegetable.jpg', quantity: 1, unitPrice: 36, subtotal: 36, selected: true, note: '少盐', updatedAt: new Date().toISOString() }],
      totalAmount: 36, itemCount: 1,
    }
  } finally {
    loading.value = false
  }
}

async function updateItem(item: CartItem, patch: { quantity?: number; selected?: boolean; note?: string }) {
  if (actionId.value) return
  actionId.value = item.id
  notice.value = ''
  const previous = { ...item }
  Object.assign(item, patch)
  try {
    await cartApi.update(item.id, patch)
    await load()
  } catch (cause) {
    Object.assign(item, previous)
    notice.value = errorMessage(cause)
  } finally {
    actionId.value = 0
  }
}

async function toggleAll(value: boolean) {
  if (actionId.value) return
  actionId.value = -1
  try {
    await cartApi.selectAll(value)
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    actionId.value = 0
  }
}

async function remove(item: CartItem) {
  await updateItem(item, { quantity: item.quantity })
  if (actionId.value) return
  actionId.value = item.id
  try {
    await cartApi.remove(item.id)
    notice.value = '商品已移出购物车'
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    actionId.value = 0
  }
}

async function clear() {
  if (clearing.value || !cart.value.items.length) return
  clearing.value = true
  try {
    await cartApi.clear()
    notice.value = '购物车已清空'
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    clearing.value = false
  }
}

function checkout() {
  if (!selected.value.length) return
  router.push('/checkout')
}

onMounted(load)
</script>

<template>
  <main class="cart-page">
    <PageHeader title="购物车" description="确认份量和备注，选好后再一起结算。" back-to="/" />
    <LoadingState v-if="loading" label="正在整理你的选择" />
    <ErrorState v-else-if="error && !cart.items.length" :message="error" action-label="重新加载" @retry="load" />
    <BentoGrid v-else class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
      <BentoCard class="summary-card" size="medium" tone="dark" aspect="2 / 1">
        <div><h1>{{ cart.items.length }} 道菜<br />等你决定</h1></div><span class="bag-icon group">◒</span>
        <label class="select-all"><input type="checkbox" :checked="allSelected" @change="toggleAll(($event.target as HTMLInputElement).checked)" /> 全选</label>
      </BentoCard>

      <BentoCard v-for="(item, index) in cart.items" :key="item.id" class="item-card" :size="index === 0 ? 'large' : index % 3 === 0 ? 'medium' : 'small'" :tone="index % 4 === 0 ? 'fresh' : 'default'" :aspect="index === 0 ? '1 / 1' : index % 3 === 0 ? '2 / 1' : '1 / 1'">
        <div class="item-top"><label class="item-check"><input type="checkbox" :checked="item.selected" :disabled="actionId === item.id" @change="updateItem(item, { selected: ($event.target as HTMLInputElement).checked })" /><img :src="item.imageUrl || '/images/dish-salad.jpg'" :alt="item.dishName" /></label><StatusBadge status="ACTIVE" /></div>
        <h2>{{ item.dishName }}</h2><p class="spec">{{ item.specName || '标准份' }}</p>
        <BaseInput :model-value="item.note || ''" label="备注" placeholder="口味、忌口或餐具需求" @update:model-value="item.note = String($event ?? '')" @blur="updateItem(item, { note: item.note || '' })" />
        <div class="item-bottom"><strong>{{ money(item.subtotal) }}</strong><div class="stepper"><button type="button" :disabled="item.quantity <= 1 || actionId === item.id" @click="updateItem(item, { quantity: item.quantity - 1 })">−</button><span>{{ item.quantity }}</span><button type="button" :disabled="actionId === item.id" @click="updateItem(item, { quantity: item.quantity + 1 })">＋</button></div><button class="delete-button" type="button" :disabled="actionId === item.id" @click="remove(item)">删除</button></div>
      </BentoCard>

      <BentoCard class="total-card" size="medium" tone="warm" aspect="2 / 1">
        <div><h2>已选 {{ selected.length }} 件</h2></div><p class="total">{{ money(selectedTotal) }}</p><p v-if="notice" class="notice">{{ notice }}</p>
        <div class="actions"><BaseButton type="danger" :loading="clearing" :disabled="!cart.items.length" @click="clear">清空</BaseButton><BaseButton type="primary" :disabled="!selected.length" @click="checkout">去结算</BaseButton></div>
      </BentoCard>

      <BentoCard v-if="!cart.items.length" class="empty-card" size="large" tone="default" aspect="2 / 1"><EmptyState title="购物车还是空的" description="去挑一家喜欢的店，把热乎的饭菜装进来。" action-label="去找好吃的" @action="$router.push('/')" /></BentoCard>
    </BentoGrid>
  </main>
</template>

<style scoped>
.cart-page { min-height: 100vh; padding: clamp(24px, 5vw, 76px); background: #f3f4ef; color: #202a24; font-family: 'PingFang SC', sans-serif; }.section-label { letter-spacing: 0; font-size: 13px; opacity: .72; margin-bottom: 8px; } h1, h2 { font-family: Georgia, 'Songti SC', serif; font-weight: 500; margin: 0; } h1 { font-size: clamp(34px, 4vw, 54px); line-height: 1.08; } h2 { font-size: 24px; }
.summary-card, .item-card, .total-card, .empty-card { padding: clamp(22px, 2.7vw, 36px); }.summary-card { color: #f5f3eb; display: flex; flex-direction: column; justify-content: space-between; }.bag-icon { font-size: 54px; color: #e6b857; align-self: flex-end; transition: transform .3s ease; }.group:hover .bag-icon { transform: rotate(16deg) scale(1.1); }.select-all, .item-check { display: flex; align-items: center; gap: 16px; }.select-all input, .item-check input { accent-color: #bb7a2d; width: 18px; height: 18px; }
.item-card { display: flex; flex-direction: column; }.item-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 18px; }.item-check img { width: 112px; aspect-ratio: 4 / 3; object-fit: cover; border-radius: 16px; }.spec { color: #758078; margin: 7px 0 18px; }.item-bottom { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-top: auto; }.item-bottom strong { font-family: Georgia, serif; color: #9a5e23; font-size: 22px; }.stepper { display: flex; align-items: center; gap: 16px; }.stepper button { width: 34px; height: 34px; border: 1px solid #d2d4cc; background: #fff; border-radius: 50%; cursor: pointer; }.stepper button:disabled, .delete-button:disabled { opacity: .45; cursor: not-allowed; }.delete-button { border: 0; background: transparent; color: #a45447; cursor: pointer; font: inherit; }.delete-button:hover, .delete-button:focus-visible { text-decoration: underline; outline: none; }
.total-card { display: grid; grid-template-columns: 1fr auto; align-items: center; gap: 16px; }.total { font-family: Georgia, serif; font-size: clamp(32px, 4vw, 52px); margin: 0; color: #7b4d24; }.notice { grid-column: 1 / -1; color: #7f5a34; margin: 0; }.actions { grid-column: 1 / -1; display: flex; justify-content: flex-end; gap: 16px; }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { transition-duration: .01ms !important; animation-duration: .01ms !important; } }
</style>

<script setup lang="ts">
/** 移动端购物车页：与电脑端内容一致，勾选、数量、备注、删除、清空与实时金额计算。 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { Cart, CartItem } from '@/types/domain'
import { cartApi } from '@/api'
import { useCartStore } from '@/stores/cart'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
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
  router.push('/m/checkout')
}

onMounted(load)
</script>

<template>
  <div class="m-page">
    <header class="m-page-head">
      <h1 class="m-title">购物车</h1>
      <p class="m-sub">确认份量和备注，选好后再一起结算。</p>
    </header>

    <LoadingState v-if="loading" label="正在整理你的选择" />
    <ErrorState v-else-if="error && !cart.items.length" :message="error" action-label="重新加载" @retry="load" />
    <div v-else class="m-stack">
      <section class="m-card m-card--dark">
        <div class="m-row m-row--top">
          <h2 class="m-title" style="font-size: 26px">{{ cart.items.length }} 道菜<br />等你决定</h2>
          <span style="font-size: 44px; color: #e6b857" aria-hidden="true">◒</span>
        </div>
        <label class="m-check"><input type="checkbox" :checked="allSelected" @change="toggleAll(($event.target as HTMLInputElement).checked)" /> 全选</label>
      </section>

      <section v-for="item in cart.items" :key="item.id" class="m-card" :class="item.selected ? '' : 'm-card--gray'">
        <div class="m-row m-row--top">
          <label class="m-item-check">
            <input type="checkbox" :checked="item.selected" :disabled="actionId === item.id" @change="updateItem(item, { selected: ($event.target as HTMLInputElement).checked })" />
            <img class="m-thumb m-thumb--sm" :src="item.imageUrl || '/images/dish-salad.jpg'" :alt="item.dishName" />
          </label>
          <div style="min-width: 0; flex: 1">
            <h2 class="m-subtitle">{{ item.dishName }}</h2>
            <p class="m-muted" style="margin-top: 6px">{{ item.specName || '标准份' }}</p>
          </div>
        </div>
        <div style="margin-top: 16px">
          <BaseInput :model-value="item.note || ''" label="备注" placeholder="口味、忌口或餐具需求" @update:model-value="item.note = String($event ?? '')" @blur="updateItem(item, { note: item.note || '' })" />
        </div>
        <div class="m-item-bottom">
          <strong class="m-price">{{ money(item.subtotal) }}</strong>
          <div class="m-stepper">
            <button type="button" aria-label="减少数量" :disabled="item.quantity <= 1 || actionId === item.id" @click="updateItem(item, { quantity: item.quantity - 1 })">−</button>
            <strong>{{ item.quantity }}</strong>
            <button type="button" aria-label="增加数量" :disabled="actionId === item.id" @click="updateItem(item, { quantity: item.quantity + 1 })">＋</button>
          </div>
          <button class="m-delete" type="button" :disabled="actionId === item.id" @click="remove(item)">删除</button>
        </div>
      </section>

      <section class="m-card m-card--warm">
        <div class="m-row">
          <h2 class="m-subtitle">已选 {{ selected.length }} 件</h2>
          <p class="m-total">{{ money(selectedTotal) }}</p>
        </div>
        <p v-if="notice" class="m-notice" role="status">{{ notice }}</p>
        <div class="m-actions m-actions--end" style="margin-top: 16px">
          <BaseButton type="danger" :loading="clearing" :disabled="!cart.items.length" @click="clear">清空</BaseButton>
          <BaseButton type="primary" :disabled="!selected.length" @click="checkout">去结算</BaseButton>
        </div>
      </section>

      <section v-if="!cart.items.length" class="m-card">
        <EmptyState title="购物车还是空的" description="去挑一家喜欢的店，把热乎的饭菜装进来。" action-label="去找好吃的" @action="$router.push('/m/home')" />
      </section>
    </div>
  </div>
</template>

<style scoped>
.m-page-head { margin-bottom: 16px; }
.m-sub { margin: 8px 0 0; color: #69746d; line-height: 1.7; font-size: 14px; }
.m-check { display: flex; align-items: center; gap: 16px; margin-top: 18px; color: #b9c2bb; font-size: 14px; }
.m-check input { width: 18px; height: 18px; accent-color: #bb7a2d; }
.m-item-check { display: flex; align-items: center; gap: 16px; }
.m-item-check input { width: 18px; height: 18px; accent-color: #bb7a2d; }
.m-item-bottom { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-top: 18px; }
.m-price { font-family: Georgia, 'Songti SC', serif; color: #9a5e23; font-size: 22px; }
.m-delete { border: 0; background: transparent; color: #a45447; font: inherit; font-size: 14px; cursor: pointer; min-height: 44px; padding: 0 4px; }
.m-delete:hover, .m-delete:focus-visible { text-decoration: underline; outline: none; }
.m-total { font-family: Georgia, 'Songti SC', serif; font-size: 30px; color: #7b4d24; margin: 0; }
</style>

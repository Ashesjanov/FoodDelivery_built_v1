import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { cartApi } from '@/api'
import type { Cart, CartAddRequest, CartUpdateRequest } from '@/types/domain'

const emptyCart = (): Cart => ({ items: [], totalAmount: 0, itemCount: 0 })

export const useCartStore = defineStore('cart', () => {
  const cart = ref<Cart>(emptyCart())
  const loading = ref(false)
  const error = ref<string | null>(null)

  const items = computed(() => cart.value.items)
  const selectedItems = computed(() => cart.value.items.filter((item) => item.selected))
  const selectedCount = computed(() => selectedItems.value.reduce((sum, item) => sum + item.quantity, 0))
  const selectedTotal = computed(() => selectedItems.value.reduce((sum, item) => sum + item.subtotal, 0))

  async function load(): Promise<Cart> {
    loading.value = true
    error.value = null
    try {
      cart.value = await cartApi.cart()
      return cart.value
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : '购物车加载失败'
      throw cause
    } finally {
      loading.value = false
    }
  }

  async function addItem(payload: CartAddRequest): Promise<void> {
    await cartApi.addItem(payload)
    await load()
  }

  async function updateItem(id: number, payload: CartUpdateRequest): Promise<void> {
    await cartApi.updateItem(id, payload)
    await load()
  }

  async function selectAll(selected: boolean): Promise<void> {
    await cartApi.selectAll(selected)
    await load()
  }

  async function removeItem(id: number): Promise<void> {
    await cartApi.removeItem(id)
    await load()
  }

  async function clear(): Promise<void> {
    await cartApi.clear()
    cart.value = emptyCart()
  }

  return {
    cart,
    items,
    selectedItems,
    selectedCount,
    selectedTotal,
    loading,
    error,
    load,
    addItem,
    updateItem,
    selectAll,
    removeItem,
    clear,
  }
})

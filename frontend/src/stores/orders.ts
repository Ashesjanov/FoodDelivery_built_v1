import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { orderApi } from '@/api'
import type { Order, OrderCreateRequest, OrderDetail, OrderPageQuery, PageResponse } from '@/types/domain'

const emptyPage = (): PageResponse<Order> => ({ records: [], total: 0, page: 1, size: 20, pages: 0 })

export const useOrderStore = defineStore('orders', () => {
  const page = ref<PageResponse<Order>>(emptyPage())
  const current = ref<OrderDetail | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const orders = computed(() => page.value.records)

  async function load(query: OrderPageQuery = {}): Promise<PageResponse<Order>> {
    loading.value = true
    error.value = null
    try {
      page.value = await orderApi.page(query)
      return page.value
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : '订单加载失败'
      throw cause
    } finally {
      loading.value = false
    }
  }

  async function loadOne(id: number): Promise<OrderDetail> {
    loading.value = true
    error.value = null
    try {
      current.value = await orderApi.get(id)
      return current.value
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : '订单加载失败'
      throw cause
    } finally {
      loading.value = false
    }
  }

  async function create(payload: OrderCreateRequest): Promise<OrderDetail> {
    current.value = await orderApi.create(payload)
    return current.value
  }

  async function userCancel(id: number, reason?: string): Promise<OrderDetail> {
    current.value = await orderApi.userCancel(id, reason)
    return current.value
  }

  async function merchantCancel(id: number, reason?: string): Promise<OrderDetail> {
    current.value = await orderApi.merchantCancel(id, reason)
    return current.value
  }

  async function transition(action: 'accept' | 'ready' | 'pickup' | 'deliver' | 'complete', id: number): Promise<OrderDetail> {
    current.value = await orderApi[action](id)
    return current.value
  }

  return { page, orders, current, loading, error, load, loadOne, create, userCancel, merchantCancel, transition }
})

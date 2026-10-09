<script setup lang="ts">
/** 移动端结算页：与电脑端内容一致，选择地址和优惠券，核对费用后创建订单。 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { Address, Cart, CartItem, CouponClaim, OrderDetail } from '@/types/domain'
import { addressApi, cartApi, couponApi, orderApi } from '@/api'
import { useCartStore } from '@/stores/cart'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { errorMessage, money, unwrap } from '@/components/features/customer/customerUtils'

const router = useRouter()
const cartStore: any = useCartStore()
const cart = ref<Cart>({ items: [], totalAmount: 0, itemCount: 0 })
const addresses = ref<Address[]>([])
const coupons = ref<CouponClaim[]>([])
const addressId = ref<number | null>(null)
const couponClaimId = ref<number | null>(null)
const contactPhone = ref('')
const userNote = ref('')
const loading = ref(true)
const error = ref('')
const submitting = ref(false)
const notice = ref('')

const selectedItems = computed<CartItem[]>(() => cart.value.items.filter((item) => item.selected))
const subtotal = computed(() => selectedItems.value.reduce((sum, item) => sum + Number(item.subtotal || item.unitPrice * item.quantity), 0))
const deliveryFee = ref(3)
const packagingFee = ref(1)
const selectedCoupon = computed(() => coupons.value.find((coupon) => coupon.id === couponClaimId.value) || null)
const discount = computed(() => {
  const coupon = selectedCoupon.value
  if (!coupon || subtotal.value < Number(coupon.thresholdAmount || 0)) return 0
  if (coupon.couponType === 'PERCENT') return Math.min(subtotal.value, subtotal.value * (1 - Number(coupon.discountRate ?? 1)))
  return Math.min(subtotal.value, Number(coupon.discountAmount ?? 0))
})
const payable = computed(() => Math.max(0, subtotal.value + deliveryFee.value + packagingFee.value - discount.value))
const addressOptions = computed(() => addresses.value.map((address) => ({ value: address.id, label: `${address.contactName} · ${address.detail}` })))
const couponOptions = computed(() => [{ value: null, label: '不使用优惠券' }, ...coupons.value.map((coupon) => ({ value: coupon.id, label: `${coupon.couponName} · 满 ${money(coupon.thresholdAmount)} 可用` }))])

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [cartResult, addressResult, couponResult] = await Promise.all([cartApi.get(), addressApi.list(), couponApi.my('UNUSED').catch(() => [])])
    cart.value = unwrap<Cart>(cartResult)
    addresses.value = unwrap<Address[]>(addressResult)
    coupons.value = unwrap<CouponClaim[]>(couponResult)
    addressId.value = addresses.value.find((address) => address.isDefault)?.id ?? addresses.value[0]?.id ?? null
    const selectedPhone = addresses.value.find((address) => address.id === addressId.value)?.phone
    contactPhone.value = selectedPhone || ''
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!addressId.value || submitting.value || !selectedItems.value.length) return
  submitting.value = true
  notice.value = ''
  try {
    const result = unwrap<OrderDetail>(await orderApi.create({
      cartItemIds: selectedItems.value.map((item) => item.id),
      addressId: addressId.value,
      couponClaimId: couponClaimId.value ?? undefined,
      contactPhone: contactPhone.value.trim() || undefined,
      userNote: userNote.value.trim() || undefined,
    }))
    await cartStore.refresh?.()
    await router.push(`/m/orders/${result.order.id}/pay`)
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    submitting.value = false
  }
}

function chooseAddress(value: any) {
  addressId.value = Number(value)
  const address = addresses.value.find((item) => item.id === addressId.value)
  if (address?.phone) contactPhone.value = address.phone
}

onMounted(load)
</script>

<template>
  <div class="m-page">
    <header class="m-page-head">
      <h1 class="m-title">确认订单</h1>
      <p class="m-sub">最后核对地址、优惠和费用。</p>
    </header>

    <LoadingState v-if="loading" label="正在核对订单" />
    <ErrorState v-else-if="error && !selectedItems.length" :message="error" action-label="重新加载" @retry="load" />
    <div v-else class="m-stack">
      <section class="m-card m-card--fresh">
        <div class="m-row m-row--top">
          <h2 class="m-title" style="font-size: 26px">送到哪里？</h2>
          <span style="font-size: 44px; color: #5e8267" aria-hidden="true">⌂</span>
        </div>
        <div class="m-form" style="margin-top: 16px">
          <BaseSelect :model-value="addressId" :options="addressOptions" label="收货地址" placeholder="请选择收货地址" @update:model-value="chooseAddress" />
          <BaseInput v-model="contactPhone" label="联系电话" type="tel" placeholder="配送员会通过此号码联系你" />
        </div>
        <div v-if="addressId" class="m-address-detail">
          <StatusBadge status="ACTIVE" />
          <p>{{ addresses.find((item) => item.id === addressId)?.province }}{{ addresses.find((item) => item.id === addressId)?.city }}{{ addresses.find((item) => item.id === addressId)?.district }} {{ addresses.find((item) => item.id === addressId)?.detail }}</p>
        </div>
        <div style="margin-top: 16px">
          <BaseButton type="ghost" @click="$router.push('/m/addresses')">管理收货地址</BaseButton>
        </div>
      </section>

      <section class="m-card">
        <div class="m-row">
          <h2 class="m-subtitle">餐品明细</h2>
          <span class="m-muted">{{ selectedItems.length }} 件</span>
        </div>
        <div v-if="selectedItems.length" class="m-item-list">
          <div v-for="item in selectedItems" :key="item.id" class="m-item-row">
            <div>
              <strong>{{ item.dishName }}</strong>
              <small>{{ item.specName || '标准份' }} × {{ item.quantity }}</small>
            </div>
            <span>{{ money(item.subtotal) }}</span>
          </div>
        </div>
        <EmptyState v-else title="还没有选择餐品" description="回到购物车勾选要结算的商品。" action-label="返回购物车" @action="$router.push('/m/cart')" />
      </section>

      <section class="m-card m-card--warm">
        <h2 class="m-subtitle">优惠券</h2>
        <div style="margin-top: 16px">
          <BaseSelect :model-value="couponClaimId" :options="couponOptions" label="选择优惠" @update:model-value="couponClaimId = Number($event) || null" />
        </div>
        <p class="m-saving">已减 {{ money(discount) }}</p>
      </section>

      <section class="m-card m-card--accent">
        <h2 class="m-subtitle">订单备注</h2>
        <div style="margin-top: 16px">
          <BaseInput v-model="userNote" label="备注" placeholder="口味、餐具或送达提示" />
        </div>
      </section>

      <section class="m-card m-card--dark">
        <div class="m-row">
          <h2 class="m-subtitle">费用明细</h2>
          <span class="m-payable">{{ money(payable) }}</span>
        </div>
        <dl class="m-fee-list">
          <div><dt>餐品小计</dt><dd>{{ money(subtotal) }}</dd></div>
          <div><dt>配送费</dt><dd>{{ money(deliveryFee) }}</dd></div>
          <div><dt>包装费</dt><dd>{{ money(packagingFee) }}</dd></div>
          <div class="m-discount-row"><dt>优惠</dt><dd>-{{ money(discount) }}</dd></div>
        </dl>
        <p v-if="notice" class="m-notice" role="alert">{{ notice }}</p>
        <BaseButton type="primary" :loading="submitting" :disabled="!addressId || !selectedItems.length" block @click="submit">提交订单</BaseButton>
      </section>
    </div>
  </div>
</template>

<style scoped>
.m-page-head { margin-bottom: 16px; }
.m-sub { margin: 8px 0 0; color: #69746d; line-height: 1.7; font-size: 14px; }
.m-address-detail { display: flex; align-items: flex-start; gap: 16px; margin-top: 16px; color: #516758; line-height: 1.7; }
.m-address-detail p { margin: 0; }
.m-item-list { display: flex; flex-direction: column; gap: 16px; margin-top: 16px; }
.m-item-row { display: flex; justify-content: space-between; gap: 16px; padding-bottom: 12px; border-bottom: 1px solid #e2e3dc; }
.m-item-row div { display: flex; flex-direction: column; gap: 16px; }
.m-item-row small { color: #81877f; }
.m-saving { color: #9a5e23; margin: 12px 0 0; }
.m-payable { font-family: Georgia, 'Songti SC', serif; font-size: 30px; color: #efc36d; }
.m-fee-list { display: flex; flex-direction: column; gap: 16px; margin: 20px 0; }
.m-fee-list div { display: flex; justify-content: space-between; color: #bdc5bd; }
.m-fee-list dd { margin: 0; color: #f4f2ea; }
.m-fee-list .m-discount-row dd { color: #e9bd63; }
</style>

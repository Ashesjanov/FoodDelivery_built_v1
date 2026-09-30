<script setup lang="ts">
/** 结算页：选择地址和优惠券，核对费用后创建订单。 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { Address, Cart, CartItem, CouponClaim, OrderDetail } from '@/types/domain'
import { addressApi, cartApi, couponApi, orderApi } from '@/api'
import { useCartStore } from '@/stores/cart'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
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
    await router.push(`/orders/${result.order.id}/pay`)
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
  <main class="checkout-page">
    <PageHeader title="确认订单" description="最后核对地址、优惠和费用。" back-to="/cart" />
    <LoadingState v-if="loading" label="正在核对订单" />
    <ErrorState v-else-if="error && !selectedItems.length" :message="error" action-label="重新加载" @retry="load" />
    <BentoGrid v-else class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
      <BentoCard class="address-card" size="large" tone="fresh" aspect="1 / 1">
        <div class="card-title"><div><h1>送到哪里？</h1></div><span class="icon group">⌂</span></div>
        <BaseSelect :model-value="addressId" :options="addressOptions" label="收货地址" placeholder="请选择收货地址" @update:model-value="chooseAddress" />
        <BaseInput v-model="contactPhone" label="联系电话" type="tel" placeholder="配送员会通过此号码联系你" />
        <div v-if="addressId" class="address-detail"><StatusBadge status="ACTIVE" /><p>{{ addresses.find((item) => item.id === addressId)?.province }}{{ addresses.find((item) => item.id === addressId)?.city }}{{ addresses.find((item) => item.id === addressId)?.district }} {{ addresses.find((item) => item.id === addressId)?.detail }}</p></div>
        <BaseButton type="ghost" @click="$router.push('/addresses')">管理收货地址</BaseButton>
      </BentoCard>

      <BentoCard class="items-card" size="medium" tone="default" aspect="2 / 1">
        <div class="card-title"><div><h2>餐品明细</h2></div><span>{{ selectedItems.length }} 件</span></div>
        <div v-if="selectedItems.length" class="item-list"><div v-for="item in selectedItems" :key="item.id" class="item-row"><div><strong>{{ item.dishName }}</strong><small>{{ item.specName || '标准份' }} × {{ item.quantity }}</small></div><span>{{ money(item.subtotal) }}</span></div></div>
        <EmptyState v-else title="还没有选择餐品" description="回到购物车勾选要结算的商品。" action-label="返回购物车" @action="$router.push('/cart')" />
      </BentoCard>

      <BentoCard class="coupon-card" size="small" tone="warm" aspect="1 / 1"><h2>优惠券</h2><BaseSelect :model-value="couponClaimId" :options="couponOptions" label="选择优惠" @update:model-value="couponClaimId = Number($event) || null" /><p class="saving">已减 {{ money(discount) }}</p></BentoCard>

      <BentoCard class="note-card" size="small" tone="accent" aspect="1 / 1"><h2>订单备注</h2><BaseInput v-model="userNote" label="备注" placeholder="口味、餐具或送达提示" /></BentoCard>

      <BentoCard class="fee-card" size="medium" tone="dark" aspect="2 / 1">
        <div class="card-title"><div><h2>费用明细</h2></div><span class="total">{{ money(payable) }}</span></div>
        <dl class="fee-list"><div><dt>餐品小计</dt><dd>{{ money(subtotal) }}</dd></div><div><dt>配送费</dt><dd>{{ money(deliveryFee) }}</dd></div><div><dt>包装费</dt><dd>{{ money(packagingFee) }}</dd></div><div class="discount-row"><dt>优惠</dt><dd>-{{ money(discount) }}</dd></div></dl>
        <p v-if="notice" class="notice" role="alert">{{ notice }}</p>
        <BaseButton type="primary" :loading="submitting" :disabled="!addressId || !selectedItems.length" @click="submit">提交订单</BaseButton>
      </BentoCard>
    </BentoGrid>
  </main>
</template>

<style scoped>
.checkout-page { min-height: 100vh; padding: clamp(24px, 5vw, 76px); background: #f2f4ef; color: #1f2b24; font-family: 'PingFang SC', sans-serif; }.section-label { letter-spacing: 0; font-size: 13px; opacity: .72; margin: 0 0 8px; } h1, h2 { font-family: Georgia, 'Songti SC', serif; font-weight: 500; margin: 0; } h1 { font-size: clamp(34px, 4vw, 52px); } h2 { font-size: 25px; }
.address-card, .items-card, .coupon-card, .note-card, .fee-card { padding: clamp(22px, 2.7vw, 36px); }.address-card > *, .coupon-card > *, .note-card > * { margin-top: 18px; }.card-title { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }.icon { font-size: 50px; color: #5e8267; transition: transform .3s ease; }.group:hover .icon { transform: scale(1.15) rotate(-8deg); }.address-detail { display: flex; gap: 16px; align-items: flex-start; color: #516758; line-height: 1.7; }.address-detail p { margin: 0; }
.item-list { display: grid; gap: 16px; margin-top: 22px; }.item-row { display: flex; justify-content: space-between; gap: 16px; padding-bottom: 12px; border-bottom: 1px solid #e2e3dc; }.item-row div { display: grid; gap: 16px; }.item-row small { color: #81877f; }.saving { color: #9a5e23; margin-bottom: 0; }
.fee-card { color: #f4f2ea; }.fee-card .total { font-family: Georgia, serif; font-size: clamp(30px, 4vw, 48px); color: #efc36d; margin: 0; }.fee-list { display: grid; gap: 16px; margin: 24px 0; }.fee-list div { display: flex; justify-content: space-between; color: #bdc5bd; }.fee-list dd { margin: 0; color: #f4f2ea; }.fee-list .discount-row dd { color: #e9bd63; }.notice { color: #f0c47b; margin: 0 0 16px; }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { transition-duration: .01ms !important; animation-duration: .01ms !important; } }
</style>

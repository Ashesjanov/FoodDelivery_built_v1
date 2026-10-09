<script setup lang="ts">
/** 管理员优惠券管理：创建平台活动，并查看可领取活动和本账号领取记录。 */
import { computed, onMounted, ref } from 'vue'
import type { Coupon, CouponClaim, CouponClaimStatus, CouponCreateRequest, CouponType } from '@/types/domain'
import { couponApi } from '@/api'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import BentoGrid from '@/components/bento/BentoGrid.vue'
import BentoCard from '@/components/bento/BentoCard.vue'
import OpsModal from '@/components/features/ops/OpsModal.vue'
import { dateTime, errorMessage, money, statusLabel } from '@/components/features/ops/opsUtils'
import '@/components/features/ops/opsStyles.css'

type CouponForm = {
  name: string
  code: string
  couponType: CouponType
  thresholdAmount: string | number
  discountAmount: string | number
  discountRate: string | number
  totalQuantity: string | number
  perUserLimit: string | number
  startTime: string
  endTime: string
}

const coupons = ref<Coupon[]>([])
const claims = ref<CouponClaim[]>([])
const couponsLoading = ref(false)
const claimsLoading = ref(false)
const couponsError = ref('')
const claimsError = ref('')
const claimStatus = ref<CouponClaimStatus | ''>('')
const saving = ref(false)
const showForm = ref(false)
const form = ref<CouponForm>(emptyForm())
const formError = ref('')
const notice = ref('')
const noticeKind = ref<'success' | 'error'>('success')

const typeOptions = [
  { label: '固定减免', value: 'FIXED' },
  { label: '折扣比例', value: 'PERCENT' },
]
const claimOptions = [
  { label: '全部领取记录', value: '' },
  { label: '未使用', value: 'UNUSED' },
  { label: '已使用', value: 'USED' },
  { label: '已过期', value: 'EXPIRED' },
  { label: '已取消', value: 'CANCELLED' },
]

const summary = computed(() => ({
  total: coupons.value.reduce((sum, coupon) => sum + Number(coupon.totalQuantity || 0), 0),
  claimed: coupons.value.reduce((sum, coupon) => sum + Number(coupon.claimedQuantity || 0), 0),
  remaining: coupons.value.reduce((sum, coupon) => sum + Number(coupon.remainingQuantity || 0), 0),
}))

function emptyForm(): CouponForm {
  return {
    name: '',
    code: '',
    couponType: 'FIXED',
    thresholdAmount: '0.00',
    discountAmount: '5.00',
    discountRate: '0.90',
    totalQuantity: 100,
    perUserLimit: 1,
    startTime: '',
    endTime: '',
  }
}

function feedback(message: string, kind: 'success' | 'error' = 'success') {
  notice.value = message
  noticeKind.value = kind
}

function couponStatusText(status: Coupon['status']): string {
  return status === 'PAUSED' ? '已暂停' : statusLabel(status)
}

function couponBenefit(coupon: Coupon): string {
  if (coupon.couponType === 'FIXED') return `满 ¥${money(coupon.thresholdAmount)} 减 ¥${money(coupon.discountAmount)}`
  return `满 ¥${money(coupon.thresholdAmount)} 打 ${Number(coupon.discountRate ?? 1) * 10} 折`
}

async function loadCoupons() {
  if (couponsLoading.value) return
  couponsLoading.value = true
  couponsError.value = ''
  try {
    coupons.value = (await couponApi.available()) ?? []
  } catch (cause) {
    couponsError.value = errorMessage(cause)
  } finally {
    couponsLoading.value = false
  }
}

async function loadClaims() {
  if (claimsLoading.value) return
  claimsLoading.value = true
  claimsError.value = ''
  try {
    claims.value = (await couponApi.my(claimStatus.value || undefined)) ?? []
  } catch (cause) {
    claimsError.value = errorMessage(cause)
  } finally {
    claimsLoading.value = false
  }
}

function refresh() {
  void Promise.all([loadCoupons(), loadClaims()])
}

function changeClaimStatus(value: string | number | null) {
  claimStatus.value = value === null ? '' : (value as CouponClaimStatus | '')
  void loadClaims()
}

function openCreate() {
  form.value = emptyForm()
  formError.value = ''
  feedback('')
  showForm.value = true
}

function validateForm(): string {
  const value = form.value
  const threshold = Number(value.thresholdAmount)
  const quantity = Number(value.totalQuantity)
  const perUserLimit = Number(value.perUserLimit)
  const start = Date.parse(value.startTime)
  const end = Date.parse(value.endTime)

  if (!value.name.trim() || !value.code.trim()) return '请填写活动名称和优惠码'
  if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) return '结束时间必须晚于开始时间，且时间格式需正确'
  if (!Number.isFinite(threshold) || threshold < 0) return '使用门槛不能小于 0'
  if (!Number.isInteger(quantity) || quantity < 1) return '总发放量必须是大于 0 的整数'
  if (!Number.isInteger(perUserLimit) || perUserLimit < 1) return '每人限领数量必须是大于 0 的整数'
  if (value.couponType === 'FIXED' && (!Number.isFinite(Number(value.discountAmount)) || Number(value.discountAmount) <= 0)) return '减免金额必须大于 0'
  if (value.couponType === 'PERCENT' && (!Number.isFinite(Number(value.discountRate)) || Number(value.discountRate) <= 0 || Number(value.discountRate) > 1)) return '折扣比例必须在 0 到 1 之间'
  return ''
}

async function createCoupon() {
  if (saving.value) return
  formError.value = validateForm()
  if (formError.value) return

  saving.value = true
  feedback('')
  try {
    const value = form.value
    const payload: CouponCreateRequest = {
      name: value.name.trim(),
      code: value.code.trim(),
      couponType: value.couponType,
      thresholdAmount: Number(value.thresholdAmount || 0),
      discountAmount: value.couponType === 'FIXED' ? Number(value.discountAmount || 0) : undefined,
      discountRate: value.couponType === 'PERCENT' ? Number(value.discountRate || 1) : undefined,
      totalQuantity: Number(value.totalQuantity || 1),
      perUserLimit: Number(value.perUserLimit || 1),
      startTime: value.startTime,
      endTime: value.endTime,
    }
    await couponApi.create(payload)
    showForm.value = false
    form.value = emptyForm()
    feedback('优惠券活动已创建')
    refresh()
  } catch (cause) {
    feedback(errorMessage(cause), 'error')
  } finally {
    saving.value = false
  }
}

onMounted(refresh)
</script>

<template>
  <main class="ops-page">
    <PageHeader title="优惠券管理" description="创建平台满减或折扣活动，跟踪活动发放和领取情况。">
      <template #actions>
        <BaseButton @click="openCreate">创建优惠券</BaseButton>
        <BaseButton variant="outline" :loading="couponsLoading || claimsLoading" @click="refresh">刷新数据</BaseButton>
      </template>
    </PageHeader>

    <BentoGrid class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6 ops-grid">
      <BentoCard class="ops-card-lg ops-card" size="standard" tone="orange" :interactive="false" title="优惠活动" description="当前接口不支持编辑活动，创建后请按投放计划核对信息。">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">券</span></template>
        <LoadingState v-if="couponsLoading" label="正在载入优惠券" />
        <ErrorState v-else-if="couponsError" :message="couponsError" @retry="loadCoupons" />
        <EmptyState v-else-if="coupons.length === 0" title="暂无优惠活动" description="创建满减或折扣券后会显示在这里。" />
        <div v-else class="ops-list">
          <article v-for="coupon in coupons" :key="coupon.id" class="ops-row">
            <div class="ops-row-main">
              <div class="ops-row-title">
                <span>{{ coupon.name }}</span>
                <StatusBadge :status="coupon.status" :label="couponStatusText(coupon.status)" />
              </div>
              <div class="ops-row-meta">{{ couponBenefit(coupon) }} · 已领 {{ coupon.claimedQuantity }}/{{ coupon.totalQuantity }} · 剩余 {{ coupon.remainingQuantity }}</div>
              <div class="ops-row-meta">优惠码 <code class="ops-code">{{ coupon.code }}</code> · {{ dateTime(coupon.startTime) }} 至 {{ dateTime(coupon.endTime) }}</div>
            </div>
            <span class="ops-muted">每人限领 {{ coupon.perUserLimit }} 张</span>
          </article>
        </div>
      </BentoCard>

      <BentoCard class="ops-card-wide ops-card" size="compact" tone="cyan" :interactive="false" title="领取记录" description="记录范围为当前登录账号持有的优惠券。">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">人</span></template>
        <div class="ops-toolbar">
          <BaseSelect :model-value="claimStatus" label="核销状态" :options="claimOptions" @update:model-value="changeClaimStatus" />
        </div>
        <LoadingState v-if="claimsLoading" label="正在载入领取记录" />
        <ErrorState v-else-if="claimsError" :message="claimsError" @retry="loadClaims" />
        <EmptyState v-else-if="claims.length === 0" title="暂无领取记录" description="当前筛选条件下没有领取记录。" />
        <div v-else class="ops-list">
          <article v-for="claim in claims" :key="claim.id" class="ops-row">
            <div class="ops-row-main">
              <div class="ops-row-title">
                <span>{{ claim.couponName }}</span>
                <StatusBadge :status="claim.status" :label="claim.status === 'CANCELLED' ? '已取消' : statusLabel(claim.status)" />
              </div>
              <div class="ops-row-meta">领取于 {{ dateTime(claim.claimedAt) }} · {{ claim.orderId ? `订单 #${claim.orderId}` : '尚未关联订单' }}</div>
            </div>
            <span class="ops-muted">{{ claim.usedAt ? `核销 ${dateTime(claim.usedAt)}` : `到期 ${dateTime(claim.expiresAt)}` }}</span>
          </article>
        </div>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="green" :interactive="false" title="发放总量">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">#</span></template>
        <strong class="text-3xl">{{ summary.total }}</strong>
        <p class="ops-muted">张活动额度</p>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="pink" :interactive="false" title="领取进度">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">↘</span></template>
        <strong class="text-3xl">{{ summary.claimed }}</strong>
        <p class="ops-muted">已领 {{ summary.claimed }} 张，剩余 {{ summary.remaining }} 张</p>
      </BentoCard>
    </BentoGrid>

    <p v-if="notice" class="ops-notice" :class="{ 'ops-success': noticeKind === 'success' }" role="status">{{ notice }}</p>

    <OpsModal :open="showForm" title="创建优惠券" @close="showForm = false">
      <form class="ops-form" @submit.prevent="createCoupon">
        <div class="ops-form-grid">
          <BaseInput v-model="form.name" label="活动名称" required placeholder="例如午间满减" />
          <BaseInput v-model="form.code" label="优惠码" required placeholder="LUNCH8" />
          <BaseSelect v-model="form.couponType" label="优惠类型" :options="typeOptions" required />
          <BaseInput v-model="form.thresholdAmount" label="使用门槛" type="number" :min="0" :step="0.01" required />
          <BaseInput v-if="form.couponType === 'FIXED'" v-model="form.discountAmount" label="减免金额" type="number" :min="0.01" :step="0.01" required />
          <BaseInput v-else v-model="form.discountRate" label="折扣比例" type="number" :min="0.0001" :max="1" :step="0.0001" required />
          <BaseInput v-model="form.totalQuantity" label="总发放量" type="number" :min="1" :step="1" required />
          <BaseInput v-model="form.perUserLimit" label="每人限领" type="number" :min="1" :step="1" required />
          <BaseInput v-model="form.startTime" label="开始时间" placeholder="2026-01-01T10:00:00" required />
          <BaseInput v-model="form.endTime" label="结束时间" placeholder="2026-01-31T22:00:00" required />
        </div>
        <p v-if="formError" class="ops-notice" role="alert">{{ formError }}</p>
        <p v-else-if="notice" class="ops-notice" :class="{ 'ops-success': noticeKind === 'success' }" role="status">{{ notice }}</p>
        <div class="ops-actions">
          <BaseButton native-type="submit" :loading="saving" :disabled="saving">创建活动</BaseButton>
          <BaseButton variant="ghost" :disabled="saving" @click="showForm = false">取消</BaseButton>
        </div>
      </form>
    </OpsModal>
  </main>
</template>

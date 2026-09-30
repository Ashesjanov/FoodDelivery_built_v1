<script setup lang="ts">
/** 商家优惠券页：创建营销活动并查看领取记录；后端无编辑接口，因此活动只读。 */
import { onMounted, ref } from 'vue'
import type { Coupon, CouponClaim, CouponClaimStatus, CouponType } from '@/types/domain'
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
import { callApi, dateTime, errorMessage, money, statusLabel } from '@/components/features/ops/opsUtils'
import '@/components/features/ops/opsStyles.css'

type CouponForm = { name: string; code: string; couponType: CouponType; thresholdAmount: string | number; discountAmount: string | number; discountRate: string | number; totalQuantity: string | number; perUserLimit: string | number; startTime: string; endTime: string }

const coupons = ref<Coupon[]>([])
const claims = ref<CouponClaim[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const showForm = ref(false)
const claimStatus = ref<CouponClaimStatus | ''>('')
const form = ref<CouponForm>(emptyForm())

function emptyForm(): CouponForm {
  return { name: '', code: '', couponType: 'FIXED', thresholdAmount: '0.00', discountAmount: '5.00', discountRate: '0.90', totalQuantity: 100, perUserLimit: 1, startTime: '', endTime: '' }
}

const typeOptions = [{ label: '固定减免', value: 'FIXED' }, { label: '折扣比例', value: 'PERCENT' }]
const claimOptions = [{ label: '全部领取记录', value: '' }, { label: '未使用', value: 'UNUSED' }, { label: '已使用', value: 'USED' }, { label: '已过期', value: 'EXPIRED' }, { label: '已取消', value: 'CANCELLED' }]

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [couponResult, claimResult] = await Promise.all([
      callApi<Coupon[]>(couponApi, ['available'], []),
      callApi<CouponClaim[]>(couponApi, ['my'], [claimStatus.value || undefined]),
    ])
    coupons.value = couponResult ?? []
    claims.value = claimResult ?? []
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function createCoupon() {
  if (saving.value) return
  const value = form.value
  if (!value.name.trim() || !value.code.trim() || !value.startTime || !value.endTime) return
  saving.value = true
  notice.value = ''
  try {
    await callApi(couponApi, ['create'], [{
      name: value.name.trim(), code: value.code.trim(), couponType: value.couponType,
      thresholdAmount: Number(value.thresholdAmount || 0),
      discountAmount: value.couponType === 'FIXED' ? Number(value.discountAmount || 0) : undefined,
      discountRate: value.couponType === 'PERCENT' ? Number(value.discountRate || 1) : undefined,
      totalQuantity: Number(value.totalQuantity || 1), perUserLimit: Number(value.perUserLimit || 1),
      startTime: value.startTime, endTime: value.endTime,
    }])
    showForm.value = false
    form.value = emptyForm()
    notice.value = '优惠券活动已创建'
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="ops-page">
    <PageHeader title="商家优惠券" description="创建满减或折扣活动，跟踪领取与使用情况。">
      <template #actions><BaseButton @click="showForm = true">创建优惠券</BaseButton></template>
    </PageHeader>

    <BentoGrid class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6 ops-grid">
      <BentoCard class="ops-card-lg ops-card" size="standard" tone="orange" :interactive="false" title="优惠活动" description="当前接口不支持编辑活动，创建后为只读。">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">券</span></template>
        <LoadingState v-if="loading" label="正在载入优惠券" />
        <ErrorState v-else-if="error" :message="error" @retry="load" />
        <EmptyState v-else-if="coupons.length === 0" title="暂无活动" description="创建满减或折扣券吸引顾客下单。" />
        <div v-else class="ops-list">
          <article v-for="coupon in coupons" :key="coupon.id" class="ops-row">
            <div class="ops-row-main">
              <div class="ops-row-title"><span>{{ coupon.name }}</span><StatusBadge :status="coupon.status" /></div>
              <div class="ops-row-meta">{{ coupon.couponType === 'FIXED' ? `满 ¥${money(coupon.thresholdAmount)} 减 ¥${money(coupon.discountAmount)}` : `满 ¥${money(coupon.thresholdAmount)} 打 ${Number(coupon.discountRate ?? 1) * 10} 折` }} · 已领 {{ coupon.claimedQuantity }}/{{ coupon.totalQuantity }}</div>
              <div class="ops-row-meta">{{ dateTime(coupon.startTime) }} 至 {{ dateTime(coupon.endTime) }}</div>
            </div>
            <StatusBadge :status="coupon.status" :label="statusLabel(coupon.status)" />
          </article>
        </div>
      </BentoCard>

      <BentoCard class="ops-card-wide ops-card" size="compact" tone="cyan" :interactive="false" title="领取记录">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">人</span></template>
        <BaseSelect v-model="claimStatus" label="核销状态" :options="claimOptions" @update:model-value="load" />
        <div v-if="claims.length" class="ops-list">
          <div v-for="claim in claims" :key="claim.id" class="ops-row">
            <div class="ops-row-main"><div class="ops-row-title"><span>{{ claim.couponName }}</span><StatusBadge :status="claim.status" /></div><div class="ops-row-meta">领取于 {{ dateTime(claim.claimedAt) }} · {{ claim.orderId ? `订单 #${claim.orderId}` : '尚未使用' }}</div></div>
            <span class="ops-muted">{{ claim.usedAt ? `核销 ${dateTime(claim.usedAt)}` : `到期 ${dateTime(claim.expiresAt)}` }}</span>
          </div>
        </div>
        <EmptyState v-else title="暂无领取记录" description="顾客领取优惠券后会显示在这里。" />
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="green" :interactive="false" title="活动数量">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">#</span></template>
        <strong style="font-size: 34px">{{ coupons.length }}</strong>
        <p class="ops-muted">个优惠活动</p>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="pink" :interactive="false" title="领取数量">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">↘</span></template>
        <strong style="font-size: 34px">{{ claims.length }}</strong>
        <p class="ops-muted">条领取记录</p>
      </BentoCard>
    </BentoGrid>

    <p v-if="notice" class="ops-notice" :class="{ 'ops-success': notice.includes('已创建') }" role="status">{{ notice }}</p>

    <OpsModal :open="showForm" title="创建优惠券" @close="showForm = false">
      <form class="ops-form" @submit.prevent="createCoupon">
        <div class="ops-form-grid">
          <BaseInput v-model="form.name" label="活动名称" required placeholder="例如午间满减" />
          <BaseInput v-model="form.code" label="优惠码" required placeholder="LUNCH8" />
          <BaseSelect v-model="form.couponType" label="优惠类型" :options="typeOptions" required />
          <BaseInput v-model="form.thresholdAmount" label="使用门槛" type="number" :min="0" :step="0.01" required />
          <BaseInput v-if="form.couponType === 'FIXED'" v-model="form.discountAmount" label="减免金额" type="number" :min="0" :step="0.01" required />
          <BaseInput v-else v-model="form.discountRate" label="折扣比例" type="number" :min="0.0001" :max="1" :step="0.0001" required />
          <BaseInput v-model="form.totalQuantity" label="总发放量" type="number" :min="1" :step="1" required />
          <BaseInput v-model="form.perUserLimit" label="每人限领" type="number" :min="1" :step="1" required />
          <BaseInput v-model="form.startTime" label="开始时间" type="url" placeholder="2026-01-01T10:00" required />
          <BaseInput v-model="form.endTime" label="结束时间" type="url" placeholder="2026-01-31T22:00" required />
        </div>
        <div class="ops-actions"><BaseButton native-type="submit" :loading="saving">创建活动</BaseButton><BaseButton variant="ghost" @click="showForm = false">取消</BaseButton></div>
      </form>
    </OpsModal>
  </main>
</template>

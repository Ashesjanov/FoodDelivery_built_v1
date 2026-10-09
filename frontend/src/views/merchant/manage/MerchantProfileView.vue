<script setup lang="ts">
/** 商家资料页：维护门店信息、营业状态、费用和分类关联。 */
import { onMounted, ref } from 'vue'
import type { Merchant, MerchantCategory } from '@/types/domain'
import { merchantApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
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
import { callApi, currentUserId, errorMessage, money, pageRecords, statusLabel } from '@/components/features/ops/opsUtils'
import '@/components/features/ops/opsStyles.css'

type ProfileForm = {
  name: string; description: string; logoUrl: string; contactName: string; contactPhone: string
  province: string; city: string; district: string; address: string; longitude: string | number; latitude: string | number
  businessHours: string; minOrderAmount: string | number; deliveryFee: string | number; packagingFee: string | number
}

const auth = useAuthStore() as any
const merchant = ref<Merchant | null>(null)
const categories = ref<MerchantCategory[]>([])
const categoryIds = ref<number[]>([])
const form = ref<ProfileForm>(emptyForm())
const businessStatus = ref<Merchant['businessStatus']>('OPEN')
const loading = ref(false)
const saving = ref('')
const error = ref('')
const notice = ref('')

function emptyForm(): ProfileForm {
  return { name: '', description: '', logoUrl: '', contactName: '', contactPhone: '', province: '', city: '', district: '', address: '', longitude: '', latitude: '', businessHours: '', minOrderAmount: '0.00', deliveryFee: '0.00', packagingFee: '0.00' }
}

const businessOptions = [
  { label: '准备中', value: 'PREPARING' }, { label: '营业中', value: 'OPEN' },
  { label: '暂停接单', value: 'PAUSED' }, { label: '已打烊', value: 'CLOSED' },
]

function fill(item: Merchant) {
  merchant.value = item
  businessStatus.value = item.businessStatus
  form.value = {
    name: item.name, description: item.description ?? '', logoUrl: item.logoUrl ?? '', contactName: item.contactName ?? '', contactPhone: item.contactPhone,
    province: item.province ?? '', city: item.city ?? '', district: item.district ?? '', address: item.address,
    longitude: item.longitude ?? '', latitude: item.latitude ?? '', businessHours: item.businessHours ?? '',
    minOrderAmount: item.minOrderAmount, deliveryFee: item.deliveryFee, packagingFee: item.packagingFee,
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await callApi<any>(merchantApi, ['list', 'page'], [{ page: 1, size: 100 }])
    const list = pageRecords<Merchant>(result)
    const store = list.find((item) => item.ownerId === currentUserId(auth)) ?? list[0]
    if (!store) throw new Error('当前账号尚未关联商家')
    const [detail, allCategories] = await Promise.all([
      callApi<{ merchant: Merchant; categoryIds: number[] }>(merchantApi, ['detail'], [store.id]),
      callApi<MerchantCategory[]>(merchantApi, ['categories'], []),
    ])
    fill(detail.merchant ?? store)
    categoryIds.value = detail.categoryIds ?? []
    categories.value = allCategories ?? []
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  const id = merchant.value?.id
  if (!id || saving.value || !form.value.name.trim() || !form.value.contactPhone.trim() || !form.value.address.trim()) return
  saving.value = 'profile'
  notice.value = ''
  try {
    const value = form.value
    const payload = {
      name: value.name.trim(), description: value.description.trim(), logoUrl: value.logoUrl.trim(), contactName: value.contactName.trim(),
      contactPhone: value.contactPhone.trim(), province: value.province.trim(), city: value.city.trim(), district: value.district.trim(), address: value.address.trim(),
      longitude: value.longitude === '' ? undefined : Number(value.longitude), latitude: value.latitude === '' ? undefined : Number(value.latitude),
      businessHours: value.businessHours.trim(), minOrderAmount: Number(value.minOrderAmount || 0), deliveryFee: Number(value.deliveryFee || 0), packagingFee: Number(value.packagingFee || 0),
      categoryIds: categoryIds.value,
    }
    fill(await callApi<Merchant>(merchantApi, ['update'], [id, payload]))
    notice.value = '商家资料已保存'
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = ''
  }
}

async function saveStatus() {
  const id = merchant.value?.id
  if (!id || saving.value) return
  saving.value = 'status'
  notice.value = ''
  try {
    fill(await callApi<Merchant>(merchantApi, ['setBusinessStatus', 'updateBusinessStatus'], [id, { status: businessStatus.value }]))
    notice.value = `营业状态已切换为${statusLabel(businessStatus.value)}`
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = ''
  }
}

async function toggleCategory(category: MerchantCategory) {
  const id = merchant.value?.id
  if (!id || saving.value) return
  const linked = categoryIds.value.includes(category.id)
  saving.value = `category-${category.id}`
  notice.value = ''
  try {
    if (linked) await callApi(merchantApi, ['removeMerchantCategory', 'removeCategory'], [id, category.id])
    else await callApi(merchantApi, ['addCategory'], [id, category.id])
    categoryIds.value = linked ? categoryIds.value.filter((value) => value !== category.id) : [...categoryIds.value, category.id]
    notice.value = linked ? '已取消分类关联' : '已关联分类'
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = ''
  }
}

onMounted(load)
</script>

<template>
  <main class="ops-page">
    <PageHeader title="商家资料" description="维护门店联系方式、费用、营业状态和分类关联。">
      <template #actions><BaseButton variant="outline" :loading="loading" @click="load">重新载入</BaseButton></template>
    </PageHeader>

    <BentoGrid class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6 ops-grid">
      <BentoCard class="ops-card-lg ops-card" size="standard" tone="orange" :interactive="false" title="门店资料" description="联系方式和地址用于顾客下单与骑手取餐。">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">店</span></template>
        <LoadingState v-if="loading" label="正在载入商家资料" />
        <ErrorState v-else-if="error" :message="error" @retry="load" />
        <form v-else-if="merchant" class="ops-form" @submit.prevent="saveProfile">
          <div class="ops-form-grid">
            <BaseInput v-model="form.name" label="商家名称" required />
            <BaseInput v-model="form.contactPhone" label="联系电话" type="tel" required />
            <BaseInput v-model="form.contactName" label="联系人" />
            <BaseInput v-model="form.businessHours" label="营业时间" placeholder="例如 09:00-22:00" />
            <BaseInput v-model="form.province" label="省份" />
            <BaseInput v-model="form.city" label="城市" />
            <BaseInput v-model="form.district" label="区县" />
            <BaseInput v-model="form.address" label="门店地址" required />
            <BaseInput v-model="form.longitude" label="经度" type="number" step="any" />
            <BaseInput v-model="form.latitude" label="纬度" type="number" step="any" />
          </div>
          <BaseInput v-model="form.logoUrl" label="Logo 地址" type="url" placeholder="https://" />
          <BaseInput v-model="form.description" label="商家简介" placeholder="一句话介绍门店特色" />
          <div class="ops-actions"><BaseButton native-type="submit" :loading="saving === 'profile'">保存资料</BaseButton></div>
        </form>
        <EmptyState v-else title="暂无商家" description="当前账号还没有可管理的门店。" />
      </BentoCard>

      <BentoCard class="ops-card-wide ops-card" size="compact" tone="green" :interactive="false" title="营业状态">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">◷</span></template>
        <div v-if="merchant" class="ops-row-title"><span>{{ merchant.name }}</span><StatusBadge :status="merchant.businessStatus" /></div>
        <div v-if="merchant" class="ops-inline-form">
          <BaseSelect v-model="businessStatus" label="当前状态" :options="businessOptions" />
          <BaseButton :loading="saving === 'status'" @click="saveStatus">切换状态</BaseButton>
        </div>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="cyan" :interactive="false" title="配送费用">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">¥</span></template>
        <div v-if="merchant" class="ops-kpis">
          <div class="ops-kpi"><span>起送价</span><strong>¥{{ money(merchant.minOrderAmount) }}</strong></div>
          <div class="ops-kpi"><span>配送费</span><strong>¥{{ money(merchant.deliveryFee) }}</strong></div>
          <div class="ops-kpi"><span>包装费</span><strong>¥{{ money(merchant.packagingFee) }}</strong></div>
          <div class="ops-kpi"><span>评分</span><strong>{{ Number(merchant.rating ?? 0).toFixed(1) }}</strong></div>
        </div>
      </BentoCard>

      <BentoCard class="ops-card-lg ops-card" size="wide" tone="cyan" :interactive="false" title="费用设置" description="金额会按两位小数提交给订单结算。">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">⌁</span></template>
        <div v-if="merchant" class="ops-form-grid">
          <BaseInput v-model="form.minOrderAmount" label="起送价" type="number" :min="0" :step="0.01" />
          <BaseInput v-model="form.deliveryFee" label="配送费" type="number" :min="0" :step="0.01" />
          <BaseInput v-model="form.packagingFee" label="包装费" type="number" :min="0" :step="0.01" />
        </div>
        <div v-if="merchant" class="ops-actions" style="margin-top: 16px"><BaseButton variant="secondary" :loading="saving === 'profile'" @click="saveProfile">保存费用</BaseButton></div>
      </BentoCard>

      <BentoCard class="ops-card-wide ops-card" size="compact" tone="pink" :interactive="false" title="分类关联" description="关联后顾客可按分类找到门店。">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">◇</span></template>
        <div v-if="categories.length" class="ops-list">
          <label v-for="category in categories" :key="category.id" class="ops-row">
            <span class="ops-row-main"><strong>{{ category.name }}</strong><span class="ops-row-meta">{{ category.enabled ? '分类已启用' : '分类已停用' }}</span></span>
            <input type="checkbox" :checked="categoryIds.includes(category.id)" :disabled="saving === `category-${category.id}`" @change="toggleCategory(category)" />
          </label>
        </div>
        <EmptyState v-else title="暂无分类" description="管理员创建分类后可在此关联。" />
      </BentoCard>
    </BentoGrid>

    <p v-if="notice" class="ops-notice" :class="{ 'ops-success': notice.includes('已') || notice.includes('保存') }" role="status">{{ notice }}</p>
  </main>
</template>

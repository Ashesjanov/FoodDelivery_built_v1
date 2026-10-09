<script setup lang="ts">
/** 管理员商家管理：检索门店、创建或编辑资料并维护营业状态。 */
import { onMounted, ref } from 'vue'
import type { Merchant, MerchantBusinessStatus, MerchantCategory } from '@/types/domain'
import { merchantApi } from '@/api'
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
import OpsPager from '@/components/features/ops/OpsPager.vue'
import { callApi, errorMessage, money, pageRecords, pageTotal, statusLabel } from '@/components/features/ops/opsUtils'
import '@/components/features/ops/opsStyles.css'

type MerchantForm = {
  id: number | null; name: string; description: string; logoUrl: string; contactName: string; contactPhone: string
  province: string; city: string; district: string; address: string; longitude: string | number; latitude: string | number
  businessHours: string; minOrderAmount: string | number; deliveryFee: string | number; packagingFee: string | number
}

const merchants = ref<Merchant[]>([])
const categories = ref<MerchantCategory[]>([])
const categoryIds = ref<number[]>([])
const keyword = ref('')
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const saving = ref('')
const error = ref('')
const notice = ref('')
const showForm = ref(false)
const form = ref<MerchantForm>(emptyForm())

const businessOptions = [
  { label: '准备中', value: 'PREPARING' }, { label: '营业中', value: 'OPEN' },
  { label: '暂停接单', value: 'PAUSED' }, { label: '已打烊', value: 'CLOSED' },
]

function emptyForm(): MerchantForm {
  return { id: null, name: '', description: '', logoUrl: '', contactName: '', contactPhone: '', province: '', city: '', district: '', address: '', longitude: '', latitude: '', businessHours: '', minOrderAmount: '0.00', deliveryFee: '0.00', packagingFee: '0.00' }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [merchantResult, categoryResult] = await Promise.all([
      callApi<any>(merchantApi, ['list', 'page'], [{ page: page.value, size: 8, keyword: keyword.value.trim() || undefined }]),
      callApi<MerchantCategory[]>(merchantApi, ['categories'], []),
    ])
    merchants.value = pageRecords<Merchant>(merchantResult)
    total.value = pageTotal(merchantResult)
    categories.value = categoryResult ?? []
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function edit(merchant?: Merchant) {
  notice.value = ''
  if (!merchant) {
    form.value = emptyForm()
    categoryIds.value = []
    showForm.value = true
    return
  }
  try {
    const detail = await callApi<{ merchant: Merchant; categoryIds: number[] }>(merchantApi, ['detail'], [merchant.id])
    const item = detail.merchant ?? merchant
    form.value = { id: item.id, name: item.name, description: item.description ?? '', logoUrl: item.logoUrl ?? '', contactName: item.contactName ?? '', contactPhone: item.contactPhone, province: item.province ?? '', city: item.city ?? '', district: item.district ?? '', address: item.address, longitude: item.longitude ?? '', latitude: item.latitude ?? '', businessHours: item.businessHours ?? '', minOrderAmount: item.minOrderAmount, deliveryFee: item.deliveryFee, packagingFee: item.packagingFee }
    categoryIds.value = detail.categoryIds ?? []
    showForm.value = true
  } catch (cause) {
    notice.value = errorMessage(cause)
  }
}

async function save() {
  const value = form.value
  if (saving.value || !value.name.trim() || !value.contactPhone.trim() || !value.address.trim()) return
  saving.value = value.id ? `merchant-${value.id}` : 'merchant-new'
  notice.value = ''
  try {
    const payload = {
      name: value.name.trim(), description: value.description.trim(), logoUrl: value.logoUrl.trim(), contactName: value.contactName.trim(), contactPhone: value.contactPhone.trim(),
      province: value.province.trim(), city: value.city.trim(), district: value.district.trim(), address: value.address.trim(), longitude: value.longitude === '' ? undefined : Number(value.longitude), latitude: value.latitude === '' ? undefined : Number(value.latitude),
      businessHours: value.businessHours.trim(), minOrderAmount: Number(value.minOrderAmount || 0), deliveryFee: Number(value.deliveryFee || 0), packagingFee: Number(value.packagingFee || 0), categoryIds: categoryIds.value,
    }
    if (value.id) await callApi(merchantApi, ['update'], [value.id, payload])
    else await callApi(merchantApi, ['create'], [payload])
    showForm.value = false
    notice.value = value.id ? '商家资料已更新' : '商家已创建'
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = ''
  }
}

async function changeStatus(merchant: Merchant, status: MerchantBusinessStatus) {
  if (saving.value) return
  saving.value = `status-${merchant.id}`
  notice.value = ''
  try {
    await callApi(merchantApi, ['setBusinessStatus', 'updateBusinessStatus'], [merchant.id, { status }])
    notice.value = `${merchant.name} 已切换为${statusLabel(status)}`
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = ''
  }
}

function changePage(next: number) {
  page.value = next
  void load()
}

onMounted(load)
</script>

<template>
  <main class="ops-page">
    <PageHeader title="商家管理" description="维护门店资料、费用、分类和营业状态。">
      <template #actions><BaseButton @click="edit()">新建商家</BaseButton><BaseButton variant="outline" :loading="loading" @click="load">刷新列表</BaseButton></template>
    </PageHeader>

    <BentoGrid class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6 ops-grid">
      <BentoCard class="ops-card-lg ops-card" size="standard" tone="orange" :interactive="false" title="门店列表" description="营业状态会直接影响顾客下单资格。">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">店</span></template>
        <div class="ops-inline-form"><BaseInput v-model="keyword" label="搜索商家" placeholder="输入名称后回车" @keyup.enter="changePage(1)" /><BaseButton variant="outline" @click="changePage(1)">搜索</BaseButton></div>
        <LoadingState v-if="loading" label="正在载入商家" />
        <ErrorState v-else-if="error" :message="error" @retry="load" />
        <EmptyState v-else-if="merchants.length === 0" title="暂无商家" description="创建门店后即可配置菜单。" />
        <div v-else class="ops-list">
          <article v-for="merchant in merchants" :key="merchant.id" class="ops-row">
            <div class="ops-row-main">
              <div class="ops-row-title"><span>{{ merchant.name }}</span><StatusBadge :status="merchant.businessStatus" /></div>
              <div class="ops-row-meta">{{ merchant.city || '未填写城市' }} · 评分 {{ Number(merchant.rating ?? 0).toFixed(1) }} · 月售 {{ merchant.monthlySales }} · 配送费 ¥{{ money(merchant.deliveryFee) }}</div>
            </div>
            <div class="ops-actions"><BaseButton size="sm" variant="outline" @click="edit(merchant)">编辑</BaseButton><BaseSelect :model-value="merchant.businessStatus" :options="businessOptions" label="状态" @update:model-value="changeStatus(merchant, $event as MerchantBusinessStatus)" /></div>
          </article>
        </div>
        <OpsPager :page="page" :pages="Math.ceil(total / 8)" :total="total" :busy="loading" @prev="changePage(page - 1)" @next="changePage(page + 1)" />
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="green" :interactive="false" title="商家总数">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">#</span></template>
        <strong style="font-size: 34px">{{ total }}</strong>
        <p class="ops-muted">家入驻门店</p>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="cyan" :interactive="false" title="营业中">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">◉</span></template>
        <strong style="font-size: 34px">{{ merchants.filter((item) => item.businessStatus === 'OPEN').length }}</strong>
        <p class="ops-muted">家可接单</p>
      </BentoCard>

      <BentoCard class="ops-card-wide ops-card" size="compact" tone="pink" :interactive="false" title="费用概览">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">¥</span></template>
        <p class="ops-muted">平均配送费 ¥{{ money(merchants.length ? merchants.reduce((sum, item) => sum + Number(item.deliveryFee ?? 0), 0) / merchants.length : 0) }}，平均评分 {{ merchants.length ? (merchants.reduce((sum, item) => sum + Number(item.rating ?? 0), 0) / merchants.length).toFixed(1) : '0.0' }}。</p>
      </BentoCard>
    </BentoGrid>

    <p v-if="notice" class="ops-notice" :class="{ 'ops-success': notice.includes('已') }" role="status">{{ notice }}</p>

    <OpsModal :open="showForm" :title="form.id ? '编辑商家' : '新建商家'" @close="showForm = false">
      <form class="ops-form" @submit.prevent="save">
        <div class="ops-form-grid">
          <BaseInput v-model="form.name" label="商家名称" required />
          <BaseInput v-model="form.contactPhone" label="联系电话" type="tel" required />
          <BaseInput v-model="form.contactName" label="联系人" />
          <BaseInput v-model="form.businessHours" label="营业时间" />
          <BaseInput v-model="form.province" label="省份" />
          <BaseInput v-model="form.city" label="城市" />
          <BaseInput v-model="form.district" label="区县" />
          <BaseInput v-model="form.address" label="地址" required />
          <BaseInput v-model="form.longitude" label="经度" type="number" step="any" />
          <BaseInput v-model="form.latitude" label="纬度" type="number" step="any" />
          <BaseInput v-model="form.minOrderAmount" label="起送价" type="number" :min="0" :step="0.01" />
          <BaseInput v-model="form.deliveryFee" label="配送费" type="number" :min="0" :step="0.01" />
          <BaseInput v-model="form.packagingFee" label="包装费" type="number" :min="0" :step="0.01" />
          <BaseInput v-model="form.logoUrl" label="Logo 地址" type="url" />
        </div>
        <BaseInput v-model="form.description" label="商家简介" />
        <div>
          <p class="ops-muted">分类关联</p>
          <label v-for="category in categories" :key="category.id" class="ops-muted" style="display: inline-flex; margin-right: 16px"><input v-model="categoryIds" type="checkbox" :value="category.id" /> {{ category.name }}</label>
        </div>
        <div class="ops-actions"><BaseButton native-type="submit" :loading="saving.startsWith('merchant')">保存商家</BaseButton><BaseButton variant="ghost" @click="showForm = false">取消</BaseButton></div>
      </form>
    </OpsModal>
  </main>
</template>

<script setup lang="ts">
/** 收货地址管理：新增、编辑、删除和设置默认地址。 */
import { onMounted, ref } from 'vue'
import type { Address, AddressRequest } from '@/types/domain'
import { addressApi } from '@/api'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import BentoGrid from '@/components/bento/BentoGrid.vue'
import BentoCard from '@/components/bento/BentoCard.vue'
import { errorMessage, unwrap } from '@/components/features/customer/customerUtils'

const addresses = ref<Address[]>([])
const loading = ref(true)
const error = ref('')
const notice = ref('')
const saving = ref(false)
const actionId = ref(0)
const editingId = ref<number | null>(null)
const showForm = ref(false)
const form = ref({ contactName: '', phone: '', province: '', city: '', district: '', detail: '', label: '', isDefault: false })

async function load() {
  loading.value = true
  error.value = ''
  try {
    addresses.value = unwrap<Address[]>(await addressApi.list())
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = { contactName: '', phone: '', province: '', city: '', district: '', detail: '', label: '', isDefault: addresses.value.length === 0 }
  showForm.value = true
}

function openEdit(address: Address) {
  editingId.value = address.id
  form.value = { contactName: address.contactName, phone: address.phone, province: address.province, city: address.city, district: address.district ?? '', detail: address.detail, label: address.label ?? '', isDefault: address.isDefault }
  showForm.value = true
}

async function save() {
  if (saving.value) return
  saving.value = true
  notice.value = ''
  try {
    const payload = form.value as AddressRequest
    if (editingId.value) await addressApi.update(editingId.value, payload)
    else await addressApi.create(payload)
    showForm.value = false
    notice.value = editingId.value ? '地址已更新' : '地址已保存'
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

async function remove(address: Address) {
  if (actionId.value) return
  actionId.value = address.id
  try {
    await addressApi.remove(address.id)
    notice.value = '地址已删除'
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    actionId.value = 0
  }
}

async function setDefault(address: Address) {
  if (actionId.value) return
  actionId.value = address.id
  try {
    await addressApi.setDefault(address.id)
    notice.value = '默认地址已更新'
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    actionId.value = 0
  }
}

onMounted(load)
</script>

<template>
  <main class="address-page">
    <PageHeader title="收货地址" description="把常用地址保存下来，下次下单更快。" back-to="/checkout" />
    <LoadingState v-if="loading" label="正在读取地址簿" />
    <ErrorState v-else-if="error && !addresses.length" :message="error" action-label="重新加载" @retry="load" />
    <BentoGrid v-else class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
      <BentoCard class="address-card" size="medium" tone="dark" aspect="2 / 1">
        <div class="card-heading"><div><p class="section-label">地址簿</p><h1>把家和公司<br />都放在这里</h1></div><span class="home-icon group">⌂</span></div>
        <BaseButton type="primary" @click="openCreate">新增地址</BaseButton>
        <p v-if="notice" class="notice">{{ notice }}</p>
      </BentoCard>

      <BentoCard v-if="showForm" class="form-card" size="large" tone="fresh" aspect="1 / 1">
        <div class="card-heading"><div><p class="section-label">{{ editingId ? '编辑地址' : '添加地址' }}</p><h2>{{ editingId ? '编辑地址' : '添加新地址' }}</h2></div><button class="close-button" type="button" aria-label="关闭" @click="showForm = false">×</button></div>
        <div class="form-grid">
          <BaseInput v-model="form.contactName" label="联系人" placeholder="收件人姓名" />
          <BaseInput v-model="form.phone" label="手机号" type="tel" placeholder="配送联系电话" />
          <BaseInput v-model="form.province" label="省份" placeholder="例如：浙江省" />
          <BaseInput v-model="form.city" label="城市" placeholder="例如：杭州市" />
          <BaseInput v-model="form.district" label="区县" placeholder="例如：西湖区" />
          <BaseInput v-model="form.label" label="标签" placeholder="家、公司或其他" />
          <BaseInput v-model="form.detail" label="详细地址" placeholder="街道、楼栋和门牌号" />
          <label class="default-row"><input v-model="form.isDefault" type="checkbox" /> 设为默认地址</label>
        </div>
        <div class="form-actions"><BaseButton type="secondary" @click="showForm = false">取消</BaseButton><BaseButton type="primary" :loading="saving" @click="save">保存地址</BaseButton></div>
      </BentoCard>

      <BentoCard v-for="(address, index) in addresses" :key="address.id" class="item-card" :size="index === 0 ? 'large' : index % 3 === 0 ? 'medium' : 'small'" :tone="index % 3 === 0 ? 'warm' : 'default'" :aspect="index === 0 ? '1 / 1' : index % 3 === 0 ? '2 / 1' : '1 / 1'">
        <div class="item-top"><div><p class="section-label">{{ address.label || '收货地址' }}</p><h2>{{ address.contactName }}</h2></div><StatusBadge :status="address.isDefault ? 'ACTIVE' : 'DEFAULT'" /></div>
        <p class="phone">{{ address.phone }}</p><p class="location">{{ address.province }} {{ address.city }} {{ address.district }}<br />{{ address.detail }}</p>
        <div class="item-actions"><BaseButton type="ghost" :disabled="actionId === address.id" @click="openEdit(address)">编辑</BaseButton><BaseButton type="ghost" :disabled="address.isDefault || actionId === address.id" @click="setDefault(address)">设为默认</BaseButton><BaseButton type="danger" :loading="actionId === address.id" @click="remove(address)">删除</BaseButton></div>
      </BentoCard>

      <BentoCard v-if="!addresses.length" class="empty-card" size="large" tone="default" aspect="2 / 1"><EmptyState title="还没有收货地址" description="添加一个地址，让热乎的饭菜准时找到你。" action-label="添加地址" @action="openCreate" /></BentoCard>
    </BentoGrid>
  </main>
</template>

<style scoped>
.address-page { min-height: 100vh; padding: clamp(24px, 5vw, 76px); background: #f2f4ef; color: #203027; font-family: 'PingFang SC', sans-serif; }.section-label { color: #a56a2d; font-size: 13px; margin: 0 0 14px; } h1, h2 { font-family: Georgia, 'Songti SC', serif; font-weight: 500; margin: 0; } h1 { font-size: clamp(34px, 4vw, 52px); line-height: 1.08; } h2 { font-size: 25px; }
.address-card, .form-card, .item-card, .empty-card { padding: clamp(22px, 2.8vw, 38px); }.address-card { color: #f2f4ed; display: flex; flex-direction: column; justify-content: space-between; }.card-heading, .item-top, .form-actions, .item-actions { display: flex; align-items: center; justify-content: space-between; gap: 16px; }.home-icon { font-size: 54px; color: #e5b55d; transition: transform .3s ease; }.group:hover .home-icon { transform: rotate(-10deg) scale(1.12); }.notice { color: #e8c783; margin-bottom: 0; }
.form-card { display: flex; flex-direction: column; }.close-button { width: 38px; height: 38px; border: 0; border-radius: 50%; background: #e3ece2; cursor: pointer; font-size: 22px; }.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px 16px; margin: 24px 0; }.default-row { display: flex; align-items: center; gap: 16px; color: #56715d; }.default-row input { accent-color: #5d8266; width: 18px; height: 18px; }.form-actions { justify-content: flex-end; margin-top: auto; }
.item-card { display: flex; flex-direction: column; }.phone { color: #8b652f; margin: 14px 0; }.location { color: #6c766e; line-height: 1.8; }.item-actions { margin-top: auto; flex-wrap: wrap; justify-content: flex-start; }
@media (max-width: 640px) { .form-grid { grid-template-columns: 1fr; } }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { transition-duration: .01ms !important; animation-duration: .01ms !important; } }
</style>

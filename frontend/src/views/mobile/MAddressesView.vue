<script setup lang="ts">
/** 移动端收货地址管理：与电脑端内容一致，新增、编辑、删除和设置默认地址。 */
import { onMounted, ref } from 'vue'
import type { Address, AddressRequest } from '@/types/domain'
import { addressApi } from '@/api'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
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
  <div class="m-page">
    <header class="m-page-head">
      <RouterLink class="m-back" to="/m/checkout"><span aria-hidden="true">←</span> 返回确认订单</RouterLink>
      <h1 class="m-title" style="margin-top: 8px">收货地址</h1>
      <p class="m-sub">把常用地址保存下来，下次下单更快。</p>
    </header>

    <LoadingState v-if="loading" label="正在读取地址簿" />
    <ErrorState v-else-if="error && !addresses.length" :message="error" action-label="重新加载" @retry="load" />
    <div v-else class="m-stack">
      <section class="m-card m-card--dark">
        <div class="m-row m-row--top">
          <div>
            <p class="m-label">地址簿</p>
            <h2 class="m-title" style="font-size: 26px">把家和公司<br />都放在这里</h2>
          </div>
          <span style="font-size: 44px; color: #e5b55d" aria-hidden="true">⌂</span>
        </div>
        <div style="margin-top: 16px">
          <BaseButton type="primary" @click="openCreate">新增地址</BaseButton>
        </div>
        <p v-if="notice" class="m-notice" role="status">{{ notice }}</p>
      </section>

      <section v-if="showForm" class="m-card m-card--fresh">
        <div class="m-row m-row--top">
          <div>
            <p class="m-label">{{ editingId ? '编辑地址' : '添加地址' }}</p>
            <h2 class="m-subtitle">{{ editingId ? '编辑地址' : '添加新地址' }}</h2>
          </div>
          <button class="m-close" type="button" aria-label="关闭" @click="showForm = false">×</button>
        </div>
        <div class="m-form" style="margin-top: 16px">
          <BaseInput v-model="form.contactName" label="联系人" placeholder="收件人姓名" />
          <BaseInput v-model="form.phone" label="手机号" type="tel" placeholder="配送联系电话" />
          <BaseInput v-model="form.province" label="省份" placeholder="例如：浙江省" />
          <BaseInput v-model="form.city" label="城市" placeholder="例如：杭州市" />
          <BaseInput v-model="form.district" label="区县" placeholder="例如：西湖区" />
          <BaseInput v-model="form.label" label="标签" placeholder="家、公司或其他" />
          <BaseInput v-model="form.detail" label="详细地址" placeholder="街道、楼栋和门牌号" />
          <label class="m-default"><input v-model="form.isDefault" type="checkbox" /> 设为默认地址</label>
        </div>
        <div class="m-actions" style="margin-top: 16px">
          <BaseButton type="secondary" @click="showForm = false">取消</BaseButton>
          <BaseButton type="primary" :loading="saving" @click="save">保存地址</BaseButton>
        </div>
      </section>

      <section v-for="address in addresses" :key="address.id" class="m-card">
        <div class="m-row m-row--top">
          <div>
            <p class="m-label" style="color: #a56a2d">{{ address.label || '收货地址' }}</p>
            <h2 class="m-subtitle">{{ address.contactName }}</h2>
          </div>
          <StatusBadge :status="address.isDefault ? 'ACTIVE' : 'DEFAULT'" />
        </div>
        <p class="m-phone">{{ address.phone }}</p>
        <p class="m-loc">{{ address.province }} {{ address.city }} {{ address.district }}<br />{{ address.detail }}</p>
        <div class="m-actions" style="margin-top: 16px">
          <BaseButton type="ghost" size="sm" :disabled="actionId === address.id" @click="openEdit(address)">编辑</BaseButton>
          <BaseButton type="ghost" size="sm" :disabled="address.isDefault || actionId === address.id" @click="setDefault(address)">设为默认</BaseButton>
          <BaseButton type="danger" size="sm" :loading="actionId === address.id" @click="remove(address)">删除</BaseButton>
        </div>
      </section>

      <section v-if="!addresses.length" class="m-card">
        <EmptyState title="还没有收货地址" description="添加一个地址，让热乎的饭菜准时找到你。" action-label="添加地址" @action="openCreate" />
      </section>
    </div>
  </div>
</template>

<style scoped>
.m-page-head { margin-bottom: 16px; }
.m-back { display: inline-flex; align-items: center; gap: 16px; color: #69746d; font-size: 14px; text-decoration: none; min-height: 44px; }
.m-back:hover, .m-back:focus-visible { color: #1d2722; outline: none; }
.m-sub { margin: 8px 0 0; color: #69746d; line-height: 1.7; font-size: 14px; }
.m-close { width: 44px; height: 44px; border: 0; border-radius: 50%; background: #e3ece2; cursor: pointer; font-size: 22px; }
.m-default { display: flex; align-items: center; gap: 16px; color: #56715d; font-size: 14px; }
.m-default input { width: 18px; height: 18px; accent-color: #5d8266; }
.m-phone { color: #8b652f; margin: 12px 0; }
.m-loc { color: #6c766e; line-height: 1.8; margin: 0; }
</style>

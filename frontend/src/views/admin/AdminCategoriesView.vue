<script setup lang="ts">
/** 管理员分类管理：维护商家分类的名称、图标、排序和启用状态。 */
import { onMounted, ref } from 'vue'
import type { MerchantCategory } from '@/types/domain'
import { merchantApi } from '@/api'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import BentoGrid from '@/components/bento/BentoGrid.vue'
import BentoCard from '@/components/bento/BentoCard.vue'
import OpsModal from '@/components/features/ops/OpsModal.vue'
import { callApi, errorMessage, pageRecords } from '@/components/features/ops/opsUtils'
import '@/components/features/ops/opsStyles.css'

type CategoryForm = { id: number | null; name: string; iconUrl: string; sortOrder: string | number; enabled: boolean }

const categories = ref<MerchantCategory[]>([])
const loading = ref(false)
const saving = ref(0)
const error = ref('')
const notice = ref('')
const showForm = ref(false)
const form = ref<CategoryForm>(emptyForm())

function emptyForm(): CategoryForm {
  return { id: null, name: '', iconUrl: '', sortOrder: 0, enabled: true }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await callApi<any>(merchantApi, ['categories'], [])
    categories.value = Array.isArray(result) ? result : pageRecords<MerchantCategory>(result)
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

function edit(category?: MerchantCategory) {
  form.value = category ? { id: category.id, name: category.name, iconUrl: category.iconUrl ?? '', sortOrder: category.sortOrder, enabled: category.enabled } : emptyForm()
  notice.value = ''
  showForm.value = true
}

async function save() {
  const value = form.value
  if (!value.name.trim() || saving.value) return
  saving.value = value.id ?? -1
  notice.value = ''
  try {
    const payload = { name: value.name.trim(), iconUrl: value.iconUrl.trim(), sortOrder: Number(value.sortOrder || 0), enabled: value.enabled }
    if (value.id) await callApi(merchantApi, ['updateCategory'], [value.id, payload])
    else await callApi(merchantApi, ['createCategory'], [payload])
    showForm.value = false
    notice.value = value.id ? '分类已更新' : '分类已创建'
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = 0
  }
}

async function remove(category: MerchantCategory) {
  if (saving.value || !window.confirm(`确认删除分类“${category.name}”？`)) return
  saving.value = category.id
  notice.value = ''
  try {
    await callApi(merchantApi, ['deleteCategory', 'removeCategory'], [category.id])
    notice.value = '分类已删除'
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = 0
  }
}

onMounted(load)
</script>

<template>
  <main class="ops-page">
    <PageHeader title="分类管理" description="维护商家分类，控制顾客端筛选入口。">
      <template #actions><BaseButton @click="edit()">新建分类</BaseButton></template>
    </PageHeader>

    <BentoGrid class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6 ops-grid">
      <BentoCard class="ops-card-lg ops-card" size="standard" tone="green" :interactive="false" title="分类列表" description="分类排序越小越靠前。">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">◇</span></template>
        <LoadingState v-if="loading" label="正在载入分类" />
        <ErrorState v-else-if="error" :message="error" @retry="load" />
        <EmptyState v-else-if="categories.length === 0" title="暂无分类" description="创建分类后商家可以关联经营品类。" />
        <div v-else class="ops-list">
          <article v-for="category in categories" :key="category.id" class="ops-row">
            <div class="ops-row-main">
              <div class="ops-row-title"><span>{{ category.name }}</span><StatusBadge :status="category.enabled ? 'ACTIVE' : 'DISABLED'" :label="category.enabled ? '已启用' : '已停用'" /></div>
              <div class="ops-row-meta">排序 {{ category.sortOrder }} · {{ category.iconUrl || '未设置图标' }}</div>
            </div>
            <div class="ops-actions">
              <BaseButton size="sm" variant="outline" @click="edit(category)">编辑</BaseButton>
              <BaseButton size="sm" variant="danger" :loading="saving === category.id" @click="remove(category)">删除</BaseButton>
            </div>
          </article>
        </div>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="cyan" :interactive="false" title="分类总数">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">#</span></template>
        <strong style="font-size: 34px">{{ categories.length }}</strong>
        <p class="ops-muted">个经营分类</p>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="orange" :interactive="false" title="启用分类">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">✓</span></template>
        <strong style="font-size: 34px">{{ categories.filter((item) => item.enabled).length }}</strong>
        <p class="ops-muted">个对顾客可见</p>
      </BentoCard>

      <BentoCard class="ops-card-wide ops-card" size="compact" tone="neutral" :interactive="false" title="分类维护提示">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">!</span></template>
        <p class="ops-muted">删除分类前请确认没有商家仍在使用，停用分类可保留历史商家关联。</p>
      </BentoCard>
    </BentoGrid>

    <p v-if="notice" class="ops-notice" :class="{ 'ops-success': notice.includes('已') }" role="status">{{ notice }}</p>

    <OpsModal :open="showForm" :title="form.id ? '编辑分类' : '新建分类'" @close="showForm = false">
      <form class="ops-form" @submit.prevent="save">
        <BaseInput v-model="form.name" label="分类名称" required placeholder="例如中式快餐" />
        <BaseInput v-model="form.iconUrl" label="图标地址（选填）" type="url" placeholder="https://" />
        <BaseInput v-model="form.sortOrder" label="排序" type="number" :min="0" :step="1" />
        <label class="ops-muted"><input v-model="form.enabled" type="checkbox" /> 启用分类</label>
        <div class="ops-actions"><BaseButton native-type="submit" :loading="saving === (form.id ?? -1)">保存分类</BaseButton><BaseButton variant="ghost" @click="showForm = false">取消</BaseButton></div>
      </form>
    </OpsModal>
  </main>
</template>

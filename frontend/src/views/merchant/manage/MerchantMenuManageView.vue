<script setup lang="ts">
/** 商家菜单管理：菜品与规格的完整 CRUD、上架、下架和售罄。 */
import { onMounted, ref } from 'vue'
import type { Dish, DishSpec, Merchant, MerchantCategory } from '@/types/domain'
import { dishApi, merchantApi } from '@/api'
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
import OpsModal from '@/components/features/ops/OpsModal.vue'
import { callApi, currentUserId, errorMessage, money, pageRecords } from '@/components/features/ops/opsUtils'
import '@/components/features/ops/opsStyles.css'

type DishForm = { id: number | null; categoryId: number | null; name: string; description: string; imageUrl: string; price: string | number; originalPrice: string | number; stock: string | number; sortOrder: string | number }
type SpecForm = { id: number | null; groupName: string; name: string; priceOffset: string | number; sortOrder: string | number; isDefault: boolean }

const auth = useAuthStore() as any
const merchant = ref<Merchant | null>(null)
const categories = ref<MerchantCategory[]>([])
const dishes = ref<Dish[]>([])
const specs = ref<DishSpec[]>([])
const selectedDish = ref<Dish | null>(null)
const loading = ref(false)
const saving = ref('')
const error = ref('')
const notice = ref('')
const showDishForm = ref(false)
const showSpecForm = ref(false)
const dishForm = ref<DishForm>(emptyDish())
const specForm = ref<SpecForm>(emptySpec())

function emptyDish(): DishForm {
  return { id: null, categoryId: null, name: '', description: '', imageUrl: '', price: '', originalPrice: '', stock: 0, sortOrder: 0 }
}

function emptySpec(): SpecForm {
  return { id: null, groupName: '', name: '', priceOffset: 0, sortOrder: 0, isDefault: false }
}

const categoryOptions = () => categories.value.map((item) => ({ label: item.name, value: item.id }))

async function resolveMerchant() {
  if (merchant.value) return merchant.value
  const result = await callApi<any>(merchantApi, ['list', 'page'], [{ page: 1, size: 100 }])
  const list = pageRecords<Merchant>(result)
  merchant.value = list.find((item) => item.ownerId === currentUserId(auth)) ?? list[0] ?? null
  return merchant.value
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const store = await resolveMerchant()
    if (!store) throw new Error('当前账号尚未关联商家')
    const [categoryResult, dishResult] = await Promise.all([
      callApi<MerchantCategory[]>(merchantApi, ['categories'], []),
      callApi<any>(dishApi, ['list', 'page'], [{ merchantId: store.id, page: 1, size: 100 }]),
    ])
    categories.value = categoryResult ?? []
    dishes.value = pageRecords<Dish>(dishResult)
    if (selectedDish.value) await openSpecs(selectedDish.value)
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

function openDish(item?: Dish) {
  notice.value = ''
  dishForm.value = item
    ? { id: item.id, categoryId: item.categoryId, name: item.name, description: item.description ?? '', imageUrl: item.imageUrl ?? '', price: item.price, originalPrice: item.originalPrice ?? '', stock: item.stock, sortOrder: item.sortOrder }
    : emptyDish()
  showDishForm.value = true
}

async function saveDish() {
  if (saving.value) return
  const form = dishForm.value
  if (!form.categoryId || !form.name.trim() || form.price === '') return
  saving.value = 'dish'
  notice.value = ''
  try {
    const payload = {
      categoryId: Number(form.categoryId), name: form.name.trim(), description: form.description.trim(), imageUrl: form.imageUrl.trim(),
      price: Number(form.price), originalPrice: form.originalPrice === '' ? undefined : Number(form.originalPrice), stock: Number(form.stock || 0), sortOrder: Number(form.sortOrder || 0),
    }
    if (form.id) await callApi(dishApi, ['update'], [form.id, payload])
    else if (merchant.value) await callApi(dishApi, ['create'], [merchant.value.id, payload])
    showDishForm.value = false
    notice.value = form.id ? '菜品已更新' : '菜品已创建'
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = ''
  }
}

async function changeDishStatus(item: Dish, status: Dish['status']) {
  if (saving.value) return
  saving.value = `dish-${item.id}`
  try {
    await callApi(dishApi, ['setStatus', 'updateStatus'], [item.id, { status }])
    notice.value = '菜品状态已更新'
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = ''
  }
}

async function removeDish(item: Dish) {
  if (!window.confirm(`确认删除菜品“${item.name}”？`) || saving.value) return
  saving.value = `remove-${item.id}`
  try {
    await callApi(dishApi, ['remove', 'delete'], [item.id])
    if (selectedDish.value?.id === item.id) selectedDish.value = null
    notice.value = '菜品已删除'
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = ''
  }
}

async function openSpecs(item: Dish) {
  selectedDish.value = item
  try {
    specs.value = (await callApi<DishSpec[]>(dishApi, ['specs'], [item.id])) ?? []
  } catch (cause) {
    notice.value = errorMessage(cause)
  }
}

function openSpec(item?: DishSpec) {
  notice.value = ''
  specForm.value = item ? { id: item.id, groupName: item.groupName ?? '', name: item.name, priceOffset: item.priceOffset, sortOrder: item.sortOrder, isDefault: item.isDefault } : emptySpec()
  showSpecForm.value = true
}

async function saveSpec() {
  const dish = selectedDish.value
  const form = specForm.value
  if (!dish || !form.name.trim() || saving.value) return
  saving.value = 'spec'
  try {
    const payload = { groupName: form.groupName.trim() || undefined, name: form.name.trim(), priceOffset: Number(form.priceOffset || 0), sortOrder: Number(form.sortOrder || 0), isDefault: form.isDefault }
    if (form.id) await callApi(dishApi, ['updateSpec'], [dish.id, form.id, payload])
    else await callApi(dishApi, ['addSpec'], [dish.id, payload])
    showSpecForm.value = false
    notice.value = '规格已保存'
    await openSpecs(dish)
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = ''
  }
}

async function removeSpec(item: DishSpec) {
  const dish = selectedDish.value
  if (!dish || !window.confirm(`确认删除规格“${item.name}”？`) || saving.value) return
  saving.value = `spec-${item.id}`
  try {
    await callApi(dishApi, ['removeSpec', 'deleteSpec'], [dish.id, item.id])
    notice.value = '规格已删除'
    await openSpecs(dish)
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
    <PageHeader title="菜单管理" description="维护菜品、规格、库存和售卖状态。">
      <template #actions><BaseButton :disabled="!merchant" @click="openDish()">新增菜品</BaseButton></template>
    </PageHeader>

    <BentoGrid class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6 ops-grid">
      <BentoCard class="ops-card-lg ops-card" size="standard" tone="green" :interactive="false" title="菜品目录" description="点击规格管理每道菜的可选项。">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">菜</span></template>
        <LoadingState v-if="loading" label="正在载入菜单" />
        <ErrorState v-else-if="error" :message="error" @retry="load" />
        <EmptyState v-else-if="dishes.length === 0" title="暂无菜品" description="创建第一道菜后即可开始接单。" />
        <div v-else class="ops-list">
          <article v-for="dish in dishes" :key="dish.id" class="ops-row">
            <div class="ops-row-main">
              <div class="ops-row-title"><span>{{ dish.name }}</span><StatusBadge :status="dish.status" /></div>
              <div class="ops-row-meta">¥{{ money(dish.price) }} · 库存 {{ dish.stock }} · 已售 {{ dish.sales }}</div>
            </div>
            <div class="ops-actions">
              <button class="ops-icon-button" type="button" title="编辑菜品" aria-label="编辑菜品" @click="openDish(dish)">✎</button>
              <BaseButton size="sm" variant="outline" @click="openSpecs(dish)">规格 {{ dish.id === selectedDish?.id ? `(${specs.length})` : '' }}</BaseButton>
              <button class="ops-icon-button" type="button" title="删除菜品" aria-label="删除菜品" :disabled="saving === `remove-${dish.id}`" @click="removeDish(dish)">×</button>
            </div>
          </article>
        </div>
      </BentoCard>

      <BentoCard class="ops-card-wide ops-card" size="compact" tone="orange" :interactive="false" title="售卖状态">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">◉</span></template>
        <div v-if="dishes[0]" class="ops-row-title"><span>{{ dishes[0].name }}</span><StatusBadge :status="dishes[0].status" /></div>
        <p class="ops-muted">可快速切换上架、下架和售罄，状态立即对顾客端生效。</p>
        <div v-if="dishes[0]" class="ops-actions">
          <BaseButton size="sm" :disabled="saving !== ''" @click="changeDishStatus(dishes[0], 'ON_SALE')">上架</BaseButton>
          <BaseButton size="sm" variant="outline" :disabled="saving !== ''" @click="changeDishStatus(dishes[0], 'OFF_SALE')">下架</BaseButton>
          <BaseButton size="sm" variant="secondary" :disabled="saving !== ''" @click="changeDishStatus(dishes[0], 'SOLD_OUT')">售罄</BaseButton>
        </div>
      </BentoCard>

      <BentoCard class="ops-card-tall ops-card" size="compact" tone="cyan" :interactive="false" title="规格管理" :description="selectedDish ? `${selectedDish.name} 的可选规格` : '先选择一道菜'">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">◇</span></template>
        <BaseButton v-if="selectedDish" size="sm" variant="outline" @click="openSpec()">新增规格</BaseButton>
        <div v-if="selectedDish && specs.length" class="ops-list">
          <div v-for="spec in specs" :key="spec.id" class="ops-row">
            <div class="ops-row-main"><strong>{{ spec.name }}</strong><div class="ops-row-meta">{{ spec.groupName || '通用' }} · 加价 ¥{{ money(spec.priceOffset) }}</div></div>
            <div class="ops-actions"><button class="ops-icon-button" type="button" title="编辑规格" aria-label="编辑规格" @click="openSpec(spec)">✎</button><button class="ops-icon-button" type="button" title="删除规格" aria-label="删除规格" @click="removeSpec(spec)">×</button></div>
          </div>
        </div>
        <EmptyState v-else-if="selectedDish" title="暂无规格" description="规格可补充口味、份量或加料选项。" />
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="pink" :interactive="false" title="菜单规模">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">#</span></template>
        <strong style="font-size: 30px">{{ dishes.length }}</strong>
        <p class="ops-muted">道菜品，{{ categories.length }} 个分类</p>
      </BentoCard>
    </BentoGrid>

    <p v-if="notice" class="ops-notice" :class="{ 'ops-success': !notice.includes('失败') && !notice.includes('权限') && !notice.includes('冲突') && !notice.includes('失效') }" role="status">{{ notice }}</p>

    <OpsModal :open="showDishForm" :title="dishForm.id ? '编辑菜品' : '新增菜品'" @close="showDishForm = false">
      <form class="ops-form" @submit.prevent="saveDish">
        <div class="ops-form-grid">
          <BaseSelect v-model="dishForm.categoryId" label="所属分类" :options="categoryOptions()" required placeholder="选择分类" />
          <BaseInput v-model="dishForm.name" label="菜品名称" required placeholder="例如招牌炒饭" />
          <BaseInput v-model="dishForm.price" label="售价" type="number" :min="0" :step="0.01" required />
          <BaseInput v-model="dishForm.originalPrice" label="原价（选填）" type="number" :min="0" :step="0.01" />
          <BaseInput v-model="dishForm.stock" label="库存" type="number" :min="0" :step="1" required />
          <BaseInput v-model="dishForm.sortOrder" label="排序" type="number" :min="0" :step="1" />
        </div>
        <BaseInput v-model="dishForm.imageUrl" label="图片地址（选填）" type="url" placeholder="https://" />
        <BaseInput v-model="dishForm.description" label="菜品描述（选填）" placeholder="配料、口味或份量说明" />
        <div class="ops-actions"><BaseButton native-type="submit" :loading="saving === 'dish'">保存菜品</BaseButton><BaseButton variant="ghost" @click="showDishForm = false">取消</BaseButton></div>
      </form>
    </OpsModal>

    <OpsModal :open="showSpecForm" :title="specForm.id ? '编辑规格' : '新增规格'" @close="showSpecForm = false">
      <form class="ops-form" @submit.prevent="saveSpec">
        <div class="ops-form-grid">
          <BaseInput v-model="specForm.groupName" label="规格分组（选填）" placeholder="例如份量" />
          <BaseInput v-model="specForm.name" label="规格名称" required placeholder="例如大份" />
          <BaseInput v-model="specForm.priceOffset" label="加价" type="number" :step="0.01" />
          <BaseInput v-model="specForm.sortOrder" label="排序" type="number" :min="0" :step="1" />
        </div>
        <label class="ops-muted"><input v-model="specForm.isDefault" type="checkbox" /> 设为默认规格</label>
        <div class="ops-actions"><BaseButton native-type="submit" :loading="saving === 'spec'">保存规格</BaseButton><BaseButton variant="ghost" @click="showSpecForm = false">取消</BaseButton></div>
      </form>
    </OpsModal>
  </main>
</template>

<script setup lang="ts">
/** 管理员用户管理：分页查看账号，调整角色和状态并删除其他账号。 */
import { onMounted, ref } from 'vue'
import type { User, UserRole, UserStatus } from '@/types/domain'
import { userApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
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
import { callApi, currentUserId, dateTime, errorMessage, pageRecords, pageTotal, roleLabel, userStatusLabel } from '@/components/features/ops/opsUtils'
import '@/components/features/ops/opsStyles.css'

const auth = useAuthStore() as any
const users = ref<User[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const saving = ref(0)
const error = ref('')
const notice = ref('')
const editing = ref<User | null>(null)
const role = ref<UserRole>('CUSTOMER')
const userStatus = ref<UserStatus>('ACTIVE')

const roleOptions = [
  { label: '顾客', value: 'CUSTOMER' }, { label: '商家', value: 'MERCHANT' },
  { label: '骑手', value: 'RIDER' }, { label: '管理员', value: 'ADMIN' },
]
const statusOptions = [
  { label: '正常', value: 'ACTIVE' }, { label: '锁定', value: 'LOCKED' }, { label: '停用', value: 'DISABLED' },
]

function isSelf(user: User) {
  return user.id === currentUserId(auth)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await callApi<any>(userApi, ['list'], [{ page: page.value, size: 10 }])
    users.value = pageRecords<User>(result)
    total.value = pageTotal(result)
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

function edit(user: User) {
  editing.value = user
  role.value = user.role
  userStatus.value = user.status
  notice.value = ''
}

async function save() {
  const user = editing.value
  if (!user || saving.value) return
  saving.value = user.id
  notice.value = ''
  try {
    await callApi(userApi, ['update', 'adminUpdate'], [user.id, { role: role.value, status: userStatus.value }])
    editing.value = null
    notice.value = '用户角色和状态已更新'
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = 0
  }
}

async function remove(user: User) {
  if (isSelf(user) || saving.value || !window.confirm(`确认删除账号“${user.username}”？`)) return
  saving.value = user.id
  notice.value = ''
  try {
    await callApi(userApi, ['remove', 'adminDelete'], [user.id])
    notice.value = '账号已删除'
    await load()
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    saving.value = 0
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
    <PageHeader title="用户管理" description="管理账号角色和可用状态，当前登录账号不能删除自己。">
      <template #actions><BaseButton variant="outline" :loading="loading" @click="load">刷新用户</BaseButton></template>
    </PageHeader>

    <BentoGrid class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6 ops-grid">
      <BentoCard class="ops-card-lg ops-card" size="standard" tone="neutral" :interactive="false" title="账号列表" description="角色变更会影响用户可访问的运营端。">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">人</span></template>
        <LoadingState v-if="loading" label="正在载入用户" />
        <ErrorState v-else-if="error" :message="error" @retry="load" />
        <EmptyState v-else-if="users.length === 0" title="暂无用户" description="注册用户会显示在这里。" />
        <div v-else class="ops-list">
          <article v-for="user in users" :key="user.id" class="ops-row">
            <div class="ops-row-main">
              <div class="ops-row-title"><span>{{ user.nickname || user.username }}</span><StatusBadge :status="user.status" :label="userStatusLabel(user.status)" /></div>
              <div class="ops-row-meta">{{ user.username }} · {{ roleLabel(user.role) }} · {{ user.phone || '未绑定手机' }} · 注册 {{ dateTime(user.createdAt) }}</div>
            </div>
            <div class="ops-actions">
              <BaseButton size="sm" variant="outline" @click="edit(user)">编辑</BaseButton>
              <BaseButton size="sm" variant="danger" :disabled="isSelf(user)" :loading="saving === user.id" @click="remove(user)">删除</BaseButton>
            </div>
          </article>
        </div>
        <OpsPager :page="page" :pages="Math.ceil(total / 10)" :total="total" :busy="loading" @prev="changePage(page - 1)" @next="changePage(page + 1)" />
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="green" :interactive="false" title="账号总数">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">#</span></template>
        <strong style="font-size: 34px">{{ total }}</strong>
        <p class="ops-muted">个注册账号</p>
      </BentoCard>

      <BentoCard class="ops-card-sm ops-card" size="compact" tone="cyan" :interactive="false" title="本页账号">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">▤</span></template>
        <strong style="font-size: 34px">{{ users.length }}</strong>
        <p class="ops-muted">条账号记录</p>
      </BentoCard>

      <BentoCard class="ops-card-wide ops-card" size="compact" tone="orange" :interactive="false" title="安全规则">
        <template #icon><span class="ops-icon text-3xl" aria-hidden="true">!</span></template>
        <p class="ops-muted">删除操作不可撤销，管理员不能删除当前登录账号。锁定或停用账号会立即失去登录态。</p>
      </BentoCard>
    </BentoGrid>

    <p v-if="notice" class="ops-notice" :class="{ 'ops-success': notice.includes('已') }" role="status">{{ notice }}</p>

    <OpsModal :open="Boolean(editing)" title="编辑用户" @close="editing = null">
      <div v-if="editing" class="ops-form">
        <p class="ops-muted">{{ editing.username }} · 创建于 {{ dateTime(editing.createdAt) }}</p>
        <BaseSelect v-model="role" label="用户角色" :options="roleOptions" required />
        <BaseSelect v-model="userStatus" label="账号状态" :options="statusOptions" required />
        <div class="ops-actions"><BaseButton :loading="saving === editing.id" @click="save">保存变更</BaseButton><BaseButton variant="ghost" @click="editing = null">取消</BaseButton></div>
      </div>
    </OpsModal>
  </main>
</template>

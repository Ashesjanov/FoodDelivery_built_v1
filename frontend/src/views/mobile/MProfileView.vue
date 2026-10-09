<script setup lang="ts">
/** 移动端个人资料页：与电脑端内容一致，修改资料、密码并提供安全的账号注销流程。 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { User } from '@/types/domain'
import { userApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { errorMessage, unwrap } from '@/components/features/customer/customerUtils'

const router = useRouter()
const authStore: any = useAuthStore()
const user = ref<User | null>(null)
const loading = ref(true)
const error = ref('')
const notice = ref('')
const savingProfile = ref(false)
const savingPassword = ref(false)
const deleting = ref(false)
const showDelete = ref(false)
const profile = ref({ phone: '', nickname: '' })
const password = ref({ currentPassword: '', newPassword: '' })

async function load() {
  loading.value = true
  error.value = ''
  try {
    user.value = unwrap<User>(await userApi.me())
    profile.value = { phone: user.value.phone ?? '', nickname: user.value.nickname ?? '' }
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (savingProfile.value) return
  savingProfile.value = true
  notice.value = ''
  try {
    user.value = unwrap<User>(await userApi.updateMe({ phone: profile.value.phone.trim() || undefined, nickname: profile.value.nickname.trim() || undefined }))
    authStore.setUser?.(user.value)
    notice.value = '资料已更新'
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    savingProfile.value = false
  }
}

async function changePassword() {
  if (savingPassword.value || password.value.newPassword.length < 8) return
  savingPassword.value = true
  notice.value = ''
  try {
    await userApi.changePassword({ currentPassword: password.value.currentPassword, newPassword: password.value.newPassword })
    password.value = { currentPassword: '', newPassword: '' }
    notice.value = '密码已更新，请妥善保管'
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    savingPassword.value = false
  }
}

async function deleteAccount() {
  if (deleting.value) return
  deleting.value = true
  try {
    await userApi.deleteMe()
    authStore.logout?.()
    await router.replace('/m/login')
  } catch (cause) {
    notice.value = errorMessage(cause)
  } finally {
    deleting.value = false
    showDelete.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="m-page">
    <header class="m-page-head">
      <h1 class="m-title">个人资料</h1>
      <p class="m-sub">管理联系方式、登录密码和账号安全。</p>
    </header>

    <LoadingState v-if="loading" label="正在读取你的资料" />
    <ErrorState v-else-if="error && !user" :message="error" action-label="重新加载" @retry="load" />
    <div v-else class="m-stack">
      <section class="m-card m-card--dark">
        <div class="m-avatar group">{{ (user?.nickname || user?.username || '客').slice(0, 1) }}</div>
        <p class="m-label" style="margin-top: 16px">{{ user?.role === 'CUSTOMER' ? '食客账户' : '平台账户' }}</p>
        <h2 class="m-title" style="font-size: 26px">{{ user?.nickname || user?.username }}</h2>
        <p class="m-muted" style="margin-top: 6px; color: #bec6bd">@{{ user?.username }}</p>
        <div class="m-row" style="margin-top: 16px; color: #aeb8ad; font-size: 13px">
          <StatusBadge :status="user?.status || 'ACTIVE'" />
          <span>加入于 {{ user?.createdAt?.slice(0, 10) || '今天' }}</span>
        </div>
      </section>

      <section class="m-card m-card--fresh">
        <p class="m-label" style="color: #a26932">基本信息</p>
        <h2 class="m-subtitle">联系方式</h2>
        <div class="m-form" style="margin-top: 16px">
          <BaseInput v-model="profile.nickname" label="昵称" placeholder="想让骑手怎么称呼你" />
          <BaseInput v-model="profile.phone" label="手机号" type="tel" placeholder="用于接收配送通知" />
        </div>
        <div style="margin-top: 16px">
          <BaseButton type="primary" :loading="savingProfile" @click="saveProfile">保存资料</BaseButton>
        </div>
        <p v-if="notice" class="m-notice" role="status">{{ notice }}</p>
      </section>

      <section class="m-card">
        <p class="m-label" style="color: #a26932">安全设置</p>
        <h2 class="m-subtitle">修改密码</h2>
        <div class="m-form" style="margin-top: 16px">
          <BaseInput v-model="password.currentPassword" label="当前密码" type="password" autocomplete="current-password" />
          <BaseInput v-model="password.newPassword" label="新密码" type="password" placeholder="至少 8 位" autocomplete="new-password" />
        </div>
        <div style="margin-top: 16px">
          <BaseButton type="secondary" :loading="savingPassword" :disabled="!password.currentPassword || password.newPassword.length < 8" @click="changePassword">更新密码</BaseButton>
        </div>
      </section>

      <section class="m-card m-card--warm">
        <p class="m-label" style="color: #a26932">登录状态</p>
        <h2 class="m-subtitle">当前会话</h2>
        <p class="m-info">当前设备已通过安全验证。退出后购物车会保留在账户中。</p>
        <div style="margin-top: 16px">
          <BaseButton type="ghost" @click="authStore.logout?.(); router.push('/m/login')">退出登录</BaseButton>
        </div>
      </section>

      <section class="m-card m-card--accent">
        <p class="m-label" style="color: #a26932">账户管理</p>
        <h2 class="m-subtitle">注销账户</h2>
        <p class="m-info">注销会永久移除资料与地址，历史订单将按平台规则保留。</p>
        <div style="margin-top: 16px">
          <BaseButton type="danger" @click="showDelete = true">注销账户</BaseButton>
        </div>
      </section>
    </div>

    <div v-if="showDelete" class="m-sheet-backdrop" role="presentation" @click.self="showDelete = false">
      <section class="m-sheet" role="dialog" aria-modal="true" aria-label="确认注销账户">
        <div class="m-sheet-head">
          <h2 class="m-sheet-title">确定注销账户？</h2>
          <button class="m-sheet-close" type="button" aria-label="关闭" @click="showDelete = false">×</button>
        </div>
        <p class="m-muted">此操作无法撤销。请确认你已经不需要保留这些资料。</p>
        <div class="m-actions" style="margin-top: 24px">
          <BaseButton type="secondary" @click="showDelete = false">暂不注销</BaseButton>
          <BaseButton type="danger" :loading="deleting" @click="deleteAccount">确认注销</BaseButton>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.m-page-head { margin-bottom: 16px; }
.m-sub { margin: 8px 0 0; color: #69746d; line-height: 1.7; font-size: 14px; }
.m-avatar { display: grid; place-items: center; width: 72px; aspect-ratio: 1; border: 1px solid #dcb45f; border-radius: 50%; color: #f2c66e; font-family: Georgia, 'Songti SC', serif; font-size: 32px; transition: transform 0.3s ease; }
.m-avatar:hover { transform: rotate(-8deg) scale(1.06); }
.m-info { color: #726b61; line-height: 1.8; margin: 10px 0 0; }
</style>

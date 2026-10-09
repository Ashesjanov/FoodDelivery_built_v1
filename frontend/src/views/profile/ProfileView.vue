<script setup lang="ts">
/** 个人资料页：修改资料、密码并提供安全的账号注销流程。 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { User } from '@/types/domain'
import { userApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import ErrorState from '@/components/base/ErrorState.vue'
import LoadingState from '@/components/base/LoadingState.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import BentoGrid from '@/components/bento/BentoGrid.vue'
import BentoCard from '@/components/bento/BentoCard.vue'
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
    await router.replace('/login')
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
  <main class="profile-page">
    <PageHeader title="个人资料" description="管理联系方式、登录密码和账号安全。" back-to="/" />
    <LoadingState v-if="loading" label="正在读取你的资料" />
    <ErrorState v-else-if="error && !user" :message="error" action-label="重新加载" @retry="load" />
    <BentoGrid v-else class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
      <BentoCard class="identity-card" size="large" tone="dark" aspect="1 / 1">
        <div class="avatar group">{{ (user?.nickname || user?.username || '客').slice(0, 1) }}</div>
        <div class="identity-copy"><p class="section-label">{{ user?.role === 'CUSTOMER' ? '食客账户' : '平台账户' }}</p><h1>{{ user?.nickname || user?.username }}</h1><p>@{{ user?.username }}</p></div>
        <div class="identity-foot"><StatusBadge :status="user?.status || 'ACTIVE'" /><span>加入于 {{ user?.createdAt?.slice(0, 10) || '今天' }}</span></div>
      </BentoCard>

      <BentoCard class="profile-card" size="medium" tone="fresh" aspect="2 / 1"><p class="section-label">基本信息</p><h2>联系方式</h2><BaseInput v-model="profile.nickname" label="昵称" placeholder="想让骑手怎么称呼你" /><BaseInput v-model="profile.phone" label="手机号" type="tel" placeholder="用于接收配送通知" /><BaseButton type="primary" :loading="savingProfile" @click="saveProfile">保存资料</BaseButton><p v-if="notice" class="notice">{{ notice }}</p></BentoCard>

      <BentoCard class="password-card" size="medium" tone="default" aspect="2 / 1"><p class="section-label">安全设置</p><h2>修改密码</h2><BaseInput v-model="password.currentPassword" label="当前密码" type="password" autocomplete="current-password" /><BaseInput v-model="password.newPassword" label="新密码" type="password" placeholder="至少 8 位" autocomplete="new-password" /><BaseButton type="secondary" :loading="savingPassword" :disabled="!password.currentPassword || password.newPassword.length < 8" @click="changePassword">更新密码</BaseButton></BentoCard>

      <BentoCard class="account-card" size="small" tone="warm" aspect="1 / 1"><p class="section-label">登录状态</p><h2>当前会话</h2><p>当前设备已通过安全验证。退出后购物车会保留在账户中。</p><BaseButton type="ghost" @click="authStore.logout?.(); router.push('/login')">退出登录</BaseButton></BentoCard>

      <BentoCard class="danger-card" size="small" tone="accent" aspect="1 / 1"><p class="section-label">账户管理</p><h2>注销账户</h2><p>注销会永久移除资料与地址，历史订单将按平台规则保留。</p><BaseButton type="danger" @click="showDelete = true">注销账户</BaseButton></BentoCard>
    </BentoGrid>

    <div v-if="showDelete" class="modal-backdrop" role="presentation" @click.self="showDelete = false">
      <section class="delete-modal" role="dialog" aria-modal="true" aria-label="确认注销账户"><h2>确定注销账户？</h2><p>此操作无法撤销。请确认你已经不需要保留这些资料。</p><div class="modal-actions"><BaseButton type="secondary" @click="showDelete = false">暂不注销</BaseButton><BaseButton type="danger" :loading="deleting" @click="deleteAccount">确认注销</BaseButton></div></section>
    </div>
  </main>
</template>

<style scoped>
.profile-page { min-height: 100vh; padding: clamp(24px, 5vw, 76px); background: #f3f3ed; color: #242822; font-family: 'PingFang SC', sans-serif; }.section-label { color: #a26932; font-size: 13px; margin: 0 0 14px; } h1, h2 { font-family: Georgia, 'Songti SC', serif; font-weight: 500; margin: 0; } h1 { font-size: clamp(38px, 5vw, 60px); } h2 { font-size: 25px; }
.identity-card, .profile-card, .password-card, .account-card, .danger-card { padding: clamp(22px, 2.8vw, 38px); }.identity-card { color: #f4f1e9; display: flex; flex-direction: column; justify-content: space-between; }.avatar { width: 84px; aspect-ratio: 1; display: grid; place-items: center; border: 1px solid #dcb45f; border-radius: 50%; color: #f2c66e; font-family: Georgia, serif; font-size: 38px; transition: transform .3s ease; }.group:hover .avatar { transform: rotate(-8deg) scale(1.08); }.identity-copy p { color: #bec6bd; }.identity-foot { display: flex; justify-content: space-between; align-items: center; gap: 16px; color: #aeb8ad; }
.profile-card > *, .password-card > *, .account-card > *, .danger-card > * { margin-top: 16px; }.notice { color: #9a5e23; margin-bottom: 0; }.account-card p, .danger-card p { color: #726b61; line-height: 1.8; }
.modal-backdrop { position: fixed; inset: 0; z-index: 20; display: grid; place-items: center; padding: 18px; background: rgba(23,28,24,.56); }.delete-modal { width: min(520px, 100%); padding: clamp(24px, 4vw, 42px); border-radius: 24px; background: #fbfaf6; box-shadow: 0 30px 70px rgba(20,28,23,.25); }.delete-modal p { color: #726e66; line-height: 1.8; }.modal-actions { display: flex; justify-content: flex-end; gap: 16px; }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { transition-duration: .01ms !important; animation-duration: .01ms !important; } }
</style>

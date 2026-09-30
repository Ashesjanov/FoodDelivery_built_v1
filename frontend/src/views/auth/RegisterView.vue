<script setup lang="ts">
/** 注册页：注册成功后直接写入登录状态并返回首页。 */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { RegisterRequest } from '@/types/domain'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BentoGrid from '@/components/bento/BentoGrid.vue'
import BentoCard from '@/components/bento/BentoCard.vue'
import { errorMessage } from '@/components/features/customer/customerUtils'

const router = useRouter()
const authStore: any = useAuthStore()
const form = ref({ username: '', password: '', phone: '', nickname: '' })
const loading = ref(false)
const error = ref('')
const canSubmit = computed(() => form.value.username.trim().length >= 3 && form.value.password.length >= 8 && !loading.value)

async function submit() {
  if (!canSubmit.value) return
  loading.value = true
  error.value = ''
  try {
    const payload: RegisterRequest = { ...form.value, username: form.value.username.trim(), nickname: form.value.nickname.trim() }
    await authStore.register(payload)
    await router.replace('/')
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="register-page">
    <BentoGrid :columns="2" class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
      <BentoCard class="register-card" size="large" tone="fresh" aspect="auto">
        <h1>从今天起，<br />把喜欢的味道带回家</h1><p class="lead">创建账户后保存地址、管理订单，并获得专属优惠提醒。</p>
        <div class="quote">“好味道值得被分享，也值得准时抵达。”</div>
      </BentoCard>
      <BentoCard class="form-card" size="medium" tone="default" aspect="auto">
        <div class="card-heading"><div><h2>创建账户</h2></div><RouterLink class="text-link" to="/login">已有账户</RouterLink></div>
        <form class="stack-form" @submit.prevent="submit">
          <BaseInput v-model="form.username" label="用户名" placeholder="3-32 位字母、数字或下划线" autocomplete="username" />
          <BaseInput v-model="form.password" label="密码" type="password" placeholder="至少 8 位" autocomplete="new-password" />
          <BaseInput v-model="form.phone" label="手机号（选填）" type="tel" placeholder="用于接收配送通知" />
          <BaseInput v-model="form.nickname" label="昵称（选填）" placeholder="想让骑手怎么称呼你" />
          <p v-if="error" class="form-error" role="alert">{{ error }}</p>
          <BaseButton type="primary" native-type="submit" :loading="loading" :disabled="!canSubmit">{{ loading ? '创建中' : '创建账户' }}</BaseButton>
        </form>
      </BentoCard>
    </BentoGrid>
  </main>
</template>

<style scoped>
.register-page { min-height: 100%; padding: clamp(16px, 2vw, 28px); background: #f2f5f0; color: #1b2a24; font-family: 'PingFang SC', sans-serif; }
.register-card { padding: clamp(26px, 4vw, 56px); min-height: 520px; display: flex; flex-direction: column; justify-content: space-between; background: #dcebdd; color: #1e3a2c; }
.form-card { padding: clamp(24px, 3vw, 42px); }
.section-label { font-size: 13px; letter-spacing: 0; opacity: .72; margin-bottom: 10px; }
h1, h2 { font-family: Georgia, 'Songti SC', serif; font-weight: 500; margin: 0; } h1 { font-size: clamp(34px, 3vw, 50px); line-height: 1.08; } h2 { font-size: 30px; }
.lead { max-width: 33ch; line-height: 1.8; color: #4f6a58; margin: 18px 0; }
.quote { font-family: Georgia, 'Songti SC', serif; color: #5d8568; border-top: 1px solid rgba(33,78,51,.2); padding-top: 18px; }
.card-heading { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }.text-link { color: #bb7a2d; text-decoration: none; font-size: 14px; border-bottom: 1px solid #e5c69f; }
.stack-form { display: grid; gap: 16px; margin-top: 28px; }.form-error { color: #b64f41; font-size: 13px; margin: 0; }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { transition-duration: .01ms !important; animation-duration: .01ms !important; } }
</style>

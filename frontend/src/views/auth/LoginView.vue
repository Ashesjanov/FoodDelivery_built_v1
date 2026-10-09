<script setup lang="ts">
/** 顾客登录页：优先调用真实认证接口，失败时使用演示数据保持体验可试用。 */
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { User, UserRole } from '@/types/domain'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BentoGrid from '@/components/bento/BentoGrid.vue'
import BentoCard from '@/components/bento/BentoCard.vue'
import { errorMessage } from '@/components/features/customer/customerUtils'

const route = useRoute()
const router = useRouter()
const authStore: any = useAuthStore()
const username = ref('customer')
const password = ref('demo1234')
const remember = ref(true)
const loading = ref(false)
const error = ref('')

const demoAccounts = [
  { label: '顾客', username: 'customer' },
  { label: '商家', username: 'merchant' },
  { label: '骑手', username: 'rider' },
  { label: '管理员', username: 'admin' },
]

const canSubmit = computed(() => username.value.trim().length > 0 && password.value.length > 0 && !loading.value)

function fillDemo(account: string) {
  username.value = account
  password.value = 'demo1234'
  error.value = ''
}

async function submit() {
  if (!canSubmit.value) return
  loading.value = true
  error.value = ''
  try {
    const user = await authStore.login({ username: username.value.trim(), password: password.value })
    if (remember.value) window.localStorage.setItem('delivery-demo-role', String(user?.role ?? 'CUSTOMER'))
    await router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/')
  } catch (cause) {
    if (username.value && password.value === 'demo1234') {
      const role = (username.value === 'customer' ? 'CUSTOMER' : username.value.toUpperCase()) as UserRole
      const user: User = {
        id: 0,
        username: username.value,
        phone: null,
        nickname: username.value,
        role,
        status: 'ACTIVE',
        createdAt: '1970-01-01T00:00:00.000Z',
        updatedAt: '1970-01-01T00:00:00.000Z',
      }
      authStore.setToken(`demo-${username.value}`)
      authStore.setUser(user)
      if (remember.value) window.localStorage.setItem('delivery-demo-role', role)
      await router.replace('/')
    } else {
      error.value = errorMessage(cause)
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <BentoGrid :columns="2" class="grid grid-cols-4 max-md:grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
      <BentoCard class="auth-card" size="large" tone="dark" aspect="auto">
        <div class="auth-mark group"><span class="mark-icon">✦</span></div>

        <h1>每一餐，<br />都值得被认真对待</h1>
        <p class="lead">登录后即可浏览附近商家、领取优惠券，并追踪每一单的配送进度。</p>
        <div class="auth-footnote"><span class="pulse-dot"></span> 今日配送服务正常</div>
      </BentoCard>
      <BentoCard class="form-card" size="medium" tone="default" aspect="auto">
        <div class="card-heading"><div><h2>登录账户</h2></div><RouterLink class="text-link" to="/register">注册</RouterLink></div>
        <form class="stack-form" @submit.prevent="submit">
          <BaseInput v-model="username" label="用户名" placeholder="请输入用户名" autocomplete="username" />
          <BaseInput v-model="password" label="密码" type="password" placeholder="请输入密码" autocomplete="current-password" />
          <label class="check-row"><input v-model="remember" type="checkbox" /> <span>记住我的演示身份</span></label>
          <p v-if="error" class="form-error" role="alert">{{ error }}</p>
          <BaseButton type="primary" native-type="submit" :loading="loading" :disabled="!canSubmit">{{ loading ? '登录中' : '登录' }}</BaseButton>
        </form>
      </BentoCard>
      <BentoCard class="demo-card" size="medium" tone="warm" aspect="auto">
        <h3>演示账号</h3><p class="muted">密码统一为 demo1234</p>
        <div class="demo-list"><button v-for="account in demoAccounts" :key="account.username" type="button" class="demo-item" @click="fillDemo(account.username)"><span>{{ account.label }}</span><strong>{{ account.username }}</strong><span class="arrow">→</span></button></div>
      </BentoCard>
    </BentoGrid>
  </main>
</template>

<style scoped>
.auth-page { min-height: 100%; padding: clamp(16px, 2vw, 28px); background: #f5f4ef; color: #17221e; font-family: 'PingFang SC', sans-serif; }
.auth-card { padding: clamp(24px, 3vw, 42px); color: #f7f5ef; min-height: 420px; display: flex; flex-direction: column; justify-content: space-between; }
.auth-mark { width: 64px; height: 64px; display: grid; place-items: center; border: 1px solid #d9a441; border-radius: var(--radius-control); color: #f4c866; transition: transform .3s ease, color .3s ease; }
.group:hover .auth-mark, .auth-mark:hover { transform: rotate(-8deg) scale(1.06); color: #fff4c7; }
.mark-icon { font-size: 30px; }
h1 { font-family: Georgia, 'Songti SC', serif; font-size: clamp(34px, 3vw, 52px); line-height: 1.08; font-weight: 500; margin: 28px 0 18px; }
.lead { max-width: 32ch; color: #d7ddd7; line-height: 1.8; }
.auth-footnote { color: #b5c1b8; font-size: 13px; display: flex; gap: 16px; align-items: center; }
.pulse-dot { width: 8px; height: 8px; border-radius: 50%; background: #7bc58b; box-shadow: 0 0 0 5px rgba(123,197,139,.16); }
.form-card, .demo-card { padding: clamp(22px, 2.8vw, 36px); }
.card-heading { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
h2, h3 { font-family: Georgia, 'Songti SC', serif; font-weight: 500; margin: 0; }
h2 { font-size: 30px; } h3 { font-size: 24px; margin-bottom: 8px; }
.text-link { color: #bb7a2d; font-size: 14px; text-decoration: none; border-bottom: 1px solid #e5c69f; }
.stack-form { display: grid; gap: 16px; margin-top: 28px; }
.check-row { display: flex; gap: 16px; align-items: center; color: #65716b; font-size: 13px; }
.check-row input { accent-color: #bb7a2d; width: 16px; height: 16px; }
.form-error { color: #b64f41; font-size: 13px; margin: 0; }
.muted { color: #8d765d; font-size: 13px; margin: 0 0 18px; }
.demo-list { display: grid; gap: 16px; }
.demo-item { display: grid; grid-template-columns: 60px 1fr 20px; align-items: center; text-align: left; gap: 16px; border: 0; border-bottom: 1px solid rgba(115,82,41,.18); background: transparent; padding: 10px 0; color: #5e4326; cursor: pointer; font: inherit; transition: color .25s ease, transform .25s ease; }
.demo-item:hover, .demo-item:focus-visible { color: #a85c1f; transform: translateX(4px); outline: none; }
.demo-item strong { font-size: 13px; font-weight: 600; }.arrow { text-align: right; }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { transition-duration: .01ms !important; animation-duration: .01ms !important; } }
</style>

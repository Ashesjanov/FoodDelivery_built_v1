<script setup lang="ts">
/** 移动端顾客登录页：与电脑端登录内容一致，优先真实接口，失败时保留演示身份体验。 */
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { User, UserRole } from '@/types/domain'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
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
    await router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/m/home')
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
      await router.replace('/m/home')
    } else {
      error.value = errorMessage(cause)
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="m-page">
    <div class="m-stack">
      <section class="m-card m-card--dark">
        <span class="m-mark" aria-hidden="true">✦</span>
        <h1 class="m-title" style="margin-top: 20px">每一餐，<br />都值得被认真对待</h1>
        <p class="m-lead">登录后即可浏览附近商家、领取优惠券，并追踪每一单的配送进度。</p>
        <p class="m-muted" style="margin-top: 16px; color: #b5c1b8"><span class="m-pulse" aria-hidden="true"></span> 今日配送服务正常</p>
      </section>

      <section class="m-card">
        <div class="m-row">
          <h2 class="m-subtitle">登录账户</h2>
          <RouterLink class="m-link" to="/m/register">注册</RouterLink>
        </div>
        <form class="m-form" style="margin-top: 16px" @submit.prevent="submit">
          <BaseInput v-model="username" label="用户名" placeholder="请输入用户名" autocomplete="username" />
          <BaseInput v-model="password" label="密码" type="password" placeholder="请输入密码" autocomplete="current-password" />
          <label class="m-check"><input v-model="remember" type="checkbox" /> <span>记住我的演示身份</span></label>
          <p v-if="error" class="m-notice m-notice--error" role="alert">{{ error }}</p>
          <BaseButton type="primary" native-type="submit" :loading="loading" :disabled="!canSubmit" block>{{ loading ? '登录中' : '登录' }}</BaseButton>
        </form>
      </section>

      <section class="m-card m-card--warm">
        <h2 class="m-subtitle">演示账号</h2>
        <p class="m-muted" style="margin-top: 8px">密码统一为 demo1234</p>
        <div class="m-stack" style="margin-top: 16px">
          <button v-for="account in demoAccounts" :key="account.username" type="button" class="m-demo-item" @click="fillDemo(account.username)">
            <span>{{ account.label }}</span><strong>{{ account.username }}</strong><span aria-hidden="true">→</span>
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.m-mark { display: grid; place-items: center; width: 56px; height: 56px; border: 1px solid #d9a441; border-radius: var(--radius-control); color: #f4c866; font-size: 26px; }
.m-pulse { display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: #7bc58b; box-shadow: 0 0 0 5px rgba(123, 197, 139, 0.16); }
.m-link { color: #bb7a2d; font-size: 14px; text-decoration: none; border-bottom: 1px solid #e5c69f; padding-bottom: 2px; }
.m-check { display: flex; align-items: center; gap: 16px; color: #65716b; font-size: 13px; }
.m-check input { width: 18px; height: 18px; accent-color: #bb7a2d; }
.m-demo-item { display: grid; grid-template-columns: 60px 1fr 20px; align-items: center; text-align: left; gap: 16px; border: 0; border-bottom: 1px solid rgba(115, 82, 41, 0.18); background: transparent; padding: 14px 0; color: #5e4326; font: inherit; cursor: pointer; }
.m-demo-item:hover, .m-demo-item:focus-visible { color: #a85c1f; outline: none; }
.m-demo-item strong { font-size: 13px; font-weight: 600; text-align: right; }
</style>

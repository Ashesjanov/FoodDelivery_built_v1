<script setup lang="ts">
/** 移动端注册页：与电脑端注册内容一致，成功后直接进入登录状态。 */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { RegisterRequest } from '@/types/domain'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
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
    await router.replace('/m/home')
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="m-page">
    <div class="m-stack">
      <section class="m-card m-card--fresh">
        <h1 class="m-title">从今天起，<br />把喜欢的味道带回家</h1>
        <p class="m-lead" style="color: #4f6a58">创建账户后保存地址、管理订单，并获得专属优惠提醒。</p>
        <p class="m-quote">“好味道值得被分享，也值得准时抵达。”</p>
      </section>

      <section class="m-card">
        <div class="m-row">
          <h2 class="m-subtitle">创建账户</h2>
          <RouterLink class="m-link" to="/m/login">已有账户</RouterLink>
        </div>
        <form class="m-form" style="margin-top: 16px" @submit.prevent="submit">
          <BaseInput v-model="form.username" label="用户名" placeholder="3-32 位字母、数字或下划线" autocomplete="username" />
          <BaseInput v-model="form.password" label="密码" type="password" placeholder="至少 8 位" autocomplete="new-password" />
          <BaseInput v-model="form.phone" label="手机号（选填）" type="tel" placeholder="用于接收配送通知" />
          <BaseInput v-model="form.nickname" label="昵称（选填）" placeholder="想让骑手怎么称呼你" />
          <p v-if="error" class="m-notice m-notice--error" role="alert">{{ error }}</p>
          <BaseButton type="primary" native-type="submit" :loading="loading" :disabled="!canSubmit" block>{{ loading ? '创建中' : '创建账户' }}</BaseButton>
        </form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.m-lead { color: #4f6a58; }
.m-quote { margin-top: 18px; font-family: Georgia, 'Songti SC', serif; color: #5d8568; border-top: 1px solid rgba(33, 78, 51, 0.2); padding-top: 18px; }
.m-link { color: #bb7a2d; font-size: 14px; text-decoration: none; border-bottom: 1px solid #e5c69f; padding-bottom: 2px; }
</style>

import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi, userApi } from '@/api'
import { ApiError, getToken, setToken as persistToken } from '@/api/http'
import type {
  AuthSession,
  LoginRequest,
  RegisterRequest,
  User,
  UserRole,
  UserProfileUpdateRequest,
} from '@/types/domain'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(getToken())
  const loading = ref(false)
  const error = ref<string | null>(null)
  const initialized = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value))
  const role = computed<UserRole | null>(() => user.value?.role ?? null)

  function setToken(nextToken: string | null): void {
    token.value = nextToken
    persistToken(nextToken)
    initialized.value = true
  }

  function setUser(nextUser: User | null): void {
    user.value = nextUser
    initialized.value = true
  }

  function applySession(session: AuthSession): User {
    setToken(session.token)
    setUser(session.user)
    return session.user
  }

  async function initialize(): Promise<void> {
    if (initialized.value) return
    initialized.value = true
    if (!token.value) return
    if (token.value.startsWith('demo-')) {
      const username = token.value.slice('demo-'.length) || 'demo'
      const storedRole = window.localStorage.getItem('delivery-demo-role') as UserRole | null
      setUser({
        id: 0,
        username,
        phone: null,
        nickname: username,
        role: storedRole && ['CUSTOMER', 'ADMIN', 'MERCHANT', 'RIDER'].includes(storedRole) ? storedRole : 'CUSTOMER',
        status: 'ACTIVE',
        createdAt: '1970-01-01T00:00:00.000Z',
        updatedAt: '1970-01-01T00:00:00.000Z',
      })
      return
    }
    try {
      setUser(await authApi.me())
    } catch (cause) {
      if (cause instanceof ApiError && (cause.status === 401 || cause.code === 401)) logout()
    }
  }

  async function login(payload: LoginRequest): Promise<User> {
    loading.value = true
    error.value = null
    try {
      return applySession(await authApi.login(payload))
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : '登录失败'
      throw cause
    } finally {
      loading.value = false
    }
  }

  async function register(payload: RegisterRequest): Promise<User> {
    loading.value = true
    error.value = null
    try {
      return applySession(await authApi.register(payload))
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : '注册失败'
      throw cause
    } finally {
      loading.value = false
    }
  }

  async function refreshCurrentUser(): Promise<User | null> {
    if (!token.value) return null
    user.value = await userApi.me()
    return user.value
  }

  async function updateProfile(payload: UserProfileUpdateRequest): Promise<User> {
    user.value = await userApi.updateMe(payload)
    return user.value
  }

  function logout(): void {
    setToken(null)
    setUser(null)
  }

  function hasRole(...roles: UserRole[]): boolean {
    return user.value !== null && roles.includes(user.value.role)
  }

  return {
    user,
    token,
    loading,
    error,
    initialized,
    isAuthenticated,
    role,
    setToken,
    setUser,
    initialize,
    login,
    register,
    refreshCurrentUser,
    updateProfile,
    logout,
    hasRole,
  }
})

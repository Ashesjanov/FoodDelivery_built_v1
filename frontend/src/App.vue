<script setup lang="ts">
/** 应用外壳：根据路由元信息切换认证布局和应用布局。 */
import { computed, defineAsyncComponent } from 'vue'
import { useRoute } from 'vue-router'

const AppShellLayout = defineAsyncComponent(() => import('@/layouts/AppShellLayout.vue'))
const AuthLayout = defineAsyncComponent(() => import('@/layouts/AuthLayout.vue'))

const route = useRoute()
const layout = computed(() => (route.meta.layout === 'auth' ? AuthLayout : AppShellLayout))
</script>

<template>
  <a
    href="#main-content"
    class="sr-only rounded-xl focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:bg-white focus:px-4 focus:py-3 focus:text-sm focus:ring-2 focus:ring-orange-500"
  >
    跳到主要内容
  </a>
  <component :is="layout" />
</template>

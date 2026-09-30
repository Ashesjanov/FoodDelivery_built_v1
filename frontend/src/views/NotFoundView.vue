<script setup lang="ts">
import { useRouter } from 'vue-router'
import EmptyState from '@/components/base/EmptyState.vue'
import BaseButton from '@/components/base/BaseButton.vue'

const router = useRouter()
</script>

<template>
  <div class="mx-auto flex max-w-3xl flex-col gap-6">
    <EmptyState title="页面走丢了" description="这个地址不存在，或者内容已经移动。" action-label="返回首页" @action="router.push('/home')">
      <template #actions>
        <BaseButton type="primary" @click="router.push('/home')">返回首页</BaseButton>
        <BaseButton type="outline" @click="router.back()">返回上一页</BaseButton>
      </template>
    </EmptyState>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ label?: string; description?: string }>(), {
  label: '正在加载',
  description: '请稍候，正在获取最新数据',
})
</script>

<template>
  <section class="flex w-full flex-col items-center justify-center gap-4 p-4 text-center md:p-6" role="status">
    <span
      class="h-10 w-10 animate-spin rounded-full border-4 border-orange-200 border-r-orange-500 motion-reduce:animate-none"
      aria-hidden="true"
    />
    <h2 class="text-lg font-semibold text-zinc-950">{{ label }}</h2>
    <p v-if="description" class="text-sm text-zinc-600">{{ description }}</p>
  </section>
</template>

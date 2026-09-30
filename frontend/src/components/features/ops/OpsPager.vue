<script setup lang="ts">
/** 分页控件，调用方负责页码状态和数据加载。 */
import BaseButton from '@/components/base/BaseButton.vue'

defineProps<{ page: number; pages: number; total: number; busy?: boolean }>()
const emit = defineEmits<{ prev: []; next: [] }>()
</script>

<template>
  <nav class="ops-pager" aria-label="分页">
    <span class="ops-muted">共 {{ total }} 条，第 {{ page }} / {{ Math.max(pages, 1) }} 页</span>
    <div class="ops-actions">
      <BaseButton variant="outline" size="sm" :disabled="busy || page <= 1" @click="emit('prev')">上一页</BaseButton>
      <BaseButton variant="outline" size="sm" :disabled="busy || page >= pages" @click="emit('next')">下一页</BaseButton>
    </div>
  </nav>
</template>

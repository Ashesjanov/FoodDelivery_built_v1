<script setup lang="ts">
withDefaults(
  defineProps<{ title?: string; message?: string; actionLabel?: string; retryLabel?: string }>(),
  { title: '加载失败', message: '请检查网络后重试', actionLabel: '', retryLabel: '重新加载' },
)

const emit = defineEmits<{ retry: [] }>()
</script>

<template>
  <section class="flex w-full flex-col items-center justify-center gap-4 p-4 text-center md:p-6" role="alert">
    <div class="flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-100 text-xl text-rose-700" aria-hidden="true">!</div>
    <h2 class="text-lg font-semibold text-zinc-950">{{ title }}</h2>
    <p class="max-w-lg text-sm leading-6 text-zinc-700">{{ message }}</p>
    <button
      type="button"
      class="min-h-12 rounded-xl bg-rose-600 px-5 py-3 text-sm font-medium text-white transition-transform duration-200 ease-out hover:-translate-y-1 hover:scale-[1.01] active:scale-95 focus:outline-none focus-visible:ring-2 focus-visible:ring-rose-600 focus-visible:ring-offset-2 motion-reduce:transform-none"
      @click="emit('retry')"
    >
      {{ actionLabel || retryLabel }}
    </button>
  </section>
</template>

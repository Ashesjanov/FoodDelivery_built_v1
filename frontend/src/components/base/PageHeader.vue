<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    description?: string
    backTo?: string
    backLabel?: string
  }>(),
  { description: '', backTo: '', backLabel: '返回' },
)
</script>

<template>
  <header class="flex flex-col gap-6">
    <RouterLink
      v-if="backTo"
      :to="backTo"
      class="inline-flex w-fit items-center gap-4 rounded-xl px-2 py-1 text-sm text-zinc-600 transition-colors duration-200 ease-out hover:bg-zinc-100 hover:text-zinc-950 focus:outline-none focus-visible:ring-2 focus-visible:ring-orange-500"
    >
      <span aria-hidden="true">←</span>{{ backLabel }}
    </RouterLink>
    <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div class="min-w-0 flex-1">
        <h1 class="text-2xl font-semibold text-zinc-950 md:text-3xl">{{ title }}</h1>
        <p v-if="description" class="mt-3 max-w-3xl text-sm leading-6 text-zinc-600 md:text-base">
          {{ description }}
        </p>
      </div>
      <div v-if="$slots.actions" class="flex flex-wrap items-center gap-4">
        <slot name="actions" />
      </div>
    </div>
  </header>
</template>

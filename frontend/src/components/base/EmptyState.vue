<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    description?: string
    actionLabel?: string
  }>(),
  { description: '', actionLabel: '' },
)

const emit = defineEmits<{ action: [] }>()
</script>

<template>
  <section class="flex w-full flex-col items-center justify-center gap-4 p-4 text-center md:p-6" role="status">
    <div class="flex h-14 w-14 items-center justify-center rounded-2xl bg-orange-100 text-orange-700 group-hover:scale-110 transition-transform duration-300 ease-out motion-reduce:transform-none">
      <slot name="icon"><span class="text-2xl" aria-hidden="true">○</span></slot>
    </div>
    <div class="max-w-lg">
      <h2 class="text-lg font-semibold text-zinc-950">{{ title }}</h2>
      <p v-if="description" class="mt-3 text-sm leading-6 text-zinc-600">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="flex flex-wrap justify-center gap-4">
      <slot name="actions" />
    </div>
    <button
      v-else-if="actionLabel"
      type="button"
      class="min-h-12 rounded-xl bg-orange-500 px-5 py-3 text-sm font-medium text-white transition-colors duration-200 ease-out hover:bg-orange-600 active:scale-95 focus:outline-none focus-visible:ring-2 focus-visible:ring-orange-500 focus-visible:ring-offset-2"
      @click="emit('action')"
    >
      {{ actionLabel }}
    </button>
  </section>
</template>

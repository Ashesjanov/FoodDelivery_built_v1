<script setup lang="ts">
withDefaults(
  defineProps<{
    dense?: boolean
    columns?: 1 | 2 | 4
  }>(),
  {
    dense: true,
    columns: 4,
  },
)
</script>

<template>
  <section
    class="bento-grid grid grid-cols-4 gap-4 md:gap-6"
    :class="[dense ? 'grid-flow-dense' : '', `bento-grid--columns-${columns}`]"
  >
    <slot />
  </section>
</template>

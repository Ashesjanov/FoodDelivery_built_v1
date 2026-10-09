<script setup lang="ts">
/** 运营端轻量对话框，表单由调用方通过默认插槽提供。 */
defineProps<{ title: string; open: boolean }>()
const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="ops-modal-backdrop" role="presentation" @click.self="emit('close')">
      <section class="ops-modal" role="dialog" aria-modal="true" :aria-label="title">
        <header class="ops-modal-head">
          <h2 class="ops-modal-title">{{ title }}</h2>
          <button class="ops-close" type="button" aria-label="关闭" title="关闭" @click="emit('close')">×</button>
        </header>
        <slot />
        <footer v-if="$slots.footer" class="ops-actions" style="margin-top: 18px">
          <slot name="footer" />
        </footer>
      </section>
    </div>
  </Teleport>
</template>

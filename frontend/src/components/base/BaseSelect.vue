<script setup lang="ts">
import { computed, useId } from 'vue'

export interface SelectOption {
  label: string
  value: string | number | null
  disabled?: boolean
}

const props = withDefaults(
  defineProps<{
    label?: string
    options: SelectOption[]
    placeholder?: string
    hint?: string
    error?: string
    disabled?: boolean
    required?: boolean
  }>(),
  {
    label: '',
    placeholder: '请选择',
    hint: '',
    error: '',
    disabled: false,
    required: false,
  },
)

const model = defineModel<string | number | null>({ default: null })
const selectId = useId()
const messageId = computed(() => (props.error || props.hint ? `${selectId}-message` : undefined))
</script>

<template>
  <label :for="selectId" class="flex w-full flex-col gap-4 text-left">
    <span v-if="label" class="text-sm font-medium text-zinc-800">
      {{ label }}<span v-if="required" class="ml-1 text-rose-600" aria-hidden="true">*</span>
    </span>
    <select
      :id="selectId"
      v-model="model"
      :disabled="disabled"
      :required="required"
      :aria-invalid="Boolean(error) || undefined"
      :aria-describedby="messageId"
      class="min-h-12 w-full bg-zinc-50 dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all px-4 py-3 text-sm text-zinc-950 disabled:cursor-not-allowed disabled:bg-zinc-100"
      :class="error ? 'border-rose-500 focus:border-rose-500 focus:ring-rose-200' : ''"
    >
      <option v-if="placeholder" value="" disabled>{{ placeholder }}</option>
      <option
        v-for="option in options"
        :key="`${option.label}-${String(option.value)}`"
        :value="option.value"
        :disabled="option.disabled"
      >
        {{ option.label }}
      </option>
    </select>
    <span
      v-if="error || hint"
      :id="messageId"
      class="text-sm"
      :class="error ? 'text-rose-600' : 'text-zinc-500'"
    >
      {{ error || hint }}
    </span>
  </label>
</template>

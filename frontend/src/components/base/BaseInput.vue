<script setup lang="ts">
import { computed, useId } from 'vue'

const props = withDefaults(
  defineProps<{
    label?: string
    type?: 'text' | 'password' | 'number' | 'tel' | 'search' | 'url'
    placeholder?: string
    hint?: string
    error?: string
    disabled?: boolean
    required?: boolean
    autocomplete?: string
    min?: number
    max?: number
    step?: number | string
  }>(),
  {
    label: '',
    type: 'text',
    placeholder: '',
    hint: '',
    error: '',
    disabled: false,
    required: false,
    autocomplete: 'off',
  },
)

const model = defineModel<string | number>({ default: '' })
const inputId = useId()
const messageId = computed(() => (props.error || props.hint ? `${inputId}-message` : undefined))
</script>

<template>
  <label :for="inputId" class="flex w-full flex-col gap-4 text-left">
    <span v-if="label" class="text-sm font-medium text-zinc-800">
      {{ label }}<span v-if="required" class="ml-1 text-rose-600" aria-hidden="true">*</span>
    </span>
    <input
      :id="inputId"
      v-model="model"
      :type="type"
      :placeholder="placeholder"
      :disabled="disabled"
      :required="required"
      :autocomplete="autocomplete"
      :min="min"
      :max="max"
      :step="step"
      :aria-invalid="Boolean(error) || undefined"
      :aria-describedby="messageId"
      class="min-h-12 w-full bg-zinc-50 dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all px-4 py-3 text-sm text-zinc-950 placeholder:text-zinc-400 disabled:cursor-not-allowed disabled:bg-zinc-100"
      :class="error ? 'border-rose-500 focus:border-rose-500 focus:ring-rose-200' : ''"
    >
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

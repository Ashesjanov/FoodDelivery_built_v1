<script setup lang="ts">
import { computed } from 'vue'

type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger'
type ButtonSize = 'sm' | 'md' | 'lg'
type NativeButtonType = 'button' | 'submit' | 'reset'
type ButtonType = ButtonVariant | NativeButtonType

const props = withDefaults(
  defineProps<{
    variant?: ButtonVariant
    type?: ButtonType
    size?: ButtonSize
    nativeType?: NativeButtonType
    disabled?: boolean
    loading?: boolean
    block?: boolean
  }>(),
  {
    variant: 'primary',
    type: undefined,
    size: 'md',
    nativeType: 'button',
    disabled: false,
    loading: false,
    block: false,
  },
)

const emit = defineEmits<{ click: [event: MouseEvent] }>()

const nativeButtonType = computed<NativeButtonType>(() => {
  if (props.type === 'submit' || props.type === 'reset' || props.type === 'button') return props.type
  return props.nativeType
})

const resolvedVariant = computed<ButtonVariant>(() => {
  if (props.type && ['primary', 'secondary', 'outline', 'ghost', 'danger'].includes(props.type)) {
    return props.type as ButtonVariant
  }
  return props.variant
})

const variantClasses: Record<ButtonVariant, string> = {
  primary: 'bg-orange-500 text-white shadow-sm hover:bg-orange-600',
  secondary: 'bg-green-100 text-green-900 hover:bg-green-200',
  outline: 'border border-zinc-300 bg-white text-zinc-800 hover:border-orange-400 hover:text-orange-700',
  ghost: 'bg-transparent text-zinc-700 hover:bg-zinc-100 hover:text-zinc-950',
  danger: 'bg-rose-600 text-white shadow-sm hover:bg-rose-700',
}

const sizeClasses: Record<ButtonSize, string> = {
  sm: 'min-h-10 text-sm',
  md: 'min-h-12 text-sm',
  lg: 'min-h-14 text-base',
}

const classes = computed(() => [
  'inline-flex items-center justify-center gap-4 px-4 py-2 md:px-6 md:py-3 rounded-xl font-medium transition-colors duration-200 ease-out',
  'focus:outline-none focus-visible:ring-2 focus-visible:ring-orange-500 focus-visible:ring-offset-2',
  'active:scale-95 disabled:pointer-events-none disabled:opacity-50',
  variantClasses[resolvedVariant.value],
  sizeClasses[props.size],
  props.block ? 'w-full' : '',
])
</script>

<template>
  <button
    :type="nativeButtonType"
    :class="classes"
    :disabled="disabled || loading"
    :aria-busy="loading || undefined"
    @click="emit('click', $event)"
  >
    <span
      v-if="loading"
      class="h-4 w-4 animate-spin rounded-full border-2 border-current border-r-transparent motion-reduce:animate-none"
      aria-hidden="true"
    />
    <span v-else class="flex h-5 w-5 items-center justify-center group-hover:scale-110 transition-transform duration-300 ease-out">
      <slot name="icon" />
    </span>
    <slot />
  </button>
</template>

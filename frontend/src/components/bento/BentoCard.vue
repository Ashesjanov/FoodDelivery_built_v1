<script setup lang="ts">
import { computed, useAttrs, type Component } from 'vue'
import { RouterLink } from 'vue-router'

type CardSize = 'large' | 'medium' | 'small' | 'compact' | 'standard' | 'wide' | 'tall' | 'feature'
type CardTone = 'default' | 'accent' | 'warm' | 'fresh' | 'dark' | 'neutral' | 'orange' | 'green' | 'cyan' | 'pink'

const props = withDefaults(
  defineProps<{
    title?: string
    description?: string
    to?: string
    href?: string
    size?: CardSize
    tone?: CardTone
    aspect?: string
    interactive?: boolean
    icon?: Component
  }>(),
  {
    title: '',
    description: '',
    to: '',
    href: '',
    size: 'standard',
    tone: 'neutral',
    aspect: '',
    interactive: true,
    icon: undefined,
  },
)

const emit = defineEmits<{ click: [event: MouseEvent] }>()
const attrs = useAttrs()
const passthroughAttrs = computed(() =>
  Object.fromEntries(Object.entries(attrs).filter(([key]) => key !== 'class' && key !== 'style')),
)

const tag = computed(() => {
  if (props.to) return RouterLink
  if (props.href) return 'a'
  return 'article'
})

const targetAttributes = computed(() => ({
  ...(props.to ? { to: props.to } : {}),
  ...(props.href ? { href: props.href } : {}),
}))

const spanClasses: Record<CardSize, string> = {
  large: 'col-span-2 row-span-2',
  medium: 'col-span-2 row-span-1',
  small: 'col-span-1 row-span-1',
  compact: 'col-span-2 row-span-1',
  standard: 'col-span-2 row-span-2',
  wide: 'col-span-2 row-span-1',
  tall: 'col-span-1 row-span-2',
  feature: 'col-span-2 row-span-2 lg:col-span-4',
}

const aspectClasses: Record<CardSize, string> = {
  large: 'aspect-[16/9]',
  medium: 'aspect-[16/10]',
  small: 'aspect-square',
  compact: 'aspect-[5/4]',
  standard: 'aspect-[4/3]',
  wide: 'aspect-[16/10]',
  tall: 'aspect-[4/5]',
  feature: 'aspect-[16/9]',
}

const aspectStyle = computed(() => (props.aspect ? { aspectRatio: props.aspect } : {}))

const toneClasses: Record<CardTone, string> = {
  default: 'border-zinc-100 dark:border-zinc-800 bg-white dark:bg-zinc-900',
  accent: 'border-orange-200 bg-orange-50',
  warm: 'border-pink-200 bg-pink-50',
  fresh: 'border-emerald-200 bg-emerald-50',
  dark: 'border-zinc-800 bg-zinc-900 text-white',
  neutral: 'border-zinc-200 bg-white',
  orange: 'border-orange-200 bg-orange-50',
  green: 'border-emerald-200 bg-emerald-50',
  cyan: 'border-cyan-200 bg-cyan-50',
  pink: 'border-pink-200 bg-pink-50',
}

const classes = computed(() => [
  'bento-card group relative flex min-w-0 flex-col justify-between overflow-hidden rounded-2xl border border-zinc-100 dark:border-zinc-800 shadow-sm hover:shadow-md hover:shadow-[0_8px_30px_rgba(0,0,0,0.08)] hover:-translate-y-1 hover:scale-[1.01] transition-all duration-200 p-5 md:p-6',
  spanClasses[props.size],
  props.aspect ? '' : aspectClasses[props.size],
  toneClasses[props.tone],
  props.interactive
    ? 'transition-transform duration-300 ease-out hover:-translate-y-1 hover:scale-[1.01] active:scale-95 focus:outline-none focus-visible:ring-2 focus-visible:ring-orange-500 focus-visible:ring-offset-2 motion-reduce:transform-none'
    : '',
])
</script>

<template>
  <component
    :is="tag"
    v-bind="{ ...targetAttributes, ...passthroughAttrs }"
    :class="[classes, attrs.class]"
    :style="aspectStyle"
    @click="emit('click', $event)"
  >
    <div v-if="$slots.icon || icon || $slots.media" class="mb-5 flex items-start justify-between gap-4">
      <div
        class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-white text-orange-700 shadow-sm group-hover:scale-110 transition-transform duration-300 ease-out motion-reduce:transform-none"
      >
        <slot name="icon">
          <component :is="icon" v-if="icon" class="h-6 w-6" aria-hidden="true" />
        </slot>
      </div>
      <slot name="media" />
    </div>

    <div class="min-w-0 flex-1">
      <slot name="overline" />
      <h2 v-if="title" class="text-lg font-semibold text-zinc-950 transition-colors duration-200 ease-out md:text-xl">
        {{ title }}
      </h2>
      <p v-if="description" class="mt-3 line-clamp-3 text-sm leading-6 text-zinc-600">
        {{ description }}
      </p>
      <slot />
    </div>

    <footer v-if="$slots.footer" class="mt-5 flex flex-wrap items-center gap-4">
      <slot name="footer" />
    </footer>
  </component>
</template>

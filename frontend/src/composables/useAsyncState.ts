import { computed, ref, type ComputedRef, type Ref } from 'vue'

export interface UseAsyncStateOptions<T, Args extends unknown[]> {
  initialData?: T
  immediate?: boolean
  immediateArgs?: Args
}

export interface UseAsyncState<T, Args extends unknown[] = []> {
  data: Ref<T | undefined>
  error: Ref<unknown>
  loading: Ref<boolean>
  isSuccess: ComputedRef<boolean>
  execute: (...args: Args) => Promise<T | undefined>
  reset: () => void
}

export function useAsyncState<T, Args extends unknown[] = []>(
  task: (...args: Args) => Promise<T>,
  options: UseAsyncStateOptions<T, Args> = {},
): UseAsyncState<T, Args> {
  const data = ref<T | undefined>(options.initialData) as Ref<T | undefined>
  const error = ref<unknown>(null)
  const loading = ref(false)
  const isSuccess = computed(() => error.value === null && data.value !== undefined)

  async function execute(...args: Args): Promise<T | undefined> {
    loading.value = true
    error.value = null
    try {
      data.value = await task(...args)
      return data.value
    } catch (cause) {
      error.value = cause
      return undefined
    } finally {
      loading.value = false
    }
  }

  function reset(): void {
    data.value = options.initialData
    error.value = null
    loading.value = false
  }

  if (options.immediate) void execute(...(options.immediateArgs ?? ([] as unknown as Args)))

  return { data, error, loading, isSuccess, execute, reset }
}

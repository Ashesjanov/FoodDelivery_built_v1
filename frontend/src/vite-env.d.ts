// Vite 环境类型声明：补充 import.meta 和资源导入类型。
/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_API_PROXY_TARGET?: string
  readonly VITE_API_TIMEOUT_MS?: string
  readonly VITE_WS_PROXY_TARGET?: string
  readonly VITE_WS_URL?: string
  readonly VITE_USE_MOCK?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

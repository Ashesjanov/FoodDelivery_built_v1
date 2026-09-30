/** 顾客端页面共享的轻量展示与接口容错工具。 */

export type CustomerRecord = Record<string, any>

/** 兼容 ApiResponse 包装和已解包的接口返回值。 */
export function unwrap<T = any>(value: any): T {
  if (value && typeof value === 'object' && 'data' in value && 'code' in value) {
    return value.data as T
  }
  return value as T
}

/** 将后端异常转换为可读提示，并覆盖常见状态码。 */
export function errorMessage(error: any): string {
  const status = Number(error?.response?.status ?? error?.status ?? 0)
  const fallback = error?.response?.data?.message || error?.message || '请求失败，请稍后重试'
  if (status === 401) return '登录状态已过期，请重新登录'
  if (status === 403) return '当前账号没有执行此操作的权限'
  if (status === 409) return '操作存在冲突，请刷新后重试'
  if (error?.code === 40001 || error?.response?.data?.code === 40001) return '登录状态已过期，请重新登录'
  return fallback
}

export function asArray<T = CustomerRecord>(value: any): T[] {
  const data = unwrap(value)
  if (Array.isArray(data)) return data as T[]
  if (Array.isArray(data?.records)) return data.records as T[]
  return []
}

export function money(value: any): string {
  const amount = Number(value ?? 0)
  return `¥${amount.toFixed(2)}`
}

export function dateTime(value: any): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

export function statusLabel(status: any): string {
  const labels: Record<string, string> = {
    PENDING: '待支付',
    PAID: '已支付',
    ACCEPTED: '商家已接单',
    PREPARING: '制作中',
    READY: '待取餐',
    PICKED: '配送中',
    DELIVERED: '已送达',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    CLOSED: '已关闭',
    OPEN: '营业中',
    CLOSED_MERCHANT: '休息中',
    ACTIVE: '可领取',
    USED: '已使用',
    EXPIRED: '已过期',
    AVAILABLE: '可领取',
    CLAIMED: '已领取',
  }
  return labels[String(status ?? '')] || String(status ?? '未知')
}

export function orderIdFromRoute(route: any): number {
  const raw = route?.params?.id
  return Number(Array.isArray(raw) ? raw[0] : raw)
}

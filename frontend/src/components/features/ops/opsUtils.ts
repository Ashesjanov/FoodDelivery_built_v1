/** 运营端页面的通用展示与错误处理工具。 */
export type OpsRecord = Record<string, any>

export function unwrap<T>(value: any): T {
  return value && typeof value === 'object' && 'data' in value ? (value.data as T) : (value as T)
}

export function errorMessage(cause: unknown): string {
  const error = cause as OpsRecord
  const status = Number(error?.status ?? error?.response?.status ?? error?.statusCode ?? 0)
  const code = Number(error?.code ?? error?.response?.data?.code ?? 0)
  const message = String(error?.response?.data?.message ?? error?.message ?? '')
  if (status === 401 || code === 401 || code === 40001) return '登录状态已失效，请重新登录后再试'
  if (status === 403 || code === 403) return '当前账号没有执行此操作的权限'
  if (status === 409 || code === 409) return '操作冲突，请刷新页面后重试'
  if (status === 400 || code === 400) return message || '请求参数有误，请检查后重试'
  return message || '操作未完成，请稍后重试'
}

export function money(value: number | string | null | undefined): string {
  const amount = Number(value ?? 0)
  return Number.isFinite(amount) ? amount.toFixed(2) : '0.00'
}

export function dateTime(value: string | null | undefined): string {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('zh-CN', { hour12: false })
}

export function statusLabel(status: string | null | undefined): string {
  const labels: Record<string, string> = {
    PENDING_PAYMENT: '待支付', PAID: '待接单', ACCEPTED: '已接单', PREPARING: '备餐中', READY: '待取餐',
    PICKED_UP: '配送中', DELIVERED: '已送达', COMPLETED: '已完成', CANCELLED: '已取消', REFUNDED: '已退款',
    ON_SALE: '售卖中', OFF_SALE: '已下架', SOLD_OUT: '已售罄', DELETED: '已删除',
    OPEN: '营业中', PAUSED: '暂停接单', PREPARING_MERCHANT: '准备中', CLOSED: '已打烊',
    ONLINE: '在线', OFFLINE: '离线', BUSY: '配送中', SUSPENDED: '已停用',
    ACTIVE: '生效中', UPCOMING: '待开始', EXPIRED: '已过期', EXHAUSTED: '已领完',
    UNUSED: '未使用', USED: '已使用', CANCELLED_CLAIM: '已取消', WAITING_RIDER: '待接单', ASSIGNED: '已接单',
    PICKED_UP_DELIVERY: '已取餐', DELIVERED_DELIVERY: '已送达', CANCELLED_DELIVERY: '已取消',
  }
  return labels[String(status ?? '')] ?? String(status ?? '—')
}

export function statusTone(status: string | null | undefined): string {
  const value = String(status ?? '')
  if (['OPEN', 'ON_SALE', 'ACTIVE', 'ONLINE', 'COMPLETED', 'DELIVERED', 'SUCCESS'].includes(value)) return 'fresh'
  if (['PAUSED', 'PREPARING', 'ACCEPTED', 'READY', 'PICKED_UP', 'BUSY', 'UPCOMING', 'WAITING_RIDER', 'ASSIGNED'].includes(value)) return 'warm'
  if (['CANCELLED', 'REFUNDED', 'CLOSED', 'OFF_SALE', 'SOLD_OUT', 'EXPIRED', 'EXHAUSTED', 'DISABLED', 'LOCKED', 'SUSPENDED'].includes(value)) return 'dark'
  return 'default'
}

export function pageRecords<T>(result: any): T[] {
  const value = unwrap<any>(result)
  return Array.isArray(value) ? value : Array.isArray(value?.records) ? value.records : []
}

export function pageTotal(result: any): number {
  const value = unwrap<any>(result)
  return Number(value?.total ?? (Array.isArray(value) ? value.length : 0))
}

/** 兼容 core API 在 list/page、setStatus/updateStatus 等命名上的过渡差异。 */
export function callApi<T = any>(service: any, names: string[], args: any[] = []): Promise<T> {
  const method = names.map((name) => service?.[name]).find((candidate) => typeof candidate === 'function')
  if (!method) throw new Error(`接口尚未就绪：${names.join('/')}`)
  return Promise.resolve(method.apply(service, args)) as Promise<T>
}

export function currentUserId(auth: any): number {
  const source = auth?.user?.value ?? auth?.user ?? auth?.currentUser?.value ?? auth?.currentUser ?? auth?.profile?.value ?? auth?.profile
  return Number(source?.id ?? 0)
}

export function toInputDate(value: string | null | undefined): string {
  return value ? String(value).slice(0, 16) : ''
}

export function roleLabel(role: string | null | undefined): string {
  return ({ CUSTOMER: '顾客', ADMIN: '管理员', MERCHANT: '商家', RIDER: '骑手' } as Record<string, string>)[String(role ?? '')] ?? String(role ?? '—')
}

export function userStatusLabel(status: string | null | undefined): string {
  return ({ ACTIVE: '正常', LOCKED: '已锁定', DISABLED: '已停用' } as Record<string, string>)[String(status ?? '')] ?? String(status ?? '—')
}

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{ status: string; label?: string }>(), { label: '' })

const statusStyles: Record<string, { label: string; className: string }> = {
  ACTIVE: { label: '正常', className: 'bg-emerald-100 text-emerald-800' },
  LOCKED: { label: '已锁定', className: 'bg-amber-100 text-amber-800' },
  DISABLED: { label: '已停用', className: 'bg-zinc-200 text-zinc-700' },
  PREPARING: { label: '准备中', className: 'bg-amber-100 text-amber-800' },
  OPEN: { label: '营业中', className: 'bg-emerald-100 text-emerald-800' },
  PAUSED: { label: '暂停接单', className: 'bg-orange-100 text-orange-800' },
  CLOSED: { label: '已打烊', className: 'bg-zinc-200 text-zinc-700' },
  ON_SALE: { label: '已上架', className: 'bg-emerald-100 text-emerald-800' },
  OFF_SALE: { label: '已下架', className: 'bg-zinc-200 text-zinc-700' },
  SOLD_OUT: { label: '已售罄', className: 'bg-orange-100 text-orange-800' },
  DELETED: { label: '已删除', className: 'bg-zinc-200 text-zinc-700' },
  PENDING_PAYMENT: { label: '待支付', className: 'bg-amber-100 text-amber-800' },
  PAID: { label: '已支付', className: 'bg-cyan-100 text-cyan-800' },
  ACCEPTED: { label: '商家已接单', className: 'bg-cyan-100 text-cyan-800' },
  READY: { label: '待取餐', className: 'bg-orange-100 text-orange-800' },
  PICKED_UP: { label: '配送中', className: 'bg-cyan-100 text-cyan-800' },
  DELIVERED: { label: '已送达', className: 'bg-emerald-100 text-emerald-800' },
  COMPLETED: { label: '已完成', className: 'bg-emerald-100 text-emerald-800' },
  CANCELLED: { label: '已取消', className: 'bg-zinc-200 text-zinc-700' },
  REFUNDED: { label: '已退款', className: 'bg-rose-100 text-rose-800' },
  PENDING: { label: '处理中', className: 'bg-amber-100 text-amber-800' },
  SUCCESS: { label: '成功', className: 'bg-emerald-100 text-emerald-800' },
  FAILED: { label: '失败', className: 'bg-rose-100 text-rose-800' },
  UNUSED: { label: '未使用', className: 'bg-cyan-100 text-cyan-800' },
  USED: { label: '已使用', className: 'bg-emerald-100 text-emerald-800' },
  EXPIRED: { label: '已过期', className: 'bg-zinc-200 text-zinc-700' },
  UPCOMING: { label: '即将开始', className: 'bg-cyan-100 text-cyan-800' },
  EXHAUSTED: { label: '已领完', className: 'bg-rose-100 text-rose-800' },
  WAITING_RIDER: { label: '等待骑手', className: 'bg-amber-100 text-amber-800' },
  ASSIGNED: { label: '骑手已接单', className: 'bg-cyan-100 text-cyan-800' },
  ONLINE: { label: '在线', className: 'bg-emerald-100 text-emerald-800' },
  OFFLINE: { label: '离线', className: 'bg-zinc-200 text-zinc-700' },
  BUSY: { label: '配送中', className: 'bg-orange-100 text-orange-800' },
  SUSPENDED: { label: '已停用', className: 'bg-rose-100 text-rose-800' },
  VISIBLE: { label: '已展示', className: 'bg-emerald-100 text-emerald-800' },
  HIDDEN: { label: '已隐藏', className: 'bg-zinc-200 text-zinc-700' },
}

const presentation = computed(() => statusStyles[props.status] ?? { label: props.status, className: 'bg-zinc-100 text-zinc-700' })
</script>

<template>
  <span
    class="inline-flex min-h-8 items-center rounded-xl px-3 py-1.5 text-sm font-medium"
    :class="presentation.className"
  >
    {{ label || presentation.label }}
  </span>
</template>

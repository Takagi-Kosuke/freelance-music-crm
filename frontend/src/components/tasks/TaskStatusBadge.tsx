import React from 'react'

type TaskStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

type TaskStatusBadgeProps = {
  status: TaskStatus
}

const STATUS_LABELS: Record<TaskStatus, string> = {
  NOT_STARTED: '未着手',
  IN_PROGRESS: '進行中',
  COMPLETED: '完了',
  CANCELLED: 'キャンセル',
}

const STATUS_CLASSES: Record<TaskStatus, string> = {
  NOT_STARTED: 'bg-slate-100 text-slate-800 border-slate-200',
  IN_PROGRESS: 'bg-amber-100 text-amber-900 border-amber-200',
  COMPLETED: 'bg-emerald-100 text-emerald-900 border-emerald-200',
  CANCELLED: 'bg-rose-100 text-rose-900 border-rose-200',
}

export function TaskStatusBadge({ status }: TaskStatusBadgeProps) {
  return (
    <span className={`inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold ${STATUS_CLASSES[status]}`}>
      {STATUS_LABELS[status]}
    </span>
  )
}

export type { TaskStatus }

'use client'

import React from 'react'
import { TaskStatusBadge, type TaskStatus } from '@/components/tasks/TaskStatusBadge'

type TaskItem = {
  id: number
  orderId: number
  categoryId: number
  categoryName: string
  orderSubject: string
  clientName: string
  clientEmail: string | null
  desiredDeliveryDate: string
  filePathUrl: string | null
  comment: string | null
  folderPath: string | null
  status: TaskStatus
  statusUpdatedAt: string | null
  createdAt: string
}

type DetailPanelProps = {
  task: TaskItem | null
  updating: boolean
  onClose: () => void
  onUpdateStatus: (status: TaskStatus) => void
  onUpdateFolderPath: (folderPath: string | null) => Promise<void>
  savingFolderPath: boolean
}

const STATUS_OPTIONS: Array<{ value: TaskStatus; label: string }> = [
  { value: 'NOT_STARTED', label: '未着手' },
  { value: 'IN_PROGRESS', label: '進行中' },
  { value: 'COMPLETED', label: '完了' },
  { value: 'CANCELLED', label: 'キャンセル' },
]

export function DetailPanel({ task, updating, onClose, onUpdateStatus, onUpdateFolderPath, savingFolderPath }: DetailPanelProps) {
  const [folderPathInput, setFolderPathInput] = React.useState('')
  const [folderPathMessage, setFolderPathMessage] = React.useState<string | null>(null)

  React.useEffect(() => {
    setFolderPathInput(task?.folderPath ?? '')
    setFolderPathMessage(null)
  }, [task?.id, task?.folderPath])

  if (!task) {
    return (
      <aside className="rounded-xl border border-gray-200 bg-white p-5">
        <h2 className="text-lg font-semibold text-gray-900">詳細パネル</h2>
        <p className="mt-3 text-sm text-gray-500">タスクを選択すると詳細が表示されます。</p>
      </aside>
    )
  }

  const saveFolderPath = async () => {
    const normalized = folderPathInput.trim()
    await onUpdateFolderPath(normalized.length > 0 ? normalized : null)
    setFolderPathMessage('フォルダパスを保存しました')
  }

  const copyFolderPath = async () => {
    const value = folderPathInput.trim()
    if (!value) {
      setFolderPathMessage('コピーするフォルダパスがありません')
      return
    }

    try {
      await navigator.clipboard.writeText(value)
      setFolderPathMessage('フォルダパスをコピーしました')
    } catch {
      setFolderPathMessage('コピーに失敗しました')
    }
  }

  return (
    <aside className="rounded-xl border border-gray-200 bg-white p-5">
      <div className="flex items-start justify-between gap-3">
        <h2 className="text-lg font-semibold text-gray-900">タスク詳細</h2>
        <button
          type="button"
          onClick={onClose}
          className="rounded-md border border-gray-300 px-2 py-1 text-xs text-gray-600 hover:bg-gray-50"
        >
          閉じる
        </button>
      </div>

      <div className="mt-4 space-y-3 text-sm">
        <Field label="タスクID" value={String(task.id)} />
        <Field label="案件ID" value={String(task.orderId)} />
        <Field label="件名" value={task.orderSubject} />
        <Field label="依頼区分" value={task.categoryName} />
        <Field label="依頼者" value={task.clientName} />
        <Field label="メール" value={task.clientEmail ?? '-'} />
        <Field label="希望納期" value={task.desiredDeliveryDate} />
        <Field label="作成日時" value={task.createdAt} />
        <Field label="状態更新日時" value={task.statusUpdatedAt ?? '-'} />
        <Field label="ファイルURL" value={task.filePathUrl ?? '-'} />
        <Field label="コメント" value={task.comment ?? '-'} multiline />

        <div>
          <p className="mb-1 text-xs font-semibold text-gray-500">現在ステータス</p>
          <TaskStatusBadge status={task.status} />
        </div>
      </div>

      <div className="mt-5 rounded-lg border border-[#CBB9A8] bg-[#EFE4DB] p-3">
        <p className="mb-2 text-xs font-semibold text-gray-600">進行中タスク用フォルダパス</p>
        <textarea
          value={folderPathInput}
          onChange={(e) => setFolderPathInput(e.target.value)}
          className="min-h-[88px] w-full rounded-md border border-[#CBB9A8] bg-white px-3 py-2 text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#145C9E]"
          placeholder="例: E:/workspace/Tasks/backend/src/main/java"
        />
        <div className="mt-3 flex flex-wrap gap-2">
          <button
            type="button"
            onClick={saveFolderPath}
            disabled={savingFolderPath}
            className="min-h-[44px] rounded-md bg-[#145C9E] px-3 py-2 text-sm font-semibold text-white hover:bg-[#0B4F6C] disabled:cursor-not-allowed disabled:opacity-60"
          >
            {savingFolderPath ? '保存中...' : '保存'}
          </button>
          <button
            type="button"
            onClick={copyFolderPath}
            className="min-h-[44px] rounded-md border border-[#CBB9A8] bg-white px-3 py-2 text-sm font-semibold text-[#0B4F6C] hover:bg-[#EAF1F6]"
          >
            コピー
          </button>
        </div>
        {folderPathMessage && <p className="mt-2 text-xs text-gray-600">{folderPathMessage}</p>}
      </div>

      <div className="mt-5">
        <p className="mb-2 text-xs font-semibold text-gray-500">ステータス更新</p>
        <div className="grid grid-cols-2 gap-2">
          {STATUS_OPTIONS.map((option) => (
            <button
              key={option.value}
              type="button"
              disabled={updating || option.value === task.status}
              onClick={() => onUpdateStatus(option.value)}
              className="rounded-md border border-[#CBB9A8] bg-[#EAF1F6] px-3 py-2 text-sm font-medium text-[#0B4F6C] hover:bg-[#D9E7F1] disabled:cursor-not-allowed disabled:opacity-50"
            >
              {option.label}
            </button>
          ))}
        </div>
      </div>
    </aside>
  )
}

type FieldProps = {
  label: string
  value: string
  multiline?: boolean
}

function Field({ label, value, multiline = false }: FieldProps) {
  return (
    <div>
      <p className="mb-1 text-xs font-semibold text-gray-500">{label}</p>
      {multiline ? (
        <p className="whitespace-pre-wrap rounded-md bg-gray-50 p-2 text-gray-900">{value}</p>
      ) : (
        <p className="text-gray-900">{value}</p>
      )}
    </div>
  )
}

export type { TaskItem }

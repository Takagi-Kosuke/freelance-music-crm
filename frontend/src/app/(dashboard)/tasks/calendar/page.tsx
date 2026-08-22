'use client'

import { useEffect, useMemo, useState } from 'react'
import { format, startOfMonth, endOfMonth, startOfWeek, endOfWeek } from 'date-fns'
import { type View, Views } from 'react-big-calendar'
import { CalendarView } from '@/components/tasks/CalendarView'
import { DetailPanel, type TaskItem } from '@/components/tasks/DetailPanel'
import { type TaskStatus } from '@/components/tasks/TaskStatusBadge'
import { apiFetch } from '@/lib/api'

function getRange(date: Date, view: View): { start: Date; end: Date } {
  if (view === Views.WEEK) {
    return {
      start: startOfWeek(date, { weekStartsOn: 1 }),
      end: endOfWeek(date, { weekStartsOn: 1 }),
    }
  }

  const monthStart = startOfMonth(date)
  const monthEnd = endOfMonth(date)
  return {
    start: startOfWeek(monthStart, { weekStartsOn: 1 }),
    end: endOfWeek(monthEnd, { weekStartsOn: 1 }),
  }
}

export default function TasksCalendarPage() {
  const [tasks, setTasks] = useState<TaskItem[]>([])
  const [currentDate, setCurrentDate] = useState<Date>(new Date())
  const [currentView, setCurrentView] = useState<View>(Views.MONTH)
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [updating, setUpdating] = useState(false)
  const [savingFolderPath, setSavingFolderPath] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const selectedTask = useMemo(
    () => tasks.find((task) => task.id === selectedTaskId) ?? null,
    [tasks, selectedTaskId]
  )

  useEffect(() => {
    const loadCalendarTasks = async () => {
      setLoading(true)
      setError(null)

      try {
        const range = getRange(currentDate, currentView)
        const start = format(range.start, 'yyyy-MM-dd')
        const end = format(range.end, 'yyyy-MM-dd')
        const response = await apiFetch(`/api/tasks/calendar?start=${start}&end=${end}`, {
          method: 'GET',
        })

        if (!response.ok) {
          if (response.status === 401) {
            setError('ログインが必要です')
          } else {
            setError('カレンダータスクの取得に失敗しました')
          }
          return
        }

        const data = (await response.json()) as TaskItem[]
        setTasks(data)

        if (data.length === 0) {
          setSelectedTaskId(null)
          return
        }

        setSelectedTaskId((prev) => {
          if (prev && data.some((task) => task.id === prev)) {
            return prev
          }
          return data[0].id
        })
      } catch {
        setError('サーバーに接続できませんでした')
      } finally {
        setLoading(false)
      }
    }

    loadCalendarTasks()
  }, [currentDate, currentView])

  const updateStatus = async (status: TaskStatus) => {
    if (!selectedTask) {
      return
    }

    setUpdating(true)
    setError(null)

    try {
      const response = await apiFetch(`/api/tasks/${selectedTask.id}/status`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ status }),
      })

      if (!response.ok) {
        if (response.status === 401) {
          setError('ログインが必要です')
        } else if (response.status === 404) {
          setError('対象タスクが見つかりません')
        } else {
          setError('ステータス更新に失敗しました')
        }
        return
      }

      const updated = (await response.json()) as TaskItem
      setTasks((prev) => prev.map((item) => (item.id === updated.id ? updated : item)))
    } catch {
      setError('サーバーに接続できませんでした')
    } finally {
      setUpdating(false)
    }
  }

  const updateFolderPath = async (folderPath: string | null) => {
    if (!selectedTask) {
      return
    }

    setSavingFolderPath(true)
    setError(null)

    try {
      const response = await apiFetch(`/api/tasks/${selectedTask.id}/folder-path`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ folderPath }),
      })

      if (!response.ok) {
        if (response.status === 401) {
          setError('ログインが必要です')
        } else if (response.status === 404) {
          setError('対象タスクが見つかりません')
        } else {
          setError('フォルダパスの保存に失敗しました')
        }
        return
      }

      const updated = (await response.json()) as TaskItem
      setTasks((prev) => prev.map((item) => (item.id === updated.id ? updated : item)))
    } catch {
      setError('サーバーに接続できませんでした')
    } finally {
      setSavingFolderPath(false)
    }
  }

  return (
    <main className="min-h-screen bg-gray-50 px-4 py-8">
      <div className="mx-auto max-w-7xl">
        <header className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900">タスクカレンダー</h1>
          <p className="mt-2 text-sm text-gray-600">月/週表示を切り替えて納期タスクを確認できます。</p>
        </header>

        {error && (
          <p role="alert" className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </p>
        )}

        {loading ? (
          <p className="text-sm text-gray-600">読み込み中...</p>
        ) : (
          <section className="grid gap-4 lg:grid-cols-[1.3fr_1fr]">
            <CalendarView
              tasks={tasks}
              currentDate={currentDate}
              currentView={currentView}
              onViewChange={setCurrentView}
              onDateChange={setCurrentDate}
              onSelectTask={setSelectedTaskId}
            />

            <DetailPanel
              task={selectedTask}
              updating={updating}
              savingFolderPath={savingFolderPath}
              onClose={() => setSelectedTaskId(null)}
              onUpdateStatus={updateStatus}
              onUpdateFolderPath={updateFolderPath}
            />
          </section>
        )}
      </div>
    </main>
  )
}

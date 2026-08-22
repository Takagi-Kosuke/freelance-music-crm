'use client'

import { useEffect, useMemo, useState } from 'react'
import { DetailPanel, type TaskItem } from '@/components/tasks/DetailPanel'
import { TaskStatusBadge, type TaskStatus } from '@/components/tasks/TaskStatusBadge'
import { apiFetch } from '@/lib/api'
import { getCsrfToken } from '@/lib/csrf'

type Category = {
  id: number
  name: string
}

export default function TasksPage() {
  const [tasks, setTasks] = useState<TaskItem[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [selectedCategoryId, setSelectedCategoryId] = useState<string>('all')
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
    const loadCategories = async () => {
      try {
        const response = await apiFetch('/api/order-categories', {
          method: 'GET',
        })

        if (!response.ok) {
          throw new Error('依頼区分の取得に失敗しました')
        }

        const data = (await response.json()) as Category[]
        setCategories(data)
      } catch {
        setError('依頼区分の取得に失敗しました')
      }
    }

    loadCategories()
  }, [])

  useEffect(() => {
    const loadTasks = async () => {
      setLoading(true)
      setError(null)

      try {
        const query = selectedCategoryId === 'all' ? '' : `?categoryId=${encodeURIComponent(selectedCategoryId)}`
        const response = await apiFetch(`/api/tasks${query}`, {
          method: 'GET',
        })

        if (!response.ok) {
          if (response.status === 401) {
            setError('ログインが必要です')
          } else {
            setError('タスク一覧の取得に失敗しました')
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

    loadTasks()
  }, [selectedCategoryId])

  const updateStatus = async (status: TaskStatus) => {
    if (!selectedTask) {
      return
    }

    setUpdating(true)
    setError(null)

    try {
      const csrf = await getCsrfToken()
      const response = await apiFetch(`/api/tasks/${selectedTask.id}/status`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          [csrf.headerName]: csrf.token,
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
      const csrf = await getCsrfToken()
      const response = await apiFetch(`/api/tasks/${selectedTask.id}/folder-path`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          [csrf.headerName]: csrf.token,
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
          <h1 className="text-2xl font-bold text-gray-900">タスク管理</h1>
          <p className="mt-2 text-sm text-gray-600">一覧・依頼区分フィルタ・詳細パネルからタスク進行を管理します。</p>
        </header>

        <div className="mb-4 flex flex-wrap items-center gap-3 rounded-lg border border-gray-200 bg-white p-4">
          <label htmlFor="categoryFilter" className="text-sm font-medium text-gray-700">
            依頼区分フィルタ
          </label>
          <select
            id="categoryFilter"
            className="min-h-[40px] rounded-md border border-gray-300 px-3 py-2 text-sm"
            value={selectedCategoryId}
            onChange={(e) => setSelectedCategoryId(e.target.value)}
          >
            <option value="all">すべて</option>
            {categories.map((category) => (
              <option key={category.id} value={String(category.id)}>
                {category.name}
              </option>
            ))}
          </select>
          <span className="text-xs text-gray-500">件数: {tasks.length}</span>
        </div>

        {error && (
          <p role="alert" className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </p>
        )}

        <section className="grid gap-4 lg:grid-cols-[1.3fr_1fr]">
          <div className="rounded-xl border border-gray-200 bg-white">
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead className="bg-gray-100 text-gray-700">
                  <tr>
                    <th className="px-4 py-3 text-left">ID</th>
                    <th className="px-4 py-3 text-left">件名</th>
                    <th className="px-4 py-3 text-left">区分</th>
                    <th className="px-4 py-3 text-left">希望納期</th>
                    <th className="px-4 py-3 text-left">ステータス</th>
                  </tr>
                </thead>
                <tbody>
                  {tasks.map((task) => {
                    const active = selectedTaskId === task.id
                    return (
                      <tr
                        key={task.id}
                        onClick={() => setSelectedTaskId(task.id)}
                        className={`cursor-pointer border-t border-gray-100 ${
                          active ? 'bg-[#EAF1F6]' : 'hover:bg-gray-50'
                        }`}
                      >
                        <td className="px-4 py-3">{task.id}</td>
                        <td className="px-4 py-3 font-medium text-gray-900">{task.orderSubject}</td>
                        <td className="px-4 py-3">{task.categoryName}</td>
                        <td className="px-4 py-3">{task.desiredDeliveryDate}</td>
                        <td className="px-4 py-3">
                          <TaskStatusBadge status={task.status} />
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            {loading && <p className="p-4 text-sm text-gray-600">読み込み中...</p>}
            {!loading && tasks.length === 0 && <p className="p-4 text-sm text-gray-600">タスクはありません。</p>}
          </div>

          <DetailPanel
            task={selectedTask}
            updating={updating}
            savingFolderPath={savingFolderPath}
            onClose={() => setSelectedTaskId(null)}
            onUpdateStatus={updateStatus}
            onUpdateFolderPath={updateFolderPath}
          />
        </section>
      </div>
    </main>
  )
}

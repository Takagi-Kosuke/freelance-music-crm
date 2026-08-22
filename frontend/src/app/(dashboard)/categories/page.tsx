'use client'

import React, { useEffect, useState } from 'react'
import { getCsrfToken } from '@/lib/csrf'

type Category = {
  id: number
  name: string
  isDefault: boolean
}

type ApiError = {
  message?: string
}

export default function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [newName, setNewName] = useState('')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editingName, setEditingName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const loadCategories = async () => {
    setLoading(true)
    setError(null)

    try {
      const response = await fetch('/api/order-categories', {
        method: 'GET',
        credentials: 'include',
      })

      if (!response.ok) {
        setError('依頼区分の取得に失敗しました')
        return
      }

      const data = (await response.json()) as Category[]
      setCategories(data)
    } catch {
      setError('サーバーに接続できませんでした')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadCategories()
  }, [])

  const createCategory = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()

    if (!newName.trim()) {
      setError('区分名は必須です')
      return
    }

    setSaving(true)
    setError(null)
    setSuccess(null)

    try {
      const csrf = await getCsrfToken()
      const response = await fetch('/api/order-categories', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify({ name: newName.trim() }),
      })

      if (!response.ok) {
        const body = (await response.json()) as ApiError
        setError(body.message ?? '依頼区分の追加に失敗しました')
        return
      }

      setNewName('')
      setSuccess('依頼区分を追加しました')
      await loadCategories()
    } catch {
      setError('サーバーに接続できませんでした')
    } finally {
      setSaving(false)
    }
  }

  const saveCategory = async (id: number) => {
    if (!editingName.trim()) {
      setError('区分名は必須です')
      return
    }

    setSaving(true)
    setError(null)
    setSuccess(null)

    try {
      const csrf = await getCsrfToken()
      const response = await fetch(`/api/order-categories/${id}`, {
        method: 'PUT',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify({ name: editingName.trim() }),
      })

      if (!response.ok) {
        const body = (await response.json()) as ApiError
        setError(body.message ?? '依頼区分の更新に失敗しました')
        return
      }

      setEditingId(null)
      setEditingName('')
      setSuccess('依頼区分を更新しました')
      await loadCategories()
    } catch {
      setError('サーバーに接続できませんでした')
    } finally {
      setSaving(false)
    }
  }

  const deleteCategory = async (id: number) => {
    setSaving(true)
    setError(null)
    setSuccess(null)

    try {
      const csrf = await getCsrfToken()
      const response = await fetch(`/api/order-categories/${id}`, {
        method: 'DELETE',
        credentials: 'include',
        headers: {
          [csrf.headerName]: csrf.token,
        },
      })

      if (!response.ok) {
        const body = (await response.json()) as ApiError
        if (response.status === 422) {
          setError(body.message ?? '使用中の区分は削除できません')
        } else {
          setError(body.message ?? '依頼区分の削除に失敗しました')
        }
        return
      }

      setSuccess('依頼区分を削除しました')
      await loadCategories()
    } catch {
      setError('サーバーに接続できませんでした')
    } finally {
      setSaving(false)
    }
  }

  return (
    <main className="min-h-screen bg-white px-4 py-8">
      <div className="mx-auto max-w-4xl">
        <h1 className="mb-2 text-2xl font-bold text-gray-900">依頼区分管理</h1>
        <p className="mb-6 text-sm text-gray-600">区分の追加・編集・削除を行います。</p>

        <form onSubmit={createCategory} className="mb-6 flex flex-col gap-3 rounded-lg border border-gray-200 p-4 sm:flex-row sm:items-center">
          <label htmlFor="newCategory" className="text-sm font-medium text-gray-700 sm:w-32">
            新規区分
          </label>
          <input
            id="newCategory"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            maxLength={50}
            className="min-h-[44px] flex-1 rounded-md border border-gray-300 px-3 py-2 text-sm"
            placeholder="例: アレンジ"
          />
          <button
            type="submit"
            disabled={saving}
            className="min-h-[44px] rounded-md bg-[#145C9E] px-4 py-2 text-sm font-semibold text-white hover:bg-[#0B4F6C] disabled:opacity-50"
          >
            追加
          </button>
        </form>

        {error && (
          <p role="alert" className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </p>
        )}
        {success && (
          <p role="status" className="mb-4 rounded-md border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-700">
            {success}
          </p>
        )}

        <div className="overflow-x-auto rounded-lg border border-gray-200">
          <table className="min-w-full text-sm">
            <thead className="bg-gray-50 text-gray-700">
              <tr>
                <th className="px-4 py-3 text-left">ID</th>
                <th className="px-4 py-3 text-left">区分名</th>
                <th className="px-4 py-3 text-left">初期区分</th>
                <th className="px-4 py-3 text-left">操作</th>
              </tr>
            </thead>
            <tbody>
              {categories.map((category) => {
                const isEditing = editingId === category.id
                return (
                  <tr key={category.id} className="border-t border-gray-100">
                    <td className="px-4 py-3">{category.id}</td>
                    <td className="px-4 py-3">
                      {isEditing ? (
                        <input
                          value={editingName}
                          onChange={(e) => setEditingName(e.target.value)}
                          maxLength={50}
                          className="min-h-[44px] w-full rounded-md border border-gray-300 px-3 py-2"
                        />
                      ) : (
                        category.name
                      )}
                    </td>
                    <td className="px-4 py-3">{category.isDefault ? 'はい' : 'いいえ'}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-2">
                        {isEditing ? (
                          <>
                            <button
                              type="button"
                              onClick={() => saveCategory(category.id)}
                              disabled={saving}
                              className="min-h-[44px] rounded-md border border-[#CBB9A8] bg-[#EAF1F6] px-3 py-2 text-xs font-semibold text-[#0B4F6C]"
                            >
                              保存
                            </button>
                            <button
                              type="button"
                              onClick={() => {
                                setEditingId(null)
                                setEditingName('')
                              }}
                              disabled={saving}
                              className="min-h-[44px] rounded-md border border-gray-300 bg-white px-3 py-2 text-xs font-semibold text-gray-700"
                            >
                              キャンセル
                            </button>
                          </>
                        ) : (
                          <>
                            <button
                              type="button"
                              onClick={() => {
                                setEditingId(category.id)
                                setEditingName(category.name)
                              }}
                              disabled={saving}
                              className="min-h-[44px] rounded-md border border-gray-300 bg-white px-3 py-2 text-xs font-semibold text-gray-700"
                            >
                              編集
                            </button>
                            <button
                              type="button"
                              onClick={() => deleteCategory(category.id)}
                              disabled={saving}
                              className="min-h-[44px] rounded-md border border-red-300 bg-red-50 px-3 py-2 text-xs font-semibold text-red-700"
                            >
                              削除
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>

          {!loading && categories.length === 0 && (
            <p className="p-4 text-sm text-gray-600">依頼区分はありません。</p>
          )}
          {loading && <p className="p-4 text-sm text-gray-600">読み込み中...</p>}
        </div>
      </div>
    </main>
  )
}

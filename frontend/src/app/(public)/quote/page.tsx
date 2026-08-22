'use client'

import React from 'react'
import { useEffect, useState } from 'react'
import { apiFetch } from '@/lib/api'
import { getCsrfToken } from '@/lib/csrf'

type Category = {
  id: number
  name: string
  isDefault: boolean
}

type ApiError = {
  message?: string
  fieldErrors?: Array<{ field: string; message: string }>
}

export default function QuotePage() {
  const [categories, setCategories] = useState<Category[]>([])
  const [loadingCategories, setLoadingCategories] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const [subject, setSubject] = useState('')
  const [clientName, setClientName] = useState('')
  const [clientEmail, setClientEmail] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [desiredDeliveryDate, setDesiredDeliveryDate] = useState('')
  const [filePathUrl, setFilePathUrl] = useState('')
  const [comment, setComment] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    const loadCategories = async () => {
      setLoadingCategories(true)
      setErrorMessage(null)

      try {
        const res = await apiFetch('/api/order-categories', {
          method: 'GET',
          signal: controller.signal,
        })

        if (!res.ok) {
          throw new Error('依頼区分の取得に失敗しました')
        }

        const data = (await res.json()) as Category[]
        setCategories(data)
        setErrorMessage(null)
        if (data.length > 0) {
          setCategoryId(String(data[0].id))
        }
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') {
          return
        }
        setErrorMessage('依頼区分を読み込めませんでした')
      } finally {
        setLoadingCategories(false)
      }
    }

    loadCategories()

    return () => {
      controller.abort()
    }
  }, [])

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setSubmitting(true)
    setSuccessMessage(null)
    setErrorMessage(null)
    setFieldErrors({})

    try {
      const csrf = await getCsrfToken()

      const response = await apiFetch('/api/quote-requests', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify({
          subject,
          clientName,
          clientEmail: clientEmail || null,
          categoryId: Number(categoryId),
          desiredDeliveryDate,
          filePathUrl: filePathUrl || null,
          comment: comment || null,
        }),
      })

      if (!response.ok) {
        const data = (await response.json()) as ApiError
        if (data.fieldErrors && data.fieldErrors.length > 0) {
          const mappedErrors: Record<string, string> = {}
          for (const fieldError of data.fieldErrors) {
            mappedErrors[fieldError.field] = fieldError.message
          }
          setFieldErrors(mappedErrors)
        }
        setErrorMessage(data.message ?? '見積依頼の送信に失敗しました')
        return
      }

      setSuccessMessage('見積依頼を受け付けました')
      setSubject('')
      setClientName('')
      setClientEmail('')
      setDesiredDeliveryDate('')
      setFilePathUrl('')
      setComment('')
      if (categories.length > 0) {
        setCategoryId(String(categories[0].id))
      }
    } catch {
      setErrorMessage('サーバーに接続できませんでした')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="min-h-screen px-4 py-8">
      <div className="max-w-2xl mx-auto rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
        <h1 className="text-2xl font-bold text-[#1F271B] mb-6">見積依頼フォーム</h1>

        {loadingCategories ? (
          <p className="text-gray-600">依頼区分を読み込み中...</p>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="subject" className="block text-sm font-medium text-gray-700 mb-1">依頼件名</label>
              <input
                id="subject"
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                required
                className="w-full border border-[#CBB9A8] rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#145C9E]"
              />
              {fieldErrors.subject && <p className="text-sm text-red-600 mt-1">{fieldErrors.subject}</p>}
            </div>

            <div>
              <label htmlFor="clientName" className="block text-sm font-medium text-gray-700 mb-1">依頼者名</label>
              <input
                id="clientName"
                value={clientName}
                onChange={(e) => setClientName(e.target.value)}
                required
                className="w-full border border-[#CBB9A8] rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#145C9E]"
              />
              {fieldErrors.clientName && <p className="text-sm text-red-600 mt-1">{fieldErrors.clientName}</p>}
            </div>

            <div>
              <label htmlFor="clientEmail" className="block text-sm font-medium text-gray-700 mb-1">依頼者メールアドレス（任意）</label>
              <input
                id="clientEmail"
                type="email"
                value={clientEmail}
                onChange={(e) => setClientEmail(e.target.value)}
                className="w-full border border-[#CBB9A8] rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#145C9E]"
              />
              {fieldErrors.clientEmail && <p className="text-sm text-red-600 mt-1">{fieldErrors.clientEmail}</p>}
            </div>

            <div>
              <label htmlFor="category" className="block text-sm font-medium text-gray-700 mb-1">依頼区分</label>
              <select
                id="category"
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
                required
                className="w-full border border-[#CBB9A8] rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#145C9E]"
              >
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
              {fieldErrors.categoryId && <p className="text-sm text-red-600 mt-1">{fieldErrors.categoryId}</p>}
            </div>

            <div>
              <label htmlFor="desiredDeliveryDate" className="block text-sm font-medium text-gray-700 mb-1">希望納期</label>
              <input
                id="desiredDeliveryDate"
                type="date"
                value={desiredDeliveryDate}
                onChange={(e) => setDesiredDeliveryDate(e.target.value)}
                required
                className="w-full border border-[#CBB9A8] rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#145C9E]"
              />
              {fieldErrors.desiredDeliveryDate && <p className="text-sm text-red-600 mt-1">{fieldErrors.desiredDeliveryDate}</p>}
            </div>

            <div>
              <label htmlFor="filePathUrl" className="block text-sm font-medium text-gray-700 mb-1">ファイルURL（任意）</label>
              <input
                id="filePathUrl"
                type="url"
                value={filePathUrl}
                onChange={(e) => setFilePathUrl(e.target.value)}
                placeholder="https://example.com/shared-file"
                className="w-full border border-[#CBB9A8] rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#145C9E]"
              />
              {fieldErrors.filePathUrl && <p className="text-sm text-red-600 mt-1">{fieldErrors.filePathUrl}</p>}
            </div>

            <div>
              <label htmlFor="comment" className="block text-sm font-medium text-gray-700 mb-1">コメント（任意）</label>
              <textarea
                id="comment"
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                maxLength={1000}
                rows={5}
                className="w-full border border-[#CBB9A8] rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#145C9E]"
              />
              <p className="text-xs text-gray-500 mt-1">{comment.length}/1000</p>
              {fieldErrors.comment && <p className="text-sm text-red-600 mt-1">{fieldErrors.comment}</p>}
            </div>

            {errorMessage && <p role="alert" className="text-sm text-red-600">{errorMessage}</p>}
            {successMessage && <p role="status" className="text-sm text-green-700">{successMessage}</p>}

            <button
              type="submit"
              disabled={submitting}
              className="w-full py-3 text-white font-semibold rounded-md transition-colors disabled:opacity-50 min-h-[44px]"
              style={{ backgroundColor: '#145C9E' }}
            >
              {submitting ? '送信中...' : '見積依頼を送信'}
            </button>
          </form>
        )}
      </div>
    </main>
  )
}

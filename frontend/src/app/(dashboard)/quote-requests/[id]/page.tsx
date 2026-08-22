'use client'

import React from 'react'
import Link from 'next/link'
import { useEffect, useState } from 'react'
import { getCsrfToken } from '@/lib/csrf'

type QuoteRequestDetail = {
  id: number
  subject: string
  clientName: string
  clientEmail: string | null
  categoryName: string
  desiredDeliveryDate: string
  filePathUrl: string | null
  comment: string | null
  status: string
  createdAt: string
}

type QuoteResponseCreateResponse = {
  id: number
  quoteRequestId: number
  amount: number
  responseDeliveryDate: string
  responseComment: string | null
  approvalToken: string
  tokenStatus: 'ACTIVE' | 'USED' | 'EXPIRED'
  createdAt: string
}

type ApiError = {
  message?: string
  fieldErrors?: Array<{ field: string; message: string }>
}

type PageProps = {
  params: {
    id: string
  }
}

export default function QuoteRequestDetailPage({ params }: PageProps) {
  const [item, setItem] = useState<QuoteRequestDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)
  const [responseToken, setResponseToken] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [amount, setAmount] = useState('')
  const [responseDeliveryDate, setResponseDeliveryDate] = useState('')
  const [responseComment, setResponseComment] = useState('')

  useEffect(() => {
    const load = async () => {
      try {
        const response = await fetch(`/api/quote-requests/${params.id}`, {
          method: 'GET',
          credentials: 'include',
        })

        if (!response.ok) {
          if (response.status === 401) {
            setError('ログインが必要です')
          } else if (response.status === 404) {
            setError('見積依頼が見つかりません')
          } else {
            setError('見積依頼詳細の取得に失敗しました')
          }
          return
        }

        const data = (await response.json()) as QuoteRequestDetail
        setItem(data)
      } catch {
        setError('サーバーに接続できませんでした')
      } finally {
        setLoading(false)
      }
    }

    load()
  }, [params.id])

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()

    if (!item) {
      return
    }

    setSubmitting(true)
    setError(null)
    setSuccessMessage(null)
    setResponseToken(null)
    setFieldErrors({})

    try {
      const csrf = await getCsrfToken()
      const response = await fetch('/api/quote-responses', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify({
          quoteRequestId: item.id,
          amount: Number(amount),
          responseDeliveryDate,
          responseComment: responseComment || null,
        }),
      })

      if (!response.ok) {
        const data = (await response.json()) as ApiError
        if (data.fieldErrors?.length) {
          const mappedErrors: Record<string, string> = {}
          for (const fieldError of data.fieldErrors) {
            mappedErrors[fieldError.field] = fieldError.message
          }
          setFieldErrors(mappedErrors)
        }
        setError(data.message ?? '見積回答の送信に失敗しました')
        return
      }

      const created = (await response.json()) as QuoteResponseCreateResponse
      setSuccessMessage('見積回答を作成しました')
      setResponseToken(created.approvalToken)
      setAmount('')
      setResponseComment('')
    } catch {
      setError('サーバーに接続できませんでした')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="min-h-screen bg-white px-4 py-8">
      <div className="max-w-3xl mx-auto">
        <div className="mb-4">
          <Link href="/quote-requests" className="text-[#0B4F6C] hover:underline">
            一覧へ戻る
          </Link>
        </div>

        <h1 className="text-2xl font-bold text-gray-900 mb-6">見積依頼詳細</h1>

        {loading && <p className="text-gray-600">読み込み中...</p>}
        {error && <p role="alert" className="text-red-600">{error}</p>}

        {!loading && !error && item && (
          <div className="space-y-6">
            <section className="space-y-4 border border-gray-200 rounded-lg p-6">
              <Field label="ID" value={String(item.id)} />
              <Field label="件名" value={item.subject} />
              <Field label="依頼者" value={item.clientName} />
              <Field label="依頼者メール" value={item.clientEmail ?? '-'} />
              <Field label="依頼区分" value={item.categoryName} />
              <Field label="希望納期" value={item.desiredDeliveryDate} />
              <Field label="ステータス" value={item.status} />
              <Field label="登録日時" value={item.createdAt} />
              <Field label="ファイルURL" value={item.filePathUrl ?? '-'} />
              <Field label="コメント" value={item.comment ?? '-'} multiline />
            </section>

            <section className="space-y-4 border border-gray-200 rounded-lg p-6">
              <div>
                <h2 className="text-lg font-semibold text-gray-900">見積回答</h2>
                <p className="mt-1 text-sm text-gray-600">
                  作業者が見積金額と回答納期を入力してクライアントへ回答します。
                </p>
              </div>

              {item.status !== 'PENDING' && (
                <p className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
                  この見積依頼は既に回答済み、承認済み、または辞退済みです。
                </p>
              )}

              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label htmlFor="amount" className="block text-sm font-medium text-gray-700 mb-1">
                    見積金額
                  </label>
                  <input
                    id="amount"
                    type="number"
                    min="0"
                    step="1"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    disabled={item.status !== 'PENDING'}
                    required
                    className="w-full rounded-md border border-gray-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#145C9E] disabled:bg-gray-100"
                  />
                  {fieldErrors.amount && <p className="mt-1 text-sm text-red-600">{fieldErrors.amount}</p>}
                </div>

                <div>
                  <label htmlFor="responseDeliveryDate" className="block text-sm font-medium text-gray-700 mb-1">
                    回答納期
                  </label>
                  <input
                    id="responseDeliveryDate"
                    type="date"
                    value={responseDeliveryDate}
                    onChange={(e) => setResponseDeliveryDate(e.target.value)}
                    disabled={item.status !== 'PENDING'}
                    required
                    className="w-full rounded-md border border-gray-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#145C9E] disabled:bg-gray-100"
                  />
                  {fieldErrors.responseDeliveryDate && <p className="mt-1 text-sm text-red-600">{fieldErrors.responseDeliveryDate}</p>}
                </div>

                <div>
                  <label htmlFor="responseComment" className="block text-sm font-medium text-gray-700 mb-1">
                    回答コメント（任意）
                  </label>
                  <textarea
                    id="responseComment"
                    value={responseComment}
                    onChange={(e) => setResponseComment(e.target.value)}
                    disabled={item.status !== 'PENDING'}
                    maxLength={1000}
                    rows={5}
                    className="w-full rounded-md border border-gray-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#145C9E] disabled:bg-gray-100"
                  />
                  <p className="mt-1 text-xs text-gray-500">{responseComment.length}/1000</p>
                  {fieldErrors.responseComment && <p className="mt-1 text-sm text-red-600">{fieldErrors.responseComment}</p>}
                </div>

                {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
                {successMessage && <p role="status" className="text-sm text-green-700">{successMessage}</p>}

                {responseToken && (
                  <p className="rounded-md border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-800">
                    クライアント確認URL:{' '}
                    <Link href={`/orders/${responseToken}`} className="font-semibold underline">
                      /orders/{responseToken}
                    </Link>
                  </p>
                )}

                <button
                  type="submit"
                  disabled={submitting || item.status !== 'PENDING'}
                  className="min-h-[44px] rounded-md bg-[#145C9E] px-5 py-3 text-sm font-semibold text-white hover:bg-[#0B4F6C] disabled:opacity-50"
                >
                  {submitting ? '送信中...' : '見積回答を送信'}
                </button>
              </form>
            </section>
          </div>
        )}
      </div>
    </main>
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
      <p className="text-xs font-semibold text-gray-500 mb-1">{label}</p>
      {multiline ? (
        <p className="text-gray-900 whitespace-pre-wrap rounded-md bg-gray-50 p-3">{value}</p>
      ) : (
        <p className="text-gray-900">{value}</p>
      )}
    </div>
  )
}

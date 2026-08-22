'use client'

import React, { useEffect, useState } from 'react'
import { buildApiUrl } from '@/lib/api'

type QuoteResponse = {
  id: number
  quoteRequestId: number
  amount: number
  responseDeliveryDate: string
  responseComment: string | null
  approvalToken: string
  tokenStatus: 'ACTIVE' | 'USED' | 'EXPIRED'
  createdAt: string
}

type PageProps = {
  params: {
    token: string
  }
}

export default function OrderTokenPage({ params }: PageProps) {
  const [quoteResponse, setQuoteResponse] = useState<QuoteResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionMessage, setActionMessage] = useState<string | null>(null)
  const [processing, setProcessing] = useState(false)

  useEffect(() => {
    const load = async () => {
      try {
        const res = await fetch(buildApiUrl(`/api/quote-responses/token/${params.token}`), {
          method: 'GET',
          credentials: 'omit',
        })

        if (!res.ok) {
          if (res.status === 404) {
            setError('見積回答が見つかりません')
          } else {
            setError('見積回答の取得に失敗しました')
          }
          return
        }

        const data = (await res.json()) as QuoteResponse
        setQuoteResponse(data)
      } catch {
        setError('サーバーに接続できませんでした')
      } finally {
        setLoading(false)
      }
    }

    load()
  }, [params.token])

  const handleAction = async (action: 'approve' | 'decline') => {
    setProcessing(true)
    setActionMessage(null)

    try {
      const res = await fetch(buildApiUrl(`/api/orders/token/${params.token}/${action}`), {
        method: 'POST',
        credentials: 'omit',
        headers: {
          'Content-Type': 'application/json',
        },
      })

      if (!res.ok) {
        setActionMessage('現在この操作は利用できません')
        return
      }

      setActionMessage(action === 'approve' ? '正式依頼を承認しました' : '依頼を辞退しました')
    } catch {
      setActionMessage('操作に失敗しました')
    } finally {
      setProcessing(false)
    }
  }

  return (
    <main className="min-h-screen px-4 py-8">
      <div className="max-w-2xl mx-auto rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
        <h1 className="text-2xl font-bold text-[#1F271B] mb-6">見積確認</h1>

        {loading && <p className="text-gray-600">読み込み中...</p>}
        {error && <p role="alert" className="text-red-600">{error}</p>}

        {!loading && !error && quoteResponse && (
          <div className="space-y-4 border border-gray-200 rounded-lg p-6">
            <Row label="見積ID" value={String(quoteResponse.id)} />
            <Row label="見積金額" value={`¥${quoteResponse.amount.toLocaleString()}`} />
            <Row label="回答納期" value={quoteResponse.responseDeliveryDate} />
            <Row label="回答コメント" value={quoteResponse.responseComment ?? '-'} multiline />

            <div className="pt-2 grid grid-cols-1 sm:grid-cols-2 gap-3">
              <button
                type="button"
                disabled={processing}
                onClick={() => handleAction('approve')}
                className="min-h-[44px] rounded-md text-white font-semibold disabled:opacity-50"
                style={{ backgroundColor: '#145C9E' }}
              >
                承認する
              </button>
              <button
                type="button"
                disabled={processing}
                onClick={() => handleAction('decline')}
                className="min-h-[44px] rounded-md border border-[#CBB9A8] text-[#1F271B] font-semibold bg-white disabled:opacity-50"
              >
                辞退する
              </button>
            </div>

            {actionMessage && <p className="text-sm text-gray-700">{actionMessage}</p>}
          </div>
        )}
      </div>
    </main>
  )
}

type RowProps = {
  label: string
  value: string
  multiline?: boolean
}

function Row({ label, value, multiline = false }: RowProps) {
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

'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'

type QuoteRequest = {
  id: number
  subject: string
  clientName: string
  categoryName: string
  desiredDeliveryDate: string
  status: 'PENDING' | 'RESPONDED' | 'APPROVED' | 'DECLINED'
}

const STATUS_LABELS: Record<QuoteRequest['status'], string> = {
  PENDING: '受付中',
  RESPONDED: '見積回答済',
  APPROVED: '承認済',
  DECLINED: '辞退',
}

export default function QuoteRequestsPage() {
  const [items, setItems] = useState<QuoteRequest[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const load = async () => {
      try {
        const response = await fetch('/api/quote-requests', {
          method: 'GET',
          credentials: 'include',
        })

        if (!response.ok) {
          if (response.status === 401) {
            setError('ログインが必要です')
          } else {
            setError('見積依頼一覧の取得に失敗しました')
          }
          return
        }

        const data = (await response.json()) as QuoteRequest[]
        setItems(data)
      } catch {
        setError('サーバーに接続できませんでした')
      } finally {
        setLoading(false)
      }
    }

    load()
  }, [])

  return (
    <main className="min-h-screen bg-white px-4 py-8">
      <div className="max-w-5xl mx-auto">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">見積依頼一覧</h1>

        {loading && <p className="text-gray-600">読み込み中...</p>}
        {error && <p role="alert" className="text-red-600">{error}</p>}

        {!loading && !error && (
          <div className="overflow-x-auto border border-gray-200 rounded-lg">
            <table className="min-w-full text-sm">
              <thead className="bg-gray-50 text-gray-700">
                <tr>
                  <th className="text-left px-4 py-3">ID</th>
                  <th className="text-left px-4 py-3">件名</th>
                  <th className="text-left px-4 py-3">依頼者</th>
                  <th className="text-left px-4 py-3">区分</th>
                  <th className="text-left px-4 py-3">希望納期</th>
                  <th className="text-left px-4 py-3">ステータス</th>
                  <th className="text-left px-4 py-3">操作</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.id} className="border-t border-gray-100 hover:bg-[#EFE4DB]">
                    <td className="px-4 py-3">
                      <Link href={`/quote-requests/${item.id}`} className="text-[#0B4F6C] hover:underline">
                        {item.id}
                      </Link>
                    </td>
                    <td className="px-4 py-3">{item.subject}</td>
                    <td className="px-4 py-3">{item.clientName}</td>
                    <td className="px-4 py-3">{item.categoryName}</td>
                    <td className="px-4 py-3">{item.desiredDeliveryDate}</td>
                    <td className="px-4 py-3">{STATUS_LABELS[item.status] ?? item.status}</td>
                    <td className="px-4 py-3">
                      <Link href={`/quote-requests/${item.id}`} className="text-[#0B4F6C] hover:underline">
                        詳細・回答
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {items.length === 0 && (
              <p className="p-6 text-gray-600">見積依頼はまだありません</p>
            )}
          </div>
        )}
      </div>
    </main>
  )
}

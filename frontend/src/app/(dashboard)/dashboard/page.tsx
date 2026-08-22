'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { apiFetch } from '@/lib/api'

type QuoteRequest = { id: number; status: string }
type Task = { id: number; status: string }

type Summary = {
  quoteRequests: number
  openTasks: number
  completedTasks: number
}

export default function DashboardPage() {
  const [summary, setSummary] = useState<Summary>({ quoteRequests: 0, openTasks: 0, completedTasks: 0 })
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const load = async () => {
      try {
        const [quoteRes, taskRes] = await Promise.all([
          apiFetch('/api/quote-requests'),
          apiFetch('/api/tasks'),
        ])

        if (!quoteRes.ok || !taskRes.ok) {
          setError('ダッシュボード情報の取得に失敗しました')
          return
        }

        const quotes = (await quoteRes.json()) as QuoteRequest[]
        const tasks = (await taskRes.json()) as Task[]
        setSummary({
          quoteRequests: quotes.length,
          openTasks: tasks.filter((task) => task.status !== 'COMPLETED').length,
          completedTasks: tasks.filter((task) => task.status === 'COMPLETED').length,
        })
      } catch {
        setError('サーバーに接続できませんでした')
      }
    }

    load()
  }, [])

  return (
    <main className="min-h-screen px-4 py-8">
      <div className="mx-auto max-w-6xl rounded-3xl border border-[#E5E7EB] bg-white p-5 shadow-sm md:p-7">
        <h1 className="text-3xl font-bold text-gray-900">ダッシュボード</h1>
        <p className="mt-2 text-sm text-gray-600">進行中案件の件数と主要画面への導線をまとめています。</p>

        {error && <p role="alert" className="mt-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}

        <section className="mt-6 grid gap-4 md:grid-cols-3">
          <KpiCard label="見積依頼数" value={summary.quoteRequests} />
          <KpiCard label="未完了タスク" value={summary.openTasks} />
          <KpiCard label="完了タスク" value={summary.completedTasks} />
        </section>

        <section className="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <ShortcutCard href="/quote-requests" title="見積依頼一覧" description="受信した依頼を確認して見積回答へ進みます。" />
          <ShortcutCard href="/tasks" title="タスク一覧" description="ステータス更新と詳細確認を行います。" />
          <ShortcutCard href="/tasks/calendar" title="カレンダー" description="納期ベースでスケジュールを確認します。" />
          <ShortcutCard href="/categories" title="依頼区分管理" description="区分の追加・編集・削除を行います。" />
          <ShortcutCard href="/settings" title="設定" description="Webhook とメール送信設定を管理します。" />
        </section>
      </div>
    </main>
  )
}

function KpiCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-2xl border border-[#E5E7EB] bg-white p-5 shadow-sm">
      <p className="text-sm font-medium text-gray-500">{label}</p>
      <p className="mt-3 text-3xl font-bold text-gray-900">{value}</p>
    </div>
  )
}

function ShortcutCard({ href, title, description }: { href: string; title: string; description: string }) {
  return (
    <Link href={href} className="rounded-2xl border border-[#E5E7EB] bg-white p-5 shadow-sm transition hover:border-[#D1D5DB] hover:bg-[#F9FAFB]">
      <h2 className="text-lg font-semibold text-gray-900">{title}</h2>
      <p className="mt-2 text-sm text-gray-600">{description}</p>
    </Link>
  )
}

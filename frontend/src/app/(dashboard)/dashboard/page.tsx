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
    <main className="min-h-screen px-3 py-6 md:px-4 md:py-8">
      <div className="mx-auto max-w-6xl rounded-[24px] border border-[#d9e3ec] bg-white/90 p-4 shadow-[0_20px_60px_rgba(15,23,42,0.05)] backdrop-blur-sm md:p-5">
        <h1 className="text-xl font-semibold tracking-[-0.05em] text-[#0f172a] md:text-2xl">ダッシュボード</h1>
        <p className="mt-2 text-[11px] text-[#475569] md:text-xs">進行中案件の件数と主要画面への導線をまとめています。</p>

        {error && <p role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-[11px] text-red-700 md:text-xs">{error}</p>}

        <section className="mt-5 grid gap-3 md:grid-cols-3">
          <KpiCard label="見積依頼数" value={summary.quoteRequests} />
          <KpiCard label="未完了タスク" value={summary.openTasks} />
          <KpiCard label="完了タスク" value={summary.completedTasks} />
        </section>

        <section className="mt-6 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
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
    <div className="rounded-2xl border border-[#d9e3ec] bg-[#f8fbff] p-3 shadow-[0_10px_24px_rgba(15,23,42,0.03)] md:p-4">
      <p className="text-[10px] font-medium text-[#475569] md:text-[11px]">{label}</p>
      <p className="mt-2 text-xl font-semibold tracking-[-0.05em] text-[#0f172a] md:text-2xl">{value}</p>
    </div>
  )
}

function ShortcutCard({ href, title, description }: { href: string; title: string; description: string }) {
  return (
    <Link href={href} className="rounded-2xl border border-[#d9e3ec] bg-white p-3 shadow-[0_10px_24px_rgba(15,23,42,0.03)] transition hover:border-[#a9c2d8] hover:bg-[#f4f9fd] md:p-4">
      <h2 className="text-sm font-semibold text-[#0f172a] md:text-base">{title}</h2>
      <p className="mt-2 text-[11px] text-[#475569] md:text-xs">{description}</p>
    </Link>
  )
}

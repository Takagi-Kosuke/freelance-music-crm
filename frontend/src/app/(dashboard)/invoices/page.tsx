'use client'

import React, { useEffect, useMemo, useState } from 'react'
import { apiFetch } from '@/lib/api'
import { getCsrfToken } from '@/lib/csrf'
import { InvoicePdfPreview } from '@/components/invoices/InvoicePdfPreview'

type TaskRow = {
  id: number
  orderId: number
  orderSubject: string
  clientName: string
  clientEmail: string | null
  desiredDeliveryDate: string
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
}

type InvoiceRow = {
  id: number
  taskId: number
  subject: string
  clientName: string
  clientEmail: string | null
  categoryName: string
  deliveryDate: string
  amount: string | number
  issueDate: string
  workerName: string
  workerContact: string | null
  createdAt: string
}

type ApiError = { message?: string }

export default function InvoicesPage() {
  const [tasks, setTasks] = useState<TaskRow[]>([])
  const [invoices, setInvoices] = useState<InvoiceRow[]>([])
  const [selectedTaskId, setSelectedTaskId] = useState<string>('')
  const [selectedInvoiceId, setSelectedInvoiceId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const completedTasks = useMemo(() => tasks.filter((task) => task.status === 'COMPLETED'), [tasks])

  const invoiceTaskIds = useMemo(() => new Set(invoices.map((invoice) => invoice.taskId)), [invoices])

  const selectedInvoice = useMemo(
    () => invoices.find((invoice) => invoice.id === selectedInvoiceId) ?? null,
    [invoices, selectedInvoiceId]
  )

  useEffect(() => {
    const load = async () => {
      try {
        const [taskRes, invoiceRes] = await Promise.all([
          apiFetch('/api/tasks'),
          apiFetch('/api/invoices'),
        ])

        if (!taskRes.ok || !invoiceRes.ok) {
          setError('請求書画面のデータ取得に失敗しました')
          return
        }

        const taskData = (await taskRes.json()) as TaskRow[]
        const invoiceData = (await invoiceRes.json()) as InvoiceRow[]
        setTasks(taskData)
        setInvoices(invoiceData)
      } catch {
        setError('サーバーに接続できませんでした')
      } finally {
        setLoading(false)
      }
    }

    load()
  }, [])

  const createInvoice = async () => {
    if (!selectedTaskId) {
      setError('請求書を発行するタスクを選択してください')
      return
    }

    setCreating(true)
    setError(null)
    setSuccess(null)

    try {
      const csrf = await getCsrfToken()
      const response = await apiFetch('/api/invoices', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify({ taskId: Number(selectedTaskId) }),
      })

      if (!response.ok) {
        const body = (await response.json()) as ApiError
        setError(body.message ?? '請求書の発行に失敗しました')
        return
      }

      const created = (await response.json()) as InvoiceRow
      setInvoices((prev) => [created, ...prev.filter((item) => item.id !== created.id)])
      setSelectedInvoiceId(created.id)
      setSuccess('請求書を発行しました')
      setSelectedTaskId('')
    } catch {
      setError('サーバーに接続できませんでした')
    } finally {
      setCreating(false)
    }
  }

  const downloadPdf = async (invoiceId: number) => {
    try {
      const response = await apiFetch(`/api/invoices/${invoiceId}/pdf`)

      if (!response.ok) {
        setError('請求書のダウンロードに失敗しました')
        return
      }

      const blob = await response.blob()
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `invoice-${invoiceId}.pdf`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    } catch {
      setError('ダウンロード中にエラーが発生しました')
    }
  }

  const sendEmail = async (invoiceId: number) => {
    setSending(true)
    setError(null)
    setSuccess(null)

    try {
      const csrf = await getCsrfToken()
      const response = await apiFetch(`/api/invoices/${invoiceId}/send-email`, {
        method: 'POST',
        headers: {
          [csrf.headerName]: csrf.token,
        },
      })

      if (!response.ok) {
        const body = (await response.json()) as ApiError
        setError(body.message ?? 'メール送信に失敗しました')
        return
      }

      setSuccess('請求書メールを送信しました')
    } catch {
      setError('サーバーに接続できませんでした')
    } finally {
      setSending(false)
    }
  }

  return (
    <main className="min-h-screen bg-[#DCC7BE] px-4 py-8">
      <div className="mx-auto max-w-7xl">
        <header className="mb-6">
          <h1 className="text-3xl font-bold text-gray-900">請求書管理</h1>
          <p className="mt-2 text-sm text-gray-600">完了済みタスクから請求書を発行し、PDF確認とメール送信を行います。</p>
        </header>

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

        <section className="grid gap-4 xl:grid-cols-[1.1fr_1fr]">
          <div className="space-y-4">
            <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
              <h2 className="text-lg font-semibold text-gray-900">請求書発行</h2>
              <p className="mt-1 text-sm text-gray-600">完了済みで未発行のタスクを選んで請求書を作成します。</p>

              <div className="mt-4 space-y-3">
                <label className="block">
                  <span className="mb-1 block text-sm font-medium text-gray-700">対象タスク</span>
                  <select
                    value={selectedTaskId}
                    onChange={(e) => setSelectedTaskId(e.target.value)}
                    className="min-h-[44px] w-full rounded-md border border-gray-300 px-3 py-2"
                  >
                    <option value="">選択してください</option>
                    {completedTasks.map((task) => {
                      const issued = invoiceTaskIds.has(task.id)
                      return (
                        <option key={task.id} value={task.id} disabled={issued}>
                          #{task.id} {task.orderSubject} {issued ? '(発行済み)' : ''}
                        </option>
                      )
                    })}
                  </select>
                </label>

                <button
                  type="button"
                  onClick={createInvoice}
                  disabled={creating || completedTasks.length === 0}
                  className="min-h-[44px] rounded-md bg-[#145C9E] px-5 py-3 text-sm font-semibold text-white hover:bg-[#0B4F6C] disabled:opacity-50"
                >
                  {creating ? '発行中...' : '請求書を発行'}
                </button>
              </div>
            </div>

            <div className="rounded-xl border border-gray-200 bg-white shadow-sm">
              <div className="border-b border-gray-100 px-5 py-4">
                <h2 className="text-lg font-semibold text-gray-900">請求書一覧</h2>
              </div>

              {loading ? (
                <p className="p-5 text-sm text-gray-600">読み込み中...</p>
              ) : invoices.length === 0 ? (
                <p className="p-5 text-sm text-gray-600">請求書はまだありません。</p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="min-w-full text-sm">
                    <thead className="bg-gray-50 text-gray-700">
                      <tr>
                        <th className="px-4 py-3 text-left">ID</th>
                        <th className="px-4 py-3 text-left">件名</th>
                        <th className="px-4 py-3 text-left">依頼者</th>
                        <th className="px-4 py-3 text-left">金額</th>
                        <th className="px-4 py-3 text-left">発行日</th>
                        <th className="px-4 py-3 text-left">操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      {invoices.map((invoice) => {
                        const active = selectedInvoiceId === invoice.id
                        return (
                          <tr key={invoice.id} className={`border-t border-gray-100 ${active ? 'bg-[#EAF1F6]' : 'hover:bg-gray-50'}`}>
                            <td className="px-4 py-3">{invoice.id}</td>
                            <td className="px-4 py-3 font-medium text-gray-900">{invoice.subject}</td>
                            <td className="px-4 py-3">{invoice.clientName}</td>
                            <td className="px-4 py-3">¥{Number(invoice.amount).toLocaleString()}</td>
                            <td className="px-4 py-3">{invoice.issueDate}</td>
                            <td className="px-4 py-3">
                              <div className="flex flex-wrap gap-2">
                                <button
                                  type="button"
                                  onClick={() => setSelectedInvoiceId(invoice.id)}
                                  className="rounded-md border border-gray-300 bg-white px-3 py-2 text-xs font-semibold text-gray-700 hover:bg-gray-50"
                                >
                                  選択
                                </button>
                                <button
                                  type="button"
                                  onClick={() => downloadPdf(invoice.id)}
                                  className="rounded-md border border-[#CBB9A8] bg-[#EAF1F6] px-3 py-2 text-xs font-semibold text-[#0B4F6C] hover:bg-[#D9E7F1]"
                                >
                                  PDFをDL
                                </button>
                                <button
                                  type="button"
                                  disabled={sending}
                                  onClick={() => sendEmail(invoice.id)}
                                  className="rounded-md bg-[#145C9E] px-3 py-2 text-xs font-semibold text-white hover:bg-[#0B4F6C] disabled:opacity-50"
                                >
                                  メール送信
                                </button>
                              </div>
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>

          <div className="space-y-4">
            {selectedInvoice && (
              <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
                <h2 className="text-lg font-semibold text-gray-900">請求書詳細</h2>
                <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
                  <Detail label="請求書ID" value={String(selectedInvoice.id)} />
                  <Detail label="タスクID" value={String(selectedInvoice.taskId)} />
                  <Detail label="件名" value={selectedInvoice.subject} />
                  <Detail label="依頼者" value={selectedInvoice.clientName} />
                  <Detail label="区分" value={selectedInvoice.categoryName} />
                  <Detail label="納品日" value={selectedInvoice.deliveryDate} />
                  <Detail label="金額" value={`¥${Number(selectedInvoice.amount).toLocaleString()}`} />
                  <Detail label="作業者" value={selectedInvoice.workerName} />
                </dl>
              </div>
            )}

            <InvoicePdfPreview invoiceId={selectedInvoiceId} />
          </div>
        </section>
      </div>
    </main>
  )
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md bg-gray-50 p-3">
      <dt className="text-xs font-semibold text-gray-500">{label}</dt>
      <dd className="mt-1 text-gray-900">{value}</dd>
    </div>
  )
}
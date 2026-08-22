'use client'

import React, { useEffect, useState } from 'react'
import { getCsrfToken } from '@/lib/csrf'

type Settings = {
  discordWebhookUrl: string | null
  discordEnabled: boolean
  smtpHost: string | null
  smtpPort: number | null
  smtpUsername: string | null
  mailEnabled: boolean
  hasSmtpPassword: boolean
}

type ApiError = { message?: string }

export default function SettingsPage() {
  const [form, setForm] = useState({
    discordWebhookUrl: '',
    discordEnabled: false,
    smtpHost: '',
    smtpPort: '587',
    smtpUsername: '',
    smtpPassword: '',
    mailEnabled: false,
  })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [hasSmtpPassword, setHasSmtpPassword] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  useEffect(() => {
    const load = async () => {
      try {
        const response = await fetch('/api/settings', {
          method: 'GET',
          credentials: 'include',
        })

        if (!response.ok) {
          const body = (await response.json()) as ApiError
          setError(body.message ?? '設定の取得に失敗しました')
          return
        }

        const data = (await response.json()) as Settings
        setForm({
          discordWebhookUrl: data.discordWebhookUrl ?? '',
          discordEnabled: data.discordEnabled,
          smtpHost: data.smtpHost ?? '',
          smtpPort: data.smtpPort ? String(data.smtpPort) : '587',
          smtpUsername: data.smtpUsername ?? '',
          smtpPassword: '',
          mailEnabled: data.mailEnabled,
        })
        setHasSmtpPassword(data.hasSmtpPassword)
      } catch {
        setError('サーバーに接続できませんでした')
      } finally {
        setLoading(false)
      }
    }

    load()
  }, [])

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    setSuccess(null)

    try {
      const csrf = await getCsrfToken()
      const response = await fetch('/api/settings', {
        method: 'PUT',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify({
          discordWebhookUrl: form.discordWebhookUrl || null,
          discordEnabled: form.discordEnabled,
          smtpHost: form.smtpHost || null,
          smtpPort: form.smtpPort ? Number(form.smtpPort) : null,
          smtpUsername: form.smtpUsername || null,
          smtpPassword: form.smtpPassword || null,
          mailEnabled: form.mailEnabled,
        }),
      })

      if (!response.ok) {
        const body = (await response.json()) as ApiError
        setError(body.message ?? '設定の保存に失敗しました')
        return
      }

      const data = (await response.json()) as Settings
      setHasSmtpPassword(data.hasSmtpPassword)
      setForm((prev) => ({ ...prev, smtpPassword: '' }))
      setSuccess('設定を保存しました')
    } catch {
      setError('サーバーに接続できませんでした')
    } finally {
      setSaving(false)
    }
  }

  return (
    <main className="min-h-screen px-4 py-8">
      <div className="mx-auto max-w-3xl rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <h1 className="text-2xl font-bold text-gray-900">設定</h1>
        <p className="mt-2 text-sm text-gray-600">Discord Webhook とメール送信設定を管理します。</p>

        {loading ? (
          <p className="mt-4 text-sm text-gray-600">読み込み中...</p>
        ) : (
          <form onSubmit={handleSubmit} className="mt-6 space-y-6">
            <section className="space-y-4">
              <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-semibold text-gray-900">Discord通知</h2>
                <Toggle checked={form.discordEnabled} onChange={(checked) => setForm((prev) => ({ ...prev, discordEnabled: checked }))} />
              </div>
              <Field label="Webhook URL">
                <input
                  value={form.discordWebhookUrl}
                  onChange={(e) => setForm((prev) => ({ ...prev, discordWebhookUrl: e.target.value }))}
                  className="min-h-[44px] w-full rounded-md border border-gray-300 px-3 py-2"
                  placeholder="https://discord.com/api/webhooks/..."
                />
              </Field>
            </section>

            <section className="space-y-4 border-t border-gray-100 pt-6">
              <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-semibold text-gray-900">メール送信</h2>
                <Toggle checked={form.mailEnabled} onChange={(checked) => setForm((prev) => ({ ...prev, mailEnabled: checked }))} />
              </div>
              <Field label="SMTPホスト">
                <input
                  value={form.smtpHost}
                  onChange={(e) => setForm((prev) => ({ ...prev, smtpHost: e.target.value }))}
                  className="min-h-[44px] w-full rounded-md border border-gray-300 px-3 py-2"
                />
              </Field>
              <Field label="SMTPポート">
                <input
                  type="number"
                  value={form.smtpPort}
                  onChange={(e) => setForm((prev) => ({ ...prev, smtpPort: e.target.value }))}
                  className="min-h-[44px] w-full rounded-md border border-gray-300 px-3 py-2"
                />
              </Field>
              <Field label="SMTPユーザー名">
                <input
                  value={form.smtpUsername}
                  onChange={(e) => setForm((prev) => ({ ...prev, smtpUsername: e.target.value }))}
                  className="min-h-[44px] w-full rounded-md border border-gray-300 px-3 py-2"
                />
              </Field>
              <Field label="SMTPパスワード">
                <input
                  type="password"
                  value={form.smtpPassword}
                  onChange={(e) => setForm((prev) => ({ ...prev, smtpPassword: e.target.value }))}
                  className="min-h-[44px] w-full rounded-md border border-gray-300 px-3 py-2"
                  placeholder={hasSmtpPassword ? '保存済み。変更時のみ入力' : ''}
                />
              </Field>
            </section>

            {error && <p role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
            {success && <p role="status" className="rounded-md border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-700">{success}</p>}

            <button
              type="submit"
              disabled={saving}
              className="min-h-[44px] rounded-md bg-[#145C9E] px-5 py-3 text-sm font-semibold text-white hover:bg-[#0B4F6C] disabled:opacity-50"
            >
              {saving ? '保存中...' : '設定を保存'}
            </button>
          </form>
        )}
      </div>
    </main>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-gray-700">{label}</span>
      {children}
    </label>
  )
}

function Toggle({ checked, onChange }: { checked: boolean; onChange: (checked: boolean) => void }) {
  return (
    <button
      type="button"
      onClick={() => onChange(!checked)}
      className={`min-h-[44px] rounded-full px-4 py-2 text-sm font-semibold ${checked ? 'bg-[#145C9E] text-white' : 'bg-gray-200 text-gray-700'}`}
    >
      {checked ? '有効' : '無効'}
    </button>
  )
}

'use client'

import { useState } from 'react'
import { apiFetch, setAuthToken } from '@/lib/api'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)

    try {
      const res = await apiFetch('/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
      })

      if (res.ok) {
        const data = (await res.json()) as { token?: string }
        setAuthToken(data.token ?? null)
        window.location.href = '/dashboard'
      } else {
        const data = await res.json()
        setError(data.message ?? 'ログインに失敗しました')
      }
    } catch {
      setError('サーバーに接続できませんでした')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="min-h-screen flex items-center justify-center px-4 py-8">
      <div className="w-full max-w-md rounded-2xl border border-[#E5E7EB] bg-white p-5 shadow-sm md:p-6">
        <h1 className="mb-6 text-center text-2xl font-bold text-[#1F271B]">
          FMC
        </h1>
        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label htmlFor="email" className="mb-1 block text-sm font-medium text-[#1F271B]">
              メールアドレス
            </label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="w-full min-h-11 border border-[#CBB9A8] rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#145C9E]"
            />
          </div>
          <div>
            <label htmlFor="password" className="mb-1 block text-sm font-medium text-[#1F271B]">
              パスワード
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="w-full min-h-11 border border-[#CBB9A8] rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#145C9E]"
            />
          </div>
          {error && (
            <p role="alert" className="text-sm text-red-600">{error}</p>
          )}
          <button
            type="submit"
            disabled={loading}
            className="min-h-11 w-full rounded-md bg-accent py-2.5 text-sm font-semibold text-white transition-colors hover:bg-accent-dark disabled:opacity-50"
            style={{ backgroundColor: '#145C9E' }}
          >
            {loading ? 'ログイン中...' : 'ログイン'}
          </button>
        </form>
      </div>
    </main>
  )
}

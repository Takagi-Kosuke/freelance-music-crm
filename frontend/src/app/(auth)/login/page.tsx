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
    <main className="min-h-screen flex items-center justify-center bg-[#DCC7BE] px-4">
      <div className="w-full max-w-md">
        <h1 className="text-2xl font-bold text-center mb-8 text-[#1F271B]">
          FreelanceMusicCRM
        </h1>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-[#1F271B] mb-1">
              メールアドレス
            </label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="w-full min-h-12 border border-[#CBB9A8] rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#145C9E]"
            />
          </div>
          <div>
            <label htmlFor="password" className="block text-sm font-medium text-[#1F271B] mb-1">
              パスワード
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="w-full min-h-12 border border-[#CBB9A8] rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#145C9E]"
            />
          </div>
          {error && (
            <p role="alert" className="text-red-600 text-sm">{error}</p>
          )}
          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 bg-accent text-white font-semibold rounded-md hover:bg-accent-dark transition-colors disabled:opacity-50 min-h-12"
            style={{ backgroundColor: '#145C9E' }}
          >
            {loading ? 'ログイン中...' : 'ログイン'}
          </button>
        </form>
      </div>
    </main>
  )
}

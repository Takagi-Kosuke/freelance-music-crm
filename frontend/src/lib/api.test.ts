import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiFetch, setAuthToken } from './api'

describe('apiFetch', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    window.localStorage.clear()
  })

  it('public auth endpoints do not send stale bearer tokens', async () => {
    setAuthToken('stale-token')
    const fetchSpy = vi.fn().mockResolvedValue({ ok: true, status: 200, headers: new Headers(), json: async () => ({}) })
    vi.stubGlobal('fetch', fetchSpy)

    await apiFetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'worker@example.com', password: 'pass' }),
    })

    const authHeader = fetchSpy.mock.calls[0][1].headers.get('Authorization')
    expect(authHeader).toBeNull()
  })

  it('401 responses clear the saved token for protected endpoints', async () => {
    setAuthToken('expired-token')
    const fetchSpy = vi.fn().mockResolvedValue({ ok: false, status: 401, headers: new Headers(), json: async () => ({ message: '認証が必要です' }) })
    vi.stubGlobal('fetch', fetchSpy)

    await apiFetch('/api/invoices/1/pdf/preview')

    expect(window.localStorage.getItem('freelance_music_crm_token')).toBeNull()
  })
})

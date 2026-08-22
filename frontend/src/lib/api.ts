export const API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_URL ?? 'https://freelance-music-crm-production.up.railway.app'
).replace(/\/$/, '')

const AUTH_TOKEN_KEY = 'freelance_music_crm_token'

export function getAuthToken(): string | null {
  if (typeof window === 'undefined') {
    return null
  }

  return window.localStorage.getItem(AUTH_TOKEN_KEY)
}

export function setAuthToken(token: string | null): void {
  if (typeof window === 'undefined') {
    return
  }

  if (token) {
    window.localStorage.setItem(AUTH_TOKEN_KEY, token)
    return
  }

  window.localStorage.removeItem(AUTH_TOKEN_KEY)
}

export function buildApiUrl(path: string): string {
  if (/^https?:\/\//.test(path)) {
    return path
  }

  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${API_BASE_URL}${normalizedPath}`
}

export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const token = getAuthToken()
  const headers = new Headers(init.headers ?? {})

  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(buildApiUrl(path), {
    ...init,
    headers,
    credentials: init.credentials ?? 'omit',
  })

  return response
}

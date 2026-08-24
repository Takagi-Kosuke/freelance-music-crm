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

function shouldSkipAuthHeader(path: string, init: RequestInit = {}): boolean {
  const method = (init.method ?? 'GET').toUpperCase()
  const normalizedPath = /^https?:\/\//.test(path) ? new URL(path).pathname : path

  return (
    normalizedPath === '/api/auth/login' ||
    normalizedPath === '/api/auth/csrf' ||
    normalizedPath === '/api/auth/session-expired' ||
    (normalizedPath === '/api/order-categories' && method === 'GET') ||
    normalizedPath.startsWith('/api/quote-responses/token/') ||
    normalizedPath.startsWith('/api/orders/token/') ||
    normalizedPath === '/api/quote-requests' && method === 'POST'
  )
}

export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const skipAuthHeader = shouldSkipAuthHeader(path, init)
  const token = getAuthToken()
  const headers = new Headers(init.headers ?? {})

  if (!skipAuthHeader && token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(buildApiUrl(path), {
    ...init,
    headers,
    credentials: init.credentials ?? 'omit',
  })

  if (response.status === 401 && !skipAuthHeader) {
    setAuthToken(null)
  }

  return response
}

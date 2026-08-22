export const API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_URL ?? 'https://freelance-music-crm-production.up.railway.app'
).replace(/\/$/, '')

export function buildApiUrl(path: string): string {
  if (/^https?:\/\//.test(path)) {
    return path
  }

  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${API_BASE_URL}${normalizedPath}`
}

export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const response = await fetch(buildApiUrl(path), {
    ...init,
    credentials: init.credentials ?? 'include',
  })

  return response
}

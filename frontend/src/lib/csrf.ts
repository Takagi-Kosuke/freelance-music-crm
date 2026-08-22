import { apiFetch } from '@/lib/api'

type CsrfResponse = {
  token: string
  headerName: string
  parameterName: string
}

export async function getCsrfToken(): Promise<CsrfResponse> {
  const response = await apiFetch('/api/auth/csrf', {
    method: 'GET',
  })

  if (!response.ok) {
    throw new Error('CSRFトークンの取得に失敗しました')
  }

  return (await response.json()) as CsrfResponse
}

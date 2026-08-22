type CsrfResponse = {
  token: string
  headerName: string
  parameterName: string
}

export async function getCsrfToken(): Promise<CsrfResponse> {
  const response = await fetch('/api/auth/csrf', {
    method: 'GET',
    credentials: 'include',
  })

  if (!response.ok) {
    throw new Error('CSRFトークンの取得に失敗しました')
  }

  return (await response.json()) as CsrfResponse
}

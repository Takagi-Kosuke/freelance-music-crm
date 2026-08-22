type CsrfResponse = {
  token: string
  headerName: string
  parameterName: string
}

export async function getCsrfToken(): Promise<CsrfResponse> {
  return {
    token: '',
    headerName: 'X-CSRF-TOKEN',
    parameterName: '_csrf',
  }
}

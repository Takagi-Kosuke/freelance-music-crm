import React from 'react'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import QuotePage from './page'

describe('QuotePage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('必須フィールド欠落時にエラーメッセージを表示する', async () => {
    const fetchMock = vi.spyOn(global, 'fetch' as never)

    fetchMock
      .mockResolvedValueOnce({
        ok: true,
        json: async () => [{ id: 1, name: '作曲', isDefault: true }],
      } as Response)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          token: 'csrf-token',
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
        }),
      } as Response)
      .mockResolvedValueOnce({
        ok: false,
        json: async () => ({
          message: '入力内容に不備があります',
          fieldErrors: [{ field: 'subject', message: '依頼件名は必須です' }],
        }),
      } as Response)

    render(<QuotePage />)

    await waitFor(() => {
      expect(screen.getByText('見積依頼を送信')).toBeInTheDocument()
    })

    fireEvent.change(screen.getByLabelText('依頼者名'), {
      target: { value: '依頼者テスト' },
    })
    fireEvent.change(screen.getByLabelText('希望納期'), {
      target: { value: '2099-12-31' },
    })

    const submitButton = screen.getByRole('button', { name: '見積依頼を送信' })
    const form = submitButton.closest('form')
    expect(form).not.toBeNull()

    if (form) {
      fireEvent.submit(form)
    }

    await waitFor(() => {
      expect(screen.getByText('依頼件名は必須です')).toBeInTheDocument()
    })
  })
})

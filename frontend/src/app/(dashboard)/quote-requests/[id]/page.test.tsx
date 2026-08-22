import React from 'react'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import QuoteRequestDetailPage from './page'

describe('QuoteRequestDetailPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('作業者が見積回答を送信できる', async () => {
    localStorage.setItem('freelance_music_crm_token', 'test-jwt-token')

    const fetchMock = vi.spyOn(global, 'fetch' as never)

    fetchMock
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          id: 7,
          subject: 'BGM制作',
          clientName: '田中太郎',
          clientEmail: 'client@example.com',
          categoryName: '作曲',
          desiredDeliveryDate: '2099-12-31',
          filePathUrl: 'https://example.com/file',
          comment: 'よろしくお願いします',
          status: 'PENDING',
          createdAt: '2026-06-28T10:00:00',
        }),
      } as Response)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          id: 11,
          quoteRequestId: 7,
          amount: 50000,
          responseDeliveryDate: '2100-01-15',
          responseComment: '通常納期です',
          approvalToken: 'approval-token-123',
          tokenStatus: 'ACTIVE',
          createdAt: '2026-06-28T12:00:00',
        }),
      } as Response)

    render(<QuoteRequestDetailPage params={{ id: '7' }} />)

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalled()
      const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
      const headers = new Headers(init.headers ?? {})

      expect(url).toContain('/api/quote-requests/7')
      expect(init.method).toBe('GET')
      expect(headers.get('Authorization')).toBe('Bearer test-jwt-token')
    })

    await waitFor(() => {
      expect(screen.getByText('見積依頼詳細')).toBeInTheDocument()
    })

    fireEvent.change(screen.getByLabelText('見積金額'), {
      target: { value: '50000' },
    })
    fireEvent.change(screen.getByLabelText('回答納期'), {
      target: { value: '2100-01-15' },
    })
    fireEvent.change(screen.getByLabelText('回答コメント（任意）'), {
      target: { value: '通常納期です' },
    })

    const submitButton = screen.getByRole('button', { name: '見積回答を送信' })
    const form = submitButton.closest('form')
    expect(form).not.toBeNull()

    if (form) {
      fireEvent.submit(form)
    }

    await waitFor(() => {
      expect(screen.getByText('見積回答を作成しました')).toBeInTheDocument()
      expect(screen.getByText('/orders/approval-token-123')).toBeInTheDocument()
    })

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledTimes(2)
      const [url, init] = fetchMock.mock.calls[1] as [string, RequestInit]
      const headers = new Headers(init.headers ?? {})

      expect(url).toContain('/api/quote-responses')
      expect(init.method).toBe('POST')
      expect(headers.get('Authorization')).toBe('Bearer test-jwt-token')
    })
  })
})
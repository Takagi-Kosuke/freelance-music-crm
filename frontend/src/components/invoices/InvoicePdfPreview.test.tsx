import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { InvoicePdfPreview } from './InvoicePdfPreview'

// URL.createObjectURL / revokeObjectURL は jsdom で未実装のためモック
const mockObjectUrl = 'blob:mock-url-12345'
vi.stubGlobal('URL', {
  createObjectURL: vi.fn(() => mockObjectUrl),
  revokeObjectURL: vi.fn(),
})

// ---------- fetch モック ヘルパー ----------
function mockPdfFetch(ok: boolean) {
  vi.stubGlobal(
    'fetch',
    vi.fn(() =>
      Promise.resolve({
        ok,
        blob: () => Promise.resolve(new Blob(['%PDF-1.4 mock'], { type: 'application/pdf' })),
        json: () => Promise.resolve({ message: 'error' }),
      })
    )
  )
}

describe('InvoicePdfPreview', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    // URL モックは毎回再設定
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => mockObjectUrl),
      revokeObjectURL: vi.fn(),
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('invoiceId が null のとき「選択してください」プレースホルダーを表示する', () => {
    render(<InvoicePdfPreview invoiceId={null} />)
    expect(screen.getByText('プレビューする請求書を選択してください。')).toBeInTheDocument()
  })

  it('「選択」後に pdf/preview API を fetch し <object> タグで表示する', async () => {
    mockPdfFetch(true)

    render(<InvoicePdfPreview invoiceId={99} />)

    // ローディング中の表示
    expect(screen.getByText('プレビュー読み込み中...')).toBeInTheDocument()

    // fetch 完了後 <object> が表示される
    await waitFor(() => {
      const obj = document.querySelector('object[data="blob:mock-url-12345"]')
      expect(obj).not.toBeNull()
    })

    // <iframe> は使われていない（DLダイアログの原因になるため）
    expect(document.querySelector('iframe')).toBeNull()

    // type="application/pdf" が明示されている
    const objectEl = document.querySelector('object')
    expect(objectEl?.getAttribute('type')).toBe('application/pdf')
  })

  it('「選択」後に Blob に type="application/pdf" を明示して createObjectURL を呼ぶ', async () => {
    mockPdfFetch(true)
    const createObjectURLSpy = vi.fn(() => mockObjectUrl)
    vi.stubGlobal('URL', {
      createObjectURL: createObjectURLSpy,
      revokeObjectURL: vi.fn(),
    })

    render(<InvoicePdfPreview invoiceId={42} />)

    await waitFor(() => {
      expect(createObjectURLSpy).toHaveBeenCalledTimes(1)
    })

    // 渡された Blob の type が application/pdf であることを確認
    const passedBlob = createObjectURLSpy.mock.calls[0][0] as Blob
    expect(passedBlob.type).toBe('application/pdf')
  })

  it('API エラー時にエラーメッセージを表示し <object> を表示しない', async () => {
    mockPdfFetch(false)

    render(<InvoicePdfPreview invoiceId={1} />)

    await waitFor(() => {
      expect(screen.getByText('請求書プレビューの取得に失敗しました')).toBeInTheDocument()
    })

    expect(document.querySelector('object')).toBeNull()
  })

  it('invoiceId が変わったとき前の Blob URL を revoke する', async () => {
    mockPdfFetch(true)
    const revokeObjectURLSpy = vi.fn()
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => mockObjectUrl),
      revokeObjectURL: revokeObjectURLSpy,
    })

    const { rerender } = render(<InvoicePdfPreview invoiceId={1} />)

    // 1件目の PDF 表示完了を待つ
    await waitFor(() => {
      expect(document.querySelector('object')).not.toBeNull()
    })

    // invoiceId を変更 → 前の Blob URL が revoke されるべき
    rerender(<InvoicePdfPreview invoiceId={2} />)

    await waitFor(() => {
      expect(revokeObjectURLSpy).toHaveBeenCalledWith(mockObjectUrl)
    })
  })
})

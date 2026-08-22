import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi, beforeEach } from 'vitest'

// Next.js ナビゲーション系をモック
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  usePathname: () => '/invoices',
}))

// CSRF トークン取得をモック
vi.mock('@/lib/csrf', () => ({
  getCsrfToken: vi.fn().mockResolvedValue({ headerName: 'X-CSRF-TOKEN', token: 'test-token' }),
}))

// InvoicePdfPreview は iframe を描画するため、テスト環境では軽量モックに差し替える
vi.mock('@/components/invoices/InvoicePdfPreview', () => ({
  InvoicePdfPreview: ({ invoiceId }: { invoiceId: number | null }) =>
    invoiceId
      ? React.createElement('div', { 'data-testid': `pdf-preview-${invoiceId}` }, 'PDFプレビュー')
      : React.createElement('div', { 'data-testid': 'pdf-preview-empty' }, 'プレビューする請求書を選択してください。'),
}))

// ---------- サンプルデータ ----------
// テスト用の完了タスク（請求書発行可能な状態）
const sampleTask = {
  id: 1,
  orderId: 10,
  orderSubject: 'BGMテスト制作',
  clientName: 'テストクライアント',
  clientEmail: 'client@example.com',
  desiredDeliveryDate: '2025-08-01',
  status: 'COMPLETED' as const,
}

// テスト用の既存請求書データ
const sampleInvoice = {
  id: 99,
  taskId: 1,
  subject: 'BGMテスト制作',
  clientName: 'テストクライアント',
  clientEmail: 'client@example.com',
  categoryName: '作曲',
  deliveryDate: '2025-07-31',
  amount: '50000',
  issueDate: '2025-08-01',
  workerName: 'テスト作業者',
  workerContact: 'worker@example.com',
  createdAt: '2025-08-01T12:00:00',
}

// ---------- fetch モック ヘルパー ----------
function mockFetch(tasks: typeof sampleTask[], invoices: typeof sampleInvoice[]) {
  vi.stubGlobal(
    'fetch',
    vi.fn((url: string) => {
      if (url.includes('/api/tasks')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve(tasks) })
      }
      if (url.includes('/api/invoices')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve(invoices) })
      }
      return Promise.resolve({ ok: false, json: () => Promise.resolve({}) })
    })
  )
}

// ---------- テスト ----------
describe('InvoicesPage — ナビ遷移時のDLダイアログ問題', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('【修正確認】既存請求書があってもページロード直後に PDF プレビューが自動表示されない', async () => {
    mockFetch([sampleTask], [sampleInvoice])

    const { default: InvoicesPage } = await import('./page')
    render(<InvoicesPage />)

    // データ読み込み完了を待機
    await waitFor(() => {
      expect(screen.queryByText('読み込み中...')).not.toBeInTheDocument()
    })

    // 請求書一覧が表示されることを確認（データ取得成功）
    expect(screen.getByText('BGMテスト制作')).toBeInTheDocument()

    // ✅ 修正後の期待動作: 自動選択されないため pdf-preview-99 は表示されない
    expect(screen.queryByTestId('pdf-preview-99')).not.toBeInTheDocument()

    // 「選択してください」プレースホルダーが表示される
    expect(screen.getByTestId('pdf-preview-empty')).toBeInTheDocument()
  })

  it('【正常動作】請求書一覧が空の場合、ページロード直後にプレビューが表示されない', async () => {
    mockFetch([sampleTask], [])

    const { default: InvoicesPage } = await import('./page')
    render(<InvoicesPage />)

    await waitFor(() => {
      expect(screen.queryByText('読み込み中...')).not.toBeInTheDocument()
    })

    // ✅ 請求書がない場合もプレビューは空
    expect(screen.getByTestId('pdf-preview-empty')).toBeInTheDocument()
    expect(screen.queryByTestId('pdf-preview-99')).not.toBeInTheDocument()
  })

  it('【正常動作】ページ遷移直後に pdf/preview API が自動呼び出しされない', async () => {
    const fetchMock = vi.fn((url: string) => {
      if (url.includes('/api/tasks')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([sampleTask]) })
      }
      if (url.includes('/api/invoices')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([sampleInvoice]) })
      }
      return Promise.resolve({ ok: false, json: () => Promise.resolve({}) })
    })
    vi.stubGlobal('fetch', fetchMock)

    const { default: InvoicesPage } = await import('./page')
    render(<InvoicesPage />)

    await waitFor(() => {
      expect(screen.queryByText('読み込み中...')).not.toBeInTheDocument()
    })

    // ✅ pdf/preview エンドポイントが自動呼び出しされていないことを確認
    const pdfPreviewCalls = fetchMock.mock.calls
      .map((c) => c[0] as string)
      .filter((url) => url?.includes('/pdf'))

    expect(pdfPreviewCalls.length).toBe(0)
  })

  it('「選択」ボタンをクリックすると該当の請求書プレビューが表示される', async () => {
    mockFetch([sampleTask], [sampleInvoice])

    const { default: InvoicesPage } = await import('./page')
    const { getByRole } = render(<InvoicesPage />)

    await waitFor(() => {
      expect(screen.queryByText('読み込み中...')).not.toBeInTheDocument()
    })

    // 初期状態ではプレビューは空
    expect(screen.getByTestId('pdf-preview-empty')).toBeInTheDocument()

    // 「選択」ボタンをクリック
    const selectButton = getByRole('button', { name: '選択' })
    selectButton.click()

    // クリック後はプレビューが表示される
    await waitFor(() => {
      expect(screen.getByTestId('pdf-preview-99')).toBeInTheDocument()
    })
  })
})

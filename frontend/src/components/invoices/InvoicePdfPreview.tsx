'use client'

import React, { useEffect, useRef, useState } from 'react'

type InvoicePdfPreviewProps = {
  invoiceId: number | null
}

export function InvoicePdfPreview({ invoiceId }: InvoicePdfPreviewProps) {
  const [pdfBlobUrl, setPdfBlobUrl] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  // Blob URL の revoke を確実に行うために ref で管理する（ステールクロージャ回避）
  const blobUrlRef = useRef<string | null>(null)

  useEffect(() => {
    if (!invoiceId) {
      setPdfBlobUrl(null)
      setError(null)
      return
    }

    let cancelled = false

    const fetchPdf = async () => {
      setLoading(true)
      setError(null)
      try {
        const response = await fetch(`/api/invoices/${invoiceId}/pdf/preview`, {
          credentials: 'include',
        })

        if (!response.ok) {
          if (!cancelled) {
            setError('請求書プレビューの取得に失敗しました')
            setPdfBlobUrl(null)
          }
          return
        }

        // type を明示して PDF として扱わせる
        const blob = await response.blob()
        const pdfBlob = new Blob([blob], { type: 'application/pdf' })
        const url = URL.createObjectURL(pdfBlob)

        if (cancelled) {
          URL.revokeObjectURL(url)
          return
        }

        // 前の Blob URL を revoke してからセット
        if (blobUrlRef.current) {
          URL.revokeObjectURL(blobUrlRef.current)
        }
        blobUrlRef.current = url
        setPdfBlobUrl(url)
      } catch {
        if (!cancelled) {
          setError('プレビュー読み込み中にエラーが発生しました')
          setPdfBlobUrl(null)
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    fetchPdf()

    return () => {
      cancelled = true
    }
  }, [invoiceId])

  // アンマウント時に残った Blob URL を解放する
  useEffect(() => {
    return () => {
      if (blobUrlRef.current) {
        URL.revokeObjectURL(blobUrlRef.current)
        blobUrlRef.current = null
      }
    }
  }, [])

  if (!invoiceId) {
    return (
      <div className="rounded-xl border border-dashed border-gray-300 bg-gray-50 p-6 text-sm text-gray-600">
        プレビューする請求書を選択してください。
      </div>
    )
  }

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-gray-900">PDFプレビュー</h2>
          <p className="text-sm text-gray-600">選択中の請求書をそのまま確認できます。</p>
        </div>
      </div>

      {error && (
        <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}

      {loading && (
        <p className="rounded-md border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-600">
          プレビュー読み込み中...
        </p>
      )}

      {pdfBlobUrl && (
        // <object> は <iframe> よりも PDF のインライン表示に適しており、
        // ブラウザが PDF を扱えない場合もフォールバックテキストを表示できる
        <object
          data={pdfBlobUrl}
          type="application/pdf"
          title={`invoice-${invoiceId}-preview`}
          className="h-[640px] w-full rounded-lg border border-gray-200 bg-gray-100"
        >
          <p className="p-4 text-sm text-gray-600">
            PDFのインライン表示に対応していないブラウザです。
          </p>
        </object>
      )}
    </div>
  )
}
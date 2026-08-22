---
inclusion: manual
---

# バグ学習記録: 請求書ページで意図しないPDFダウンロードダイアログが開く問題

## 概要

FreelanceMusicCRM の請求書ページ（`/invoices`）において、以下の2つの操作でPDFダウンロードダイアログが意図せず開く不具合が発生した。本ドキュメントはその根本原因と修正方針を記録し、同種の不具合を再発させないための学習資料とする。

---

## 発生した不具合

### 不具合 1: ページ遷移時にDLダイアログが開く

- **操作**: ナビゲーションメニューの「請求書」をクリックしてページ遷移する
- **誤動作**: ページ表示と同時にPDFダウンロードダイアログが開く
- **期待動作**: ページ遷移のみが行われる

### 不具合 2: 「選択」ボタン押下時にDLダイアログが開く

- **操作**: 請求書一覧の「選択」ボタンをクリックしてプレビュー対象を選ぶ
- **誤動作**: 選択と同時にPDFダウンロードダイアログが開く
- **期待動作**: 画面内のPDFプレビューエリアに請求書が表示される

---

## 根本原因

### 原因 1（不具合 1）: データ取得後の自動選択

**ファイル**: `frontend/src/app/(dashboard)/invoices/page.tsx`

```tsx
// ❌ 修正前: useEffect 内でデータ取得後に invoiceData[0] を自動選択していた
useEffect(() => {
  const load = async () => {
    const invoiceData = (await invoiceRes.json()) as InvoiceRow[]
    setInvoices(invoiceData)

    // この3行が問題: ページロード直後に selectedInvoiceId がセットされる
    if (invoiceData.length > 0) {
      setSelectedInvoiceId(invoiceData[0].id)  // ← ここ
    }
  }
  load()
}, [])
```

`selectedInvoiceId` がセットされると `<InvoicePdfPreview invoiceId={selectedInvoiceId} />` が即座にPDFフェッチを開始する。これがページ遷移直後のダイアログ発生の直接原因。

**修正**: `setSelectedInvoiceId(invoiceData[0].id)` の自動選択ブロックを削除する。

```tsx
// ✅ 修正後: 自動選択しない
const taskData = (await taskRes.json()) as TaskRow[]
const invoiceData = (await invoiceRes.json()) as InvoiceRow[]
setTasks(taskData)
setInvoices(invoiceData)
// invoiceData[0] の自動選択は行わない
```

---

### 原因 2（不具合 2）: `<iframe>` + Blob URL + MIME type 未指定

**ファイル**: `frontend/src/components/invoices/InvoicePdfPreview.tsx`

```tsx
// ❌ 修正前: 2つの問題がある実装
const blob = await response.blob()
// 問題A: response.blob() はサーバーが返す Content-Type をそのまま継承するが、
//        認証リダイレクト等で text/html が返ってくる場合に type が不正になる
const url = URL.createObjectURL(blob)

// 問題B: <iframe> はブラウザのPDF対応状況に依存する
//        PDF表示に非対応の場合、ブラウザはファイルをダウンロードしようとする
<iframe src={pdfBlobUrl} />
```

加えて、クリーンアップ関数でステールクロージャが発生していた。

```tsx
// ❌ 修正前: pdfBlobUrl はクロージャ時点の古い値を参照する（常に null か前回値）
return () => {
  if (pdfBlobUrl) {           // ← useState の値はクロージャで古いまま
    URL.revokeObjectURL(pdfBlobUrl)
  }
}
```

**修正 A**: Blob 生成時に `type: 'application/pdf'` を明示する。

```tsx
// ✅ 修正後: MIME type を明示してブラウザにPDFと認識させる
const blob = await response.blob()
const pdfBlob = new Blob([blob], { type: 'application/pdf' })
const url = URL.createObjectURL(pdfBlob)
```

**修正 B**: `<iframe>` を `<object>` タグに変更する。

```tsx
// ✅ 修正後: <object> はPDFインライン表示に適しており、
//           フォールバックテキストも指定できる
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
```

**修正 C**: ステールクロージャを `useRef` で回避する。

```tsx
// ✅ 修正後: ref で Blob URL を追跡し、クリーンアップで確実に revoke する
const blobUrlRef = useRef<string | null>(null)

// fetchPdf 内
if (blobUrlRef.current) {
  URL.revokeObjectURL(blobUrlRef.current)
}
blobUrlRef.current = url
setPdfBlobUrl(url)

// アンマウント時
useEffect(() => {
  return () => {
    if (blobUrlRef.current) {
      URL.revokeObjectURL(blobUrlRef.current)
      blobUrlRef.current = null
    }
  }
}, [])
```

また、非同期処理の競合（rapid re-render）を防ぐために `cancelled` フラグも導入した。

```tsx
// ✅ 修正後: invoiceId が変わった場合に前のフェッチ結果を捨てる
let cancelled = false
fetchPdf()
return () => { cancelled = true }
```

---

## 教訓とルール

### ルール 1: データ取得後の自動選択は行わない

`useEffect` でデータを取得した後、UIの選択状態を自動セットしてはいけない。ユーザーが明示的に操作した結果としてのみ選択状態が変わるべき。

```tsx
// ❌ 禁止パターン
useEffect(() => {
  fetch('/api/items').then(res => res.json()).then(data => {
    setItems(data)
    setSelectedId(data[0].id)  // ← 自動選択は副作用を生む
  })
}, [])

// ✅ 推奨パターン
useEffect(() => {
  fetch('/api/items').then(res => res.json()).then(data => {
    setItems(data)
    // 選択はユーザー操作に任せる
  })
}, [])
```

### ルール 2: PDFのインライン表示には `<object>` を使い、Blob の type を明示する

`<iframe>` にBlobURLを渡すとブラウザのPDF対応状況によってダウンロードが起動する場合がある。

```tsx
// ❌ 避けるべきパターン
const blob = await response.blob()                    // type が不定
const url = URL.createObjectURL(blob)
<iframe src={url} />                                  // ブラウザ依存でDLになる場合あり

// ✅ 推奨パターン
const blob = await response.blob()
const pdfBlob = new Blob([blob], { type: 'application/pdf' })  // type を明示
const url = URL.createObjectURL(pdfBlob)
<object data={url} type="application/pdf">            // <object> を使う
  <p>PDFのインライン表示に対応していません。</p>
</object>
```

### ルール 3: `useEffect` クリーンアップで `useState` の値を参照しない（ステールクロージャ）

`useEffect` のクリーンアップ関数はクロージャが作成された時点の値を保持するため、`useState` の最新値を参照できない。`useRef` を使って最新値を追跡する。

```tsx
// ❌ 避けるべきパターン
const [url, setUrl] = useState<string | null>(null)
useEffect(() => {
  // ...
  return () => {
    if (url) URL.revokeObjectURL(url)  // url は常に古い値（ステールクロージャ）
  }
}, [dependency])

// ✅ 推奨パターン
const urlRef = useRef<string | null>(null)
useEffect(() => {
  // ...
  urlRef.current = newUrl
  setUrl(newUrl)
  return () => { /* urlRef.current は不要 */ }
}, [dependency])

useEffect(() => {
  return () => {
    if (urlRef.current) URL.revokeObjectURL(urlRef.current)  // ref は常に最新値
  }
}, [])
```

### ルール 4: 非同期処理には `cancelled` フラグでレースコンディションを防ぐ

ユーザーが素早く操作を切り替えた場合、古いフェッチ結果が後から到着してstateを上書きする。`cancelled` フラグで制御する。

```tsx
// ✅ 推奨パターン
useEffect(() => {
  let cancelled = false
  const fetchData = async () => {
    const data = await fetch(url).then(r => r.json())
    if (!cancelled) setState(data)  // キャンセル済みなら state を更新しない
  }
  fetchData()
  return () => { cancelled = true }
}, [url])
```

---

## 修正ファイル一覧

| ファイル | 修正内容 |
|---|---|
| `frontend/src/app/(dashboard)/invoices/page.tsx` | `useEffect` 内の `setSelectedInvoiceId(invoiceData[0].id)` 自動選択ブロックを削除 |
| `frontend/src/components/invoices/InvoicePdfPreview.tsx` | Blob type 明示、`<iframe>` → `<object>` 変更、`useRef` によるステールクロージャ解消、`cancelled` フラグ追加 |

## 関連テスト

- `frontend/src/app/(dashboard)/invoices/page.test.tsx`
  - `【修正確認】既存請求書があってもページロード直後にPDFプレビューが自動表示されない`
  - `【正常動作】ページ遷移直後に pdf/preview API が自動呼び出しされない`
  - `「選択」ボタンをクリックすると該当の請求書プレビューが表示される`

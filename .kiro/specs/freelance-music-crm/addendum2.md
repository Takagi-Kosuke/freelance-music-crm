# 追加仕様書 2 — フロントエンド デザイン確認レポート

> 作成日: 2026-07-26  
> 目的: フロントエンド全ページのデザイン・UI実装を仕様書（requirements.md / design.md）と照合した結果と、修正指示を記載する。

---

## 1. 確認結果サマリー

### ✅ 仕様通りに実装済み（問題なし）

| 確認項目                    | 詳細                                                                                                                                                |
| --------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| アクセントカラー `#FF6B00`  | `globals.css` の CSS 変数・`tailwind.config.ts` の `colors.accent`・各ページの `bg-orange-600` / `style={{backgroundColor:'#FF6B00'}}` で統一済み   |
| 白背景                      | `body { background: #ffffff }` / 各ページ `bg-white` または `bg-gray-50` 設定済み                                                                   |
| タップ領域 44px 以上        | 全ボタン・リンクに `min-h-[44px]` 付与済み（要件 10.4 準拠）                                                                                        |
| カレンダー縦スクロール対応  | `max-h-[70vh] overflow-y-auto` でスマートフォン向け縦スクロール対応済み（要件 10.5 準拠）                                                           |
| 月/週切替ボタン             | `CalendarView` に独立したトグルボタンとして実装済み（要件 6.1 準拠）                                                                                |
| 完了/未完了タスクの配色分け | `task-event-completed`（緑: `#10b981`）/ `task-event-active`（黄: `#f59e0b`）で `globals.css` に定義・`eventPropGetter` で適用済み（要件 6.4 準拠） |
| DetailPanel の並列表示      | タスク一覧・カレンダー両ページで `lg:grid-cols-[1.3fr_1fr]` のグリッドレイアウトで並列表示（要件 6.5 準拠）                                         |
| Navbar のレスポンシブ対応   | `lg:flex` でサイドバー、モバイルでは上部ボーダーバーに切り替わる（要件 10.1 準拠）                                                                  |
| バリデーションエラー表示    | 全フォームでフィールド別エラーメッセージ表示済み（要件 2.3 準拠）                                                                                   |
| 受付完了メッセージ          | 見積依頼フォームの送信成功時に `successMessage` として表示済み（要件 2.7 準拠）                                                                     |
| API プロキシ設定            | `next.config.mjs` で `/api/*` → `http://localhost:8080/api/*` にリライト設定済み                                                                    |
| ルートグループ構成          | `(public)` / `(auth)` / `(dashboard)` の3グループが正しく構成済み                                                                                   |
| middleware 認証保護         | `JSESSIONID` Cookie の有無で保護ページへのアクセスを制御済み                                                                                        |

---

## 2. 軽微な問題（要修正）

### ❌ 問題 1: ログインページの `focus:ring-accent` が機能しない

**対象ファイル:**  
`frontend/src/app/(auth)/login/page.tsx`

**現状のコード:**

```tsx
className =
  "w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-accent";
```

**問題:**  
`tailwind.config.ts` で `accent` は `colors.accent.DEFAULT: '#FF6B00'` として定義されているが、Tailwind の `ring-accent` は `ring-accent-DEFAULT` と解釈されず、**フォーカスリングが表示されない**。

**修正方針:**  
`ring-accent` を `ring-[#FF6B00]` または `ring-orange-500` に変更する。

```tsx
// 修正後
className =
  "w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#FF6B00]";
```

---

### ❌ 問題 2: 見積依頼一覧のステータス表示が英語のまま

**対象ファイル:**  
`frontend/src/app/(dashboard)/quote-requests/page.tsx`

**現状のコード:**

```tsx
<td className="px-4 py-3">{item.status}</td>
// → "PENDING", "RESPONDED", "APPROVED", "DECLINED" と英語で表示される
```

**問題:**  
ユーザー向けの一覧画面でバックエンドの enum 値がそのまま表示されており、UX が悪い。

**修正方針:**  
日本語ラベルへの変換マップを追加する。

```tsx
// 追加するマッピング
const STATUS_LABELS: Record<string, string> = {
  PENDING:   '受付中',
  RESPONDED: '見積回答済',
  APPROVED:  '承認済',
  DECLINED:  '辞退',
}

// テーブルセルの変更
<td className="px-4 py-3">
  {STATUS_LABELS[item.status] ?? item.status}
</td>
```

---

## 3. 修正タスク一覧

| #   | ファイル                              | 修正内容                                               | 優先度           |
| --- | ------------------------------------- | ------------------------------------------------------ | ---------------- |
| 1   | `(auth)/login/page.tsx`               | `focus:ring-accent` → `focus:ring-[#FF6B00]` に変更    | 低（見た目のみ） |
| 2   | `(dashboard)/quote-requests/page.tsx` | ステータス表示を日本語ラベルに変換するマッピングを追加 | 中（UX影響あり） |

---

## 4. 変更不要な実装（確認済み・触らないこと）

- `globals.css`（カラー変数・カレンダーイベント CSS クラス）
- `tailwind.config.ts`（`accent` カラー定義）
- `CalendarView.tsx`（月/週切替・配色分け・縦スクロール）
- `TaskStatusBadge.tsx`（ステータス別バッジカラー）
- `DetailPanel.tsx`（全フィールド表示・ステータス更新ボタン）
- `Navbar.tsx`（レスポンシブサイドナビ・ログアウト処理）
- `layout.tsx`（ダッシュボード共通グリッドレイアウト）
- `next.config.mjs`（API プロキシ設定）
- `middleware.ts`（認証ガード）

---

## 5. Spectrum ガイドライン準拠チェック（2026-07-26 追記）

参照:

- https://spectrum.adobe.com/page/principles/
- https://spectrum.adobe.com/page/inclusive-design/
- https://spectrum.adobe.com/page/platform-scale/

### 5.1 判定基準

- **Human / For everyone**: アクセシビリティ、キーボード操作、読みやすさ
- **Focused**: 過剰な装飾や不要なアニメーションを避ける
- **Platform scale**: モバイルでの操作サイズ・レスポンシブ整合

### 5.2 今回の確認結果

| 観点                              | 判定                                | コメント                                              |
| --------------------------------- | ----------------------------------- | ----------------------------------------------------- |
| キーボードフォーカスの視認性      | 要改善 → **対応済み**               | フォーカス状態の見え方を全体で強化                    |
| タッチ操作サイズ（目安 48px）     | 一部要改善 → **主要導線を対応済み** | ヘッダー、トップCTA、ログインフォームを 48px 高へ調整 |
| レスポンシブ（Desktop/Mobile）    | 概ね準拠                            | 既存のレイアウト切替は有効                            |
| 動きの配慮（reduced motion）      | 要改善 → **対応済み**               | OS設定 `prefers-reduced-motion` への追従を追加        |
| 構造と一貫性（フォーム/操作導線） | 準拠                                | ラベル付き入力、エラー表示、主要操作導線を維持        |

### 5.3 実施した修正

1. **フォーカス可視化の強化**

- `globals.css` に `:focus-visible` の共通アウトラインを追加（2px, 青系）。
- キーボード利用時に操作対象が明確になるように変更。

2. **reduced motion 対応**

- `@media (prefers-reduced-motion: reduce)` を追加。
- アニメーション/トランジションを最小化し、スクロール挙動を抑制。

3. **主要ボタン/リンクの操作サイズ改善**

- Navbar のメニューリンクとログアウトボタンを `min-h-12` に統一。
- トップページ CTA ボタン（「見積依頼はこちら」「作業者ログイン」）を `min-h-12` に統一。

4. **ログインフォームの操作サイズ改善**

- メール・パスワード入力を `min-h-12` に変更。
- 送信ボタンを `min-h-12` に変更。

### 5.4 補足

- Spectrum は実装ライブラリ（React Spectrum 等）の利用も推奨しているが、本プロジェクトは既存の Next.js + Tailwind 構成を維持したまま、**原則（可読性・一貫性・アクセシビリティ）準拠**を優先して修正した。

---

## 6. カラーパレット更新（2026-07-26 追記）

参照パレット:

- https://coolors.co/1f271b-0b4f6c-145c9e-cbb9a8-dcc7be

採用カラー:

- `#1F271B` : メイン文字色
- `#0B4F6C` : サブ文字 / セカンダリアクション
- `#145C9E` : プライマリアクション（CTA / フォーカス）
- `#CBB9A8` : ボーダー / ソフト背景
- `#DCC7BE` : ページ背景基調

実装反映:

1. `tailwind.config.ts` の `accent` を青系トーンへ更新。
2. `globals.css` の CSS 変数・背景グラデーション・フォーカス色を更新。
3. `Navbar` / トップページ / ログイン画面の主要UIカラーを新パレットへ置換。

備考:

- 既存機能（認証、ルーティング、フォーム送信、タスク・請求書機能）の挙動には影響しない配色変更のみを実施。

---

## 7. 継続対応（全画面カラー統一）

`進めてください` の追加指示により、未統一だった画面の旧オレンジ系スタイルを新パレットへ置換。

対象:

- タスク詳細パネル
- カレンダートグル
- 公開見積依頼フォーム
- クライアント承認/辞退ページ
- 請求書管理
- 設定
- ダッシュボード導線カード
- 見積依頼一覧/詳細
- 依頼区分管理

検証:

- `frontend/src/**` を対象に `orange-` / `#FF6B00` 残存検索を実施し、**残存 0 件**を確認。

注記:

- 本書 1 章の「アクセントカラー `#FF6B00`」記述は、現在は `#145C9E` を中心とする新パレット運用へ更新済み。

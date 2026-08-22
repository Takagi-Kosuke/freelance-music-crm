# Implementation Plan: FreelanceMusicCRM

## Overview

Spring Boot 3.x (Java 21) によるバックエンド REST API と Next.js 14 (App Router) によるフロントエンドを段階的に実装する。
データベースは PostgreSQL を使用し、JPA/Hibernate でアクセスする。
プロパティベーステストには jqwik 1.8.x を使用し、各 Correctness Property を 1 つのプロパティテストで検証する。

---

## Task Dependency Graph

```json
{
  "waves": [
    { "wave": 1, "tasks": ["1"] },
    { "wave": 2, "tasks": ["2"] },
    { "wave": 3, "tasks": ["3"] },
    { "wave": 4, "tasks": ["4"] },
    { "wave": 5, "tasks": ["5"] },
    { "wave": 6, "tasks": ["6", "7"] },
    { "wave": 7, "tasks": ["8"] },
    { "wave": 8, "tasks": ["9"] },
    { "wave": 9, "tasks": ["10", "11"] },
    { "wave": 10, "tasks": ["12"] },
    { "wave": 11, "tasks": ["13", "14", "15", "16"] },
    { "wave": 12, "tasks": ["17"] },
    { "wave": 13, "tasks": ["18"] },
    { "wave": 14, "tasks": ["19"] }
  ]
}
```

---

## Tasks

- [x] 1. プロジェクト基盤のセットアップ
  - Spring Boot 3.x プロジェクト (Gradle/Maven) を作成し、依存関係 (Spring Web, Spring Security, Spring Data JPA, PostgreSQL Driver, Validation, jqwik 1.8.x, Testcontainers, Apache PDFBox 3.x, JavaMailSender, Spring WebFlux) を追加する
  - Next.js 14 (App Router) プロジェクトを作成し、依存関係 (Vitest, React Testing Library, react-big-calendar, TailwindCSS) を追加する
  - `docker-compose.yml` で PostgreSQL ローカル開発環境を構成する
  - バックエンドの CORS 設定 (`WebMvcConfig.java`) とグローバル例外ハンドラ (`GlobalExceptionHandler.java`) を実装する
  - _要件: 全般_

- [ ] 2. データベースエンティティとリポジトリの実装
  - [x] 2.1 JPA エンティティの実装
    - `Worker`, `WorkerSettings`, `OrderCategory`, `QuoteRequest`, `QuoteResponse`, `Order`, `Task`, `Invoice` エンティティを ER 図に従って実装する
    - ステータス列挙型 (`QuoteRequestStatus`, `TaskStatus`, `TokenStatus`) を定義する
    - _要件: 1.1, 2.4, 3.2, 4.2, 5.1, 7.1, 8.2_
  - [x] 2.2 Spring Data JPA リポジトリの実装
    - 各エンティティの Repository インターフェースを作成する
    - カテゴリフィルタ、期間クエリなどのカスタムクエリメソッドを定義する
    - _要件: 5.4, 6.2_
  - [x] 2.3 Flyway または Liquibase によるスキーマ初期化
    - マイグレーションスクリプトを作成し、初期 5 区分 (作曲・ミックス・バンド演奏・楽譜作成・その他) のシードデータを挿入する
    - _要件: 7.1_

- [x] 3. 認証機能の実装
  - [x] 3.1 Spring Security 設定と Worker 認証の実装
    - `SecurityConfig.java` でセッション管理・CSRF 保護・BCrypt・公開エンドポイントの設定を行う
    - `AuthService.java` にログイン・ログアウト・失敗カウント管理・アカウントロックを実装する
    - `AuthController.java` に `POST /api/auth/login`、`POST /api/auth/logout` を実装する
    - _要件: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 11.4, 11.5, 11.6_
  - [x]\* 3.2 認証のプロパティベーステストを作成する
    - **Property 1: 認証結果の正確性** — 任意の認証情報ペアに対して、有効なら成功・無効なら失敗する
    - **Validates: Requirements 1.1, 1.2**
  - [x]\* 3.3 認証のユニットテストを作成する
    - ログイン成功・失敗シナリオ、5 回失敗後のアカウントロック動作を検証する
    - _要件: 1.1, 1.2, 11.5_

- [x] 4. チェックポイント — 認証基盤の動作確認
  - 全テストが通過することを確認し、不明点があればユーザーに確認する。

- [x] 5. 入力バリデーション・セキュリティ基盤の実装
  - [x] 5.1 DTO バリデーションの実装
    - `QuoteRequestCreateDto`、`QuoteResponseCreateDto`、`TaskStatusUpdateDto`、`OrderCategoryDto` など全 DTO に Bean Validation アノテーションを付与する
    - `GlobalExceptionHandler` で `MethodArgumentNotValidException` を捕捉しフィールド別エラーレスポンスを返す
    - _要件: 2.2, 2.3, 2.5, 2.6, 3.3, 7.3, 11.1_
  - [x]\* 5.2 必須フィールド欠落バリデーションのプロパティベーステストを作成する
    - **Property 3: 必須フィールド欠落時のバリデーションエラー** — 件名・依頼者名・区分・納期のいずれかが null/空のとき常にエラーを返す
    - **Validates: Requirements 2.2, 2.3**
  - [x]\* 5.3 URL フォーマットバリデーションのプロパティベーステストを作成する
    - **Property 5: URL フィールドのフォーマットバリデーション** — `http://` または `https://` 以外の文字列はエラーを返す
    - **Validates: Requirements 2.5**
  - [x]\* 5.4 コメント文字数バリデーションのプロパティベーステストを作成する
    - **Property 6: コメント文字数上限バリデーション** — 1001 文字以上のコメントは常にエラーを返す
    - **Validates: Requirements 2.6**
  - [x]\* 5.5 区分名文字数バリデーションのプロパティベーステストを作成する
    - **Property 17: 区分名文字数バリデーション** — 0 文字または 51 文字以上はエラー、1〜50 文字は受け入れる
    - **Validates: Requirements 7.3**
  - [x]\* 5.6 未認証アクセス拒否のプロパティベーステストを作成する
    - **Property 2: 未認証アクセスは常に拒否される** — 有効なセッショントークンなしの保護済み API は常に 401 を返す
    - **Validates: Requirements 1.5, 11.6**
  - [x]\* 5.7 SQL インジェクション入力の安全性プロパティベーステストを作成する
    - **Property 24: SQL インジェクション入力の安全な処理** — インジェクション試みパターンがエラーまたは正常応答となりDB エラーを起こさない
    - **Validates: Requirements 11.2**
  - [x]\* 5.8 XSS 入力のエスケープ処理プロパティベーステストを作成する
    - **Property 25: XSS 入力のエスケープ処理** — スクリプトタグ等を含む入力が生の HTML タグとして保存・返却されない
    - **Validates: Requirements 11.3**

- [x] 6. 見積依頼機能の実装
  - [x] 6.1 見積依頼バックエンドの実装
    - `QuoteRequestService.java` に見積依頼の作成・一覧取得・詳細取得を実装する
    - `QuoteRequestController.java` に `POST /api/quote-requests`、`GET /api/quote-requests`、`GET /api/quote-requests/{id}` を実装する
    - 認証不要エンドポイント (`POST /api/quote-requests`) を SecurityConfig で設定する
    - _要件: 2.1, 2.2, 2.4, 2.7_
  - [x]\* 6.2 有効な見積依頼のラウンドトリップ保存プロパティベーステストを作成する
    - **Property 4: 有効な見積依頼のラウンドトリップ保存** — 任意の有効な依頼データを送信→取得したとき全フィールドが一致する
    - **Validates: Requirements 2.4**
  - [x] 6.3 見積依頼フロントエンドの実装
    - `app/(public)/quote/page.tsx` に見積依頼フォームを実装する (`<QuoteRequestForm />` コンポーネント)
    - `app/(dashboard)/quote-requests/page.tsx` に見積依頼一覧を実装する
    - `app/(dashboard)/quote-requests/[id]/page.tsx` に依頼詳細表示を実装する
    - _要件: 2.1, 2.2, 2.3, 2.7, 3.1_
  - [x]\* 6.4 見積依頼フォームのフロントエンドテストを作成する
    - `<QuoteRequestForm />` で必須フィールド欠落時のエラーメッセージ表示を検証する
    - _要件: 2.2, 2.3_

- [x] 7. 見積回答機能の実装
  - [x] 7.1 見積回答バックエンドの実装
    - `QuoteResponseService.java` に見積回答作成・トークン生成・トークン検索を実装する
    - `QuoteResponseController.java` に `POST /api/quote-responses`、`GET /api/quote-responses/token/{token}` を実装する
    - UUID を使ったトークン生成ロジックを実装する
    - _要件: 3.2, 3.3, 3.4, 3.5_
  - [x]\* 7.2 見積回答データのラウンドトリップ保存プロパティベーステストを作成する
    - **Property 7: 見積回答データのラウンドトリップ保存** — 任意の有効な見積回答を保存→取得したとき全フィールドが一致する
    - **Validates: Requirements 3.2, 3.3**
  - [x]\* 7.3 QuoteRequest 1 件に対する QuoteResponse 一意性プロパティベーステストを作成する
    - **Property 8: QuoteRequest 1 件に対する QuoteResponse の一意性** — 既存 QuoteResponse がある場合に 2 件目の作成は常にエラーを返す
    - **Validates: Requirements 3.4**
  - [x]\* 7.4 承認トークンの一意性プロパティベーステストを作成する
    - **Property 9: 承認トークンの一意性** — 複数の QuoteResponse が持つトークンは互いに異なる
    - **Validates: Requirements 3.5**
  - [x] 7.5 見積確認ページ (クライアント向け) のフロントエンド実装
    - `app/(public)/orders/[token]/page.tsx` に見積内容表示・承認/辞退ボタンを実装する
    - _要件: 4.1_

- [x] 8. チェックポイント — 見積〜回答フローの動作確認
  - 全テストが通過することを確認し、不明点があればユーザーに確認する。

- [x] 9. 正式依頼発行機能の実装
  - [x] 9.1 正式依頼バックエンドの実装
    - `OrderService.java` に承認・辞退処理を実装する (Order + Task の一括生成、重複防止チェック)
    - `OrderController.java` に `POST /api/orders/token/{token}/approve`、`POST /api/orders/token/{token}/decline` を実装する
    - トークン検証ロジック (不正・期限切れ・使用済みは全て 404) を実装する
    - _要件: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_
  - [x]\* 9.2 承認操作による Order・Task 一括生成のプロパティベーステストを作成する
    - **Property 10: 承認操作による Order・Task の一括生成とデータ完全性** — 有効トークン承認時に Order (受注済み) と Task (未着手) が生成され全フィールドが元データと一致する
    - **Validates: Requirements 4.2, 4.3, 4.5**
  - [x]\* 9.3 辞退操作で Task が生成されないプロパティベーステストを作成する
    - **Property 11: 辞退操作で Task が生成されない** — 有効トークン辞退時に QuoteRequest が「辞退」に更新され Task は生成されない
    - **Validates: Requirements 4.4**
  - [x]\* 9.4 Order の重複生成防止プロパティベーステストを作成する
    - **Property 12: Order の重複生成防止** — 承認済みトークンへの再承認は常にエラーを返し新 Order は生成されない
    - **Validates: Requirements 4.6**

- [x] 10. タスク管理機能の実装
  - [x] 10.1 タスク管理バックエンドの実装
    - `TaskService.java` に一覧取得 (区分フィルタ対応)・ステータス更新・カレンダー用期間クエリを実装する
    - `TaskController.java` に `GET /api/tasks`、`PATCH /api/tasks/{id}/status`、`GET /api/tasks/calendar` を実装する
    - _要件: 5.1, 5.2, 5.3, 5.4, 6.2_
  - [x]\* 10.2 Task ステータス変更の正確性プロパティベーステストを作成する
    - **Property 13: Task ステータス変更の正確性と更新日時記録** — 任意の有効ステータスへの変更後、status と status_updated_at が正しく更新される
    - **Validates: Requirements 5.1, 5.2**
  - [x]\* 10.3 依頼区分フィルタリングの正確性プロパティベーステストを作成する
    - **Property 14: 依頼区分フィルタリングの正確性** — 任意の区分 ID でフィルタしたとき返却タスクは全てその区分に属する
    - **Validates: Requirements 5.4**
  - [x]\* 10.4 カレンダー期間クエリの正確性プロパティベーステストを作成する
    - **Property 15: カレンダー期間クエリの正確性** — 任意の期間 (開始日・終了日) で取得したタスクは全て期間内の納期を持つ
    - **Validates: Requirements 6.2**
  - [x] 10.5 タスク一覧・詳細パネルのフロントエンド実装
    - `app/(dashboard)/tasks/page.tsx` にタスク一覧と `<DetailPanel />` を実装する
    - `<TaskStatusBadge />` コンポーネントを実装する
    - 依頼区分フィルタ UI を実装する
    - _要件: 5.1, 5.3, 5.4, 5.5_
  - [x] 10.6 カレンダービューのフロントエンド実装
    - `app/(dashboard)/tasks/calendar/page.tsx` に react-big-calendar を使ったカレンダービューを実装する (`<CalendarView />` コンポーネント)
    - 月/週切替・完了/未完了タスクの配色分け・DetailPanel との並列表示を実装する
    - スマートフォン向け縦スクロール対応を追加する
    - _要件: 6.1, 6.2, 6.3, 6.4, 6.5, 10.5_
  - [x]\* 10.7 タスク管理のフロントエンドテストを作成する
    - `<TaskStatusBadge />` のステータス別カラークラス、`<DetailPanel />` の開閉動作、`<CalendarView />` の月/週切替・完了/未着手の CSS クラス差異を検証する
    - _要件: 5.1, 6.4_

- [x] 11. 依頼区分管理機能の実装
  - [x] 11.1 依頼区分管理バックエンドの実装
    - `OrderCategoryController.java` に CRUD エンドポイント (`GET/POST /api/order-categories`、`PUT/DELETE /api/order-categories/{id}`) を実装する
    - 使用中区分の削除拒否ロジック (422 エラー) を実装する
    - _要件: 7.1, 7.2, 7.3, 7.4, 7.5_
  - [x]\* 11.2 依頼区分 CRUD ラウンドトリップのプロパティベーステストを作成する
    - **Property 16: 依頼区分の CRUD ラウンドトリップ** — 任意の有効な区分名を追加/更新後に一覧取得するとその区分名が含まれ一致する
    - **Validates: Requirements 7.2, 7.4**
  - [x]\* 11.3 使用中区分の削除拒否プロパティベーステストを作成する
    - **Property 18: 使用中区分の削除拒否** — Task に紐づく区分の削除は常に 422 エラーを返し区分は残る
    - **Validates: Requirements 7.5**
  - [x] 11.4 依頼区分管理画面のフロントエンド実装
    - `app/(dashboard)/categories/page.tsx` に区分の追加・編集・削除 UI を実装する
    - _要件: 7.1, 7.2, 7.4, 7.5_

- [x] 12. チェックポイント — タスク管理・区分管理の動作確認
  - 全テストが通過することを確認し、不明点があればユーザーに確認する。

- [x] 13. 請求書・PDF・メール送信機能の実装
  - [x] 13.1 Invoice バックエンドの実装
    - `InvoiceService.java` に Invoice 保存・重複防止チェック・Task 完了状態の検証を実装する
    - `InvoiceController.java` に `POST /api/invoices`、`GET /api/invoices/{id}/pdf`、`POST /api/invoices/{id}/send-email` を実装する
    - _要件: 8.1, 8.2, 8.7_
  - [x]\* 13.2 Invoice データ完全性のプロパティベーステストを作成する
    - **Property 19: Invoice データの完全性（ラウンドトリップ）** — 任意の有効な Invoice データを保存→取得したとき全フィールドが一致する
    - **Validates: Requirements 8.2**
  - [x]\* 13.3 Invoice 重複発行防止のプロパティベーステストを作成する
    - **Property 21: Invoice の重複発行防止** — 既に Invoice がある Task への再発行は常に 409 エラーを返す
    - **Validates: Requirements 8.7**
  - [x] 13.4 PDF 生成機能の実装
    - `PdfGeneratorService.java` に Apache PDFBox を使った請求書 PDF 生成を実装する (日本語フォント埋め込み対応)
    - _要件: 8.3, 8.4_
  - [x]\* 13.5 PDF 生成の有効性プロパティベーステストを作成する
    - **Property 20: PDF 生成の有効性（日本語含む）** — 任意の Invoice データ (日本語フィールド含む) から生成された PDF が有効なフォーマット (`%PDF-` ヘッダー) であり主要フィールドのテキストを含む
    - **Validates: Requirements 8.3, 8.4**
  - [x] 13.6 メール送信機能の実装
    - `EmailSenderService.java` に JavaMailSender を使った PDF 添付メール送信 (TLS 暗号化) を実装する
    - `MailConfig.java` にメール設定を実装する
    - _要件: 8.5, 8.6_
  - [x] 13.7 請求書一覧・発行フロントエンドの実装
    - `app/(dashboard)/invoices/page.tsx` に請求書一覧・発行フォーム・`<InvoicePdfPreview />` コンポーネントを実装する
    - _要件: 8.1, 8.2, 8.3, 8.5_

- [x] 14. Discord 通知機能の実装
  - [x] 14.1 Discord 通知バックエンドの実装
    - `DiscordNotifierService.java` に WebClient による非同期 Webhook 送信を `@Async` で実装する
    - QuoteRequest 生成・Order 生成・Task 完了の各イベント発火点に通知呼び出しを追加する
    - 送信失敗時のエラーログ記録とシステム継続処理を実装する
    - _要件: 9.1, 9.2, 9.3, 9.4, 9.5_
  - [x]\* 14.2 Discord 通知トリガーのプロパティベーステストを作成する
    - **Property 22: Discord 通知のトリガー確認** — 任意の QuoteRequest 生成・Order 生成・Task 完了イベント時に、通知有効設定下では Webhook クライアントの送信が必ず呼び出される
    - **Validates: Requirements 9.1, 9.2, 9.3**
  - [x]\* 14.3 通知失敗時のシステム継続性プロパティベーステストを作成する
    - **Property 23: 通知失敗時のシステム継続性** — Webhook 送信が例外をスローしても親処理 (QuoteRequest 保存・Order 生成・Task 更新) は成功する
    - **Validates: Requirements 9.4**

- [x] 15. 設定画面の実装
  - [x] 15.1 設定バックエンドの実装
    - `SettingsController.java` に `GET /api/settings`、`PUT /api/settings` を実装する
    - SMTP パスワードの暗号化保存ロジックを実装する
    - _要件: 9.5_
  - [x] 15.2 設定画面フロントエンドの実装
    - `app/(dashboard)/settings/page.tsx` に Discord Webhook URL・メール設定の入力フォームを実装する
    - _要件: 9.5_

- [x] 16. ダッシュボードとナビゲーションの実装
  - `app/(dashboard)/layout.tsx` に `<Navbar />` サイドナビゲーションを実装する
  - `app/(dashboard)/dashboard/page.tsx` に KPI・最近の依頼サマリーを表示するダッシュボードを実装する
  - ログインページ `app/(auth)/login/page.tsx` を実装する
  - 未認証時のログインページリダイレクト (Next.js middleware) を実装する
  - _要件: 1.1, 1.5, 10.1, 10.2, 10.3_

- [x] 17. チェックポイント — 全機能の統合動作確認
  - 全テストが通過することを確認し、不明点があればユーザーに確認する。

- [x] 18. インテグレーションテストの実装
  - [x]\* 18.1 見積依頼フロー E2E インテグレーションテストを作成する
    - Testcontainers (PostgreSQL) を使い「見積依頼送信 → 見積回答 → 承認 → Task 生成」の全フローを検証する
    - _要件: 2.4, 3.2, 4.2, 4.3_
  - [x]\* 18.2 Invoice 発行フロー E2E インテグレーションテストを作成する
    - Task 完了 → Invoice 発行 → PDF ダウンロードの一連のフローを検証する
    - _要件: 8.1, 8.2, 8.3_
  - [x]\* 18.3 メール送信インテグレーションテストを作成する
    - JavaMailSender の TLS 設定と PDF 添付送信を検証する
    - _要件: 8.5, 8.6_

- [x] 19. 最終チェックポイント — 全テスト通過確認
  - 全テストが通過することを確認し、不明点があればユーザーに確認する。

---

## Notes

- `*` 付きのサブタスクはオプションであり、MVP 実装を優先する場合はスキップ可能
- 各タスクは参照する要件番号を明記し、トレーサビリティを確保する
- プロパティベーステストは jqwik の `@Property(tries = 100)` で最低 100 イテレーション実行する
- 各プロパティテストには `@Tag("Feature: freelance-music-crm, Property N: <property_text>")` を付与する
- ユニットテストは具体的なシナリオに集中し、過多にならないようにする
- プロパティテストとユニットテストは補完的な関係であり、両方を組み合わせて包括的なカバレッジを実現する

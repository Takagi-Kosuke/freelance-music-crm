# 追加仕様書 / 回答書 — FreelanceMusicCRM

> 作成日: 2026-07-26  
> 目的: 仕様書（requirements.md / design.md）と実装済みコードの差分を整理し、残タスクのルールと修正方針を他のAIエージェント向けに明示する。

---

## 1. 現状サマリー

### ✅ 実装済み（仕様通り）

| カテゴリ | 内容 |
|---|---|
| エンティティ | Worker / OrderCategory / QuoteRequest / QuoteResponse / Order / Task / Invoice / WorkerSettings すべて ER 図通り実装済み |
| ステータス列挙型 | QuoteRequestStatus / TaskStatus / TokenStatus 正しく定義済み |
| Flyway | V1（スキーマ）/ V2（初期5区分シード）実装済み |
| 認証 | BCrypt / セッション管理 / ログイン失敗5回でアカウントロック / CSRF Cookie対応 |
| CORS | WebMvcConfig.java 実装済み |
| セキュリティ設定 | 公開エンドポイント・認証必須エンドポイント SecurityConfig で正しく設定済み |
| REST API | 全エンドポイント（21本）実装済み |
| バリデーション DTO | QuoteRequestCreateDto / QuoteResponseCreateDto / TaskStatusUpdateDto / OrderCategoryUpsertDto 実装済み |
| OrderCategory CRUD | 使用中区分の削除拒否 (422) 実装済み |
| Discord 通知 | @Async による非同期 Webhook、失敗時のシステム継続実装済み |
| PDF 生成 | PdfGeneratorService (PDFBox 3.x) 実装済み |
| メール送信 | InvoiceEmailService / MailConfig 実装済み |
| 設定画面 | SettingsController / SettingsService / SMTP パスワード暗号化 実装済み |
| フロントエンド構成 | Next.js App Router ルートグループ (public) / (dashboard) / middleware 実装済み |
| フロントエンドコンポーネント | Navbar / DetailPanel / TaskStatusBadge / CalendarView / InvoicePdfPreview 実装済み |
| テスト | jqwik PBT 14ファイル / UnitTest / IntegrationTest 実装済み |

---

## 2. 仕様との差分（要修正事項）

### ❌ 差分 1: `Order.status` が enum ではなく String

**仕様（design.md）:**
ステータス遷移の一貫性のため、他のステータスフィールドと同様に enum で管理すべき。

**現状のコード（Order.java）:**
```java
@Column(name = "status", nullable = false)
private String status;
```

**現状のコード（OrderService.java）:**
```java
private static final String ORDER_STATUS_RECEIVED = "受注済み";
order.setStatus(ORDER_STATUS_RECEIVED);
```

**問題:**
- `TaskStatus`・`QuoteRequestStatus` は enum で型安全なのに `Order.status` だけ `String` で統一性がない
- タイポリスクがある
- `@Enumerated(EnumType.STRING)` による DB 保存・検索が使えない

**修正方針:**
`OrderStatus` enum を新規作成し、`Order.status` を `@Enumerated(EnumType.STRING)` に変更する。

```java
// 新規作成: entity/OrderStatus.java
public enum OrderStatus {
    RECEIVED  // 受注済み
}

// Order.java の変更
@Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false)
private OrderStatus status;
```

Flyway マイグレーション（V1）の `orders.status` カラムの型は `VARCHAR(50)` のままでよい。  
既存データがある場合は `V3__migrate_order_status.sql` で `"受注済み"` → `"RECEIVED"` へ UPDATE すること。

---

### ❌ 差分 2: `QuoteResponseCreateDto.amount` の型が仕様と異なる

**仕様（design.md / requirements.md 3.3）:**
> 見積金額が 0 円以上の **整数** であることを検証する

**現状のコード（QuoteResponseCreateDto.java）:**
```java
@DecimalMin(value = "0", inclusive = true)
BigDecimal amount   // 小数も通ってしまう（例: 100.50）
```

**問題:**
- 整数チェックがないため 100.50 などの小数が通過する
- DB カラムも `DECIMAL(10, 2)` で小数2桁まで保存できてしまう

**修正方針:**
DTO に整数チェックを追加する。

```java
// QuoteResponseCreateDto.java に追加
@Digits(integer = 10, fraction = 0, message = "見積金額は整数で入力してください")
```

---

### ❌ 差分 3: `quote-requests` フロントエンドのルーティングが重複

**仕様（design.md）:**
```
app/
└── (dashboard)/
    └── quote-requests/
        ├── page.tsx        ← 見積依頼一覧（要認証）
        └── [id]/page.tsx   ← 見積依頼詳細（要認証）
```

**現状:**
```
app/
├── (dashboard)/            ← ここには quote-requests が存在しない
└── quote-requests/
    ├── page.tsx            ← (dashboard) グループ外にある
    └── [id]/page.tsx       ← (dashboard) グループ外にある
```

**問題:**
- `quote-requests` が `(dashboard)` ルートグループ外に置かれているため、ダッシュボード共通レイアウト（`Navbar` など）が適用されない

**修正方針:**
`app/quote-requests/` を `app/(dashboard)/quote-requests/` に移動する。

---

### ❌ 差分 4: `app/dashboard/` と `app/(dashboard)/dashboard/` が重複して存在

**現状のディレクトリ:**
```
app/
├── dashboard/              ← 空ディレクトリ（ファイルなし）
└── (dashboard)/
    └── dashboard/
        └── page.tsx        ← 正しいダッシュボードページ
```

**問題:**
- `app/dashboard/`（空）が残っており、Next.js がルート衝突を起こす可能性がある

**修正方針:**
`app/dashboard/`（空ディレクトリ）を削除する。

---

### ❌ 差分 5: `app/orders/[token]/` が `(public)` グループ外にある

**仕様（design.md）:**
```
app/
└── (public)/
    └── orders/[token]/page.tsx  ← クライアント向け（認証不要）
```

**現状:**
```
app/
└── orders/[token]/   ← (public) グループ外
```

**修正方針:**
`app/orders/` を `app/(public)/orders/` に移動する。

---

### ❌ 差分 6: `app/quote/` が `(public)` グループ外にある

**仕様（design.md）:**
```
app/
└── (public)/
    └── quote/page.tsx   ← 見積依頼フォーム（認証不要）
```

**現状:**
```
app/
└── quote/   ← (public) グループ外
```

**修正方針:**
`app/quote/` を `app/(public)/quote/` に移動する。

---

### ❌ 差分 7: `app/login/` が `(auth)` グループ外にある

**仕様（design.md）:**
```
app/
└── (auth)/
    └── login/page.tsx
```

**現状:**
```
app/
└── login/   ← (auth) グループ外
```

**修正方針:**
`app/login/` を `app/(auth)/login/` に移動する。

---

### ⚠️ 差分 8: `build.gradle` の Java バージョンが Java 25 になっている

**仕様（tasks.md / design.md）:**
> Spring Boot 3.x (Java **21**) によるバックエンド

**現状（build.gradle）:**
```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)  // ← Java 25！
    }
}
```

**問題:**
- 仕様では Java 21 (LTS) を指定している
- Java 25 は Preview/Early Access バージョン
- Railway / Render などの無料枠ホスティングでは Java 21 サポートが一般的

**修正方針:**
```groovy
languageVersion = JavaLanguageVersion.of(21)
```

---

### ⚠️ 差分 9: `InvoiceService` の Worker 取得が不安定

**現状（InvoiceService.java）:**
```java
Worker worker = workerRepository.findAll().stream().findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("作業者情報が見つかりません"));
```

**問題:**
- 複数 Worker が存在する場合に順序が不定
- セッションから認証済みユーザーを取得するべき

**修正方針:**
```java
String email = SecurityContextHolder.getContext().getAuthentication().getName();
Worker worker = workerRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("作業者情報が見つかりません"));
```

---

### ⚠️ 差分 10: `SettingsService` も同様に Worker 取得が不安定

`InvoiceService` と同じ問題が `SettingsService` にも存在する可能性が高い。同様の修正を適用すること。

---

## 3. 残タスク一覧（修正・追加実装が必要なもの）

| # | タスク | 優先度 | 影響範囲 |
|---|---|---|---|
| A | `OrderStatus` enum 新規作成 + `Order.java` の status フィールドを enum に変更 | 高 | backend/entity, OrderService |
| B | Flyway `V3__migrate_order_status.sql` を追加（既存データ移行） | 高 | backend/resources/db/migration |
| C | `QuoteResponseCreateDto.amount` に `@Digits(fraction=0)` を追加 | 高 | backend/dto |
| D | フロントエンドのルートグループ移動（差分3〜7） | 中 | frontend/src/app |
| E | `build.gradle` の Java バージョンを 21 に修正 | 中 | backend/build.gradle |
| F | `InvoiceService` と `SettingsService` の Worker 取得をセッションベースに修正 | 中 | backend/service |

---

## 4. 実装ルール（他のAIエージェントへの指示）

以下のルールを**必ず遵守**して実装すること。

### 4.1 命名・型のルール

- **ステータス系フィールドは必ず enum で定義する。** `String` でのハードコーディングは禁止。
  - 既存: `QuoteRequestStatus`, `TaskStatus`, `TokenStatus`
  - 追加: `OrderStatus`
- **金額フィールド (`amount`) は `BigDecimal` を使用する。** `double` / `float` は禁止。
- **日付フィールドは `LocalDate`、日時フィールドは `LocalDateTime` を使用する。**

### 4.2 バリデーションのルール

- **すべての DTO にサーバーサイド Bean Validation アノテーションを付与する。**
- 必須フィールド: `@NotBlank`（文字列）/ `@NotNull`（オブジェクト・プリミティブ）
- 金額（整数）: `@DecimalMin("0") @Digits(integer=10, fraction=0)`
- URL: `@Pattern(regexp = "^https?://.+$")`（null は許容 → `@Pattern` は null を通過させる）
- 文字数上限: `@Size(max = 1000)`（コメント）/ `@Size(min=1, max=50)`（区分名）

### 4.3 エラーレスポンスのルール

エラー時は **必ず以下の形式** で返す（`GlobalExceptionHandler` 経由）。

```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "エラーメッセージ",
  "path": "/api/...",
  "fieldErrors": [
    { "field": "フィールド名", "message": "エラーメッセージ" }
  ]
}
```

HTTP ステータスコードの対応：

| 状況 | ステータス |
|---|---|
| バリデーションエラー | 400 |
| 認証失敗 / 未認証 | 401 |
| アカウントロック | 423 |
| リソース未発見 / 不正トークン | 404 |
| 重複リソース | 409 |
| 使用中区分の削除など | 422 |
| サーバー内部エラー | 500 |

### 4.4 セキュリティのルール

- **トークン検証失敗（不正・期限切れ・使用済み）は全て 404 を返す。** 理由を外部に漏らさない。
- **パスワードは BCrypt でハッシュ化する。** 平文保存・MD5・SHA1は禁止。
- **SMTP パスワードは暗号化して保存する。** `SecretEncryptionService` を必ず使用する。
- **SQL は JPA パラメータバインディングのみ使用する。** ネイティブクエリで文字列結合は禁止。

### 4.5 非同期処理のルール

- **Discord 通知とメール送信は `@Async` メソッドで実装する。**
- 例外が発生しても **呼び出し元の処理（QuoteRequest保存・Order生成・Task更新）を中断してはならない。**
- 失敗時は `logger.warn(...)` でログを記録するのみ。`throw` しない。

### 4.6 フロントエンドのルーティングルール

Next.js App Router のルートグループは以下の規則に従う：

| グループ | パス | 対象 | 認証 |
|---|---|---|---|
| `(public)` | `/quote`, `/orders/[token]` | クライアント向けページ | 不要 |
| `(auth)` | `/login` | 認証ページ | 不要 |
| `(dashboard)` | `/dashboard`, `/quote-requests`, `/tasks`, `/categories`, `/invoices`, `/settings` | 作業者向け管理画面 | 必要 |

**ルートグループ外（`app/` 直下）にページファイルを置いてはならない。**  
`app/page.tsx`（ルートリダイレクト）のみ例外として許容する。

### 4.7 テストのルール

- **プロパティベーステストは `@Property(tries = 100)` で最低100イテレーション実行する。**
- **タグ形式: `@Tag("Feature: freelance-music-crm, Property N: <property_text>")`**
- `*` 付きのサブタスクはオプションだが、セキュリティ関連（Property 24, 25）は **MVP でも実装を推奨**。
- テストファイルは `backend/src/test/java/com/freelancemusiccrm/` に配置する。
- フロントエンドテストは `*.test.tsx` で同一ディレクトリに配置する。

---

## 5. 変更不要な実装（触らないこと）

以下はすでに仕様通りに実装されているため、変更しないこと。

- `entity/` 配下の全 JPA エンティティ（OrderStatus enum 追加後の Order.java を除く）
- `db/migration/V1__initial_schema.sql` / `V2__seed_order_categories.sql`
- `SecurityConfig.java`（公開エンドポイント設定は正しい）
- `WebMvcConfig.java`
- `GlobalExceptionHandler.java`
- `DiscordNotifierService.java`（`@Async` + エラー継続の実装は正しい）
- `TaskRepository.java`（カスタムクエリメソッドは正しい）
- `docker-compose.yml`

---

## 6. 確認チェックリスト（実装完了後に必ず確認）

- [ ] `OrderStatus` enum が存在し、`Order.status` が `@Enumerated(EnumType.STRING)` になっている
- [ ] `QuoteResponseCreateDto.amount` に `@Digits(fraction=0)` が付与されている
- [ ] フロントエンドの全ページが正しいルートグループ内に配置されている
- [ ] `app/dashboard/`（空ディレクトリ）が削除されている
- [ ] `build.gradle` の `languageVersion` が `21` になっている
- [ ] `InvoiceService` の Worker 取得がセッションベースになっている
- [ ] `./gradlew test` が全テスト通過する
- [ ] `npm run test` が全テスト通過する
- [ ] `./gradlew bootRun` でアプリケーションが起動する

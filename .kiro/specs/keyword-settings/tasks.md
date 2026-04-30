# 実装計画: keyword-settings

## タスク一覧

### 1. ドメイン層: Keyword エンティティ・リポジトリ

- [x] 1.1 Keyword エンティティを実装する (P)
  - `backend/src/main/kotlin/com/newssummary/domain/keyword/Keyword.kt` を作成する
  - `@Entity @Table(name="keywords", uniqueConstraints=[...])` を設定し、属性 `id: Long`・`userId: Long`・`word: String`・`isActive: Boolean = true` を定義する
  - `userId + word` に UNIQUE 制約を付与する
  - コンパイルが通り、JPA エンティティとして認識される
  - _Requirements: 1.1, 1.8_
  - _Boundary: Keyword_

- [x] 1.2 KeywordRepository インターフェースを実装する (P)
  - `backend/src/main/kotlin/com/newssummary/domain/keyword/KeywordRepository.kt` を作成する
  - `save`, `findById`, `findAllByUserId`, `existsByUserIdAndWord`, `deleteById` を定義する
  - インターフェースのみ（実装なし）でコンパイルが通る
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.8_
  - _Boundary: KeywordRepository_

### 2. ドメイン層: SummaryConfig エンティティ・リポジトリ

- [x] 2.1 SummaryConfig エンティティを実装する (P)
  - `backend/src/main/kotlin/com/newssummary/domain/summaryconfig/SummaryConfig.kt` を作成する
  - `@Entity @Table(name="summary_configs")` を設定し、属性 `id: Long`・`userId: Long`（UNIQUE）・`executionTime: String`・`lookbackDays: Int`・`fetchCount: Int`・`aiProviderName: String` を定義する
  - デフォルト値: executionTime="07:00", lookbackDays=1, fetchCount=10, aiProviderName="gemini"
  - コンパイルが通り、JPA エンティティとして認識される
  - _Requirements: 2.1, 2.2, 2.3_
  - _Boundary: SummaryConfig_

- [x] 2.2 SummaryConfigRepository インターフェースを実装する (P)
  - `backend/src/main/kotlin/com/newssummary/domain/summaryconfig/SummaryConfigRepository.kt` を作成する
  - `save`, `findByUserId` を定義する
  - インターフェースのみでコンパイルが通る
  - _Requirements: 2.1, 2.2, 2.3_
  - _Boundary: SummaryConfigRepository_

### 3. インフラ層: JPA リポジトリ実装

- [x] 3.1 KeywordJpaRepository を実装する (P)
  - `backend/src/main/kotlin/com/newssummary/infrastructure/persistence/KeywordJpaRepository.kt` を作成する
  - `KeywordRepository`（ドメイン）を実装する `@Component` クラスとして定義する
  - 内部に `SpringDataKeywordJpaRepository`（`JpaRepository<Keyword, Long>`）を委譲パターンで保持する
  - `existsByUserIdAndWord`・`findAllByUserId`・`deleteById` を Spring Data のメソッド命名規約で実装する
  - Spring Boot 起動時にビーンが正常に登録される
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.8_
  - _Boundary: KeywordJpaRepository_
  - _Depends: 1.1, 1.2_

- [x] 3.2 SummaryConfigJpaRepository を実装する (P)
  - `backend/src/main/kotlin/com/newssummary/infrastructure/persistence/SummaryConfigJpaRepository.kt` を作成する
  - `SummaryConfigRepository`（ドメイン）を実装する `@Component` クラスとして定義する
  - 内部に `SpringDataSummaryConfigJpaRepository`（`JpaRepository<SummaryConfig, Long>`）を委譲パターンで保持する
  - `findByUserId` を Spring Data のメソッド命名規約で実装する
  - Spring Boot 起動時にビーンが正常に登録される
  - _Requirements: 2.1, 2.2, 2.3_
  - _Boundary: SummaryConfigJpaRepository_
  - _Depends: 2.1, 2.2_

### 4. アプリケーション層: DTO定義

- [x] 4.1 Keyword DTO を定義する (P)
  - `backend/src/main/kotlin/com/newssummary/application/keyword/dto/` 以下に以下のファイルを作成する
    - `CreateKeywordRequest.kt`: `@field:NotBlank val word: String`
    - `UpdateKeywordRequest.kt`: `@field:NotNull val isActive: Boolean`
    - `KeywordResponse.kt`: `val id: Long, val userId: Long, val word: String, val isActive: Boolean`
  - コンパイルが通る
  - _Requirements: 1.1, 1.2, 1.3, 1.7_
  - _Boundary: KeywordService_

- [x] 4.2 SummaryConfig DTO を定義する (P)
  - `backend/src/main/kotlin/com/newssummary/application/summaryconfig/dto/` 以下に以下のファイルを作成する
    - `SummaryConfigRequest.kt`: `executionTime`（Pattern正規表現）・`lookbackDays`（Min/Max）・`fetchCount`（Min/Max）・`aiProviderName`（NotBlank）
    - `SummaryConfigResponse.kt`: `userId, executionTime, lookbackDays, fetchCount, aiProviderName`
  - コンパイルが通る
  - _Requirements: 2.1, 2.3, 2.4, 2.5, 2.6_
  - _Boundary: SummaryConfigService_

### 5. アプリケーション層: サービス実装

- [x] 5.1 KeywordService を実装する
  - `backend/src/main/kotlin/com/newssummary/application/keyword/KeywordService.kt` を作成する
  - `createKeyword(userId, request)`: 重複チェック後に Keyword を保存して `KeywordResponse` を返す。重複時は `DuplicateKeywordException` をスロー
  - `getKeywords(userId)`: userId に紐づく全 Keyword を返す
  - `updateKeyword(userId, keywordId, request)`: 存在確認・所有者確認後に `isActive` を更新する。存在しない場合は `KeywordNotFoundException`、所有者不一致は `ForbiddenResourceException` をスロー
  - `deleteKeyword(userId, keywordId)`: 存在確認・所有者確認後に削除する
  - `KeywordNotFoundException`・`DuplicateKeywordException`・`ForbiddenResourceException` ドメイン例外クラスをそれぞれ作成する
  - 全メソッドが正しい例外をスローすることを手動で確認できる
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8_
  - _Depends: 1.1, 1.2, 3.1, 4.1_

- [x] 5.2 SummaryConfigService を実装する
  - `backend/src/main/kotlin/com/newssummary/application/summaryconfig/SummaryConfigService.kt` を作成する
  - `getOrCreateConfig(userId)`: `findByUserId` で取得。存在しない場合はデフォルト値で新規作成して保存し、`SummaryConfigResponse` を返す
  - `updateConfig(userId, request)`: `findByUserId` で取得後、全フィールドを更新して保存し、`SummaryConfigResponse` を返す
  - GET の初回アクセス時にデフォルト設定が自動作成されることを確認できる
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_
  - _Depends: 2.1, 2.2, 3.2, 4.2_

### 6. プレゼンテーション層: コントローラー実装

- [x] 6.1 GlobalExceptionHandler に例外ハンドラを追加する
  - `backend/src/main/kotlin/com/newssummary/presentation/GlobalExceptionHandler.kt` を修正する
  - `KeywordNotFoundException` → 404、`DuplicateKeywordException` → 409、`ForbiddenResourceException` → 403 のハンドラを追加する
  - 各例外がスローされたときに正しい HTTP ステータスと `ErrorResponse` が返ることを確認できる
  - _Requirements: 1.5, 1.6, 1.8_
  - _Depends: 5.1_

- [x] 6.2 KeywordController を実装する
  - `backend/src/main/kotlin/com/newssummary/presentation/KeywordController.kt` を作成する
  - `POST /api/keywords` → `createKeyword(userId, request)` を呼び出し 201 を返す
  - `GET /api/keywords` → `getKeywords(userId)` を呼び出し 200 を返す
  - `PATCH /api/keywords/{id}` → `updateKeyword(userId, id, request)` を呼び出し 200 を返す
  - `DELETE /api/keywords/{id}` → `deleteKeyword(userId, id)` を呼び出し 204 を返す
  - `SecurityContext`（`JwtAuthenticationFilter` が設定）から userId を取得してサービスに渡す
  - 全エンドポイントが Swagger/cURL で正しい HTTP ステータスを返すことを確認できる
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 3.1, 3.2, 3.3_
  - _Depends: 5.1, 6.1_

- [x] 6.3 SummaryConfigController を実装する
  - `backend/src/main/kotlin/com/newssummary/presentation/SummaryConfigController.kt` を作成する
  - `GET /api/summary-config` → `getOrCreateConfig(userId)` を呼び出し 200 を返す
  - `PUT /api/summary-config` → `updateConfig(userId, request)` を呼び出し 200 を返す
  - `SecurityContext` から userId を取得してサービスに渡す
  - 全エンドポイントが正しい HTTP ステータスを返すことを確認できる
  - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 3.4_
  - _Depends: 5.2_

- [x] 6.4 SecurityConfig に新規エンドポイントの認証設定を追加する
  - `backend/src/main/kotlin/com/newssummary/infrastructure/security/SecurityConfig.kt` を修正する
  - `/api/keywords/**` および `/api/summary-config/**` が許可リストに含まれないことを確認し（デフォルトで認証必須）、認証なしのリクエストが 401 を返すことを確認できる
  - _Requirements: 3.1, 3.2_
  - _Depends: 6.2, 6.3_

### 7. フロントエンド: API クライアント

- [x] 7.1 keywordsApi.ts を実装する (P)
  - `frontend/src/api/keywordsApi.ts` を作成する
  - `getAll()`: GET `/api/keywords` → `KeywordResponse[]`
  - `create(word)`: POST `/api/keywords` → `KeywordResponse`
  - `update(id, isActive)`: PATCH `/api/keywords/{id}` → `KeywordResponse`
  - `remove(id)`: DELETE `/api/keywords/{id}` → `void`
  - 既存の `axiosClient`（infrastructure スペック提供・JWT自動付与）を使用する
  - TypeScript 型定義（`KeywordResponse` インターフェース）をファイル内に定義する
  - _Requirements: 4.1, 4.2, 4.3, 4.4_
  - _Boundary: keywordsApi_

- [x] 7.2 summaryConfigApi.ts を実装する (P)
  - `frontend/src/api/summaryConfigApi.ts` を作成する
  - `get()`: GET `/api/summary-config` → `SummaryConfigResponse`
  - `update(config)`: PUT `/api/summary-config` → `SummaryConfigResponse`
  - 既存の `axiosClient` を使用する
  - TypeScript 型定義（`SummaryConfigRequest`・`SummaryConfigResponse` インターフェース）をファイル内に定義する
  - _Requirements: 5.1, 5.2_
  - _Boundary: summaryConfigApi_

### 8. フロントエンド: Reactコンポーネント

- [x] 8.1 KeywordList コンポーネントを実装する (P)
  - `frontend/src/components/KeywordList.tsx` を作成する
  - Props: `keywords: KeywordResponse[]`, `onToggle: (id, isActive) => void`, `onDelete: (id) => void`
  - 各キーワードの `word`・`isActive` 状態・トグルボタン・削除ボタンを表示する
  - Tailwind CSS でスタイリングする
  - _Requirements: 4.1, 4.3, 4.4, 4.6_
  - _Boundary: KeywordList_

- [x] 8.2 KeywordAddForm コンポーネントを実装する (P)
  - `frontend/src/components/KeywordAddForm.tsx` を作成する
  - Props: `onAdd: (word: string) => Promise<void>`
  - テキスト入力フィールドと送信ボタンを持つフォームを実装する
  - 送信後に入力フィールドをクリアする
  - Tailwind CSS でスタイリングする
  - _Requirements: 4.2, 4.6_
  - _Boundary: KeywordAddForm_

- [x] 8.3 KeywordsPage を実装する
  - `frontend/src/pages/KeywordsPage.tsx` を作成する
  - ページ表示時に `keywordsApi.getAll()` を呼び出して一覧を useState で管理する
  - `KeywordAddForm` の `onAdd` で `keywordsApi.create()` を呼び出し、成功後に一覧を更新する
  - `KeywordList` の `onToggle` で `keywordsApi.update()` を呼び出し、UIを即時更新する
  - `KeywordList` の `onDelete` で `keywordsApi.remove()` を呼び出し、一覧から除去する
  - APIエラー発生時に画面上にエラーメッセージを表示する
  - ブラウザで `/keywords` にアクセスしてキーワードの追加・トグル・削除が正常に動作することを確認できる
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_
  - _Depends: 7.1, 8.1, 8.2_

- [x] 8.4 SummaryConfigForm コンポーネントを実装する (P)
  - `frontend/src/components/SummaryConfigForm.tsx` を作成する
  - Props: `config: SummaryConfigResponse`, `onSubmit: (config: SummaryConfigRequest) => Promise<void>`, `errors?: Record<string, string>`
  - `executionTime` は `<input type="time">`、`lookbackDays`・`fetchCount` は `<input type="number">` で実装する
  - `aiProviderName` はテキスト入力または選択UIとして実装する
  - フィールドごとのエラーメッセージ表示領域を持つ
  - Tailwind CSS でスタイリングする
  - _Requirements: 5.1, 5.3, 5.4_
  - _Boundary: SummaryConfigForm_

- [x] 8.5 SettingsPage を実装する
  - `frontend/src/pages/SettingsPage.tsx` を作成する
  - ページ表示時に `summaryConfigApi.get()` を呼び出してフォームに初期値をセットする
  - `SummaryConfigForm` の `onSubmit` で `summaryConfigApi.update()` を呼び出す
  - 成功時に「設定を保存しました」などの完了フィードバックを表示する
  - APIエラー（400 バリデーション等）時にフィールドエラーを `SummaryConfigForm` に渡して表示する
  - ブラウザで `/settings` にアクセスして設定の取得・更新が正常に動作することを確認できる
  - _Requirements: 5.1, 5.2, 5.3, 5.4_
  - _Depends: 7.2, 8.4_

### 9. ルーティング統合

- [x] 9.1 フロントエンドルーティングに新ページを追加する
  - `frontend/src/App.tsx` を修正する
  - `/keywords` → `<KeywordsPage>` ルートを追加する
  - `/settings` → `<SettingsPage>` ルートを追加する
  - ブラウザで `/keywords` および `/settings` にアクセスしてページが表示されることを確認できる
  - _Requirements: 4.1, 5.1_
  - _Depends: 8.3, 8.5_

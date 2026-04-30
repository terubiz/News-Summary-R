# 実装計画: ai-summary-engine

## タスク一覧

- [x] 1. ドメイン層の実装（Summary集約・AIProviderPort）
- [x] 1.1 Summaryエンティティを作成する (P)
  - `backend/src/main/kotlin/com/newssummary/domain/summary/Summary.kt` を新規作成する
  - `@Entity @Table(name = "summaries")` アノテーションを付与し、`id: Long`・`userId: Long`・`content: String`（`@Column(columnDefinition = "TEXT")`）・`keywords: String`・`generatedAt: Instant` フィールドを定義する
  - kotlin-jpaプラグインによりno-argコンストラクタが自動生成されることを確認する
  - ファイルが存在し、コンパイルエラーなく`./gradlew compileKotlin`が通ること
  - _Requirements: 4.1_
  - _Boundary: domain/summary_

- [x] 1.2 SummaryRepositoryインターフェースを作成する (P)
  - `backend/src/main/kotlin/com/newssummary/domain/summary/SummaryRepository.kt` を新規作成する
  - `save(summary: Summary): Summary` と `findAllByUserIdOrderByGeneratedAtDesc(userId: Long): List<Summary>` メソッドを定義する
  - _Requirements: 4.2, 4.5_
  - _Boundary: domain/summary_

- [x] 1.3 AIProviderPortインターフェースを作成する (P)
  - `backend/src/main/kotlin/com/newssummary/domain/summary/AIProviderPort.kt` を新規作成する
  - `generateSummary(keywords: List<String>, config: SummaryConfig): AIProviderResult` メソッドを定義する
  - `data class AIProviderResult(val summaryText: String)` を同ファイルまたは同パッケージに定義する
  - _Requirements: 1.1, 1.5_
  - _Boundary: domain/summary_

---

- [x] 2. カスタム例外クラスの実装
- [x] 2.1 ai-summary-engine固有の例外クラスを作成する (P)
  - `backend/src/main/kotlin/com/newssummary/application/summary/` 配下または共通の`exception/`パッケージに `AIProviderException`・`UnsupportedAIProviderException`・`NoActiveKeywordsException` クラスを作成する
  - 各例外は`RuntimeException`を継承し、適切なコンストラクタメッセージを持つ
  - `./gradlew compileKotlin` が通ること
  - _Requirements: 6.1_
  - _Boundary: application/summary_

---

- [x] 3. インフラ/永続化層の実装（SummaryJpaRepository）
- [x] 3.1 SummaryJpaRepositoryを実装する
  - `backend/src/main/kotlin/com/newssummary/infrastructure/persistence/SummaryJpaRepository.kt` を新規作成する
  - 内部に`SpringDataSummaryJpaRepository`（`JpaRepository<Summary, Long>`を継承するSpring Dataインターフェース）を委譲パターンで保持する
  - `SummaryRepository`（ドメイン）インターフェースを実装し、`save`・`findAllByUserIdOrderByGeneratedAtDesc` を委譲する
  - `findAllByUserIdOrderByGeneratedAtDesc` はSpring Dataのメソッド命名規約で自動生成されることを確認する
  - `./gradlew compileKotlin` が通ること
  - _Requirements: 4.3, 4.4, 4.5_
  - _Boundary: infrastructure/persistence_
  - _Depends: 1.1, 1.2_

---

- [x] 4. インフラ/AI層の実装（GeminiVertexAIAdapter・AIProviderRouter）
- [x] 4.1 build.gradle.ktsにVertex AI SDK依存を追加する
  - `backend/build.gradle.kts` の `dependencies` ブロックに `implementation("com.google.cloud:google-cloud-vertexai")` を追加する
  - バージョンはBOMまたは明示的なバージョン指定（`google-cloud-bom`推奨）を使用する
  - `./gradlew dependencies` でVertex AI SDKが依存ツリーに現れること
  - _Requirements: 2.4_
  - _Boundary: build.gradle.kts_

- [x] 4.2 application.ymlにGCP・Gemini設定を追加する (P)
  - `backend/src/main/resources/application.yml` に `gcp.project-id`・`gcp.location`（デフォルト: `us-central1`）・`ai.gemini.model-name`（デフォルト: `gemini-1.5-pro`）の設定を追加する
  - `gcp.project-id` が未設定の場合にSpringBoot起動時にエラーが発生するよう `@Value("\${gcp.project-id}")` でバリデーションされることを設計上確認する
  - application.ymlに3項目の設定エントリが存在すること
  - _Requirements: 2.5, 6.3, 6.4_
  - _Boundary: application.yml_

- [x] 4.3 GeminiVertexAIAdapterを実装する
  - `backend/src/main/kotlin/com/newssummary/infrastructure/ai/GeminiVertexAIAdapter.kt` を新規作成する
  - `@Value` で `gcp.project-id`・`gcp.location`・`ai.gemini.model-name` を注入する
  - `VertexAI(projectId, location)` でクライアント初期化し、`GenerativeModel` にGoogle Search Grounding `Tool` を設定する
  - キーワードリストと`SummaryConfig.lookbackDays`を組み込んだプロンプトを構築して`generateContent`を呼び出す
  - レスポンスから`candidates[0].content.parts[0].text`を抽出し`AIProviderResult`を返す
  - レスポンスが空または`candidates`が存在しない場合は`AIProviderException("Empty response from Gemini")`をスローする
  - ネットワーク・APIエラーは`AIProviderException`にラップしてスローする
  - エラー発生時にERRORレベルでログを出力する（要件6.2）
  - `./gradlew compileKotlin` が通ること
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 6.2_
  - _Boundary: infrastructure/ai_
  - _Depends: 1.3, 4.1, 4.2_

- [x] 4.4 AIProviderRouterを実装する
  - `backend/src/main/kotlin/com/newssummary/infrastructure/ai/AIProviderRouter.kt` を新規作成する
  - `AIProviderPort`インターフェースを実装し、`config.aiProviderName` の `when` 式で `"gemini"` → `GeminiVertexAIAdapter`、それ以外 → `UnsupportedAIProviderException` をスローする
  - `@Component` アノテーションを付与し、Springが`AIProviderPort`型として`GenerateSummaryUseCase`にDIできることを確認する
  - `./gradlew compileKotlin` が通ること
  - _Requirements: 1.2, 1.3, 1.4_
  - _Boundary: infrastructure/ai_
  - _Depends: 1.3, 4.3, 2.1_

---

- [x] 5. アプリケーション層の実装（GenerateSummaryUseCase・SummaryService）
- [x] 5.1 GenerateSummaryUseCaseを実装する
  - `backend/src/main/kotlin/com/newssummary/application/summary/GenerateSummaryUseCase.kt` を新規作成する
  - `@Service` アノテーションを付与し、`KeywordRepository`・`SummaryConfigRepository`・`AIProviderPort`（DIされるのはAIProviderRouter）・`SummaryRepository` をコンストラクタ注入する
  - `execute(userId: Long): SummaryResult` を実装する:
    1. `keywordRepository.findAllByUserId(userId)` でキーワード取得 → `isActive == true` でフィルタリング
    2. アクティブキーワードが0件の場合は `NoActiveKeywordsException` をスロー
    3. `summaryConfigRepository.findByUserId(userId)` で設定取得（nullの場合はデフォルト`SummaryConfig`を生成）
    4. `aiProviderPort.generateSummary(keywordWords, config)` を呼び出す
    5. `summaryRepository.save(Summary(userId, result.summaryText, keywordsString, Instant.now()))` で永続化
    6. `SummaryResult(id, content, generatedAt)` を返す
  - `data class SummaryResult(val id: Long, val content: String, val generatedAt: Instant)` を同ファイルまたはdto/配下に定義する
  - `./gradlew compileKotlin` が通ること
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_
  - _Boundary: application/summary_
  - _Depends: 1.1, 1.2, 1.3, 2.1, 3.1, 4.4_

- [x] 5.2 SummaryServiceを実装する (P)
  - `backend/src/main/kotlin/com/newssummary/application/summary/SummaryService.kt` を新規作成する
  - `@Service` アノテーションを付与し、`SummaryRepository` をコンストラクタ注入する
  - `getSummaries(userId: Long): List<SummaryResponse>` を実装し、`summaryRepository.findAllByUserIdOrderByGeneratedAtDesc(userId)` を呼び出して`SummaryResponse`リストに変換して返す
  - `data class SummaryResponse(val id: Long, val content: String, val keywords: String, val generatedAt: Instant)` をdto/配下に定義する
  - `./gradlew compileKotlin` が通ること
  - _Requirements: 5.1, 5.2, 5.3, 5.4_
  - _Boundary: application/summary_
  - _Depends: 1.1, 1.2, 3.1_

---

- [x] 6. プレゼンテーション層の実装（SummaryController・GlobalExceptionHandler修正）
- [x] 6.1 SummaryControllerを実装する
  - `backend/src/main/kotlin/com/newssummary/presentation/SummaryController.kt` を新規作成する
  - `@RestController @RequestMapping("/api/summaries")` を付与し、`GenerateSummaryUseCase`・`SummaryService` をコンストラクタ注入する
  - `GET /api/summaries` → `SummaryService.getSummaries(userId)` を呼び出し`List<SummaryResponse>`を200で返す
  - `POST /api/summaries/generate` → `GenerateSummaryUseCase.execute(userId)` を呼び出し`SummaryResponse`を200で返す
  - `@AuthenticationPrincipal`またはSecurityContextから`userId`（Long型）を取得する
  - `./gradlew compileKotlin` が通ること
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_
  - _Boundary: presentation_
  - _Depends: 5.1, 5.2_

- [x] 6.2 GlobalExceptionHandlerに例外ハンドラを追加する (P)
  - `backend/src/main/kotlin/com/newssummary/presentation/GlobalExceptionHandler.kt` を修正する
  - `AIProviderException` → HTTP 502・`UnsupportedAIProviderException` → HTTP 400・`NoActiveKeywordsException` → HTTP 422 のハンドラメソッドをそれぞれ追加する
  - 既存の`ErrorResponse`形式（`timestamp`, `status`, `error`, `message`）で返す
  - `./gradlew compileKotlin` が通ること
  - _Requirements: 6.1_
  - _Boundary: presentation_
  - _Depends: 2.1_

---

- [x] 7. SecurityConfig修正（/api/summaries エンドポイント保護）
- [x] 7.1 SecurityConfigに/api/summariesエンドポイントの認証設定を追加する
  - `backend/src/main/kotlin/com/newssummary/infrastructure/security/SecurityConfig.kt` を修正する
  - `/api/summaries`・`/api/summaries/generate` が `permitAll` に含まれていないことを確認する（デフォルトで認証必須になっていることを確認する）
  - `./gradlew compileKotlin` が通ること
  - _Requirements: 5.3_
  - _Boundary: infrastructure/security_
  - _Depends: 6.1_

---

- [x] 8. 統合確認（docker compose up での動作確認）
- [x] 8.1 docker compose upで全サービスが起動し、エンドポイントが疎通することを確認する
  - `docker compose up` で Spring Boot・PostgreSQL・Redis が正常起動すること
  - `POST /api/auth/login` でJWTトークンを取得できること
  - `GET /api/summaries`（JWT付き）が200・空配列を返すこと
  - `POST /api/summaries/generate`（JWT付き、アクティブキーワードなし）が422を返すこと
  - キーワードを登録後に `POST /api/summaries/generate` を呼び出し、`summaries` テーブルにレコードが保存されること（GCP認証情報が設定されている環境でのみ確認）
  - `GET /api/summaries` が保存された要約を返すこと
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 4.3, 5.1, 5.5_
  - _Depends: 6.1, 6.2, 7.1_

# 要件定義書: ai-summary-engine

## はじめに

本スペックは、ニュース要約配信アプリケーション（News-Summary-R）の**AIニュース要約エンジン**を実装する。認証済みユーザーが登録したキーワードリストを受け取り、Gemini REST API の Google Search Grounding 機能を活用してリアルタイムにニュースを調査・要約し、その結果を `Summary` エンティティとして永続化する機能を提供する。

本スペックは `keyword-settings` スペックが提供する `Keyword`・`SummaryConfig` エンティティを上流依存とし、`scheduler`（`GenerateSummaryUseCase`を呼び出す）および `notification-delivery`（`Summary`集約を読み取る）を下流とする。

## 境界コンテキスト（オプション）

- **スコープ内**: `AIProviderPort`出力ポートインターフェース定義、`GeminiApiAdapter`実装（Gemini REST API + Google Search Grounding）、`GenerateSummaryUseCase`（キーワード受取→AI呼出→SummaryResult返却）、`Summary`エンティティ（id, userId, content, keywords, generatedAt）の永続化、AIプロバイダー名によるアダプタールーティング
- **スコープ外**: スケジューリング（`scheduler`スペックが担当）、通知配信（`notification-delivery`スペックが担当）、プロバイダー管理画面（`keyword-settings`スペックの設定フォームが担当）、ニュースソースの直接取得（AIのGrounding検索に委ねる）
- **隣接する期待**: `scheduler`スペックは`GenerateSummaryUseCase`を呼び出すことができる。`notification-delivery`スペックは`SummaryRepository`から`Summary`を読み取ることができる。

## 要件

### 要件 1: AIプロバイダー出力ポート（AIProviderPort）

**目的**: 開発者として、AIプロバイダーを抽象化したインターフェースを通じてニュース要約を生成したい。将来的に複数のAIプロバイダーへの切り替えを可能にするため。

#### 受け入れ基準

1. The ai-summary-engine shall define an `AIProviderPort` output port interface in the domain layer that accepts a list of keywords and summary configuration, and returns a summary result text.
2. When `GenerateSummaryUseCase` calls `AIProviderPort`, the ai-summary-engine shall route to the appropriate adapter based on `SummaryConfig.aiProviderName`.
3. If `SummaryConfig.aiProviderName` is `"gemini"`, the ai-summary-engine shall delegate to `GeminiApiAdapter`.
4. If `SummaryConfig.aiProviderName` refers to an unknown provider name, the ai-summary-engine shall throw an `UnsupportedAIProviderException` and not generate a summary.
5. The ai-summary-engine shall keep `AIProviderPort` in the domain layer, with all concrete adapters in the infrastructure layer, to maintain dependency inversion.

---

### 要件 2: Gemini API アダプター（GeminiApiAdapter）

**目的**: 開発者として、Gemini REST API を直接呼び出してGeminiモデルを利用し、Google Search Grounding を有効化したニュース要約を生成したい。リアルタイムのニュース情報をAIが調査・要約できるようにするため。

#### 受け入れ基準

1. When `GeminiApiAdapter.generateSummary` is called with a keyword list, the ai-summary-engine shall construct a prompt that instructs Gemini to research and summarize recent news related to each keyword.
2. When calling the Gemini API, the ai-summary-engine shall enable Google Search Grounding on the request by including `tools: [{"googleSearch": {}}]` in the request body to allow the model to retrieve real-time news information.
3. When the Gemini API call succeeds, the ai-summary-engine shall extract the generated text from the response and return it as `AIProviderResult`.
4. The ai-summary-engine shall call the Gemini REST API endpoint (`https://generativelanguage.googleapis.com/v1beta/models/{modelName}:generateContent?key={apiKey}`) using Spring `RestClient` without any external AI SDK.
5. The ai-summary-engine shall read the API key from `gemini.api-key` (`GEMINI_API_KEY` 環境変数), the API URL from `gemini.api-url`, and the model name from `gemini.model-name` in `application.yml`.
6. If the Gemini API call fails due to a network error or API error, the ai-summary-engine shall throw an `AIProviderException` with the original error details.
7. If the Gemini API response contains no generated text content, the ai-summary-engine shall throw an `AIProviderException` indicating an empty response.

---

### 要件 3: ニュース要約生成ユースケース（GenerateSummaryUseCase）

**目的**: スケジューラーとして（または手動実行として）、ユーザーIDを指定してニュース要約生成を実行したい。キーワードに基づいたニュース要約を生成してデータベースに保存するため。

#### 受け入れ基準

1. When `GenerateSummaryUseCase.execute` is called with a `userId`, the ai-summary-engine shall retrieve the user's active keywords from `KeywordRepository` and the user's `SummaryConfig` from `SummaryConfigRepository`.
2. When active keywords exist for the user, the ai-summary-engine shall call `AIProviderPort.generateSummary` with the keyword list and summary configuration.
3. When `AIProviderPort` returns a summary result, the ai-summary-engine shall create a `Summary` entity with `userId`, `content` (the returned text), `keywords` (the keyword list used), and `generatedAt` (current timestamp), and persist it via `SummaryRepository`.
4. When the use case completes successfully, the ai-summary-engine shall return a `SummaryResult` containing the saved `Summary` entity's id, content, and generatedAt.
5. If the user has no active keywords, the ai-summary-engine shall not call `AIProviderPort` and shall throw a `NoActiveKeywordsException`.
6. If `SummaryConfig` does not exist for the user, the ai-summary-engine shall use default configuration values (as defined by `SummaryConfig` defaults: executionTime="07:00", lookbackDays=1, fetchCount=10, aiProviderName="gemini").
7. If `AIProviderPort` throws an `AIProviderException`, the ai-summary-engine shall propagate the exception to the caller without creating a `Summary` entity.

---

### 要件 4: Summary エンティティと永続化

**目的**: 開発者として（および下流スペックの`notification-delivery`として）、生成されたニュース要約をPostgreSQLに永続化し取得したい。要約履歴の保存と配信に使用するため。

#### 受け入れ基準

1. The ai-summary-engine shall define a `Summary` domain entity with the following fields: `id: Long`（自動採番）、`userId: Long`（User.idへの論理参照）、`content: String`（要約テキスト、TEXT型）、`keywords: String`（使用したキーワードのカンマ区切りリスト）、`generatedAt: Instant`（生成日時）.
2. The ai-summary-engine shall define a `SummaryRepository` interface in the domain layer with methods to save a `Summary` and find summaries by `userId`.
3. When a `Summary` is saved via `SummaryRepository`, the ai-summary-engine shall persist it to the `summaries` table in PostgreSQL using Spring Data JPA (blocking JPA only, no R2DBC).
4. The ai-summary-engine shall provide a `SummaryJpaRepository` in the infrastructure layer that implements `SummaryRepository`.
5. When querying summaries by `userId`, the ai-summary-engine shall return them ordered by `generatedAt` descending (newest first).

---

### 要件 5: REST API エンドポイント（SummaryController）

**目的**: 認証済みユーザーとして、自分のニュース要約の一覧をAPIで取得したい。Webダッシュボードやフロントエンドで要約を表示するため。

#### 受け入れ基準

1. The ai-summary-engine shall expose a `GET /api/summaries` endpoint that returns the authenticated user's summaries ordered by `generatedAt` descending.
2. When a request is made to `GET /api/summaries`, the ai-summary-engine shall return a list of `SummaryResponse` objects containing `id`, `content`, `keywords`, and `generatedAt`.
3. While a user is authenticated via JWT, the ai-summary-engine shall restrict `GET /api/summaries` to return only summaries belonging to that user's `userId` (derived from the JWT token).
4. If no summaries exist for the user, the ai-summary-engine shall return an empty list with HTTP 200.
5. The ai-summary-engine shall expose a `POST /api/summaries/generate` endpoint that triggers `GenerateSummaryUseCase` for the authenticated user and returns the generated `SummaryResponse`.
6. When `POST /api/summaries/generate` is called and `NoActiveKeywordsException` is thrown, the ai-summary-engine shall return HTTP 422 with an error message indicating no active keywords are configured.
7. When `POST /api/summaries/generate` is called and `AIProviderException` is thrown, the ai-summary-engine shall return HTTP 502 with an error message indicating the AI provider failed.

---

### 要件 6: エラーハンドリングと設定

**目的**: 運用者として、AIプロバイダーの障害や設定ミスを適切にハンドリングしたい。障害の原因を素早く特定し、システムの安定性を維持するため。

#### 受け入れ基準

1. The ai-summary-engine shall add `UnsupportedAIProviderException` (→ HTTP 400), `NoActiveKeywordsException` (→ HTTP 422), and `AIProviderException` (→ HTTP 502) to `GlobalExceptionHandler`.
2. When an `AIProviderException` occurs, the ai-summary-engine shall log the full error details (error message, stack trace) at ERROR level before propagating the exception.
3. The ai-summary-engine shall read the Gemini API key from `gemini.api-key` in `application.yml` (populated via `GEMINI_API_KEY` 環境変数). If the API key is empty, the adapter will fail at call time.
4. The ai-summary-engine shall read the Gemini model name from `gemini.model-name` in `application.yml` (defaulting to `"gemini-1.5-pro"`) to allow model version configuration without code changes.

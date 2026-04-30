# 設計書: ai-summary-engine

## 概要

本スペックは、ニュース要約配信アプリケーション（News-Summary-R）の**AIニュース要約エンジン**を実装する。認証済みユーザーが登録したキーワードを基に、Google Vertex AI（Gemini）のGoogle Search Grounding機能を活用してリアルタイムにニュースを調査・要約し、結果を`Summary`エンティティとしてPostgreSQLに永続化する。

**対象ユーザー**: JWT認証済みのアプリケーションエンドユーザー（APIを通じた要約取得）、および`scheduler`スペックのバッチ実行（`GenerateSummaryUseCase`の呼び出し元）。

**影響範囲**: `keyword-settings`スペックで確立した`Keyword`・`SummaryConfig`エンティティおよびリポジトリを上流依存とし、新たに`AIProviderPort`出力ポート・`GeminiVertexAIAdapter`・`GenerateSummaryUseCase`・`Summary`集約・`SummaryController`をDDD 4層パッケージ構造に追加する。

### ゴール

- `AIProviderPort`出力ポートインターフェースを定義し、将来の複数プロバイダー切り替えを設計上サポートする
- `GeminiVertexAIAdapter`（Vertex AI SDK + Google Search Grounding）を実装し、キーワードに基づくリアルタイムニュース要約を生成する
- `GenerateSummaryUseCase`でキーワード取得→AI呼出→Summary永続化のオーケストレーションを実装する
- `Summary`エンティティとリポジトリを提供し、`notification-delivery`スペックが読み取れる状態を確立する
- `GET /api/summaries`・`POST /api/summaries/generate`エンドポイントを公開する

### 非ゴール

- スケジューリング（`scheduler`スペックが担当）
- 通知配信・メール送信（`notification-delivery`スペックが担当）
- プロバイダー管理画面・追加削除UI（`keyword-settings`の設定フォームが担当）
- ニュースソースの直接取得（AIのGrounding検索に委ねる）
- テストコード（プロジェクトポリシーによりスコープ外）
- DBマイグレーションツール（`ddl-auto=update`で代替）

---

## 境界コミットメント（Boundary Commitments）

### このスペックが所有するもの

- `AIProviderPort`ドメイン出力ポートインターフェース（プロバイダー抽象化の境界）
- `GeminiVertexAIAdapter`インフラアダプター（Vertex AI SDK + Google Search Grounding実装）
- `AIProviderRouter`（`SummaryConfig.aiProviderName`によるアダプタールーティング）
- `GenerateSummaryUseCase`アプリケーションサービス（キーワード取得→AI呼出→Summary永続化）
- `Summary`ドメインエンティティ（id, userId, content, keywords, generatedAt）と`SummaryRepository`インターフェース
- `SummaryJpaRepository`インフラ実装
- `SummaryController` REST APIエンドポイント（`GET /api/summaries`・`POST /api/summaries/generate`）
- `SummaryService`アプリケーションサービス（一覧取得ユースケース）
- カスタム例外クラス（`AIProviderException`・`UnsupportedAIProviderException`・`NoActiveKeywordsException`）

### 境界外（このスペックが所有しないもの）

- `Keyword`・`SummaryConfig`エンティティおよびリポジトリ（`keyword-settings`スペックが所有）
- `User`エンティティ・JWT認証フィルター・SecurityConfig（`infrastructure`スペックが所有）
- スケジューリングロジック・ジョブ履歴（`scheduler`スペックが担当）
- Email配信・Webダッシュボード画面（`notification-delivery`スペックが担当）
- GCPプロジェクト・IAM設定・ADC認証情報の管理（インフラ運用の責務）

### 許可された依存

- `infrastructure`スペック提供: `User`（id: Long）、`JwtAuthenticationFilter`・`SecurityConfig`・`GlobalExceptionHandler`
- `keyword-settings`スペック提供: `Keyword`・`KeywordRepository`・`SummaryConfig`・`SummaryConfigRepository`
- `com.google.cloud:google-cloud-vertexai`（早期アクセスSDK不可）
- Spring Boot 3.x / Spring Data JPA（blocking JPA のみ）
- PostgreSQL（Docker Compose経由）

### 再検証トリガー

- `Summary`エンティティの属性・ID型を変更した場合、`notification-delivery`は読み取りモデルを再検証すること
- `GenerateSummaryUseCase`のインターフェース（引数・戻り値）を変更した場合、`scheduler`は呼び出しコードを再検証すること
- `AIProviderPort`のインターフェースを変更した場合、すべての実装アダプターを再検証すること
- `SummaryRepository`のインターフェースを変更した場合、`notification-delivery`を再検証すること
- Vertex AI SDKのバージョンアップによりAPIが変更された場合、`GeminiVertexAIAdapter`を再検証すること

---

## アーキテクチャ

### DDD 4層パッケージ構造への追加

本スペックは`keyword-settings`スペックが確立したパッケージ構造に、以下のパッケージを追加する。

```
com.newssummary
├── domain/
│   ├── user/                       # ← infrastructure スペック（変更なし）
│   ├── keyword/                    # ← keyword-settings スペック（変更なし）
│   ├── summaryconfig/              # ← keyword-settings スペック（変更なし）
│   ├── summary/                    # ← 本スペックで追加
│   │   ├── Summary.kt              # Summary JPA エンティティ兼ドメインモデル
│   │   ├── SummaryRepository.kt    # リポジトリインターフェース（ドメイン層）
│   │   └── AIProviderPort.kt       # AI呼び出し出力ポート（ドメイン層）
├── application/
│   ├── auth/                       # ← infrastructure スペック（変更なし）
│   ├── keyword/                    # ← keyword-settings スペック（変更なし）
│   ├── summaryconfig/              # ← keyword-settings スペック（変更なし）
│   └── summary/                    # ← 本スペックで追加
│       ├── GenerateSummaryUseCase.kt  # キーワード取得→AI呼出→Summary永続化
│       ├── SummaryService.kt          # Summary一覧取得ユースケース
│       └── dto/
│           ├── SummaryResult.kt       # GenerateSummaryUseCaseの戻り値DTO
│           └── SummaryResponse.kt     # REST APIレスポンスDTO
├── infrastructure/
│   ├── persistence/
│   │   ├── UserJpaRepository.kt    # ← infrastructure スペック（変更なし）
│   │   ├── KeywordJpaRepository.kt # ← keyword-settings スペック（変更なし）
│   │   ├── SummaryConfigJpaRepository.kt # ← keyword-settings スペック（変更なし）
│   │   └── SummaryJpaRepository.kt # ← 本スペックで追加（SummaryRepository の JPA 実装）
│   ├── ai/                         # ← 本スペックで追加
│   │   ├── GeminiVertexAIAdapter.kt   # AIProviderPort の Gemini 実装
│   │   └── AIProviderRouter.kt        # aiProviderName によるルーティング
│   ├── redis/                      # ← infrastructure スペック（変更なし）
│   └── security/                   # ← infrastructure スペック（変更なし）
└── presentation/
    ├── AuthController.kt           # ← infrastructure スペック（変更なし）
    ├── KeywordController.kt        # ← keyword-settings スペック（変更なし）
    ├── SummaryConfigController.kt  # ← keyword-settings スペック（変更なし）
    ├── GlobalExceptionHandler.kt   # ← 本スペックで例外追加のみ
    └── SummaryController.kt        # ← 本スペックで追加
```

### アーキテクチャパターン・境界マップ

```mermaid
graph TB
    subgraph Presentation
        SC[SummaryController]
        GEH[GlobalExceptionHandler ← 例外追加]
    end

    subgraph Application
        GSUC[GenerateSummaryUseCase]
        SS[SummaryService]
    end

    subgraph Domain
        SummaryE[Summary Entity]
        SR[SummaryRepository IF]
        APP[AIProviderPort IF]
        KR[KeywordRepository IF ← keyword-settings]
        SCR[SummaryConfigRepository IF ← keyword-settings]
    end

    subgraph Infrastructure
        SJPA[SummaryJpaRepository]
        GVA[GeminiVertexAIAdapter]
        APR[AIProviderRouter]
        JAF[JwtAuthenticationFilter ← infra]
    end

    subgraph ExternalServices
        PG[(PostgreSQL)]
        VertexAI[Google Vertex AI / Gemini]
    end

    SC --> GSUC
    SC --> SS
    GSUC --> KR
    GSUC --> SCR
    GSUC --> APR
    GSUC --> SR
    SS --> SR
    APR -.implements via.-> APP
    APR --> GVA
    GVA --> VertexAI
    SJPA -.implements.-> SR
    SJPA --> PG
    JAF --> SC
```

**依存方向**: Presentation → Application → Domain ← Infrastructure（依存性逆転を維持）

`AIProviderPort`はドメイン層に定義し、`GeminiVertexAIAdapter`はインフラ層に実装する。`AIProviderRouter`はインフラ層でアダプターを保持し、`AIProviderPort`インターフェースを通じてアプリケーション層から呼ばれる。

### テクノロジースタック

| レイヤー | 選択 / バージョン | 役割 | 備考 |
|---------|------------------|------|------|
| AIプロバイダー | com.google.cloud:google-cloud-vertexai | Gemini API呼び出し・Google Search Grounding | 早期アクセスSDK不可 |
| バックエンド | Kotlin + Spring Boot 3.x | REST API・ユースケース・アダプター | Spring Security 6.x (infra継承) |
| ORM | Spring Data JPA / Hibernate | Summary永続化 | blocking JPA のみ（R2DBC禁止）|
| データストア | PostgreSQL 15 | Summary保存 | ddl-auto=update |
| コンテナ | Docker Compose v2 | 開発環境 | GCP ADC はホストマシンの認証情報をマウント |

---

## ファイル構造計画

### ディレクトリ構造

```
backend/src/main/kotlin/com/newssummary/
├── domain/
│   └── summary/
│       ├── Summary.kt                        # Summary JPA エンティティ兼ドメインモデル
│       ├── SummaryRepository.kt              # ドメイン層リポジトリIF
│       └── AIProviderPort.kt                 # AI呼び出し出力ポートIF（ドメイン層）
├── application/
│   └── summary/
│       ├── GenerateSummaryUseCase.kt         # キーワード取得→AI呼出→Summary永続化オーケストレーター
│       ├── SummaryService.kt                 # Summary一覧取得ユースケース
│       └── dto/
│           ├── SummaryResult.kt              # GenerateSummaryUseCaseの戻り値（id, content, generatedAt）
│           └── SummaryResponse.kt            # REST APIレスポンスDTO
├── infrastructure/
│   ├── persistence/
│   │   └── SummaryJpaRepository.kt           # SummaryRepository の JPA 実装
│   └── ai/
│       ├── GeminiVertexAIAdapter.kt           # AIProviderPort の Gemini Vertex AI 実装
│       └── AIProviderRouter.kt                # aiProviderName でアダプターをルーティング
└── presentation/
    └── SummaryController.kt                   # /api/summaries エンドポイント

backend/src/main/resources/
└── application.yml                            # gcp.project-id, gcp.location, ai.gemini.model-name を追記
```

### 修正対象ファイル

- `backend/src/main/kotlin/com/newssummary/presentation/GlobalExceptionHandler.kt` — `AIProviderException`（→502）・`UnsupportedAIProviderException`（→400）・`NoActiveKeywordsException`（→422）の例外ハンドラを追加
- `backend/src/main/resources/application.yml` — `gcp.project-id`・`gcp.location`・`ai.gemini.model-name`設定を追加

---

## システムフロー

### ニュース要約生成フロー（GenerateSummaryUseCase）

```mermaid
sequenceDiagram
    participant Caller as Caller (scheduler / SummaryController)
    participant GSUC as GenerateSummaryUseCase
    participant KR as KeywordRepository
    participant SCR as SummaryConfigRepository
    participant APR as AIProviderRouter
    participant GVA as GeminiVertexAIAdapter
    participant VertexAI as Google Vertex AI
    participant SR as SummaryRepository

    Caller->>GSUC: execute(userId)
    GSUC->>KR: findAllByUserId(userId)
    KR-->>GSUC: List<Keyword> (isActive=true のみフィルタリング)
    alt アクティブキーワードなし
        GSUC-->>Caller: NoActiveKeywordsException
    end
    GSUC->>SCR: findByUserId(userId)
    SCR-->>GSUC: SummaryConfig or null（nullの場合はデフォルト値使用）
    GSUC->>APR: generateSummary(keywords, config)
    APR->>APR: aiProviderName で適切なアダプターを選択
    APR->>GVA: generateSummary(keywords, config)
    GVA->>GVA: プロンプト構築（キーワードごとのニュース調査・要約指示）
    GVA->>VertexAI: generateContent(prompt, GoogleSearchGrounding有効)
    VertexAI-->>GVA: GenerateContentResponse
    alt API失敗またはレスポンスが空
        GVA-->>APR: AIProviderException
        APR-->>GSUC: AIProviderException
        GSUC-->>Caller: AIProviderException
    end
    GVA-->>APR: AIProviderResult(summaryText)
    APR-->>GSUC: AIProviderResult(summaryText)
    GSUC->>SR: save(Summary(userId, content, keywords, generatedAt=now()))
    SR-->>GSUC: Summary（id採番済み）
    GSUC-->>Caller: SummaryResult(id, content, generatedAt)
```

### 要約一覧取得フロー

```mermaid
sequenceDiagram
    participant Client
    participant SummaryController
    participant SummaryService
    participant SummaryRepository

    Client->>SummaryController: GET /api/summaries
    Note over SummaryController: JwtAuthFilter で userId 取得済み
    SummaryController->>SummaryService: getSummaries(userId)
    SummaryService->>SummaryRepository: findAllByUserIdOrderByGeneratedAtDesc(userId)
    SummaryRepository-->>SummaryService: List<Summary>
    SummaryService-->>SummaryController: List<SummaryResponse>
    SummaryController-->>Client: 200 OK [ { id, content, keywords, generatedAt }, ... ]
```

---

## 要件トレーサビリティ

| 要件 | 概要 | コンポーネント | インターフェース | フロー |
|------|------|--------------|----------------|--------|
| 1.1–1.5 | AIProviderPort定義・ルーティング | AIProviderPort, AIProviderRouter | — | 要約生成フロー |
| 2.1–2.7 | GeminiVertexAIAdapter実装 | GeminiVertexAIAdapter | AIProviderPort | 要約生成フロー |
| 3.1–3.7 | GenerateSummaryUseCase | GenerateSummaryUseCase, AIProviderRouter | — | 要約生成フロー |
| 4.1–4.5 | Summaryエンティティ・永続化 | Summary, SummaryRepository, SummaryJpaRepository | — | — |
| 5.1–5.7 | SummaryController REST API | SummaryController, SummaryService | GET/POST /api/summaries | 一覧取得フロー |
| 6.1–6.4 | エラーハンドリング・設定 | GlobalExceptionHandler, GeminiVertexAIAdapter, application.yml | — | — |

---

## コンポーネントとインターフェース

### コンポーネントサマリー

| コンポーネント | 層 | 意図 | 要件カバレッジ | 主要依存 |
|---|---|---|---|---|
| Summary | ドメイン | Summaryエンティティ | 4.1–4.5 | — |
| SummaryRepository | ドメイン | リポジトリIF | 4.1–4.5 | — |
| AIProviderPort | ドメイン | AI呼び出し出力ポートIF | 1.1–1.5 | — |
| GenerateSummaryUseCase | アプリケーション | 要約生成オーケストレーター | 3.1–3.7 | KeywordRepository, SummaryConfigRepository, AIProviderRouter, SummaryRepository |
| SummaryService | アプリケーション | Summary一覧取得ユースケース | 5.1–5.4 | SummaryRepository |
| SummaryJpaRepository | インフラ/永続化 | SummaryRepository JPA実装 | 4.3–4.5 | Spring Data JPA |
| GeminiVertexAIAdapter | インフラ/AI | Gemini Vertex AI実装 | 2.1–2.7 | google-cloud-vertexai |
| AIProviderRouter | インフラ/AI | プロバイダールーティング | 1.2–1.4 | GeminiVertexAIAdapter |
| SummaryController | プレゼンテーション | /api/summaries エンドポイント | 5.1–5.7 | GenerateSummaryUseCase, SummaryService |

---

### ドメイン層

#### Summary エンティティ

| フィールド | 詳細 |
|---|---|
| 意図 | ニュース要約を表すJPAエンティティ兼ドメインモデル。`notification-delivery`スペックが読み取る集約ルート |
| 要件 | 4.1, 4.2, 4.3 |

**責務と制約**
- 属性: `id: Long`（自動採番）、`userId: Long`（User.idへの論理参照）、`content: String`（要約テキスト、TEXT型）、`keywords: String`（使用キーワードのカンマ区切りリスト）、`generatedAt: Instant`
- kotlin-jpaプラグインによりno-argコンストラクタを自動生成（`@Entity`アノテーション）
- `content`は`@Column(columnDefinition = "TEXT")`でTEXT型にマッピング

##### ドメインモデル

```kotlin
@Entity
@Table(name = "summaries")
class Summary(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,
    @Column(nullable = false)
    val keywords: String,
    @Column(name = "generated_at", nullable = false)
    val generatedAt: Instant = Instant.now()
)
```

#### SummaryRepository インターフェース

| フィールド | 詳細 |
|---|---|
| 意図 | ドメイン層のリポジトリインターフェース（インフラ実装への依存逆転） |
| 要件 | 4.2, 4.5 |

```kotlin
interface SummaryRepository {
    fun save(summary: Summary): Summary
    fun findById(id: Long): Summary?                                        // notification-delivery が使用
    fun findAllByUserIdOrderByGeneratedAtDesc(userId: Long): List<Summary>
}
```

#### AIProviderPort インターフェース

| フィールド | 詳細 |
|---|---|
| 意図 | AIプロバイダー呼び出しの出力ポートインターフェース（ドメイン層に配置し依存逆転を実現） |
| 要件 | 1.1, 1.5 |

**コントラクト**: Service [x]

##### サービスインターフェース

```kotlin
interface AIProviderPort {
    fun generateSummary(keywords: List<String>, config: SummaryConfig): AIProviderResult
}

data class AIProviderResult(val summaryText: String)
```

- 事前条件: `keywords`が非空リスト
- 事後条件: 要約テキストを含む`AIProviderResult`を返す
- 例外: AI呼び出し失敗時は`AIProviderException`をスロー

---

### アプリケーション層

#### GenerateSummaryUseCase

| フィールド | 詳細 |
|---|---|
| 意図 | ユーザーのアクティブキーワードを取得してAIプロバイダーを呼び出し、Summary を永続化するオーケストレーターユースケース |
| 要件 | 3.1–3.7 |

**依存**
- Inbound: SummaryController (`POST /api/summaries/generate`)、scheduler スペック（直接呼び出し）（P0）
- Outbound: KeywordRepository — アクティブキーワード取得（P0）
- Outbound: SummaryConfigRepository — ユーザー設定取得（P0）
- Outbound: AIProviderRouter（`AIProviderPort`経由）— AI要約生成（P0）
- Outbound: SummaryRepository — Summary保存（P0）

**コントラクト**: Service [x]

##### サービスインターフェース

```kotlin
@Service
class GenerateSummaryUseCase(
    private val keywordRepository: KeywordRepository,
    private val summaryConfigRepository: SummaryConfigRepository,
    private val aiProviderPort: AIProviderPort,   // AIProviderRouter がインジェクト
    private val summaryRepository: SummaryRepository
) {
    fun execute(userId: Long): SummaryResult
}

data class SummaryResult(
    val id: Long,
    val content: String,
    val generatedAt: Instant
)
```

- 事前条件: ユーザーにアクティブなキーワードが1件以上存在すること
- 事後条件: `Summary`がDBに保存され、`SummaryResult`が返される
- 例外: アクティブキーワードなし → `NoActiveKeywordsException`、AI呼び出し失敗 → `AIProviderException`を伝播

**実装ノート**
- `KeywordRepository.findAllByUserId(userId)` で取得後、`isActive == true` のものだけをフィルタリングしてAIに渡す
- `SummaryConfig` が存在しない場合（`findByUserId` が null を返す）、デフォルト値を持つ`SummaryConfig`インスタンスを生成して使用する
- `keywords`フィールドへの保存形式はカンマ区切り文字列（例: `"S&P,日経225,戦争"`）

#### SummaryService

| フィールド | 詳細 |
|---|---|
| 意図 | Summary一覧取得ユースケースを実装するアプリケーションサービス |
| 要件 | 5.1–5.4 |

**コントラクト**: Service [x]

##### サービスインターフェース

```kotlin
interface SummaryServiceInterface {
    fun getSummaries(userId: Long): List<SummaryResponse>
}

data class SummaryResponse(
    val id: Long,
    val content: String,
    val keywords: String,
    val generatedAt: Instant
)
```

---

### インフラ/AI層

#### GeminiVertexAIAdapter

| フィールド | 詳細 |
|---|---|
| 意図 | `AIProviderPort`のGoogle Vertex AI（Gemini）実装。Google Search Groundingを有効化してリアルタイムニュース要約を生成する |
| 要件 | 2.1–2.7 |

**依存**
- External: `com.google.cloud:google-cloud-vertexai` — Vertex AI API呼び出し（P0）
- External: Google Vertex AI Gemini（us-central1推奨リージョン）（P0）

**コントラクト**: Service [x]

##### サービスインターフェース

```kotlin
@Component
class GeminiVertexAIAdapter(
    @Value("\${gcp.project-id}") private val projectId: String,
    @Value("\${gcp.location}") private val location: String,
    @Value("\${ai.gemini.model-name:gemini-1.5-pro}") private val modelName: String
) : AIProviderPort {
    override fun generateSummary(keywords: List<String>, config: SummaryConfig): AIProviderResult
}
```

**実装ノート**
- `VertexAI` クライアントは`VertexAI(projectId, location)`で初期化。ADCは自動的に使用される
- `GenerativeModel(modelName, vertexAI)` でモデルを取得
- Google Search Groundingは`Tool.newBuilder().setGoogleSearchRetrieval(GoogleSearchRetrieval.newBuilder().build()).build()`で有効化し、`GenerativeModel`に設定する
- プロンプト構築例:
  ```
  以下のキーワードに関する最新ニュースを調査し、各キーワードについて要約してください。
  キーワード: {keyword1}, {keyword2}, ...
  過去{lookbackDays}日分のニュースを対象にしてください。
  ```
- レスポンスから`response.candidates[0].content.parts[0].text`でテキストを抽出
- candidatesが空またはtextが空の場合は`AIProviderException("Empty response from Gemini")`をスロー
- ネットワークエラー・APIエラーは`catch`して`AIProviderException`にラップしてスロー

#### AIProviderRouter

| フィールド | 詳細 |
|---|---|
| 意図 | `SummaryConfig.aiProviderName`の値に基づいて適切な`AIProviderPort`実装アダプターにルーティングする |
| 要件 | 1.2–1.4 |

**コントラクト**: Service [x]

##### サービスインターフェース

```kotlin
@Component
class AIProviderRouter(
    private val geminiAdapter: GeminiVertexAIAdapter
) : AIProviderPort {
    override fun generateSummary(keywords: List<String>, config: SummaryConfig): AIProviderResult {
        return when (config.aiProviderName) {
            "gemini" -> geminiAdapter.generateSummary(keywords, config)
            else -> throw UnsupportedAIProviderException(config.aiProviderName)
        }
    }
}
```

**実装ノート**
- `GenerateSummaryUseCase`には`AIProviderPort`型でDI（SpringがAIProviderRouterをインジェクト）
- 将来プロバイダーを追加する場合、`when`節にケースを追加し対応アダプターをDIに追加するだけで拡張可能

---

### インフラ/永続化層

#### SummaryJpaRepository

| フィールド | 詳細 |
|---|---|
| 意図 | `SummaryRepository`インターフェースのJPA実装。Spring Data JPAの`JpaRepository`を内部で使用 |
| 要件 | 4.3–4.5 |

**実装ノート**
- `SummaryRepository`（ドメイン）インターフェースを実装するSpring Component
- 内部に`SpringDataSummaryJpaRepository`（`JpaRepository<Summary, Long>`）を委譲パターンで保持
- `findAllByUserIdOrderByGeneratedAtDesc`はSpring Dataのメソッド命名規約で自動生成

---

### プレゼンテーション層

#### SummaryController

| フィールド | 詳細 |
|---|---|
| 意図 | 要約一覧取得・要約生成のREST エンドポイントを提供する |
| 要件 | 5.1–5.7, 6.1 |

**コントラクト**: API [x]

##### API コントラクト

| メソッド | エンドポイント | リクエスト | レスポンス | エラー |
|---------|-------------|-----------|-----------|-------|
| GET | /api/summaries | — | `List<SummaryResponse>` | 401, 500 |
| POST | /api/summaries/generate | — | `SummaryResponse` | 400, 401, 422, 502, 500 |

**DTO 定義**

```kotlin
data class SummaryResponse(
    val id: Long,
    val content: String,
    val keywords: String,
    val generatedAt: Instant
)
```

**実装ノート**
- `@AuthenticationPrincipal`またはSecurityContextから`userId`を取得し、サービスに渡す
- `POST /api/summaries/generate`は`GenerateSummaryUseCase.execute(userId)`を呼び出し、`SummaryResult`を`SummaryResponse`に変換して返す
- 全エンドポイントはJWT認証必須（`SecurityConfig`の`permitAll`リストから除外）

#### GlobalExceptionHandler（修正）

既存の`GlobalExceptionHandler`に以下の例外ハンドリングを追加する：

```kotlin
@ExceptionHandler(AIProviderException::class)          // → 502 Bad Gateway
@ExceptionHandler(UnsupportedAIProviderException::class) // → 400 Bad Request
@ExceptionHandler(NoActiveKeywordsException::class)    // → 422 Unprocessable Entity
```

---

## データモデル

### ドメインモデル

- **Summary集約**: `Summary`（userId, content, keywords, generatedAt）。集約ルート = Summary。不変条件: 一度保存されたSummaryは変更されない（追記のみ）。
- `notification-delivery`スペックはこの集約を読み取り専用で参照する。

### 物理データモデル

```sql
CREATE TABLE summaries (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    content        TEXT NOT NULL,
    keywords       VARCHAR(1000) NOT NULL,
    generated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_summaries_user_id_generated_at ON summaries(user_id, generated_at DESC);
```

※ `ddl-auto=update` によりJPAが自動生成する。

### application.yml への追記

```yaml
gcp:
  project-id: ${GCP_PROJECT_ID}
  location: ${GCP_LOCATION:us-central1}

ai:
  gemini:
    model-name: ${GEMINI_MODEL_NAME:gemini-1.5-pro}
```

---

## エラーハンドリング

### エラー戦略

全例外は既存の`GlobalExceptionHandler`（infrastructure スペック）で捕捉し、統一`ErrorResponse`形式で返す。本スペック固有の例外クラスを追加し、それぞれ適切なHTTPステータスにマッピングする。AIプロバイダーエラーは詳細をログに記録した上で502を返す。

### カスタム例外クラス

```kotlin
class AIProviderException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class UnsupportedAIProviderException(providerName: String) : RuntimeException("Unsupported AI provider: $providerName")
class NoActiveKeywordsException(userId: Long) : RuntimeException("No active keywords found for user: $userId")
```

### エラーカテゴリとレスポンス

- **ユーザーエラー (4xx)**:
  - 認証未済 → 401（JwtAuthenticationFilter、infraスペックが処理）
  - 不明なプロバイダー名 → 400（`UnsupportedAIProviderException`）
  - アクティブキーワードなし → 422（`NoActiveKeywordsException`）
- **外部サービスエラー (5xx)**:
  - Vertex AI API失敗 → 502（`AIProviderException`）— 詳細はERRORログに記録
  - DB接続失敗 → 500（infraの`GlobalExceptionHandler`が処理）

### モニタリング

- `AIProviderException`発生時にERRORレベルでログ出力（プロバイダー名・エラーメッセージ・スタックトレース）
- 正常な要約生成完了時にINFOレベルでログ出力（userId・キーワード数・generatedAt）

---

## セキュリティ考慮事項

- GCPプロジェクトID・リージョンは環境変数から注入（コードにハードコードしない）
- Application Default Credentials（ADC）を使用し、サービスアカウントキーファイルのコードへの埋め込みを禁止
- 全`/api/summaries`エンドポイントは`SecurityConfig`の`permitAll`リストから除外し、JWT認証を必須とする
- `userId`はJWTクレームから取得し、リクエストボディやパスパラメーターからの取得を禁止（改ざん防止）
- `content`（要約テキスト）はTEXT型で保存するが、APIレスポンスへの出力時はエスケープ処理をSpring MVCに委ねる

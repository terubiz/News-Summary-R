# 要件定義書

## はじめに

本スペックは、ニュース要約配信Webアプリケーション（News-Summary-R）の**共通インフラ基盤**を構築するための要件を定義する。対象ユーザーは、アプリケーションの開発者および将来的なエンドユーザーであり、現状はコードベースが空の状態から、`docker compose up` 一発で全サービスが起動し、JWTを用いた認証付きAPIとReact画面が疎通できる状態を目指す。

本スペックは後続5スペック（keyword-settings / ai-summary-engine / scheduler / notification-delivery）すべての土台となり、ここで確立したDDDパッケージ構造・認証基盤・Redisトークン管理が全スペックで共有される。

## 境界コンテキスト（Boundary Context）

- **スコープ内**: Docker Compose環境（PostgreSQL・Redis含む）、Spring Boot + Kotlinスキャフォールド（DDD 4層パッケージ）、React + Tailwind CSSスキャフォールド、JWT認証（ログイン / トークン検証 / ログアウト）、Redisによるトークン管理（TTL設定・ブラックリスト方式）、Userエンティティ + UserRepository、Kotlinコンパイラプラグイン設定（kotlin-jpa, kotlin-spring）、グローバル例外ハンドラ、**JUnit 5によるユニットテスト・統合テスト**
- **スコープ外**: DBマイグレーションツール（Flyway等）、CI/CD設定、本番環境設定、ビジネスロジック、画面コンポーネント（ルーティング設定のみ）
- **隣接する期待事項**: 後続スペックはこの基盤が提供するUserエンティティ・JWT認証フィルター・DDDパッケージ構造を前提として実装を行う

---

## 要件

### 要件 1: Docker Compose環境構築

**目的**: 開発者として、単一コマンドで全サービスを起動できる開発環境が欲しい。そうすることで、環境構築に時間をかけずにアプリケーション開発に集中できる。

#### 受け入れ基準

1. When `docker compose up` コマンドを実行したとき、the システム shall PostgreSQL・Redis・Spring Boot・Reactの全サービスを起動する
2. When 全サービスが起動したとき、the システム shall Spring Bootが8080番ポート、Reactが3000番ポートで外部からアクセス可能な状態にする
3. When PostgreSQLコンテナが起動したとき、the システム shall 指定したデータベース名・ユーザー・パスワードで接続可能なPostgreSQLインスタンスを提供する
4. When Redisコンテナが起動したとき、the システム shall 6379番ポートでRedisへの接続を受け付ける
5. The システム shall docker-composeファイルにヘルスチェック設定を含み、依存サービスの起動順序を保証する
6. When Spring Bootアプリが起動したとき、the システム shall 環境変数からDB接続情報・Redis接続情報・JWT秘密鍵を読み込む

### 要件 2: Spring Boot + Kotlin スキャフォールド

**目的**: 開発者として、DDD + クリーンアーキテクチャ準拠の4層パッケージ構造が初期状態で用意されていて欲しい。そうすることで、後続スペックの開発時に一貫したアーキテクチャを維持できる。

#### 受け入れ基準

1. The システム shall `domain`・`application`・`infrastructure`・`presentation` の4層パッケージ構造を持つ
2. The システム shall `build.gradle.kts` に `kotlin-jpa` プラグインを設定し、JPAエンティティクラスへの引数なしコンストラクタ自動生成を有効化する
3. The システム shall `build.gradle.kts` に `kotlin-spring`（allopen）プラグインを設定し、Spring管理クラスへのallopen適用を有効化する
4. When Spring Bootアプリが起動したとき、the システム shall `application.yml` に設定された `spring.jpa.hibernate.ddl-auto=update` でJPAスキーマを自動生成・更新する
5. The システム shall Spring Boot 3.x と Kotlin が正常にビルドできる最小限の `build.gradle.kts` を提供する
6. When アプリケーションが起動したとき、the システム shall アクチュエーターの `/actuator/health` エンドポイントが200レスポンスを返す

### 要件 3: React + Tailwind CSS スキャフォールド

**目的**: 開発者として、React + Tailwind CSSが設定済みのフロントエンドスキャフォールドが欲しい。そうすることで、後続スペックでUI実装を即座に開始できる。

#### 受け入れ基準

1. The システム shall React + Vite（またはCreate React App）で初期化されたフロントエンドプロジェクトを提供する
2. The システム shall Tailwind CSSが設定・動作する状態のフロントエンド環境を提供する
3. The システム shall React Routerによるクライアントサイドルーティング設定（ログインページ・ダッシュボードページの基本ルート）を含む
4. When フロントエンドが起動したとき、the システム shall Reactのトップページが3000番ポートで表示される
5. The システム shall バックエンドAPIへのリクエスト時にJWTトークンをAuthorizationヘッダーに付与するためのHTTPクライアント設定（Axiosまたはfetch）を提供する

### 要件 4: JWT認証 - ログイン

**目的**: ユーザーとして、メールアドレスとパスワードでログインし、JWTアクセストークンを取得したい。そうすることで、認証が必要なAPIエンドポイントを利用できる。

#### 受け入れ基準

1. When 正しいメールアドレスとパスワードでPOST `/api/auth/login` を呼び出したとき、the 認証サービス shall JWTアクセストークンをレスポンスボディに含めて返す
2. When ログインに成功したとき、the 認証サービス shall 発行したJWTトークンのJTI（JWT ID）とTTLをRedisに保存する
3. When 存在しないメールアドレスまたは誤ったパスワードでログインを試みたとき、the 認証サービス shall 401 Unauthorizedを返し、エラー詳細は開示しない
4. The 認証サービス shall JWTトークンにユーザーIDおよびメールアドレスをクレームとして含める
5. The 認証サービス shall JWTの有効期限（exp）をアプリケーション設定から読み込み、デフォルト値を24時間とする
6. When ログインリクエストのボディが不正（メールアドレス形式不正・パスワード未指定等）のとき、the 認証サービス shall 400 Bad Requestとバリデーションエラー詳細を返す

### 要件 5: JWT認証 - トークン検証

**目的**: ユーザーとして、有効なJWTトークンをリクエストに含めることで、認証が必要なAPIエンドポイントにアクセスしたい。

#### 受け入れ基準

1. While 有効なJWTトークンがAuthorizationヘッダーに含まれているとき、the セキュリティフィルター shall リクエストを認証済みとして処理する
2. If JWTトークンが存在しない、または署名が無効な場合、the セキュリティフィルター shall 401 Unauthorizedを返す
3. If JWTトークンの有効期限が切れている場合、the セキュリティフィルター shall 401 Unauthorizedを返す
4. If JWTトークンのJTIがRedisブラックリストに登録されている場合、the セキュリティフィルター shall 401 Unauthorizedを返す（ログアウト済みトークンの無効化）
5. The セキュリティフィルター shall `/api/auth/login` および `/api/auth/register` エンドポイントへのリクエストは認証不要とする
6. When JWTトークンが検証に通過したとき、the セキュリティフィルター shall トークン内のユーザー情報をSpring SecurityのSecurityContextに設定する

### 要件 6: JWT認証 - ログアウト

**目的**: ユーザーとして、ログアウトを実行することで現在のJWTトークンを無効化したい。そうすることで、セキュリティリスクを低減できる。

#### 受け入れ基準

1. When 認証済みユーザーがPOST `/api/auth/logout` を呼び出したとき、the 認証サービス shall 現在のJWTのJTIをRedisブラックリストに登録する
2. When ログアウト処理を実行したとき、the 認証サービス shall RedisブラックリストのTTLをトークンの残存有効期限と同期させる
3. When ログアウト後に同じJWTトークンで認証が必要なAPIを呼び出したとき、the セキュリティフィルター shall 401 Unauthorizedを返す
4. When ログアウトに成功したとき、the 認証サービス shall 200 OKを返す

### 要件 7: ユーザー管理 - ユーザー登録

**目的**: 新規ユーザーとして、メールアドレスとパスワードを使ってアカウントを作成したい。

#### 受け入れ基準

1. When POST `/api/auth/register` を新規メールアドレスで呼び出したとき、the ユーザー管理サービス shall パスワードをハッシュ化してUserエンティティをPostgreSQLに保存する
2. When ユーザー登録が成功したとき、the ユーザー管理サービス shall 201 Createdと作成されたユーザー情報（パスワードを除く）を返す
3. If すでに登録済みのメールアドレスで登録を試みたとき、the ユーザー管理サービス shall 409 Conflictを返す
4. The ユーザー管理サービス shall パスワードをBCryptでハッシュ化してデータベースに保存する
5. When 登録リクエストのボディが不正（メールアドレス形式不正・パスワード短すぎ等）のとき、the ユーザー管理サービス shall 400 Bad Requestとバリデーションエラー詳細を返す

### 要件 8: グローバル例外ハンドラ

**目的**: 開発者として、アプリケーション全体で一貫したエラーレスポンス形式を持ちたい。そうすることで、フロントエンドが統一された方法でエラーを処理できる。

#### 受け入れ基準

1. The システム shall 未処理の例外が発生したとき、統一されたJSONエラーレスポンス形式（`timestamp`・`status`・`error`・`message`）でレスポンスを返す
2. When バリデーションエラーが発生したとき、the システム shall フィールドごとのバリデーションエラーメッセージを含む400レスポンスを返す
3. When 認証エラーが発生したとき、the システム shall 401レスポンスと一貫したエラーボディを返す
4. When 存在しないリソースへアクセスしたとき、the システム shall 404レスポンスと一貫したエラーボディを返す
5. The システム shall 500系エラーの際に内部エラー詳細（スタックトレース等）をクライアントに漏洩しない

### 要件 9: JUnit 5 によるテストコード

**目的**: 開発者として、各コンポーネントの動作を JUnit 5 のテストコードで検証できる状態にしたい。そうすることで、後続スペックの実装時にリグレッションを早期に検出できる。

#### 受け入れ基準

1. The システム shall `build.gradle.kts` に `spring-boot-starter-test`（JUnit 5 を含む）と TestContainers 依存ライブラリを設定する
2. The システム shall JUnit 5（`@ExtendWith(MockitoExtension::class)`）を使用した `JwtTokenProvider` のユニットテストを提供する（トークン生成・検証・期限切れ・クレーム抽出・JTI抽出を各々独立したテストメソッドで検証する）
3. The システム shall JUnit 5 と Mockito を使用した `AuthService` のユニットテストを提供する（ログイン成功・存在しないメールアドレス・パスワード不一致・重複登録・ログアウトを各々独立したテストメソッドで検証する）
4. The システム shall JUnit 5 と `@WebMvcTest` + MockMvc を使用した `AuthController` のユニットテストを提供する（`AuthService` をモック化し、各エンドポイントのHTTPステータス・レスポンスボディ・バリデーション挙動を独立したテストメソッドで検証する）
5. The システム shall JUnit 5 と TestContainers（PostgreSQL・Redis）を使用した `AuthController` の統合テストを提供する（ログイン → JWT取得 → 保護エンドポイントアクセス → ログアウト → 再アクセス拒否の一連フローを検証する）
6. When `./gradlew test` を実行したとき、the システム shall 全テストが通過する
7. The システム shall テストコードを `src/test/kotlin` ディレクトリ以下に、本番コードと同一のパッケージ構造で配置する

# 実装計画

## タスク一覧

- [ ] 1. Docker Compose 環境構築
- [ ] 1.1 全サービス定義と依存関係の設定
  - PostgreSQL サービス（ポート5432、データボリューム永続化、`pg_isready` ヘルスチェック）を定義する
  - Redis サービス（ポート6379、`redis-cli ping` ヘルスチェック）を定義する
  - Spring Boot サービス（ポート8080）を定義し、`depends_on: {postgres: condition: service_healthy, redis: condition: service_healthy}` で起動順序を保証する
  - React サービス（ポート3000、開発モード）を定義する
  - `.env.example` に必要な環境変数（DB接続情報・Redis接続情報・JWT秘密鍵）を記載する
  - `docker compose up` を実行すると全4サービスが起動し、Spring Boot が8080、React が3000 でアクセス可能になる
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

- [ ] 2. Spring Boot + Kotlin スキャフォールド
- [ ] 2.1 ビルド設定と Kotlin プラグイン設定
  - `build.gradle.kts` に Spring Boot 3.x + Kotlin の最小依存ライブラリを設定する（`spring-boot-starter-web`、`spring-boot-starter-data-jpa`、`spring-boot-starter-data-redis`、`spring-boot-starter-security`、`jjwt-api`/`jjwt-impl`/`jjwt-jackson`、`postgresql`）
  - `kotlin-jpa` プラグインを設定し、`@Entity` クラスへの引数なしコンストラクタ自動生成を有効化する
  - `kotlin-spring`（allopen）プラグインを設定し、Spring 管理クラスへの allopen 適用を有効化する
  - `settings.gradle.kts` にプロジェクト名を設定する
  - `./gradlew build` が成功し、コンパイルエラーなしでビルドが通る
  - _Requirements: 2.2, 2.3, 2.5_

- [ ] 2.2 DDD 4層パッケージ構造と起動設定
  - `com.newssummary` 配下に `domain`・`application`・`infrastructure`・`presentation` の4層パッケージ構造を作成する
  - `NewsSummaryApplication.kt` を `src/main/kotlin/com/newssummary/` に配置する
  - `application.yml` に DB 接続情報・Redis 接続情報・JWT 設定・`spring.jpa.hibernate.ddl-auto=update`・Spring Actuator 設定を記述する（環境変数から読み込む形式）
  - `backend/Dockerfile` を作成し、Spring Boot アプリケーションをコンテナ化する
  - アプリケーション起動後、`/actuator/health` が200を返す
  - _Requirements: 2.1, 2.4, 2.6_
  - _Depends: 2.1_

- [ ] 3. (P) React + Tailwind CSS スキャフォールド
- [ ] 3.1 フロントエンドプロジェクト初期化と設定
  - React 18 + Vite でフロントエンドプロジェクトを初期化する
  - Tailwind CSS 3.x を設定し、`tailwind.config.ts` と CSS ファイルを用意する
  - `frontend/Dockerfile` を作成し、開発サーバーをコンテナで起動できるようにする
  - フロントエンドコンテナが起動し、3000番ポートで React トップページが表示される
  - _Requirements: 3.1, 3.2, 3.4_
  - _Boundary: React スキャフォールド_

- [ ] 3.2 (P) ルーティングと HTTP クライアント設定
  - React Router v6 を使用し、`App.tsx` にログインページ（`/login`）とダッシュボードページ（`/dashboard`）の基本ルートを定義する
  - `LoginPage.tsx` と `DashboardPage.tsx` のスタブコンポーネントを作成する
  - `src/api/axiosClient.ts` を作成し、リクエストインターセプターで `localStorage` からJWTトークンを読み出して `Authorization: Bearer <token>` ヘッダーに付与する設定を行う
  - フロントエンド起動時にルーティングが機能し、`/login` と `/dashboard` に遷移できる
  - _Requirements: 3.3, 3.5_
  - _Boundary: React スキャフォールド_

- [ ] 4. ドメイン層: User エンティティと UserRepository インターフェース
- [ ] 4.1 User エンティティ定義
  - `domain/user/User.kt` に `@Entity @Table(name = "users")` アノテーションを付与した User クラスを定義する（属性: `id: Long`（自動採番）、`email: String`（UNIQUE NOT NULL）、`passwordHash: String`、`createdAt: Instant`）
  - kotlin-jpa プラグインにより no-arg コンストラクタが自動生成される
  - JPA のスキーマ自動生成で `users` テーブルが作成される
  - _Requirements: 7.1, 7.4_
  - _Depends: 2.1, 2.2_

- [ ] 4.2 UserRepository インターフェース定義
  - `domain/user/UserRepository.kt` に `save(user: User): User`、`findById(id: Long): User?`、`findByEmail(email: String): User?`、`existsByEmail(email: String): Boolean` メソッドを持つインターフェースを定義する
  - インターフェースはドメイン層に配置し、インフラ実装への依存を持たない
  - `UserRepository` インターフェースが定義され、`AuthService` から参照可能になる
  - _Requirements: 7.1, 7.3_

- [ ] 5. インフラ/永続化層: UserJpaRepository
- [ ] 5.1 JPA リポジトリ実装
  - `infrastructure/persistence/` に `SpringDataUserJpaRepository`（`JpaRepository<User, Long>` を extends する Spring Data インターフェース）を定義する
  - `UserJpaRepository` を Spring Component として実装し、`UserRepository` インターフェースを実装する（委譲パターン）
  - `UserJpaRepository` が `UserRepository` を実装し、DI コンテナに登録される
  - _Requirements: 7.1, 7.3_
  - _Depends: 4.1, 4.2_

- [ ] 6. (P) インフラ/Redis 層: TokenRedisRepository
- [ ] 6.1 Redis トークン管理実装
  - `infrastructure/redis/TokenRedisRepository.kt` を作成し、`StringRedisTemplate` を使用する
  - `saveToken(jti: String, ttlMs: Long)` で Redis キー `token:{jti}`（値: "valid"）を TTL 付きで保存する
  - `isBlacklisted(jti: String): Boolean` で Redis キー `blacklist:{jti}` の存在確認を行う
  - `addToBlacklist(jti: String, remainingTtlMs: Long)` で Redis キー `blacklist:{jti}`（値: "revoked"）を残存 TTL 付きで保存する
  - `TokenRedisRepository` が DI コンテナに登録され、Redis へのトークン保存・ブラックリスト確認が動作する
  - _Requirements: 4.2, 5.4, 6.1, 6.2_
  - _Boundary: TokenRedisRepository_
  - _Depends: 2.2_

- [ ] 7. (P) インフラ/セキュリティ層: JwtTokenProvider
- [ ] 7.1 JWT 発行・検証ロジック実装
  - `infrastructure/security/JwtTokenProvider.kt` を作成し、`application.yml` から JWT 秘密鍵（`jwt.secret`）と有効期限ミリ秒（`jwt.expirationMs`、デフォルト 86400000）を注入する
  - `generateToken(userId: Long, email: String): TokenData` で UUID JTI・クレーム（`sub`: userId、`email`、`jti`、`iat`、`exp`）を含む HS256 署名済み JWT を発行し、`TokenData(token, jti, expiresAt)` を返す
  - `validateToken(token: String): Boolean` で署名検証・有効期限チェックを行い、不正・期限切れは `false` を返す
  - `getJtiFromToken(token: String): String` でトークンから JTI を抽出する
  - `getUserIdFromToken(token: String): Long` でトークンからユーザーID を抽出する
  - `getRemainingTtl(token: String): Long` でトークンの残存 TTL（ミリ秒）を返す
  - `generateToken` を呼び出すと有効な JWT が生成され、`validateToken` が `true` を返す
  - _Requirements: 4.1, 4.4, 4.5, 5.1, 5.2, 5.3_
  - _Boundary: JwtTokenProvider_
  - _Depends: 2.1_

- [ ] 8. Spring Security 設定と JWT 認証フィルター
- [ ] 8.1 JwtAuthenticationFilter 実装
  - `infrastructure/security/JwtAuthenticationFilter.kt` を `OncePerRequestFilter` を継承して作成する
  - `Authorization: Bearer <token>` ヘッダーからトークンを抽出し、`JwtTokenProvider.validateToken` で署名・有効期限を検証する
  - 有効なトークンは `TokenRedisRepository.isBlacklisted` でブラックリスト確認を行い、ブラックリスト未登録の場合のみ Spring Security の `SecurityContext` に認証情報を設定する
  - トークン不正・期限切れ・ブラックリスト登録のいずれかで `response.sendError(401)` を呼び出しフィルターチェーンを中断する
  - 有効なJWTを含むリクエストが `SecurityContext` に認証情報を持ち、保護リソースにアクセスできる
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.6_
  - _Depends: 7.1, 6.1_

- [ ] 8.2 SecurityConfig 実装
  - `infrastructure/security/SecurityConfig.kt` に `@Configuration @EnableWebSecurity` を設定する
  - CSRF 無効化・セッション管理を `STATELESS` に設定する
  - `/api/auth/login`・`/api/auth/register` への リクエストを `permitAll` に設定する
  - `JwtAuthenticationFilter` を `UsernamePasswordAuthenticationFilter` の前に追加する
  - 認証不要パスへの未認証リクエストが通過し、保護パスへの未認証リクエストが401を返す
  - _Requirements: 5.5_
  - _Depends: 8.1_

- [ ] 9. アプリケーション層: AuthService
- [ ] 9.1 ログイン・登録ユースケース実装
  - `application/auth/dto/` に `LoginRequest`（`@field:NotBlank email`、`@field:NotBlank password`）、`LoginResponse`（`token`、`email`）、`RegisterRequest`（`@field:Email email`、`@field:Size(min=8) password`）、`UserResponse`（`id`、`email`、`createdAt`）を作成する
  - `application/auth/AuthService.kt` に `login(request: LoginRequest): LoginResponse` を実装する（`UserRepository.findByEmail` でユーザー取得 → BCrypt パスワード検証 → `JwtTokenProvider.generateToken` でトークン発行 → `TokenRedisRepository.saveToken` で Redis 保存）
  - `register(request: RegisterRequest): UserResponse` を実装する（`UserRepository.existsByEmail` で重複確認 → BCrypt ハッシュ化 → `UserRepository.save` で保存 → `UserResponse` を返す）
  - 正しい資格情報でログインすると JWT が返され、新規メールアドレスで登録するとユーザーが保存される
  - _Requirements: 4.1, 4.2, 4.4, 4.5, 4.6, 7.1, 7.2, 7.4, 7.5_
  - _Depends: 4.1, 4.2, 5.1, 6.1, 7.1_

- [ ] 9.2 認証失敗・ログアウトユースケース実装
  - `login` で存在しないメールアドレスまたはパスワード不一致の場合、エラー詳細を開示せずに `UnauthorizedException`（401）をスローする
  - `register` で既登録メールアドレスの場合、`EmailAlreadyExistsException`（409 用カスタム例外）をスローする
  - `logout(token: String)` を実装する（`JwtTokenProvider.getJtiFromToken` で JTI 取得 → `JwtTokenProvider.getRemainingTtl` で残存 TTL 取得 → `TokenRedisRepository.addToBlacklist` で登録）
  - 誤パスワードで `login` を呼ぶと例外がスローされ、ログアウト後に `TokenRedisRepository.isBlacklisted` が `true` を返す
  - _Requirements: 4.3, 6.1, 6.2, 7.3_

- [ ] 10. プレゼンテーション層: AuthController
- [ ] 10.1 認証エンドポイント実装
  - `presentation/AuthController.kt` に `@RestController @RequestMapping("/api/auth")` を設定する
  - `POST /api/auth/login`: `@RequestBody @Valid LoginRequest` を受け取り、`AuthService.login` を呼び出して `LoginResponse` を200で返す
  - `POST /api/auth/register`: `@RequestBody @Valid RegisterRequest` を受け取り、`AuthService.register` を呼び出して `UserResponse` を201で返す
  - `POST /api/auth/logout`: `Authorization` ヘッダーからトークンを抽出し、`AuthService.logout` を呼び出して200を返す
  - 各エンドポイントへのリクエストが正しい HTTP ステータスとレスポンスボディを返す
  - _Requirements: 4.1, 4.6, 6.1, 6.4, 7.1, 7.2, 7.5_
  - _Depends: 9.1, 9.2_

- [ ] 11. グローバル例外ハンドラ
- [ ] 11.1 統一エラーレスポンス実装
  - `presentation/GlobalExceptionHandler.kt` に `@RestControllerAdvice` を設定し、`ErrorResponse(timestamp, status, error, message)` 形式のレスポンスを返す
  - `MethodArgumentNotValidException` を捕捉し、フィールドごとのバリデーションエラーメッセージを含む400レスポンスを返す
  - `UnauthorizedException` / Spring Security 認証エラーを捕捉し、詳細を開示しない401レスポンスを返す
  - `EmailAlreadyExistsException` を捕捉し、409レスポンスを返す
  - `EntityNotFoundException` を捕捉し、404レスポンスを返す
  - その他の例外を捕捉し、スタックトレースをログに記録した上でクライアントには "Internal Server Error" のみを含む500レスポンスを返す
  - バリデーションエラーリクエストで400が返り、スタックトレースがレスポンスに含まれないことが確認できる
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

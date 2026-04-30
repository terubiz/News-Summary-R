# 実装計画: notification-delivery

## タスク一覧

---

- [ ] 1. ドメイン層: notification パッケージの作成
- [ ] 1.1 DeliveryChannelPort インターフェースを作成する
  - `backend/src/main/kotlin/com/newssummary/domain/notification/DeliveryChannelPort.kt` を新規作成
  - `deliver(summary: Summary, user: User)` メソッドシグネチャを定義する
  - `Summary` は `com.newssummary.domain.summary.Summary`、`User` は `com.newssummary.domain.user.User` をインポートすること
  - ファイルが作成され、Spring が `List<DeliveryChannelPort>` としてインジェクション可能な状態になっていること
  - _Requirements: 1.1, 1.2_
  - _Boundary: DeliveryChannelPort_

- [ ] 1.2 DeliveryRecord エンティティと DeliveryRecordRepository インターフェースを作成する (P)
  - `backend/src/main/kotlin/com/newssummary/domain/notification/DeliveryRecord.kt` を新規作成
  - `id: Long`・`summaryId: Long`・`userId: Long`・`deliveredAt: Instant`・`channel: String` 属性を `@Entity` で定義する
  - `backend/src/main/kotlin/com/newssummary/domain/notification/DeliveryRecordRepository.kt` を新規作成
  - `save(record: DeliveryRecord): DeliveryRecord` メソッドを定義する
  - JPA エンティティとして正しくマッピングされ、`@Column` アノテーションが正しく付与されていること
  - _Requirements: 3.1, 3.2_
  - _Boundary: DeliveryRecord, DeliveryRecordRepository_

- [ ] 1.3 UserNotificationSetting エンティティと UserNotificationSettingRepository インターフェースを作成する (P)
  - `backend/src/main/kotlin/com/newssummary/domain/notification/UserNotificationSetting.kt` を新規作成
  - `id: Long`・`userId: Long`（UNIQUE）・`emailNotificationEnabled: Boolean = true` 属性を `@Entity` で定義する
  - `backend/src/main/kotlin/com/newssummary/domain/notification/UserNotificationSettingRepository.kt` を新規作成
  - `findByUserId(userId: Long): UserNotificationSetting?` と `save(setting: UserNotificationSetting): UserNotificationSetting` メソッドを定義する
  - `userId` に `unique = true` 制約が付与されていること
  - _Requirements: 5.1_
  - _Boundary: UserNotificationSetting, UserNotificationSettingRepository_

---

- [ ] 2. インフラ/永続化層: JPA リポジトリ実装
- [ ] 2.1 DeliveryRecordJpaRepository を実装する
  - `backend/src/main/kotlin/com/newssummary/infrastructure/persistence/DeliveryRecordJpaRepository.kt` を新規作成
  - `DeliveryRecordRepository` インターフェースを実装する Spring `@Component` クラスを作成する
  - 内部に `SpringDataDeliveryRecordJpaRepository`（`JpaRepository<DeliveryRecord, Long>` を extends する Spring Data インターフェース）を委譲パターンで保持する
  - `save` は `JpaRepository.save()` に委譲する
  - クラスが Spring Bean として登録され、`DeliveryRecord` を PostgreSQL に保存できること
  - _Requirements: 3.1_
  - _Boundary: DeliveryRecordJpaRepository_
  - _Depends: 1.2_

- [ ] 2.2 UserNotificationSettingJpaRepository を実装する
  - `backend/src/main/kotlin/com/newssummary/infrastructure/persistence/UserNotificationSettingJpaRepository.kt` を新規作成
  - `UserNotificationSettingRepository` インターフェースを実装する Spring `@Component` クラスを作成する
  - 内部に `SpringDataUserNotificationSettingJpaRepository`（`JpaRepository<UserNotificationSetting, Long>` を extends する Spring Data インターフェース）を委譲パターンで保持する
  - `findByUserId` は Spring Data メソッド命名規約で自動生成する
  - クラスが Spring Bean として登録され、`userId` による検索と保存ができること
  - _Requirements: 5.1–5.4_
  - _Boundary: UserNotificationSettingJpaRepository_
  - _Depends: 1.3_

---

- [ ] 3. インフラ/通知層: 配信アダプター実装
- [ ] 3.1 DashboardDeliveryAdapter を実装する
  - `backend/src/main/kotlin/com/newssummary/infrastructure/notification/DashboardDeliveryAdapter.kt` を新規作成
  - `DeliveryChannelPort` を実装する `@Component` クラスを作成する
  - `deliver(summary, user)` で `DeliveryRecord(summaryId=summary.id, userId=user.id, channel="DASHBOARD")` を作成して `deliveryRecordRepository.save()` を呼び出す
  - 保存が成功した場合に `DeliveryRecord` が DB に存在すること
  - _Requirements: 3.1, 3.2_
  - _Boundary: DashboardDeliveryAdapter_
  - _Depends: 1.1, 2.1_

- [ ] 3.2 Spring Mail 依存関係と設定を追加する
  - `backend/build.gradle.kts` に `spring-boot-starter-mail` 依存関係を追加する
  - `backend/src/main/resources/application.yml` に `spring.mail.*` 設定ブロックを追加する（`MAIL_HOST`・`MAIL_PORT`・`MAIL_USERNAME`・`MAIL_PASSWORD`・`MAIL_FROM`・`MAIL_SMTP_AUTH`・`MAIL_SMTP_STARTTLS` の環境変数参照）
  - `application.yml` に `spring.mail.from: ${MAIL_FROM:noreply@news-summary.local}` が存在し、Spring Boot が起動時に `JavaMailSender` Bean を生成できること
  - _Requirements: 2.2_
  - _Boundary: application.yml, build.gradle.kts_

- [ ] 3.3 EmailDeliveryAdapter を実装する
  - `backend/src/main/kotlin/com/newssummary/infrastructure/notification/EmailDeliveryAdapter.kt` を新規作成
  - `DeliveryChannelPort` を実装する `@Component` クラスを作成する
  - `deliver(summary, user)` で `userNotificationSettingRepository.findByUserId(user.id)?.emailNotificationEnabled ?: true` を確認し、`false` の場合は INFO ログを出力してリターン
  - `SimpleMailMessage` を使用してメールを構築する（件名: `"【ニュース要約】${generatedAt の日付}"`、本文: keywords + content）
  - `@Value("\${spring.mail.from}")` で送信元アドレスを注入する
  - メール送信が失敗した場合に例外をスローし、`emailNotificationEnabled=false` のユーザーへの送信はスキップされること
  - _Requirements: 2.1–2.7_
  - _Boundary: EmailDeliveryAdapter_
  - _Depends: 1.1, 1.3, 2.2, 3.2_

---

- [ ] 4. アプリケーション層: ユースケース実装
- [ ] 4.1 DeliverSummaryUseCase を実装する
  - `backend/src/main/kotlin/com/newssummary/application/notification/DeliverSummaryUseCase.kt` を新規作成
  - `@Service` アノテーションを付与し、`summaryRepository: SummaryRepository`・`userRepository: UserRepository`・`deliveryChannels: List<DeliveryChannelPort>` をコンストラクタインジェクション
  - `execute(summaryId: Long, userId: Long)` メソッドを実装する
  - `summaryRepository.findById(summaryId)` が存在しない場合は `SummaryNotFoundException` をスローする
  - `deliveryChannels.forEach { channel -> try { channel.deliver(summary, user); successCount++ } catch (e: Exception) { errorCount++; logger.error(...) } }`
  - `INFO "Delivery completed: attempted={N}, succeeded={M}, userId={userId}"` を出力すること
  - 1チャネルが例外をスローしても他チャネルの配信が継続されること
  - _Requirements: 4.1–4.4_
  - _Boundary: DeliverSummaryUseCase_
  - _Depends: 1.1, 3.1, 3.3_

- [ ] 4.2 NotificationSettingService を実装する
  - `backend/src/main/kotlin/com/newssummary/application/notification/NotificationSettingService.kt` を新規作成
  - `getSettings(userId: Long): NotificationSettingResponse` — `findByUserId` が null の場合はデフォルト値（`emailNotificationEnabled=true`）を返す
  - `updateSettings(userId: Long, emailNotificationEnabled: Boolean): NotificationSettingResponse` — 既存設定を更新するか、存在しない場合は新規作成する
  - `dto/NotificationSettingResponse.kt` を同ディレクトリに作成する（`userId: Long`, `emailNotificationEnabled: Boolean`）
  - `getSettings` で設定が存在しないユーザーに対してデフォルト値（`emailNotificationEnabled=true`）が返却されること
  - _Requirements: 5.1–5.5_
  - _Boundary: NotificationSettingService_
  - _Depends: 1.3, 2.2_

- [ ] 4.3 SummaryDeliveryResponse DTO を作成する (P)
  - `backend/src/main/kotlin/com/newssummary/application/notification/dto/SummaryDeliveryResponse.kt` を新規作成
  - `id: Long`・`content: String`・`keywords: String`・`generatedAt: Instant`・`isPreview: Boolean = false` を持つ data class を定義する
  - ファイルが作成され、コンパイルが通ること
  - _Requirements: 3.6, 3.7_
  - _Boundary: SummaryDeliveryResponse_

---

- [ ] 5. プレゼンテーション層: REST エンドポイント実装
- [ ] 5.1 SummaryRepository に findById メソッドを追加する
  - `backend/src/main/kotlin/com/newssummary/domain/summary/SummaryRepository.kt` に `findById(id: Long): Summary?` メソッドを追加する
  - `backend/src/main/kotlin/com/newssummary/infrastructure/persistence/SummaryJpaRepository.kt` に対応する実装を追加する（Spring Data の `JpaRepository.findById()` に委譲）
  - `findById` が存在しない ID に対して `null` を返し、存在する ID に対して `Summary` を返すこと
  - _Requirements: 3.4, 4.1_
  - _Boundary: SummaryRepository, SummaryJpaRepository_

- [ ] 5.2 SummaryNotFoundException と GlobalExceptionHandler への追加を実装する
  - `backend/src/main/kotlin/com/newssummary/application/notification/` に `SummaryNotFoundException.kt` を作成する（`class SummaryNotFoundException(summaryId: Long) : RuntimeException(...)`）
  - `GlobalExceptionHandler.kt` に `@ExceptionHandler(SummaryNotFoundException::class)` で 404 を返すハンドラを追加する
  - `SummaryNotFoundException` がスローされた場合に 404 `ErrorResponse` が返却されること
  - _Requirements: 4.2_
  - _Boundary: SummaryNotFoundException, GlobalExceptionHandler_

- [ ] 5.3 DeliveryController を実装する
  - `backend/src/main/kotlin/com/newssummary/presentation/DeliveryController.kt` を新規作成
  - `GET /api/delivery/summaries` — `summaryRepository.findAllByUserIdOrderByGeneratedAtDesc(userId)` から `SummaryDeliveryResponse` リストに変換（`content` は先頭200文字、`isPreview=true`）して返す
  - `GET /api/delivery/summaries/{id}` — `summaryRepository.findById(id)` で取得し、`summary.userId != requestUserId` の場合は 403、存在しない場合は `SummaryNotFoundException` をスロー
  - SecurityContext から `userId` を取得すること（`@AuthenticationPrincipal` または `SecurityContextHolder`）
  - 別ユーザーの要約 ID へのリクエストが 403 を返し、存在しない ID が 404 を返すこと
  - _Requirements: 3.3–3.7_
  - _Boundary: DeliveryController_
  - _Depends: 4.3, 5.1, 5.2_

- [ ] 5.4 NotificationSettingController を実装する
  - `backend/src/main/kotlin/com/newssummary/presentation/NotificationSettingController.kt` を新規作成
  - `GET /api/notification/settings` — `notificationSettingService.getSettings(userId)` を呼び出して返す
  - `PUT /api/notification/settings` — `@Valid` 付きの `UpdateNotificationSettingRequest(emailNotificationEnabled: Boolean)` を受け取り `notificationSettingService.updateSettings()` を呼び出す
  - `UpdateNotificationSettingRequest` は `presentation` パッケージまたは `dto` サブパッケージに配置すること
  - `emailNotificationEnabled` が boolean 以外の値の場合に 400 が返却されること
  - _Requirements: 5.2–5.5_
  - _Boundary: NotificationSettingController_
  - _Depends: 4.2_

- [ ] 5.5 SecurityConfig に新規エンドポイントの認証設定を追加する
  - `backend/src/main/kotlin/com/newssummary/infrastructure/security/SecurityConfig.kt` を修正する
  - `/api/delivery/**` と `/api/notification/**` が `permitAll` リストに含まれないことを確認し、JWT 認証が必須であることを保証する
  - 未認証リクエストに対して 401 が返却されること
  - _Requirements: 3.3, 5.2_
  - _Boundary: SecurityConfig_

---

- [ ] 6. scheduler スペックとの統合
- [ ] 6.1 ExecuteScheduledSummaryUseCase に DeliverSummaryUseCase の呼び出しを追加する
  - `backend/src/main/kotlin/com/newssummary/application/scheduler/ExecuteScheduledSummaryUseCase.kt` を修正する
  - `DeliverSummaryUseCase` をコンストラクタインジェクションで追加する
  - `generateSummaryUseCase.execute(userId)` 成功後（`SummaryResult` 取得後）に `deliverSummaryUseCase.execute(summaryResult.id, userId)` を呼び出す
  - `deliverSummaryUseCase.execute()` の例外は `ExecuteScheduledSummaryUseCase` の try-catch で捕捉してジョブ FAILURE として記録すること（スケジューラー全体を停止させない）
  - 要約生成成功後に配信ユースケースが呼び出され、配信例外がジョブ履歴に記録されること
  - _Requirements: 4.5_
  - _Boundary: ExecuteScheduledSummaryUseCase_
  - _Depends: 4.1_

---

- [ ] 7. フロントエンド: API クライアント関数の作成
- [ ] 7.1 deliveryApi.ts を作成する (P)
  - `frontend/src/api/deliveryApi.ts` を新規作成
  - `fetchSummaries(): Promise<SummaryDeliveryResponse[]>` — `GET /api/delivery/summaries` を呼び出す
  - `fetchSummaryDetail(id: number): Promise<SummaryDeliveryResponse>` — `GET /api/delivery/summaries/{id}` を呼び出す
  - `SummaryDeliveryResponse` 型（`id: number`, `content: string`, `keywords: string`, `generatedAt: string`, `isPreview: boolean`）を定義する
  - `axiosClient`（既存の `src/api/axiosClient.ts`）を使用して JWT を自動付与すること
  - ファイルが作成され、型エラーなくインポートできること
  - _Requirements: 6.1–6.7_
  - _Boundary: deliveryApi_

- [ ] 7.2 notificationSettingApi.ts を作成する (P)
  - `frontend/src/api/notificationSettingApi.ts` を新規作成
  - `fetchNotificationSettings(): Promise<NotificationSettingResponse>` — `GET /api/notification/settings` を呼び出す
  - `updateNotificationSettings(emailNotificationEnabled: boolean): Promise<NotificationSettingResponse>` — `PUT /api/notification/settings` を呼び出す
  - `NotificationSettingResponse` 型（`userId: number`, `emailNotificationEnabled: boolean`）を定義する
  - ファイルが作成され、型エラーなくインポートできること
  - _Requirements: 7.1–7.5_
  - _Boundary: notificationSettingApi_

---

- [ ] 8. フロントエンド: React 画面の実装
- [ ] 8.1 DashboardPage を実装する
  - `frontend/src/pages/DashboardPage.tsx` を実装する（既存スタブを置き換え）
  - `useEffect` で `fetchSummaries()` を呼び出し、取得したデータを状態管理する
  - ローディング中は Spinner コンポーネントを表示し、0件時は「まだ要約がありません」を表示する
  - 各行に `generatedAt`（`YYYY/MM/DD HH:mm` 形式）・`keywords`・本文プレビューを表示し、クリックで `/dashboard/summaries/{id}` に遷移する
  - Tailwind CSS でスタイリングする
  - 0件・ローディング・エラー状態がそれぞれ正しく表示されること
  - _Requirements: 6.1–6.7_
  - _Boundary: DashboardPage_
  - _Depends: 7.1_

- [ ] 8.2 SummaryDetailPage を実装する (P)
  - `frontend/src/pages/SummaryDetailPage.tsx` を新規作成
  - URL パラメーター `id` を `useParams` で取得し、`fetchSummaryDetail(id)` を呼び出す
  - 要約全文・キーワード・生成日時を表示する
  - ローディング・エラー（403/404 含む）状態を表示する
  - Tailwind CSS でスタイリングする
  - 存在する ID の詳細が正しく表示されること
  - _Requirements: 6.3, 6.4, 6.6, 6.7_
  - _Boundary: SummaryDetailPage_
  - _Depends: 7.1_

- [ ] 8.3 SettingsPage に通知設定セクションを追加する (P)
  - `frontend/src/pages/SettingsPage.tsx`（`keyword-settings` スペックが作成済み）に「通知設定」セクションを追加する（新規ファイル作成ではない）
  - `useEffect` で `fetchNotificationSettings()` を呼び出し、トグルの初期値を設定する
  - トグル変更時に `updateNotificationSettings(enabled)` を呼び出す
  - API 呼び出し中はトグルを `disabled` にして二重送信を防ぐ
  - 保存成功時は「設定を保存しました」、失敗時は「エラーが発生しました」を表示する
  - Tailwind CSS でスタイリングする
  - トグル操作後に設定が保存され、UI が即座に反映されること
  - _Requirements: 7.1–7.5_
  - _Boundary: SettingsPage_
  - _Depends: 7.2_

- [ ] 8.4 App.tsx にルーティングを追加する
  - `frontend/src/App.tsx` に `/dashboard/summaries/:id` → `SummaryDetailPage` のルートを追加する
  - `/settings` → `SettingsPage` のルートを追加する
  - 新規ルートが React Router v6 の `<Routes>` 内に正しく定義され、直接 URL アクセスで画面が表示されること
  - _Requirements: 6.3, 7.1_
  - _Boundary: App.tsx_
  - _Depends: 8.2, 8.3_

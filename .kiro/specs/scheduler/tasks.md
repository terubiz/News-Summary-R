# 実装計画: scheduler

## タスク一覧

- [x] 1. 基盤: @EnableScheduling 有効化とドメイン層の追加
- [x] 1.1 `NewsSummaryApplication.kt` に `@EnableScheduling` を追加する
  - `@EnableScheduling` アノテーションをメインクラスに付与する
  - アプリケーション起動時に Spring Scheduling が有効化され、`@Scheduled` メソッドが実行されることを確認できる
  - _Requirements: 1.4_

- [x] 1.2 `SchedulerPort` インターフェースをドメイン層に作成する
  - `com.newssummary.domain.scheduler.SchedulerPort` インターフェースを新規作成する
  - `executeScheduledJobs()` メソッドを定義する
  - ファイル `/domain/scheduler/SchedulerPort.kt` が存在し、Kotlin でコンパイルが通ることを確認できる
  - _Requirements: 1.5, 5.1_

- [x] 1.3 `JobExecutionHistory` エンティティをドメイン層に作成する
  - `com.newssummary.domain.scheduler.JobExecutionHistory` JPA エンティティを新規作成する
  - 属性: `id: Long`（自動採番）、`userId: Long`、`status: String`、`executedAt: Instant`、`errorMessage: String?`
  - `@Entity` および `@Table(name = "job_execution_histories")` を設定する
  - kotlin-jpa プラグインによる no-arg コンストラクタが有効であることを確認（`@Entity` アノテーションが設定済みであること）
  - _Requirements: 3.3_

- [x] 1.4 `JobExecutionHistoryRepository` インターフェースをドメイン層に作成する
  - `com.newssummary.domain.scheduler.JobExecutionHistoryRepository` インターフェースを新規作成する
  - `save(history: JobExecutionHistory): JobExecutionHistory` メソッドを定義する
  - ファイル `/domain/scheduler/JobExecutionHistoryRepository.kt` が存在する
  - _Requirements: 3.5_

- [x] 1.5 `SummaryConfigRepository` に `findAll()` メソッドを追加する
  - `keyword-settings` スペックで定義された `SummaryConfigRepository` インターフェースに `findAll(): List<SummaryConfig>` を追加する
  - `SummaryConfigJpaRepository`（インフラ実装）でも `findAll()` を実装する
  - コンパイルエラーがなく、既存の `SummaryConfigService` への影響がないことを確認できる
  - _Requirements: 1.1_

---

- [x] 2. コア: アプリケーション層とインフラ層の実装
- [x] 2.1 `JobExecutionHistoryJpaRepository` をインフラ層に作成する
  - `com.newssummary.infrastructure.persistence.JobExecutionHistoryJpaRepository` を新規作成する
  - `JobExecutionHistoryRepository`（ドメイン）インターフェースを実装する Spring `@Repository` コンポーネントとして作成する
  - 内部に `SpringDataJobExecutionHistoryJpaRepository`（`JpaRepository<JobExecutionHistory, Long>`）を委譲パターンで保持する
  - `save` メソッドが `JpaRepository.save()` に委譲され、`JobExecutionHistory` がデータベースに保存されることを確認できる
  - _Requirements: 3.3_
  - _Boundary: JobExecutionHistoryJpaRepository_

- [x] 2.2 `ExecuteScheduledSummaryUseCase` をアプリケーション層に作成する
  - `com.newssummary.application.scheduler.ExecuteScheduledSummaryUseCase` を新規作成する
  - コンストラクタ注入: `SummaryConfigRepository`、`GenerateSummaryUseCase`、`JobExecutionHistoryRepository`
  - `ConcurrentHashMap<Long, Boolean>` による重複実行防止フラグを実装する
  - `run()` メソッド内で全 SummaryConfig を取得し、`executionTime` と現在時刻（HH:mm）を照合する
  - 時刻一致ユーザーに対して、重複フラグ確認後に `GenerateSummaryUseCase.execute(userId)` を呼び出す
  - 成功時は `JobExecutionHistory(status="SUCCESS")` を保存し、INFO ログを出力する
  - `NoActiveKeywordsException` 発生時は WARN ログ出力後に `JobExecutionHistory(status="FAILURE")` を保存する
  - `AIProviderException` およびその他の例外発生時は ERROR ログ出力後に `JobExecutionHistory(status="FAILURE")` を保存する
  - `finally` ブロックで `runningJobs.remove(userId)` を確実に呼び出す
  - 個別ユーザーのジョブ失敗後も次ユーザーの処理が継続されることを確認できる（例外が `run()` メソッド外に伝播しない）
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 3.1, 3.2, 4.1, 4.2, 4.3, 4.4_
  - _Boundary: ExecuteScheduledSummaryUseCase_
  - _Depends: 1.3, 1.4, 1.5_

- [x] 2.3 `SpringSchedulerAdapter` をインフラ層に作成する
  - `com.newssummary.infrastructure.scheduler.SpringSchedulerAdapter` を新規作成する
  - `SchedulerPort` インターフェースを実装する `@Component` として作成する
  - `@Scheduled(fixedDelay = 60000)` アノテーションを `executeScheduledJobs()` メソッドに付与する
  - `executeScheduledJobs()` 内で `executeScheduledSummaryUseCase.run()` に処理を委譲する
  - アプリケーション起動後、毎分ログが出力されることで定期実行が確認できる
  - _Requirements: 1.3, 1.5, 5.2, 5.3_
  - _Boundary: SpringSchedulerAdapter_
  - _Depends: 1.2, 2.2_

---

- [ ] 3. 統合: エンドツーエンド動作確認
- [x] 3.1 スケジューラー全体の統合確認を行う
  - `docker compose up` でアプリケーションを起動する
  - `SummaryConfig.executionTime` を現在時刻（HH:mm）に設定し、1分以内に `GenerateSummaryUseCase` が呼び出されることをログで確認する
  - `job_execution_histories` テーブルに SUCCESS または FAILURE レコードが INSERT されることを確認できる
  - 同一ユーザーの重複実行スキップログが `ConcurrentHashMap` ロック時に出力されることを確認できる
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 3.1, 3.2, 3.3_
  - _Note: FAILUREパス・SUCCESSパス共に確認済み。使用モデル: gemini-2.5-flash_

- [x] 3.2 エラー耐性の動作確認を行う
  - アクティブキーワードが0件のユーザーの `SummaryConfig.executionTime` を現在時刻に設定し、WARN ログ出力と FAILURE 履歴記録後に他ユーザーの処理が継続されることを確認できる
  - 複数ユーザーが同一 `executionTime` を持つ場合、全ユーザー分の履歴レコードが記録されることを確認できる
  - _Requirements: 4.1, 4.2, 4.3_

- [x] 3.3 SchedulerPort 抽象化の設計整合性を確認する
  - `SpringSchedulerAdapter` が `SchedulerPort` インターフェースを実装していることをコンパイルで確認する
  - `ExecuteScheduledSummaryUseCase` が `SchedulerPort` に依存せず独立していることをコードレビューで確認できる（将来の Quartz 移行時に `SpringSchedulerAdapter` のみ変更すれば動作する構造）
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

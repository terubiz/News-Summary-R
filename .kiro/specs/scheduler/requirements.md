# 要件定義書: scheduler

## はじめに

本スペックは、ニュース要約配信アプリケーション（News-Summary-R）の**スケジューラー機能**を定義する。ユーザーが `SummaryConfig` に設定した実行時刻（`executionTime`）に基づき、毎分チェックして該当ユーザーの要約生成ジョブを自動実行する。

現状は `ai-summary-engine` スペックで手動実行（`POST /api/summaries/generate`）のみが可能な状態である。本スペックにより、設定された時刻に自動で `GenerateSummaryUseCase` が呼び出され、ジョブ実行結果（成功／失敗・実行時刻・エラー内容）がデータベースに記録される。

## 境界コンテキスト

- **スコープ内**: `@Scheduled` ジョブ実装、`ExecuteScheduledSummaryUseCase`、`SchedulerPort` 抽象化、`JobExecutionHistory` エンティティ（userId, status, executedAt, errorMessage）
- **スコープ外**: ジョブ管理UI、Quartz への移行実装、再試行ロジック、通知配信（`notification-delivery` の責務）
- **隣接期待値**: `notification-delivery` スペックは `JobExecutionHistory`（成功ステータス）をトリガーとして要約配信を実行することを想定する

---

## 要件

### 要件 1: スケジュールジョブの自動実行

**目的:** 認証済みユーザーとして、設定した実行時刻に自動でニュース要約が生成されることで、毎日手動操作なしに最新ニュースを受け取りたい。

#### 受け入れ条件

1. When スケジューラーが起動するとき、the スケジューラーシステム shall 毎分1回 `SummaryConfig` テーブルの全レコードを照合する
2. When 現在時刻（HH:mm）が `SummaryConfig.executionTime` と一致するユーザーが存在するとき、the スケジューラーシステム shall そのユーザーの `GenerateSummaryUseCase.execute(userId)` を呼び出す
3. The スケジューラーシステム shall Spring `@Scheduled(fixedDelay = 60000)` を使用して毎分定期起動する
4. When Spring Boot アプリケーションが起動するとき、the スケジューラーシステム shall スケジューラーを自動的に有効化する
5. The スケジューラーシステム shall `SchedulerPort` インターフェースを経由して実行ロジックを呼び出し、将来の Quartz 移行に対応できる抽象化を維持する

### 要件 2: 同一ユーザーの重複実行防止

**目的:** システム運用者として、同一ユーザーに対するジョブが重複して実行されないことで、無駄な AI 呼び出しや重複した要約が生成されるのを防ぎたい。

#### 受け入れ条件

1. While あるユーザーのジョブが実行中であるとき、the スケジューラーシステム shall 同一ユーザーに対する新たなジョブ実行を開始しない
2. The スケジューラーシステム shall ユーザーごとの実行中フラグ（`ConcurrentHashMap` または同等のインメモリ構造）を保持し、ジョブ開始時にフラグを立て、完了時（成功・失敗問わず）にフラグを解除する
3. If 同一ユーザーのジョブが既に実行中であるとき、the スケジューラーシステム shall そのチェックサイクルの当該ユーザーをスキップし、ログに記録する

### 要件 3: ジョブ実行履歴の記録

**目的:** システム運用者として、各ユーザーのジョブ実行結果（成功・失敗・実行時刻）が記録されることで、スケジューラーの稼働状況やエラーを後から確認できるようにしたい。

#### 受け入れ条件

1. When `GenerateSummaryUseCase.execute(userId)` が正常に完了したとき、the スケジューラーシステム shall `JobExecutionHistory`（userId, status="SUCCESS", executedAt=現在時刻, errorMessage=null）をデータベースに保存する
2. If `GenerateSummaryUseCase.execute(userId)` が例外をスローしたとき、the スケジューラーシステム shall `JobExecutionHistory`（userId, status="FAILURE", executedAt=現在時刻, errorMessage=例外メッセージ）をデータベースに保存する
3. The スケジューラーシステム shall `JobExecutionHistory` エンティティを `job_execution_histories` テーブルに永続化する（カラム: id, user_id, status, executed_at, error_message）
4. When ジョブ実行が失敗したとき、the スケジューラーシステム shall エラー詳細を ERROR レベルでログに出力した上でジョブ履歴を記録し、スケジューラー全体を停止させない
5. The スケジューラーシステム shall `JobExecutionHistoryRepository` ドメインインターフェースを通じて履歴を永続化する

### 要件 4: エラー耐性とロギング

**目的:** システム運用者として、個々のユーザーのジョブ失敗がスケジューラー全体の動作に影響しないことで、他のユーザーへの要約生成が継続的に行われるようにしたい。

#### 受け入れ条件

1. If あるユーザーのジョブ実行中に例外が発生したとき、the スケジューラーシステム shall その例外をキャッチしてジョブ履歴に記録し、次のユーザーのジョブ処理を継続する
2. If `GenerateSummaryUseCase` が `NoActiveKeywordsException` をスローしたとき、the スケジューラーシステム shall WARN レベルでログに出力し、ジョブステータスを "FAILURE" として履歴に記録する
3. If `GenerateSummaryUseCase` が `AIProviderException` をスローしたとき、the スケジューラーシステム shall ERROR レベルでログに出力し、ジョブステータスを "FAILURE" として履歴に記録する
4. When ジョブが正常に完了したとき、the スケジューラーシステム shall userId・実行時刻・処理時間を INFO レベルでログに出力する

### 要件 5: SchedulerPort 抽象化

**目的:** 開発者として、スケジューラーの実行ロジックが `SchedulerPort` インターフェースで抽象化されることで、将来 Quartz 等へ移行する際にインフラ層のみの変更で対応できるようにしたい。

#### 受け入れ条件

1. The スケジューラーシステム shall `SchedulerPort` インターフェースをドメイン層に定義し、`executeScheduledJobs()` メソッドを公開する
2. The スケジューラーシステム shall `SpringSchedulerAdapter` がインフラ層で `SchedulerPort` を実装し、`@Scheduled` アノテーションによる定期起動エントリポイントとなる
3. When `SpringSchedulerAdapter` が呼び出されたとき、the スケジューラーシステム shall `ExecuteScheduledSummaryUseCase.run()` に処理を委譲する
4. The スケジューラーシステム shall 将来 Quartz に移行する場合でも `ExecuteScheduledSummaryUseCase` を変更せずに `SpringSchedulerAdapter` の代替実装に差し替えられる設計を維持する

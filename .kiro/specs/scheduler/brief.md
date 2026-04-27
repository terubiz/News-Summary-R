# Brief: scheduler

## Problem
設定した時刻になると自動でAI要約が実行されるスケジューリング機能が必要。

## Current State
ai-summary-engine スペック完了後。手動実行のみ可能な状態。

## Desired Outcome
SummaryConfig の executionTime を参照し、毎分チェックして該当ユーザーの要約ジョブを自動実行できる状態。

## Approach
Spring `@Scheduled`（毎分起動）でSummaryConfigを全件照合し、現在時刻と一致するユーザーの `GenerateSummaryUseCase` を呼び出す。ジョブ実行結果（成功/失敗・実行時刻）をDBに記録する。

## Scope
- **In**: `@Scheduled` ジョブ実装、`ExecuteScheduledSummaryUseCase`、JobExecutionHistory エンティティ（userId, status, executedAt, errorMessage）
- **Out**: ジョブ管理UI、Quartzへの移行、再試行ロジック

## Boundary Candidates
- ジョブ実行履歴（管理画面での状態確認に利用可能）

## Out of Boundary
- 要約内容の配信（notification-delivery 側の責務）

## Upstream / Downstream
- **Upstream**: ai-summary-engine（GenerateSummaryUseCase）、keyword-settings（SummaryConfig）
- **Downstream**: notification-delivery（実行完了後に Summary の配信をトリガー）

## Constraints
- 同一ユーザーの重複実行を防ぐ（単純なフラグで可）

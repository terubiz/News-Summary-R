# Brief: keyword-settings

## Problem
ユーザーがAIへの入力となるキーワードと実行スケジュール設定を管理できる画面が必要。

## Current State
infrastructure スペック完了後。キーワード・設定の保存手段なし。

## Desired Outcome
管理画面でキーワードのCRUDと実行設定が行え、設定がDBに保存される状態。

## Approach
Keyword・SummaryConfig を JPA エンティティとして実装。REST API を提供し、React の管理画面から操作できるようにする。

## Scope
- **In**: Keyword エンティティ（id, userId, word, isActive）、SummaryConfig エンティティ（userId, executionTime, lookbackDays, fetchCount, aiProviderName）、CRUD ユースケース、REST API（`/api/keywords`, `/api/summary-config`）、管理画面 React ページ（キーワード一覧/追加/削除、設定フォーム）
- **Out**: AI呼び出し、スケジューラ、通知配信

## Boundary Candidates
- Keyword集約（AIエンジン側が読み取る）
- SummaryConfig集約（スケジューラ側が読み取る）

## Out of Boundary
- キーワードの有効性チェック（AIが判断）

## Upstream / Downstream
- **Upstream**: infrastructure（JWT認証、User）
- **Downstream**: ai-summary-engine（Keyword/SummaryConfig を読み取る）、scheduler（SummaryConfig の executionTime を参照）

## Constraints
- 認証済みユーザーのリソースのみ操作可能（JWT）

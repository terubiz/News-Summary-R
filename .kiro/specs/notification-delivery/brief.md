# Brief: notification-delivery

## Problem
生成された要約をWebダッシュボードとEmailで受け取れる配信機能が必要。将来はLINE/Discord等への拡張も想定。

## Current State
scheduler スペック完了後。要約はDBに保存されているが配信手段なし。

## Desired Outcome
要約生成後にEmailが送信され、Webダッシュボードで要約履歴を閲覧できる状態。

## Approach
`DeliveryChannelPort` インターフェース（output port）を定義し、`EmailDeliveryAdapter`（Spring Mail）と `DashboardDeliveryAdapter`（DBへの永続化）が実装。scheduler の完了後に呼び出す。将来の拡張はアダプターを追加するだけで対応できる構造にする。

## Scope
- **In**: `DeliveryChannelPort` 出力ポートIF、`EmailDeliveryAdapter`（Spring Mail + SMTP）、`DashboardDeliveryAdapter`（Summary をDBに永続化してAPI提供）、`DeliverSummaryUseCase`、要約履歴一覧・詳細の React 画面、ユーザー設定でEmail ON/OFF切替
- **Out**: LINE/Discord アダプターの実装（インターフェース定義のみ）、プッシュ通知

## Boundary Candidates
- `DeliveryChannelPort`（将来の拡張ポイント）
- 要約履歴API（ダッシュボード用）

## Out of Boundary
- 要約の再生成
- 配信失敗時の自動再試行

## Upstream / Downstream
- **Upstream**: scheduler（要約生成完了イベント）、ai-summary-engine（Summary エンティティ）
- **Downstream**: なし（末端）

## Constraints
- SMTP設定は環境変数で管理
- Email送信はベストエフォート（失敗してもアプリはクラッシュしない）

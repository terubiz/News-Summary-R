# ロードマップ

## 概要

毎日指定した時刻に、ユーザーが登録したキーワード（例: "S&P", "日経225", "戦争"）をAIに渡し、関連ニュースを調査・要約して配信するWebアプリケーション。アプリ自体はニュースやETFデータを直接取得せず、AI（Gemini）のGrounding検索機能を活用してリアルタイムのニュース情報を収集・要約する。

Webダッシュボードでの閲覧とEmailでの受信を基本とし、将来的なLINE/Discord等への拡張、複数AIプロバイダーの切り替え、マルチユーザーSaaS化を見据えた設計とする。バックエンドはDDD + クリーンアーキテクチャ準拠で設計し、各境界付きコンテキストを独立したスペックとして段階的に実装する。

## アプローチ決定

- **採用**: 5スペック・垂直スライス構成（infrastructure → keyword-settings → ai-summary-engine → scheduler → notification-delivery）
- **理由**: 各スペックが独立した動作可能な機能単位となり、依存関係が一方向に整理される。DDD境界付きコンテキストとスペック境界が対応するため、レビューと並行実装がしやすい。
- **却下した案**: 3スペック粗粒度（1スペックの肥大化とレビュー境界の曖昧さが懸念）、BE/FE分離（統合が最後まで見えない）

## スコープ

- **In**: JWT認証・ユーザー管理 / キーワード・スケジュール設定管理（管理画面）/ Gemini AIによるニュース調査・要約エンジン（プロバイダー抽象化）/ スケジューラー（指定時刻に自動実行）/ Webダッシュボード表示 + Email配信（将来拡張設計）/ Docker コンテナ化
- **Out**: ニュース・ETFデータの直接取得 / LINE・Discord配信の実装（設計のみ・将来フェーズ）/ 課金・マルチテナント機能（将来フェーズ）

## 技術制約

- **AIプロバイダー**: Google Vertex AI SDK (`com.google.cloud:google-cloud-vertexai`) を使用。Google AI早期アクセスSDKは使用不可。Google Search Groundingの有効化が必要（GCPリージョン: us-central1 推奨）
- **Kotlinコンパイラプラグイン**: `kotlin-jpa`（JPA no-argコンストラクタ）と `kotlin-spring`（allopen）を必須設定
- **DBアクセス戦略**: blocking JPA に統一。reactive R2DBC + coroutines との混在禁止
- **フロントエンド**: React + Tailwind CSS
- **バックエンド**: Kotlin + Spring Boot 3.x + PostgreSQL
- **コンテナ**: Docker Compose

## 境界戦略

- **なぜこの分割か**: 各スペックが単一の境界付きコンテキストに対応し、ドメインモデルの独立性を保つ。`infrastructure`は共通基盤を提供し、後続スペックはそれを前提として各ドメインに集中できる。
- **注意すべき跨ぎ目**: `ai-summary-engine` ↔ `scheduler`（ユースケース呼び出しIF）、`ai-summary-engine` ↔ `notification-delivery`（Summary集約の共有）

## スペック（依存順）

- [x] infrastructure — Dockerセットアップ・DB基盤・Spring Boot/Reactスキャフォールド・JWT認証・ユーザー管理。依存: なし
- [x] keyword-settings — キーワードCRUD・スケジュール設定管理・管理画面UI。依存: infrastructure
- [x] ai-summary-engine — Gemini Vertex AI統合・AIプロバイダー抽象化・ニュース要約生成エンジン。依存: keyword-settings
- [x] scheduler — スケジューラー実装・ユーザーごとの自動実行・ジョブ履歴管理。依存: ai-summary-engine
- [x] notification-delivery — Email配信・Webダッシュボード表示・配信チャネル抽象化（将来拡張対応）。依存: scheduler

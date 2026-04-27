# Brief: infrastructure

## Problem
新規プロジェクトとして、バックエンド・フロントエンド・DBが動作する最小限の開発環境が必要。

## Current State
コードベースは空。

## Desired Outcome
`docker compose up` で全サービスが起動し、JWT認証付きのAPIとReact画面が疎通できる状態。

## Approach
Docker Compose でPostgreSQL・Redis・Spring Boot・Reactを起動。Spring Boot はDDD 4層パッケージ構造のスキャフォールドのみ用意し、JPA の `ddl-auto=update` でスキーマを自動生成。JWT認証はSpring Security 6.x + JJWT で実装し、発行したトークンをRedisで管理（有効期限・失効処理）。

## Scope
- **In**: Docker Compose（PostgreSQL + Redis含む）、Spring Boot + Kotlin スキャフォールド（DDD 4層パッケージ）、React + Tailwind スキャフォールド、JWT認証（ログイン/トークン検証/ログアウト）、Redisによるトークン管理（TTL設定・ブラックリスト方式によるトークン失効）、User エンティティ + UserRepository、Kotlinコンパイラプラグイン設定（kotlin-jpa, kotlin-spring）、グローバル例外ハンドラ
- **Out**: DBマイグレーションツール（Flyway等）、テストコード、CI/CD、本番環境設定

## Boundary Candidates
- 認証ミドルウェア（JWTフィルター）
- Userドメインモデル（他スペックが参照）

## Out of Boundary
- ビジネスロジック一切
- 画面コンポーネント（ルーティング設定のみ）

## Upstream / Downstream
- **Upstream**: なし
- **Downstream**: 全スペックがこの基盤に依存

## Constraints
- Kotlinプラグイン: `kotlin-jpa`, `kotlin-spring`(allopen) 必須
- DBアクセス: blocking JPA に統一
- Redis: Spring Data Redis（Lettuce）で接続、トークンのTTLはJWTの有効期限と同期

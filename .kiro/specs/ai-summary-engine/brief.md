# Brief: ai-summary-engine

## Problem
ユーザーのキーワードを元に、AIがリアルタイムでニュースを調査・要約する機能が必要。

## Current State
keyword-settings スペック完了後。AIとの接続手段なし。

## Desired Outcome
キーワードリストを渡すと、Gemini（Google Search Grounding有効）がニュースを調査・要約した結果テキストを返せる状態。

## Approach
`AIProviderPort` インターフェース（output port）を定義し、`GeminiApiAdapter` がそれを実装。SummaryConfig.aiProviderName の値でアダプターをルーティングすることで将来の複数プロバイダー切替を可能にする。

## Scope
- **In**: `AIProviderPort` 出力ポートIF、`GeminiApiAdapter`（Gemini REST API + Google Search Grounding）、`GenerateSummaryUseCase`（キーワード受取→AI呼出→SummaryResult返却）、Summary エンティティ（id, userId, content, keywords, generatedAt）
- **Out**: スケジューリング、通知配信、プロバイダー管理画面（設定フォームは keyword-settings 側）

## Boundary Candidates
- `AIProviderPort`（プロバイダー抽象化の境界）
- Summary エンティティ（notification-delivery 側が読み取る）

## Out of Boundary
- ニュースソースの直接取得（AIのGrounding検索に委ねる）
- プロバイダーの追加・削除UI

## Upstream / Downstream
- **Upstream**: keyword-settings（Keyword, SummaryConfig）
- **Downstream**: scheduler（GenerateSummaryUseCase を呼び出す）、notification-delivery（Summary を読み取る）

## Constraints
- Gemini REST API（`generativelanguage.googleapis.com`）を Spring RestClient で直接呼び出す（外部SDKなし）
- Google Search Grounding を有効化すること（`tools: [{"googleSearch": {}}]`）
- `GEMINI_API_KEY` 環境変数による API キー認証

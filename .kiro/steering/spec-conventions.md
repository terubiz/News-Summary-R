# スペック開発規約

## テストコードの命名規約

テストメソッド名は英語で記述し、`@DisplayName` アノテーションに日本語で説明を記載する。

```kotlin
@Test
@DisplayName("login: 正しい資格情報で LoginResponse を返す")
fun loginReturnsLoginResponseWithValidCredentials() { ... }
```

**理由**: メソッド名を英語にすることでIDE補完・grep・グローバルな可読性を確保しつつ、`@DisplayName` の日本語でテストレポートと仕様書としての可読性を両立する。

## インターフェース定義の一元管理

### ルール

リポジトリ・サービス・ポートのインターフェース定義は、**そのインターフェースを所有するスペックの `design.md` に一元管理する**。

下流スペックが上流インターフェースに新しいメソッドを必要とする場合は、上流スペックの `design.md`（インターフェース定義）と `tasks.md`（実装タスク）を更新してからレビューに進むこと。

下流スペックの `design.md` に「注記」や「実装ノート」としてメソッド追加を記述するだけでは不十分。

### 背景

このルールは、Wave 5（notification-delivery）スペック設計時の実際の問題から生まれた。

`notification-delivery` は `ai-summary-engine` が所有する `SummaryRepository` に `findById()` が必要だったが、`notification-delivery/design.md` の実装ノートに「`findById` を追加する」と注記するだけで、`ai-summary-engine/design.md` の公式インターフェース定義には反映しなかった。同様に `infrastructure` の `UserRepository` にも `findById()` の追加が必要だったが、どのスペックにも明記されていなかった。

これらはクロススペックレビューを実施するまで発見されず、実装フェーズに持ち越されるところだった。

インターフェース定義が所有スペックの外に散在すると、実装者が「どのメソッドを実装すべきか」を正しく把握できない。定義は常に所有スペックが一元管理することで、実装者がスペックを単独で読んで完結できる状態を維持する。

### 適用例

- `scheduler` が `SummaryConfigRepository`（keyword-settings 所有）に `findAll()` を追加する場合
  → `keyword-settings/design.md` の `SummaryConfigRepository` 定義に `findAll()` を追記し、`keyword-settings/tasks.md` に実装タスクを追加する
- 下流スペックの design.md には「上流スペックで追加されたメソッドを使用する」と参照元を明記する

---

## クロススペックレビュー

### ルール

全スペックの tasks-generated が完了した後、実装フェーズ（`/kiro-impl`）に入る前にクロススペックレビューを実施する。

### 確認項目

1. 各スペックが依存するインターフェースのメソッドが、所有スペックの定義に含まれているか
2. フロントエンドのページ名・ルートパスが複数スペック間で衝突していないか
3. 下流スペックが上流スペックのファイルを変更する箇所が、上流スペックの tasks.md に記載されているか
4. Wave 順（依存方向）に矛盾がないか

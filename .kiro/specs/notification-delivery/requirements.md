# 要件定義書: notification-delivery

## はじめに

本機能は、ニュース要約配信アプリケーション（News-Summary-R）の**通知配信機能**を実装する。スケジューラーが生成した要約（`Summary`エンティティ）をユーザーへ届けるための2つのチャネル（Email・Webダッシュボード）を提供し、将来の LINE/Discord 等への拡張を見据えた `DeliveryChannelPort` 抽象化を確立する。

認証済みユーザーは、要約生成完了後に自動でメール通知を受け取り、Webダッシュボードで要約履歴を閲覧できる。ユーザーはプロフィール設定でEmail通知のON/OFFを切り替えられる。

## 境界コンテキスト

- **スコープ内**:
  - `DeliveryChannelPort` 出力ポートインターフェースの定義
  - `EmailDeliveryAdapter`（Spring Mail + SMTP）の実装
  - `DashboardDeliveryAdapter`（Summary の DB への永続化）の実装
  - `DeliverSummaryUseCase`（配信オーケストレーション）の実装
  - Email 通知設定（ON/OFF）のユーザー設定 API
  - 要約履歴一覧・詳細の REST API
  - 要約履歴閲覧・詳細表示の React 画面
  - Email 通知設定 ON/OFF の React UI

- **スコープ外**:
  - LINE/Discord アダプターの実装（インターフェース定義のみでアダプター実装は将来フェーズ）
  - プッシュ通知（ブラウザ Push / モバイル Push）
  - 要約の再生成
  - 配信失敗時の自動再試行
  - 配信ログの管理 UI

- **隣接システムへの期待**:
  - `scheduler` スペック: `ExecuteScheduledSummaryUseCase` 完了後に `DeliverSummaryUseCase` を呼び出すこと
  - `ai-summary-engine` スペック: `Summary` エンティティ（id, userId, content, keywords, generatedAt）と `SummaryRepository` を提供すること
  - `infrastructure` スペック: `User` エンティティ（id, email）・JWT 認証フィルター・`GlobalExceptionHandler` を提供すること

---

## 要件

### 要件 1: DeliveryChannelPort 出力ポート

**目的:** 開発者として、統一された配信チャネル抽象化インターフェースを定義したい。将来の LINE/Discord 等への拡張をアダプター追加のみで実現できる設計を確立するため。

#### 受け入れ基準

1. `DeliveryChannelPort` インターフェースを定義し、`deliver(summary: Summary, user: User)` メソッドを持つこと
2. `DeliveryChannelPort` はドメイン層（`domain/notification/`）に配置し、インフラ実装への依存を逆転させること
3. `DeliverSummaryUseCase` が `List<DeliveryChannelPort>` を受け取り、全チャネルに対してループで配信を試みること
4. 各チャネルの配信失敗は他チャネルの配信を妨げないこと（個別の try-catch で隔離すること）
5. 将来のアダプター追加は `DeliveryChannelPort` を実装する新クラスを Spring Bean として登録するだけで対応できること

---

### 要件 2: Email 配信

**目的:** ユーザーとして、要約が生成されたときにメールで通知を受け取りたい。外出中でもニュース要約を把握できるようにするため。

#### 受け入れ基準

1. `EmailDeliveryAdapter` は `DeliveryChannelPort` を実装し、Spring Mail（`JavaMailSender`）を使用して SMTP 経由でメールを送信すること
2. メール送信の SMTP 設定（ホスト・ポート・ユーザー名・パスワード）は環境変数で管理し、`application.yml` にハードコードしないこと
3. ユーザーの `emailNotificationEnabled` が `false` の場合、`EmailDeliveryAdapter` はメールを送信せずにスキップすること
4. メール件名は「【ニュース要約】{generatedAt の日付}」形式であること
5. メール本文は要約内容（`summary.content`）とキーワード一覧（`summary.keywords`）を含むこと
6. メール送信が失敗した場合、アプリケーションはクラッシュせず ERROR ログを出力してベストエフォートで継続すること
7. `from` アドレスは環境変数 `MAIL_FROM` で設定できること

---

### 要件 3: ダッシュボード配信（Dashboard 永続化）

**目的:** ユーザーとして、Webダッシュボードで過去の要約履歴を一覧・詳細閲覧したい。いつでも過去の要約を振り返れるようにするため。

#### 受け入れ基準

1. `DashboardDeliveryAdapter` は `DeliveryChannelPort` を実装し、`DeliveryRecord` エンティティを PostgreSQL に保存することで要約の配信記録を永続化すること
2. `DeliveryRecord` は `summaryId`・`userId`・`deliveredAt`・`channel`（"DASHBOARD"）を属性として持つこと
3. 要約一覧 API（`GET /api/delivery/summaries`）は認証済みユーザーの要約を `generatedAt` 降順で返すこと
4. 要約詳細 API（`GET /api/delivery/summaries/{id}`）は指定 ID の要約内容を返すこと
5. `GET /api/delivery/summaries/{id}` は別ユーザーの要約に対して 403 を返すこと
6. 一覧レスポンスには `id`・`content`（先頭200文字のプレビュー）・`keywords`・`generatedAt` を含むこと
7. 詳細レスポンスには全フィールドを含むこと（`id`・`content`（全文）・`keywords`・`generatedAt`）

---

### 要件 4: DeliverSummaryUseCase（配信オーケストレーション）

**目的:** システムとして、要約生成完了後に全配信チャネルへ自動的に配信したい。ユーザーが手動操作なしに要約を受け取れるようにするため。

#### 受け入れ基準

1. `DeliverSummaryUseCase` は `summaryId` と `userId` を受け取り、`SummaryRepository` から `Summary` を取得して全 `DeliveryChannelPort` に配信すること
2. `Summary` が見つからない場合は `SummaryNotFoundException` をスローし、ERROR ログを出力すること
3. 各チャネルの配信は独立した try-catch で保護し、1チャネルの失敗が他チャネルの配信を中断しないこと
4. 配信完了後に配信試行数と成功数を INFO ログに出力すること
5. `scheduler` スペックの `ExecuteScheduledSummaryUseCase` から `DeliverSummaryUseCase.execute(summaryId, userId)` を呼び出せること

---

### 要件 5: Email 通知設定

**目的:** ユーザーとして、Email 通知の ON/OFF を自分で切り替えたい。不要なメール通知をコントロールできるようにするため。

#### 受け入れ基準

1. `UserNotificationSetting` エンティティが `userId`・`emailNotificationEnabled`（デフォルト: `true`）を保持すること
2. `GET /api/notification/settings` は認証済みユーザーの通知設定を返すこと
3. `PUT /api/notification/settings` は `emailNotificationEnabled` フィールドで通知設定を更新すること
4. 設定が存在しないユーザーの場合、`GET /api/notification/settings` はデフォルト値（emailNotificationEnabled: true）を返すこと
5. `PUT /api/notification/settings` でリクエストボディが不正な場合（型不一致等）は 400 を返すこと

---

### 要件 6: 要約履歴 React 画面

**目的:** ユーザーとして、Webブラウザで要約履歴を一覧・詳細閲覧したい。デスクトップからアクセスして情報を確認できるようにするため。

#### 受け入れ基準

1. 要約履歴一覧ページ（`/dashboard`）は認証済みユーザーの要約一覧を `generatedAt` 降順で表示すること
2. 一覧には各要約の `generatedAt`（日付表示）・`keywords`・本文プレビュー（先頭200文字 + 省略記号）を表示すること
3. 一覧の各行クリックで要約詳細ページ（`/dashboard/summaries/{id}`）に遷移すること
4. 詳細ページは要約の全文・キーワード・生成日時を表示すること
5. 要約が0件の場合、「まだ要約がありません」メッセージを表示すること
6. データ取得中はローディング状態を表示すること
7. API エラー時はエラーメッセージを表示すること

---

### 要件 7: Email 通知設定 React 画面

**目的:** ユーザーとして、ダッシュボード上で Email 通知の ON/OFF をトグルできるようにしたい。設定画面を開くだけで通知設定を変更できるようにするため。

#### 受け入れ基準

1. 設定ページ（`/settings`）に Email 通知 ON/OFF トグルスイッチを表示すること
2. トグルを切り替えると即座に `PUT /api/notification/settings` を呼び出し設定を保存すること
3. 保存成功時には成功メッセージを表示し、失敗時にはエラーメッセージを表示すること
4. ページ表示時に `GET /api/notification/settings` を呼び出し、現在の設定状態をトグルに反映すること
5. API 呼び出し中はトグルを無効化して二重送信を防ぐこと

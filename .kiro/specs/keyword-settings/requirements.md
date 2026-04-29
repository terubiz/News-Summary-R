# 要件定義書


## はじめに

本スペックは、ニュース要約配信Webアプリケーション（News-Summary-R）の**キーワード設定・サマリー実行設定管理機能**を構築するための要件を定義する。対象ユーザーは認証済みのアプリケーションユーザーであり、現状はキーワードや実行スケジュールを保存する手段がない。本スペック完了後、管理画面からキーワードのCRUD操作と実行設定の変更が行え、設定がDBに永続化された状態を実現する。

本スペックは `infrastructure` スペックが確立したUser エンティティ・JWT認証・DDD 4層パッケージ構造を前提とし、後続の `ai-summary-engine`（Keyword/SummaryConfigの読み取り）および `scheduler`（SummaryConfigのexecutionTime参照）の上流コントラクトを提供する。

## 境界コンテキスト（Boundary Context）

- **スコープ内**: Keyword エンティティ（id, userId, word, isActive）のCRUD / SummaryConfig エンティティ（userId, executionTime, lookbackDays, fetchCount, aiProviderName）の取得・更新 / REST API（`/api/keywords`、`/api/summary-config`）/ 管理画面 React ページ（キーワード一覧・追加・削除、設定フォーム）/ JWT認証によるリソースアクセス制御
- **スコープ外**: AI呼び出し / スケジューラー実装 / 通知配信 / キーワードの有効性バリデーション（AIが判断）/ テストコード
- **隣接する期待事項**: 後続の `ai-summary-engine` は本スペックが提供する `Keyword` 集約をリードモデルとして読み取る。`scheduler` は `SummaryConfig.executionTime` を参照して実行時刻を決定する。これらの集約インターフェースに変更を加えた場合、後続スペックの再検証が必要となる。

---

## 要件

### 要件 1: キーワードCRUD

**目的**: 認証済みユーザーとして、AIに渡すキーワードを管理画面から登録・有効化/無効化・削除したい。そうすることで、ニュース要約のトピックを自由に制御できる。

#### 受け入れ基準

1. When 認証済みユーザーがPOST `/api/keywords` を有効なwordで呼び出したとき、the キーワードサービス shall 当該ユーザーに紐づく新しいKeywordをPostgreSQLに保存し、201 Createdと作成されたKeywordリソース（id, userId, word, isActive）を返す
2. When 認証済みユーザーがGET `/api/keywords` を呼び出したとき、the キーワードサービス shall 自分のKeyword一覧（id, word, isActive）を返す
3. When 認証済みユーザーがPATCH `/api/keywords/{id}` で `isActive` フィールドを更新したとき、the キーワードサービス shall 対象Keywordの有効/無効状態を更新し、200 OKと更新後のKeywordリソースを返す
4. When 認証済みユーザーがDELETE `/api/keywords/{id}` を呼び出したとき、the キーワードサービス shall 対象Keywordを削除し、204 No Contentを返す
5. If 他のユーザーが所有するKeywordに対してPATCH/DELETEを試みた場合、the キーワードサービス shall 403 Forbiddenを返す
6. If 存在しないKeyword IDに対してPATCH/DELETEを試みた場合、the キーワードサービス shall 404 Not Foundを返す
7. When キーワード登録リクエストのwordが空文字または未指定のとき、the キーワードサービス shall 400 Bad Requestとバリデーションエラー詳細を返す
8. The キーワードサービス shall 同一ユーザーによる同一wordの重複登録を許容しない。重複時は 409 Conflictを返す

### 要件 2: サマリー実行設定の管理

**目的**: 認証済みユーザーとして、AIによるニュース要約の実行時刻・調査期間・取得件数・AIプロバイダーを設定したい。そうすることで、自分のライフスタイルに合わせた要約配信を受けられる。

#### 受け入れ基準

1. When 認証済みユーザーがGET `/api/summary-config` を呼び出したとき、the 設定サービス shall 自分のSummaryConfig（executionTime, lookbackDays, fetchCount, aiProviderName）を返す
2. When 認証済みユーザーが初めてGET `/api/summary-config` を呼び出したとき（設定未作成）、the 設定サービス shall デフォルト値（executionTime: "07:00", lookbackDays: 1, fetchCount: 10, aiProviderName: "gemini"）のSummaryConfigを自動作成して返す
3. When 認証済みユーザーがPUT `/api/summary-config` で設定値を送信したとき、the 設定サービス shall SummaryConfigを更新し、200 OKと更新後の設定を返す
4. If executionTime が "HH:mm" 形式でない場合、the 設定サービス shall 400 Bad Requestとバリデーションエラーを返す
5. If lookbackDays が1以上365以下の整数でない場合、the 設定サービス shall 400 Bad Requestとバリデーションエラーを返す
6. If fetchCount が1以上100以下の整数でない場合、the 設定サービス shall 400 Bad Requestとバリデーションエラーを返す

### 要件 3: JWT認証によるリソースアクセス制御

**目的**: アプリケーションとして、認証済みユーザーのみが自分のキーワードと設定にアクセスできる状態を保ちたい。そうすることで、他ユーザーのデータへの不正アクセスを防止できる。

#### 受け入れ基準

1. While 有効なJWTトークンがAuthorizationヘッダーに含まれているとき、the APIエンドポイント shall `/api/keywords` および `/api/summary-config` へのリクエストを処理する
2. If JWTトークンが存在しない、または無効な場合、the APIエンドポイント shall 401 Unauthorizedを返す
3. The キーワードサービス shall JWTのsubjectクレームからユーザーIDを抽出し、操作対象リソースの所有者と照合する
4. The 設定サービス shall JWTのsubjectクレームからユーザーIDを抽出し、操作対象SummaryConfigの所有者と照合する

### 要件 4: キーワード管理画面（React UI）

**目的**: 認証済みユーザーとして、Webブラウザ上でキーワードを視覚的に管理したい。そうすることで、APIを直接叩かずにキーワードの追加・有効化/無効化・削除が行える。

#### 受け入れ基準

1. When ユーザーがキーワード管理ページを開いたとき、the 画面 shall 自分のキーワード一覧を isActive 状態と共に表示する
2. When ユーザーがキーワード追加フォームにwordを入力してSubmitしたとき、the 画面 shall バックエンドAPIを呼び出し、成功した場合は一覧を更新する
3. When ユーザーが一覧上のキーワードのトグル（有効/無効）を操作したとき、the 画面 shall PATCH APIを呼び出し、UIの状態をリアルタイムに更新する
4. When ユーザーが削除ボタンをクリックしたとき、the 画面 shall DELETE APIを呼び出し、成功した場合は一覧から当該キーワードを除去する
5. When APIエラーが発生したとき、the 画面 shall ユーザーに分かりやすいエラーメッセージを表示する
6. The 画面 shall Tailwind CSSを使用してスタイリングし、レスポンシブなレイアウトとする

### 要件 5: サマリー設定画面（React UI）

**目的**: 認証済みユーザーとして、Webブラウザ上でサマリー実行設定を変更したい。そうすることで、自分のスケジュールに合った実行時刻や調査期間を容易に設定できる。

#### 受け入れ基準

1. When ユーザーが設定ページを開いたとき、the 画面 shall 現在のSummaryConfig（executionTime, lookbackDays, fetchCount, aiProviderName）をフォームに表示する
2. When ユーザーが設定フォームを変更してSubmitしたとき、the 画面 shall PUT APIを呼び出し、成功した場合は保存完了のフィードバックを表示する
3. When フォームのバリデーションエラーが発生したとき（APIレスポンスまたはクライアントバリデーション）、the 画面 shall フィールドごとのエラーメッセージを表示する
4. The 画面 shall Tailwind CSSを使用してスタイリングし、設定フォームを直感的なUI（時刻ピッカー等）で提供する

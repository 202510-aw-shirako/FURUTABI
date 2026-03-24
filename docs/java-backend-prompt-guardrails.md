# FURUTABI Java Backend Prompt Guardrails

このファイルは、Java バックエンド実装プロンプトを安定させるための補助メモです。

## この文書の扱い

- この文書は固定仕様書ではない
- 現物ファイル、migration、既存 Codex 実装、会話の整理に応じて変更してよい
- ただし変更する場合は、何を固定前提から外したか、何を追加したかを明示する
- Codex がこの文書を更新する必要がある場合は、更新前に変更案を提案として明示し、合意後に更新する
- `next-chat-handoff.md` と矛盾した場合は、まず現物ファイル・migration・既存 Codex 実装を確認してから更新する

## 目的

- 理想再設計ではなく、既存 `HTML / JS / migration` に整合する Spring Boot バックエンド実装へ寄せる
- Java 実装プロンプト作成時の認識ズレを減らす
- 未確定事項を勝手に固定しない

## ガードレール

### 1. 正とするもの

- 第一優先は現物ファイルと既存 Codex 実装
- DB の真実は migration
- handoff やメモは補助線
- 未確認のものは推測で埋めず、必要最小限の仮実装に留める

### 2. 認証方式

- ログインIDは `email` を基本線として実装してよい
- ただし「現物から確認できた事実」と「実装判断としての採用」は最後に分けて明記する
- 認証は `email + password`
- パスワードは `BCrypt`
- 認証状態は Spring Security のセッション認証
- 電話番号はログインIDにしない
- `remember-me` や多要素認証は今回は入れない

### 3. ログイン成功後遷移

- `returnTo` がある場合は最優先でそこへ戻す
- `returnTo` がない場合:
  - `LOCAL` ロールは `/app/local-member-home`
  - それ以外は `/app/home`
- 不正な `returnTo` は拒否し、既定ホームへ遷移させる

### 4. SMS 認証

- SMS 認証は登録時のみ使う
- ログイン時の2段階認証にはしない
- `sms_verifications` を履歴・コード検証用に使う
- 成功時に `users.sms_verified = true`
- `sms_verifications.status` のような存在しない列は前提にしない
- 状態は service ロジックで扱う
- `expires_at` を使う
- 有効期限は現行 handoff 前提を優先し、5分を基本とする
- ただし「何分か」は DB から固定値としては確定できないため、その点を最後に明記する
- 再送間隔や試行回数上限は、現物画面を成立させる最小限に留める
- 認証成功後は再利用不可

### 5. 登録途中状態

- 仮登録途中状態はセッションで持つ
- 仮登録専用テーブルは今回は増やさない
- セッション切れ時は登録フロー最初へ戻す

### 6. ロール

- 最低限 `USER`, `LOCAL`, `BRIDGE`, `ADMIN` を扱う
- `BRIDGE` は `ADMIN` ほど強くないが、proposal / okatte / 橋渡し導線では MVP でも重要な関与者として扱う
- `BRIDGE` を後景化しすぎない
- 地域ユーザーは `USER` の属性違いではなく role として扱ってよい

### 7. 公開範囲

- `PUBLIC`: 誰でも見える
- `PRIVATE`: 本人と管理側のみ
- `LIMITED`: ログイン済み全体公開ではなく、関係者判定で見せる
- `LIMITED` は role 公開ではなく、MVPでは「関係者判定」で近似する
- 関係者の最低限:
  - 投稿者本人
  - `proposal / application / chat` の相手方
  - 必要に応じて `BRIDGE / ADMIN`
- `LOCAL` ロールだから自動的に `LIMITED` を見られる、とはしない

### 8. 申請状態とチャット状態

- `proposal_application.status` と `chat_thread.status` は分離する
- `proposal_application` は申請や進行状態
- `chat_thread` は会話路の状態

### 9. チャット開通条件

- chat thread は申請作成時には作成しない
- chat thread は入口ごとの承認条件を満たした時点で作成または開通する
- 入口側:
  - 架け橋さんが対応可能として承認したらチャット開通
- おかって側:
  - 架け橋さんが候補提案
  - ユーザーが選択
  - 申請
  - 地域側が承認
  - その時点でチャット開通
- 承認前の状態管理は `proposal_application` 側で持つ
- chat は「開通後の連絡路」として扱う
- 関係者側の負担を増やさない方向を優先する

### 10. enum の扱い

- DB 保存値は migration に合わせる
- Java enum は `UPPER_SNAKE_CASE` でよい
- 画面表示文言は日本語で別管理
- つまり
  - DB値
  - Java enum
  - 表示文言
  の3層を分ける
- migration が lowercase なら DB も lowercase のまま使う

### 11. URL / 既存導線

- 既存 `HTML / query` 導線を大きく壊さない
- `context=app` や `?tab=footprints` は互換維持してよい
- path variable 化は既存フロントコメントがある箇所から優先
- 一気に URL 再設計はしない

### 12. 公開ページとログイン必須ページ

- 公開:
  - `/`
  - `/gate`
  - story 系閲覧
  - gate 系 proposal の概要閲覧
- ログイン必須:
  - 申請
  - 相談開始
  - コメント投稿
  - reaction
  - わたしの地図
  - `messages / chat`
  - `mypage` 以降の個人設定
  - okatte の候補提案後の関与導線
- okatte は gate と同列の単純公開 proposal 一覧/詳細として読まない
- 「見る」と「関わる」を分ける

### 13. migration 追加の条件

- migration 追加は、現物画面を既存思想の範囲で成立させるのに不可欠なものだけに限る
- 未確定仕様を解決するための migration 追加はしない
- 存在しない列を補う前に、まず既存列と service ロジックで吸収できないか検討する
- migration を増やした場合は、なぜ不可欠だったかを説明する

### 14. 今回は決めないもの

- 追加本人確認の本格状態遷移
- login history / last login 保存仕様の完成形
- notice/notices の最終データ源
- support 詳細フォームの最終仕様
- 画像保存の本番最適化
- `BRIDGE` の詳細運用ルールの完成形
- URL の全面美化

## 使い方

- Java バックエンド実装を Codex / ChatGPT に依頼するときに、`next-chat-handoff.md` と一緒に渡す
- まず handoff を前提共有として使い、この文書で「今回どこまで固定するか」を補強する
- 実装後は、どの前提を確定事項として扱い、どこを仮置きにしたかを必ず照合する

## 実装の進め方

- FURUTABI は機能ごとに分けて一つずつ実装する
- 一括実装は避ける
- まず機能境界・権限・公開範囲・状態遷移を確認する
- 必要に応じて先にテスト観点 / Javadoc 観点 / 実装計画を整理する
- 記述がない機能を、関連しそうという理由だけで先回り実装しない
- 今回の作業範囲を超えるものは、提案はしても勝手に実装しない

## Javadoc 対象ファイルの扱い

- Javadoc の対象ファイルは事前固定しない
- その時点で実在する Java ファイルのうち、今回明示したものだけを対象にする
- まだ存在しないクラスは対象に含めない
- 新しく Java ファイルが増えた場合は、その都度対象一覧へ追加する
- package-info.java も、必要になった既存パッケージだけ都度対象にする

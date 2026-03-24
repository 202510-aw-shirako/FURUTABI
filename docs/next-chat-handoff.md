# FURUTABI Next Chat Handoff

このファイルは、次の ChatGPT / Codex チャットに前提共有するための引き継ぎメモです。

## この文書の扱い

- この文書は固定仕様書ではなく、現時点の前提共有メモとして扱う
- 後で認識が更新されたり、現物ファイルや実装が進んだ場合は変更してよい
- ただし変更する場合は、何をどう変えたかを次のチャットやコミットメッセージ等で明示する
- Codex がこの文書を更新する必要がある場合は、更新前に変更案を提案として明示し、合意後に更新する
- 認識がぶつかったら、まず現物ファイル・migration・既存 Codex 実装を優先して見直す

## 最重要ルール

- 認識がぶつかったら、まずは現物ファイルと Codex 実装を優先して見る
- いまは理想的な再設計より、既存 HTML を基準に Java 実装へつなぐ段階として考えている
- URL の全面再設計や新規ページの大量追加は、現時点では急がない前提で進めている

## まず参照するファイル

1. `furutabi/docs/technical-foundation.md`
2. `furutabi/pom.xml`
3. `furutabi/src/main/resources/db/migration/`
4. `public/`, `app/`, `auth/`, `scss/`, `css/`

## プロジェクト構成

- フロントの既存ワイヤ資産
  - `public/`
  - `app/`
  - `auth/`
  - `scss/`
  - `css/`
- Spring Boot バックエンド
  - `furutabi/`

## バックエンド技術前提

- Java `21`
- Spring Boot `3.5.11`
- Maven
- Thymeleaf
- Spring Security
- MyBatis
- Spring Validation
- Flyway
- 開発DB: H2
- 本番想定DB: MySQL

設定ファイル:

- `furutabi/src/main/resources/application.yml`
- `furutabi/src/main/resources/application-dev.yml`
- `furutabi/src/main/resources/application-prod.yml`

H2 Console:

- `/h2-console`

## DB マイグレーション

既存 migration:

- `V1__init.sql`
- `V2__user_related_tables.sql`
- `V3__create_map_tables.sql`
- `V4__create_story_tables.sql`
- `V5__create_proposal_tables.sql`
- `V6__create_notification_tables.sql`
- `V7__create_chat_tables.sql`
- `V8__create_map_reaction_tables.sql`
- `V9__create_support_tables.sql`
- `V10__create_file_tables.sql`
- `V11__create_sms_verification_table.sql`

場所:

- `furutabi/src/main/resources/db/migration/`

## いまの主要ページ

公開側:

- `public/index.html`
- `public/gate.html`
- `public/gate-entry.html`
- `public/story.html`
- `public/faq.html`
- `public/safety.html`
- `public/safety-complete.html`
- `public/okatte-entry.html`

認証:

- `auth/login.html`
- `auth/register.html`
- `auth/register-sms.html`
- `auth/register-profile.html`
- `auth/register-complete.html`
- `auth/register-verify.html`

ログイン後:

- `app/home.html`
- `app/local-home.html`
- `app/local-member-home.html`
- `app/messages.html`
- `app/chat.html`
- `app/notification-center.html`
- `app/mypage.html`
- `app/account.html`
- `app/profile.html`
- `app/privacy-settings.html`
- `app/notifications.html`
- `app/history.html`
- `app/security.html`
- `app/support.html`
- `app/password-change.html`
- `app/password-change-complete.html`
- `app/account-close.html`
- `app/account-close-complete.html`

## 地域ページの整理

- `public/local.html`
  - 公開の「地域の方へ」ページ
  - ログイン前の地域の方全体向け
- `app/local-member-home.html`
  - 登録済み地域ユーザー向けログイン後ページ
  - 将来、通常のログイン後ホームとは別に育てる前提

## ログイン / セッションに関する前提

- 現在のフロントのログイン状態は、まだワイヤレベルの表示制御が中心
- 本実装では Spring Security のセッション認証に置き換える形が自然
- `app/mypage.html` のログアウトは、今はワイヤとして `auth/login.html` に戻すだけ
- 本実装では Spring Security の logout 処理に差し替える想定

## SMS 認証

いまの前提:

- `users` は仮登録本体
- SMS 認証コードや期限、再送回数などの進行管理は `sms_verifications`
- 1ユーザーにつき、その時点で有効な認証コードは1つだけ

想定状態:

- `PENDING`
- `VERIFIED`
- `EXPIRED`
- `LOCKED`

推奨運用:

- 有効期限: 5分
- 再送間隔: 60秒
- 再送上限: 5回
- 入力失敗上限: 5回

## 履歴ページの考え方

`history.html` は「自分の記録」ではなく **関わりの記録** として扱う方向で考えている。

含めるもの:

- 自分が残した地図記録
- 自分が書いたごひいきさんの足あと
- 自分がした申請
- 承認 / 見送りの履歴
- 自分がしたコメント
- 自分が押したありがとう / いいね

区切り案:

- 時系列
- コメントしたもの
- 反応したもの

時系列に入るもの:

- ごひいきさんの足あと
- わたしの地図
- ちいきの入り口
- ちいきのおかって
- 非公開チェックイン

空状態:

- 過剰に親切な案内は置かず、静かなままでよい

## おかっての公開レベル

- `ちいきのおかって` は、今のところ公開一覧ページとしては扱わない方向
- ログイン前には、おかってのボタン自体を出さない前提
- ログイン後でも誰にでも一覧表示するのではなく、架け橋さんが選んだ候補を提案する形を想定
- 実装上もログイン必須寄りで扱う想定

想定フロー:

- 架け橋さんが本人に合いそうな候補を 3 件程度提案
- 本人が選ぶ
- 地域側が判断する
- OK が出たらチャットをつなぐ

候補がない時:

- `まだありません` より、`準備中` に近い静かなトーンを優先する

## 状態の責務分離

`proposal_application.status`

- `NOT_APPLIED`
- `PENDING`
- `APPROVED`
- `DECLINED`
- `VERIFICATION_REQUIRED`

`chat_thread.status`

- `OPEN`
- `CLOSED`
- `ARCHIVED`

いまの整理:

- 申請の状態は `proposal_application.status`
- 会話部屋の状態は `chat_thread.status`
- 同じ意味の状態を両方に持たせない整理が自然

## 地図UIの現状

### わたしの地図

- 初期表示で、現在見えている記録の中で最新のものを左上カードに表示
- 登録済みピンをクリックすると
  - 左上カードが出る
  - クリック地点のそばに縦並びで `新規作成 / 編集 / 削除`
- 空き場所をクリックすると
  - その場所に下書きピンが立つ
  - クリック地点のそばに `新規作成`
- `地域の方まで` は内部値として `limited`
- `一般に公開` は内部値として `public`

### ごひいきさんの足あと

- `年 / 季節 / 記録タイプ` フィルターあり
- フィルターはカード切り替えではなくピン表示に効く
- 条件変更時、残ったピンの中で最新のものをカード表示
- サイズは `わたしの地図` と同じ体感サイズへ戻している

### ちいきのおかって / ちいきの入り口 詳細地図

- ピンと下カードが連動
- カードにも番号表示あり
- 地図は初期表示サイズを保ちつつ、拡大時に横へ広がれる余地を持たせている

### ちいきの入り口 一覧ページ

- `public/gate.html` にも、`public/gate-entry.html` と同じ考え方の地図 + カード領域を置いている
- 一覧ページの地図とカードは連動する
- `context=app` のときは公開向け案内カードを隠す

### 連絡導線

- `app/home.html` / `app/local-member-home.html` に `連絡（非公開）` セクションあり
- ヘッダー主ナビにも `連絡` ボタンがあり、`app/messages.html` に飛ぶ
- `messages.html` / `chat.html` は同じ連絡導線として扱う

### ログイン後文脈の保持

- ログイン後から公開側ページへ移動するときは、必要な範囲で `context=app` を付けてログイン後文脈を保つ
- 対象の中心:
  - `public/gate.html`
  - `public/gate-entry.html`
  - `public/faq.html`
  - `public/safety.html`
  - `public/okatte-entry.html`
- フッターや一部導線から公開側へ飛んでも、可能な限りログイン前状態へ戻さない方向

## 公開範囲と反映先

- `PRIVATE`
  - 本人中心
- `LIMITED`（表示文言: 地域の方まで）
  - 地域ユーザー向けの `ごひいきさんの足あと` 地図に反映
- `PUBLIC`（表示文言: 一般に公開）
  - 公開側トップ
  - 公開側足あと詳細
  - ログイン後ホームの `ごひいきさんの足あと` 地図
  - 地域ユーザー側の `ごひいきさんの足あと` 地図

補足:

- 地域ページの `わたしの地図` には共有ピンを出さない前提
- 地域向け共有記録は `ごひいきさんの足あと` タブで見る前提
- `LIMITED` はログイン済みユーザー全体公開ではなく、ロール + 案件との関係で判定する前提
- MVP では汎用共有機能まで広げず、案件や関係性に応じてサーバー側で判定する方向

## パッケージ構成方針

今のところは、機能別 + 層分けが自然:

- `com.furutabi.config`
- `com.furutabi.security`
- `com.furutabi.common`
- `com.furutabi.auth`
- `com.furutabi.user`
- `com.furutabi.map`
- `com.furutabi.story`
- `com.furutabi.proposal`
- `com.furutabi.notification`
- `com.furutabi.chat`
- `com.furutabi.support`
- `com.furutabi.file`

各機能配下:

- `controller`
- `service`
- `mapper`
- `dto`
- `domain`

## まだ独立ファイル化していないもの

この handoff 内には要約を入れているが、まだ独立ファイルとしては未作成:

- 権限マトリクス
- 命名辞書

以下はこの handoff 内に暫定版あり:

- URL 一覧
- 画面とテーブルの対応表
- ステータス enum 一覧

必要なら次に Codex 側で別ファイルへ切り出せる。

## 暫定 URL 一覧

現時点では、理想的な再設計 URL ではなく、**今ある HTML を基準**に考えるのがよさそう。

公開側:

- `/` 相当
  - `public/index.html`
- `ちいきの入り口`
  - `public/gate.html`
- `ちいきの入り口 詳細`
  - `public/gate-entry.html`
- `ごひいきさんの足あと 詳細`
  - `public/story.html`
- `FAQ`
  - `public/faq.html`
- `お問い合わせ / 安心と連絡`
  - `public/safety.html`
- `ちいきのおかって 詳細`
  - `public/okatte-entry.html`

認証:

- `ログイン`
  - `auth/login.html`
- `新規登録`
  - `auth/register.html`
- `SMS 認証`
  - `auth/register-sms.html`
- `登録後記入`
  - `auth/register-profile.html`
- `登録完了`
  - `auth/register-complete.html`
- `追加本人確認`
  - `auth/register-verify.html`

ログイン後:

- `ホーム`
  - `app/home.html`
- `地域ログイン後ホーム`
  - `app/local-member-home.html`
- `連絡一覧`
  - `app/messages.html`
- `チャット`
  - `app/chat.html`
- `通知一覧`
  - `app/notification-center.html`
- `マイページ`
  - `app/mypage.html`
- `アカウント`
  - `app/account.html`
- `プロフィール`
  - `app/profile.html`
- `公開範囲とプライバシー`
  - `app/privacy-settings.html`
- `通知と連絡設定`
  - `app/notifications.html`
- `関わりの記録`
  - `app/history.html`
- `ログインとセキュリティ`
  - `app/security.html`
- `安心とサポート`
  - `app/support.html`

補足:

- 主ナビの `わたしの地図` は `#tab-map`
- 主ナビの `ごひいきさんの足あと` は `#tab-footprints`
- `app/home.html` / `app/local-member-home.html` 内でタブ切り替えしている
- `ごひいきさんの足あと` へ戻す導線は、`#tab-footprints` だけでなく `?tab=footprints` も使って安定させている

## 画面とテーブルの対応表

`auth/register.html`

- 主テーブル: `users`
- 目的: 仮登録本体の作成

`auth/register-sms.html`

- 主テーブル: `sms_verifications`
- 関連: `users`
- 目的: SMS 認証の進行管理

`auth/register-profile.html`

- 主テーブル: `user_profiles`
- 関連: `contact_preferences`
- 目的: 初期プロフィールと連絡設定

`app/account.html`

- 主テーブル: `users`
- 目的: アカウント情報更新

`app/profile.html`

- 主テーブル: `user_profiles`
- 目的: 公開プロフィール更新

`app/privacy-settings.html`

- 主テーブル: `contact_preferences`
- 将来追加: 公開範囲デフォルト設定
- 目的: プライバシーと公開範囲系設定

`app/notifications.html`

- 主テーブル: `contact_preferences`
- 目的: 通知と連絡設定

`app/history.html`

- 読み取り対象:
  - `map_records`
  - `story_posts`
  - `proposal_applications`
  - `map_record_comments`
  - `story_post_comments`
  - `map_record_reactions`
  - `story_post_reactions`
- 目的: 関わりの記録の閲覧

`app/support.html`

- 主テーブル: `support_requests`
- 関連: `support_request_status_history`
- 目的: 安心とサポート導線

`app/messages.html`

- 主テーブル: `chat_threads`
- 目的: スレッド一覧の閲覧

`app/chat.html`

- 主テーブル: `chat_messages`
- 関連: `chat_threads`
- 目的: メッセージ送受信

`app/home.html`

- `わたしの地図`
  - `map_records`
  - `map_record_images`
  - `map_record_comments`
  - `map_record_reactions`
- `ごひいきさんの足あと` 地図
  - `story_posts`
  - `story_post_comments`
  - `story_post_reactions`
  - `map_records` の `limited/public` 共有反映

`app/local-member-home.html`

- 地域ユーザー向け `ごひいきさんの足あと` 地図
  - `story_posts`
  - `map_records` の `limited/public` 共有反映

`public/story.html`

- 主テーブル:
  - `story_posts`
  - `story_post_comments`
  - `story_post_reactions`
- 関連:
  - `map_records` の `public` 共有反映

`public/gate-entry.html` / `public/okatte-entry.html`

- 主テーブル:
  - `proposals`
  - `proposal_applications`
- 関連:
  - `chat_threads`
  - `chat_messages`

## ステータス / enum 一覧

`ProposalApplicationStatus`

- `NOT_APPLIED`
- `PENDING`
- `APPROVED`
- `DECLINED`
- `VERIFICATION_REQUIRED`

`ChatThreadStatus`

- `OPEN`
- `CLOSED`
- `ARCHIVED`

`AdditionalVerificationStatus`

- `UNREQUESTED`
- `REQUESTED`
- `SUBMITTED`
- `REVIEWING`
- `APPROVED`
- `RETURNED`
- `REJECTED`

`Visibility`

- `PRIVATE`
- `LIMITED`
- `PUBLIC`

表示文言:

- `PRIVATE` → `本人のみ`
- `LIMITED` → `地域の方まで`
- `PUBLIC` → `一般に公開`

`ReactionType`

- `THANK_YOU`
- `LIKE`

`SupportRequestStatus`

- `RECEIVED`
- `REVIEWING`
- `WAITING_USER`
- `RESOLVED`
- `CLOSED`
- `REJECTED`

`SmsVerificationStatus`

- `PENDING`
- `VERIFIED`
- `EXPIRED`
- `LOCKED`

## 権限の簡易整理

`USER`

- 通常の旅人側
- 地図記録、足あと、申請、反応、コメント、チャット利用

`LOCAL`

- 地域側アカウント全般
- 地域向けログイン後ページあり
- `LOCAL` であること自体が全案件の閲覧権限を意味するわけではない
- 受け入れ責任や閲覧範囲は proposal 単位で持つ
- 本人が選んだ後に、その案件に関係する地域側だけが見られる前提

地域側が見られる情報の例:

- ニックネーム
- 性別
- 年齢層
- 関心
- 関わり方のメモ
- 過去の訪問歴
- 食事制限 / アレルギー
- 配慮事項
- 投稿
- 架け橋さんメモ
- 会った時の印象メモ
- いつ頃行きたいか

地域側が見られない情報の例:

- 実名
- 住所
- メールアドレス
- 通常時の電話番号
- 本人確認情報

`BRIDGE`

- 関係づけと提案の橋渡し
- 提案作成 / 状態変更 / 一部調整
- 管理者より弱い
- 深い個人情報の閲覧や強い管理権限は持たせない

架け橋さんが見られる情報の例:

- ニックネーム
- 性別
- 年齢層
- 登録後に本人が書いた関心やメモ
- 過去の訪問歴
- 関係する `LIMITED` 情報
- 本人と会った時の印象メモ

架け橋さんが見られない情報の例:

- 実名
- 住所
- メールアドレス
- 通常時の電話番号
- 本人確認書類

例外:

- 当日連絡がつかない等、必要時だけ電話番号を扱うことがある

`ADMIN`

- 全体管理
- 非表示、運用判断、監査寄り

補足:

- 少なくとも以下はロールだけでなく関係性でも判定する前提
- `LIMITED` の閲覧
- `ちいきのおかって` 候補閲覧
- `LOCAL` の受け入れ判断
- チャット閲覧

## 主要フロントファイル一覧

共通ナビ / 共通導線:

- `public/assets/scripts/shared-ui.js`

足あと地図:

- `public/assets/scripts/footprints.js`

おかって / 入り口 詳細地図:

- `public/assets/scripts/proposal-detail.js`
- `public/assets/scripts/proposals.js`

ログイン後ホーム:

- `app/home.html`
- `app/local-member-home.html`

スタイル:

- `scss/_base.scss`
- `scss/_legacy.scss`
- `css/style.css`

## 命名の最低辞書

- `わたしの地図`
  - テーブル中心: `map_records`
- `ごひいきさんの足あと`
  - テーブル中心: `story_posts`
- `ちいきの入り口` / `ちいきのおかって`
  - テーブル中心: `proposals`
- `安心とサポート`
  - テーブル中心: `support_requests`
- `連絡一覧 / チャット`
  - テーブル中心: `chat_threads`, `chat_messages`
- `通知`
  - テーブル中心: `notifications`

## 登録後記入の補正方針

今のところ維持する項目:

- 自己紹介
- アイコン画像
- 興味のある地域
- 過去の訪問歴
- 配慮してほしいこと
- 子ども連れかどうか
- 食事制限 / アレルギー
- 関わり方のメモ
- 性別
- 会う可否に関する項目

補足:

- 性別は必要な場面がある想定だが、`回答しない` は維持する
- `居住地の大まかな地域` は、今のところ外す方向

## コメント運用の方向

- コメントは感想 / お礼 / 小さな応答を残すためのもの
- 長い議論や自由掲示板にはしない
- 文字数は 250 文字前後
- 公開コメント欄には長い注意書きを置かない

モデレーション方針:

- 投稿者が望めば、自分の投稿についたコメントを一時非表示できる
- 第三者通報が入った場合も一時非表示
- 最終判断は `ADMIN`
- `BRIDGE` に重いモデレーション負担は持たせない

反応:

- `ありがとう / いいね` は軽い好意の反応
- 追加可
- 取消し可
- 二重押し不可

## ファイル保存の最低限ルール

- MVP では画像のみ許可
- 許可形式: `jpg / jpeg / png / webp`
- 1 ファイル上限: 5MB
- `avatar` は 1 枚
- 投稿画像は 5 枚まで
- ファイル名は UUID 化する前提

## 次チャットでの使い方

まずこのファイルを貼る。  
その後、必要なら以下を追加で渡す形で十分そう。

- `furutabi/docs/technical-foundation.md`
- `furutabi/pom.xml`
- `furutabi/src/main/resources/db/migration/`
- 該当する HTML / JS / SCSS

## 参考コミット

- `a39a048` `Refine map flows and local member wireframes`

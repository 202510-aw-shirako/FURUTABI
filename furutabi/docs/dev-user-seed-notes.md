# Dev User Seed Notes

- `dev` 専用です
- 本番仕様ではありません
- seed 値は仮置きです
- 手動確認用の固定資格情報です

現在の固定資格情報:

- `user@example.com` / `password123`
- `local@example.com` / `password123`
- `bridge@example.com` / `password123`
- `admin@example.com` / `password123`

補足:

- `application-dev.yml` の `app.dev-seed.enabled=true` のときだけ有効です
- `prod` では使いません
- 後で削除するときは `DevUserSeed.java` と `app.dev-seed.enabled` を外せば戻せます

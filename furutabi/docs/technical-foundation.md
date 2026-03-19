# FURUTABI Technical Foundation

## Current baseline

- Backend project: `furutabi/`
- Java: `21`
- Spring Boot: `3.5.11`
- Build: `Maven`
- Template engine: `Thymeleaf`
- Security: `Spring Security`
- SQL mapping: `MyBatis`
- Validation: `Spring Validation`
- Migration: `Flyway`
- Dev DB: `H2`
- Prod target DB: `MySQL`

## Configuration policy

- Shared settings: `application.yml`
- Dev only: `application-dev.yml`
- Prod only: `application-prod.yml`
- `spring.profiles.active` is not fixed in config files

## Current page baseline

Use the existing HTML pages as the implementation baseline for now.
Do not add many new pages only to match a cleaner URL proposal.

### Public / auth pages

- `public/index.html`
- `public/gate.html`
- `public/gate-entry.html`
- `public/story.html`
- `public/faq.html`
- `public/safety.html`
- `public/safety-complete.html`
- `public/okatte-entry.html`
- `auth/login.html`
- `auth/register.html`
- `auth/register-sms.html`
- `auth/register-profile.html`
- `auth/register-complete.html`
- `auth/register-verify.html`

### Logged-in pages

- `app/home.html`
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

## Package structure

Use feature-first packages with layers inside each feature.

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

Each feature can contain:

- `controller`
- `service`
- `mapper`
- `dto`
- `domain`

Recommended detail:

- `domain` for DB-near models
- `domain.enums` or `domain.type` for enums
- split DTOs later into `dto.form` and `dto.view` if the code volume grows

## Agreed implementation direction

### Public vs login-required

Public read:

- top page
- gate list / detail
- story detail
- FAQ
- safety / contact

Login required:

- comment posting
- reactions
- map record creation and editing
- proposal application
- notifications
- chat
- mypage
- privacy updates
- additional verification

### Visibility

- `PRIVATE`
  - owner
  - admin
- `LIMITED`
  - owner
  - admin
  - bridge
  - directly related counterpart for the relevant proposal / application / relationship
- `PUBLIC`
  - visible on the public surface

### Roles

- `USER`
- `LOCAL`
- `BRIDGE`
- `ADMIN`

`LOCAL` is a local-side role in general.
Proposal acceptance is handled as proposal-level responsibility, not as the whole meaning of `LOCAL`.

### Proposal types

- `gate`
  - lighter first step
  - public-facing detail can exist
  - lower application friction
- `okatte`
  - deeper relationship-oriented proposal
  - stronger bridge / local involvement
  - additional verification can be required more often

### Support

- `support_requests` and chat are separated first
- support can later open a chat thread when needed

### File storage

- start with local storage
- keep metadata in `stored_files` and `file_links`
- use UUID-based stored filenames
- treat identity verification files as external-flow assets later

## Screen responsibility notes

- `auth/register.html`
  - provisional user creation
- `auth/register-sms.html`
  - SMS verification only
- `auth/register-profile.html`
  - profile + contact preference setup
- `app/account.html`
  - account information update
- `app/profile.html`
  - public profile update
- `app/privacy-settings.html`
  - privacy-related settings
  - not limited to contact preferences only
- `app/messages.html`
  - thread list read side
  - no main save responsibility for now
- `app/chat.html`
  - message posting
- `app/history.html`
  - primarily "what the current user has done / left / reacted to"

## State ownership notes

- `proposal_application.status` owns application state
- `chat_thread.status` should stay lighter than proposal application state
- recommended chat thread status direction:
  - `OPEN`
  - `CLOSED`
  - `ARCHIVED`

## SMS verification persistence

Adopt `users + sms_verifications`.

- create the user first as provisional
- keep `users.sms_verified = false`
- store verification code lifecycle in `sms_verifications`

Suggested `sms_verifications` responsibility:

- `user_id`
- `phone_number`
- `verification_code_hash`
- `expires_at`
- `used_at`
- `retry_count`

## Notes for future Java implementation

- The current frontend login state is still wireframe-level display/context switching, not a real authenticated session
- Real login state must later be backed by Spring Security session auth
- If design notes conflict with the implemented files or migrations, prefer the implemented project state

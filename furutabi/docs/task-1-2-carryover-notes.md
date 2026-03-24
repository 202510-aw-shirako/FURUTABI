# Task 1 and 2 Carryover Notes

This note records what task 1 and task 2 already established, and what they did not establish.

## Task 1: Startup Preview and Static Assets

Confirmed:

- `GET /preview/login` exists only as a preview route
- `templates/auth/login.html` is a preview connection of the existing wireframe
- `static/css/style.css` and `static/assets/scripts/shared-ui.js` are served by Spring Boot
- `SecurityConfig` permits `/preview/**` and `/assets/**`
- `docs/` is a GitHub Pages mirror and is not the backend source of truth

Not established:

- `/preview/login` is not the real `/login`
- backend `returnTo` handling is not implemented
- preview route does not establish login, role, visibility, proposal, okatte, or chat behavior

Practical reading:

- treat preview code as startup scaffolding
- do not use `docs/` as a Java-side source of truth
- do not read the inline `returnTo` link adjustment in `login.html` as backend behavior

## Task 2: Flyway and Migration Alignment

Confirmed:

- Flyway reads the migrations under `src/main/resources/db/migration/`
- migrations `V1` through `V11` run in the current H2 development setup
- development startup can be checked with the `dev` profile
- H2 console is available in development

Migration set:

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

Main table groups confirmed from migrations:

- account and profile:
  - `users`
  - `user_roles`
  - `user_profiles`
  - `contact_preferences`
- map and footprints:
  - `map_records`
  - `map_record_images`
  - `map_record_comments`
  - `map_record_reactions`
  - `story_posts`
  - `story_post_comments`
  - `story_post_reactions`
- proposal and application:
  - `proposals`
  - `proposal_tags`
  - `proposal_applications`
  - `proposal_application_status_history`
- chat:
  - `chat_threads`
  - `chat_messages`
- support and notification:
  - `notifications`
  - `support_requests`
  - `support_request_status_history`
- files and SMS:
  - `stored_files`
  - `file_links`
  - `sms_verifications`

Important "not present" reminders from current migrations:

- `users.registration_completed` does not exist
- `sms_verifications.status` does not exist
- do not assume extra completion flags or status columns unless they are added later by an explicit migration

## What the Next Tasks Can Safely Rely On

- preview route and static asset serving exist for startup checks
- Flyway and H2 development startup work with the current migration set
- database truth should be read from the migration files first

## What the Next Tasks Should Not Assume Yet

- real login route behavior
- backend `returnTo`
- role-based business behavior
- visibility rules
- proposal, okatte, or chat implementation
- migration-backed fields that are not present in the current SQL

## Suggested Next Focus

If the next task needs backend implementation, prefer moving to:

1. real login connection
2. authentication flow boundaries
3. service-level implementation for business rules


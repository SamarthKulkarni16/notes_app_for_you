# notes_app_for_you

A minimal public journaling Android app. Write, leave — autosaves locally
and to the cloud automatically. No save button, no nav chrome. Each signed-in
user gets their own private notes; nobody else can see them.

## Current status: Phase 1 — Auth

This phase delivers a working sign-in/sign-up flow (email+password and
Google) against a shared Supabase project, with a placeholder home screen
that just confirms "you're signed in." The real write/history screen with
the triple-tap toggle is Phase 3.

## Stack

- Kotlin + Jetpack Compose
- Supabase Auth (`auth-kt` 3.6.0, via the shared Flow Timer Supabase project)
- Google Sign-In via Android Credential Manager (no deprecated
  `GoogleSignInClient`)
- minSdk 26, targetSdk/compileSdk 34

## Local setup

1. Copy `local.properties.example` to `local.properties` (gitignored).
2. Fill in `sdk.dir` (your Android SDK path), plus `SUPABASE_URL`,
   `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID`.
3. Open in Android Studio, let Gradle sync, run on a device/emulator.

The anon key is safe to have in the built app — it is gated by Postgres
Row Level Security (RLS) on the backend, not by secrecy. Never put a
`service_role` key here.

## CI

`.github/workflows/build.yml` builds a signed release APK on every push to
`main` (or manually via "Run workflow"). It needs these repo secrets:

| Secret | Purpose |
|---|---|
| `SUPABASE_URL` | Same value as local.properties |
| `SUPABASE_ANON_KEY` | Same value as local.properties |
| `GOOGLE_WEB_CLIENT_ID` | Same value as local.properties |
| `RELEASE_KEYSTORE_BASE64` | Your release keystore, base64-encoded |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias inside the keystore |
| `RELEASE_KEY_PASSWORD` | Key password |

The built APK is uploaded as a workflow artifact named
`notes-app-release`.

## What's NOT done yet (later phases)

- **Phase 2 — Storage:** `journal_entries` table in the shared Supabase
  project, with RLS so each user only sees their own rows.
- **Phase 3 — Editor:** the actual write screen (blank textarea,
  autosave-as-you-type) and the read-only history list, wired to triple-tap
  on empty space to toggle between them.
- **Phase 4 — Polish + distribution:** loading/error states, adaptive
  launcher icon, landing page + download button on your own site (not
  Play Store).

## Notes on this repo vs. the other "Notes" repo

This is a brand-new Android app, separate from `SamarthKulkarni16/Notes`
(the GitHub-Pages markdown journal that feeds `samarth-brain` via
`sync.py`). They are intentionally unrelated — this app's data lives in
its own Supabase table, never touching your personal `notes`/`memories`
tables or that sync pipeline.

# Kodo — Architecture

Single-module Android app, Kotlin + Jetpack Compose (Material 3), Hilt DI.

```
UI (Compose)
  Inbox / Accounts / Settings / Onboarding  ── ViewModels (Hilt)
        │
  Repository  ── CodeRepository (extract + de-dupe + persist)
        │
  ┌─────┴───────────────────────────────┐
Sources                              Storage
  SMS   User Consent API → receiver → bus   Room (codes)
  Gmail REST API (per account, on-device)   DataStore (settings, accounts)
        │
  Background: WorkManager (GmailSyncWorker, periodic)
  Output: CodeNotifier (high-priority notification with Copy action)
```

## Key pieces
- **CodeExtractor** (`data/extract`): precision-first OTP detection, EN + TR,
  keyword-gated with negative filters (prices, years, order ids). Unit-tested.
- **CodeRepository**: the only writer. Extracts a code, computes a stable
  `dedupeKey` (Gmail message id, or SMS sender+minute) so the same code is never
  stored twice, then persists via Room.
- **SMS**: `SmsUserConsentManager` arms the listener; `SmsUserConsentReceiver`
  forwards the system consent Intent through `SmsConsentBus` to `MainActivity`,
  which launches it and hands the message text to `MainViewModel`.
- **Gmail**: `GmailAuthManager` handles Google Sign-In + per-account
  `GoogleAccountCredential`; `GmailClient` queries recent verification mail and
  decodes the body; `GmailSyncWorker` runs it periodically for every enabled
  account and notifies on new codes.
- **Privacy**: everything is on-device. Backup/data-extraction rules exclude the
  code database. No analytics, no network except Google's own Gmail endpoints.

## Why these choices
- **No SMS permission** → Play-compliant (see PLAY_STORE.md).
- **On-device polling** for Gmail (vs. Pub/Sub push) keeps it serverless and
  private, at the cost of up-to-`interval` latency.
- **Room + DataStore** give reactive Flows the UI observes directly.

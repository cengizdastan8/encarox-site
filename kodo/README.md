<p align="center">
  <img src="brand/logo-wordmark.svg" alt="Kodo" width="360">
</p>

<h3 align="center">Tüm doğrulama kodların, tek kutuda.</h3>
<p align="center">Every verification code — from all your Gmail accounts and SMS — in one inbox.</p>

---

**Kodo** is an Android app that automatically collects one-time / verification
codes (OTP) from multiple Gmail accounts and from SMS, and drops them in front of
you with a one-tap **Copy**. No more hopping between inboxes to find a code.

## Features
- 📥 **One inbox for every code** — Gmail (multi-account) + SMS together.
- 🔔 **Instant notification** with a Copy action the moment a code arrives.
- 🔐 **Privacy-first** — everything stays on your device; no server, no tracking.
- 🏷️ **Smart detection** — recognizes the service (Instagram, Google, banks…),
  works in Turkish and English, ignores prices / order numbers / years.
- 📱 **Play-compliant SMS** via the SMS User Consent API (no SMS permission).

## Tech
Kotlin · Jetpack Compose (Material 3) · Hilt · Room · WorkManager · DataStore ·
Gmail REST API · Play Services SMS User Consent API.

## Getting started
See **[docs/SETUP.md](docs/SETUP.md)** — build, and the one-time Google Cloud /
OAuth setup needed for Gmail.

```bash
./gradlew assembleDebug
./gradlew test
```

## Publishing
Two Google requirements to plan for (SMS policy + Gmail CASA assessment) are
documented in **[docs/PLAY_STORE.md](docs/PLAY_STORE.md)**. Store copy and a
privacy policy are in **[store/](store/)**.

## Architecture
See **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.

## License
See [LICENSE](LICENSE).

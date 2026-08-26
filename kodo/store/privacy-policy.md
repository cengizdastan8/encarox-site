# Kodo — Privacy Policy
_Last updated: 2026-08-26_

Kodo ("the app") is designed to keep your verification codes private.

## What Kodo accesses
- **Gmail (read-only):** with your explicit Google sign-in consent, Kodo reads
  recent messages from the Gmail accounts you connect, solely to detect
  verification/OTP codes. It uses the `gmail.readonly` scope and never sends,
  deletes, or modifies mail.
- **SMS (via SMS User Consent API):** when you arm SMS capture, Android shows a
  system prompt and, only if you approve, shares the text of a single incoming
  message with Kodo so it can extract the code. Kodo does not request the
  `READ_SMS` or `RECEIVE_SMS` permission in its default build.

## What Kodo stores
- Extracted codes and their source metadata are stored **only on your device**
  (local database). You control retention (default 14 days) and can delete any
  code at any time.

## What Kodo does NOT do
- No data is uploaded to any Kodo server — there is no Kodo server.
- No analytics, advertising, or third-party trackers.
- Your email/SMS content is never shared with anyone.

## Network
The only network calls are directly to Google's Gmail API to read your own mail,
authenticated by your Google account.

## Your controls
- Disconnect a Gmail account anytime in Settings → Gmail accounts.
- Codes are excluded from cloud backup by design.
- Uninstalling the app removes all locally stored codes.

## Contact
encarox@proton.me

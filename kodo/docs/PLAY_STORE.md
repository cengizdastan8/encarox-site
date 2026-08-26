# Kodo — Publishing notes & policy constraints

Two Google requirements shape this app. Read before submitting.

## A. SMS — why Kodo does NOT request READ_SMS by default
Google Play's **SMS & Call Log Permissions policy** heavily restricts `READ_SMS`
and `RECEIVE_SMS`. A general "read my OTP texts" app does **not** qualify for the
permitted use cases and would be **rejected** unless the app is the device's
default SMS handler.

**Kodo's compliant default:** the **SMS User Consent API**
(`play-services-auth-api-phone`). It needs **no SMS permission**. When armed, the
next incoming code SMS triggers a one-tap system consent sheet; on approval Kodo
reads just that one message. This passes review.

**Optional "power mode" (personal / sideload only):** if *you* want fully
automatic, zero-tap SMS capture, uncomment the `RECEIVE_SMS` / `READ_SMS` lines in
`AndroidManifest.xml` and add a `SmsReceiver`. This build will **not** pass normal
Play review without an approved Permissions Declaration — keep it for your own
sideloaded APK.

## B. Gmail — restricted scope verification (CASA)
`gmail.readonly` is a **restricted scope**. To ship on Play to the public you must:
1. Verify the OAuth consent screen (brand verification).
2. Pass an annual **CASA** third-party security assessment.
This typically takes ~6–12 weeks and has a cost for the security assessment.

**Until then** you can use the app fully with up to **100 test users** (your own
Gmail accounts) added in the OAuth consent screen — no assessment needed. That is
enough for personal use and closed testing.

## C. Data safety form (Play Console)
- Kodo stores codes **only on the device**; nothing is sent to a Kodo server.
- Gmail/SMS content is processed on-device for the sole purpose of surfacing codes.
- Declare: no data shared with third parties; data not collected off-device.
- Link the privacy policy in `store/privacy-policy.md` (host it at a public URL).

## D. Listing assets
- Icon: export `brand/logo-mark.svg` → 512×512 PNG.
- Feature graphic: export `brand/feature-graphic.svg` → 1024×500 PNG.
- Screenshots: capture Inbox, Accounts, Settings on a device (min 2, phone).
- Copy: `store/play-listing.md`.

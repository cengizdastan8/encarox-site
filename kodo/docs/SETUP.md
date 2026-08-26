# Kodo — Developer setup

## 1. Prerequisites
- Android Studio (Koala / 2024.1+), JDK 17.
- An Android device or emulator on API 26+.

## 2. Open & build
```bash
# from the kodo/ folder
./gradlew assembleDebug          # build the debug APK
./gradlew installDebug           # install on a connected device
./gradlew test                   # run unit tests (CodeExtractor)
```
The Gradle wrapper (8.9) is checked in, so no local Gradle install is needed.

## 3. Google Cloud / OAuth (required for Gmail)
Gmail reading uses OAuth. You must create your own OAuth client — none is committed.

1. Go to <https://console.cloud.google.com/> and create a project **"Kodo"**.
2. **APIs & Services → Enable APIs → Gmail API** → Enable.
3. **OAuth consent screen**:
   - User type: External.
   - Add the scope `https://www.googleapis.com/auth/gmail.readonly` (restricted).
   - Add your Gmail accounts as **Test users** (up to 100 while unverified).
4. **Credentials → Create OAuth client ID → Android**:
   - Package name: `com.encarox.kodo` (use `com.encarox.kodo.debug` for debug builds).
   - SHA-1: get it with
     ```bash
     ./gradlew signingReport      # copy the SHA1 of the debug (and release) keystore
     ```
   - Create one client per SHA-1 (debug + release).
5. No client secret or JSON goes into the app for an Android OAuth client — Google
   ties the grant to the package name + SHA-1. Just install and sign in.

## 4. Signing for release
Create a keystore and set it in `~/.gradle/gradle.properties` or a `signingConfigs`
block (not committed). Then:
```bash
./gradlew bundleRelease          # produces the .aab for Play
```

## 5. Test the flow
- **Gmail**: Settings → Gmail accounts → Add account → pick account → grant the
  read-only Gmail consent. Codes from the last day appear within one sync cycle
  (default 15 min) or immediately if you trigger a sync.
- **SMS**: Settings → "Listen for the next SMS code", then send yourself a code
  SMS. Android shows a one-tap consent sheet; approve it and the code lands in Kodo.

See `PLAY_STORE.md` for the two policy items you must plan for before publishing.

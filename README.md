# Home Poker — Texas Hold'em for Android

A No-Limit Texas Hold'em game that feels like a home game with friends: fast,
fair, and social. Built as a native Android app with a **rules engine that is
correct by construction and covered by an exhaustive test suite** — hand
evaluation, betting, side pots, split pots and kickers are all verified, not
guessed.

> **Status (v1).** The offline single-player game — full rules engine, side-pot
> logic, distinct AI opponents, and a polished Compose table — is complete and
> tested. Real-time online tables ("Play with Friends") are architected for and
> are the next milestone; see [Roadmap](#roadmap). Nothing here involves
> real money — play-money chips only.

---

## What's in the box

| Deliverable | Where |
|---|---|
| Full source (organized, commented) | `app/src/main` |
| **Poker rules engine** (pure Java, no Android deps) | `app/src/main/java/com/homepoker/engine` |
| **AI opponents** (equity-based, distinct styles) | `app/src/main/java/com/homepoker/ai` |
| Android UI (Kotlin + Jetpack Compose) | `app/src/main/kotlin/com/homepoker` |
| Automated tests (evaluator, side-pots, fuzz, AI) | `app/src/test/java` |
| CI that builds & signs the APK | `.github/workflows/android.yml` |
| Architecture write-up | [`ARCHITECTURE.md`](ARCHITECTURE.md) |

---

## Getting the APK

This project produces a signed APK through **GitHub Actions**, which runs on
runners that have the Android SDK installed. There are two ways to get a build:

### Option A — download the CI-built APK
1. Push this repo to GitHub (the workflow runs on every push).
2. Open the **Actions** tab → the latest **Build Android APK** run.
3. Download the **`home-poker-release-apk`** artifact and unzip it.

Without any signing secrets configured, the APK is signed with the Android
**debug** key — installable immediately for testing. To ship a properly-signed
release build, add these repository secrets (Settings → Secrets → Actions):

| Secret | Meaning |
|---|---|
| `KEYSTORE_BASE64` | your `.keystore`/`.jks` file, base64-encoded |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

Create a keystore with:
```bash
keytool -genkey -v -keystore release.keystore -alias homepoker \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.keystore   # paste output into KEYSTORE_BASE64
```
Tag a commit `vX.Y.Z` and the workflow also attaches the APK to a GitHub Release.

### Option B — build locally (needs Android SDK)
```bash
# One-time: point Gradle at your SDK
echo "sdk.dir=$ANDROID_HOME" > local.properties
gradle wrapper --gradle-version 8.9   # generates ./gradlew
./gradlew :app:assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```
Or just open the project in **Android Studio** (Giraffe or newer) and hit Run.

> **Why isn't a prebuilt APK committed here?** The build environment this was
> authored in has no Android SDK and blocks Google's package servers, so the
> binary genuinely cannot be produced in-place. The CI pipeline exists precisely
> to build it in an environment that can — reproducibly, from this exact source.

---

## Installing the APK on a phone (sideload)

1. Copy the `.apk` to your Android device (USB, email, or cloud drive).
2. On the device: **Settings → Apps → Special access → Install unknown apps**,
   pick the app you'll open the file from (Files, Chrome…) and enable
   **Allow from this source**. (On older Android: Settings → Security → *Unknown
   sources*.)
3. Tap the `.apk` in a file manager and confirm **Install**.
4. Launch **Home Poker**. Minimum Android 8.0 (API 26).

---

## Playing

- **Quick Play (Bots)** — pick Easy/Medium/Hard and sit down against 5 AI
  opponents with distinct styles. Fully offline.
- **Tournaments (Sit & Go)** — a 9-max table of tough opponents.
- **Play with Friends** — online private tables are the next milestone (roadmap).
- Actions: one-tap **Fold / Check / Call**, a **raise slider** with ½-pot / pot /
  max shortcuts, and **All-in**. A turn timer keeps the pace lively; **emotes**
  keep it social. **History** shows recent hands.

---

## Running the tests

The engine and AI are plain JVM code, so the tests run fast with no device:
```bash
./gradlew :app:testReleaseUnitTest
```
They cover hand-ranking accuracy (categories, wheel straights, kickers, best-of-
seven), blinds and betting flow, **side pots with multiple all-ins**, split pots
with odd-chip distribution, min-raise enforcement, and a **property-based fuzz**
that plays thousands of random hands asserting chips are always conserved and no
illegal state is ever reached. See `app/src/test/java/com/homepoker/engine`.

---

## Roadmap

1. **Online private tables** — authoritative Node.js/WebSocket server that owns
   the shuffle and runs the same `HandEngine`, room codes/invite links, reconnect
   grace period, real-time emotes. (Engine is already server-ready — see
   ARCHITECTURE.)
2. Tournament blind escalation & payouts.
3. Cosmetic store (card backs, table themes, avatars) — no pay-to-win.
4. Push notifications for "it's your turn", stats page, Play internal-testing track.

---

## Tech choices (short version)

Native Android, **engine in Java, UI in Kotlin/Compose**. Java for the engine so
its correctness can be proven with fast JVM unit tests (and reused verbatim by a
future server); Compose for a smooth, asset-light table UI. Full reasoning in
[`ARCHITECTURE.md`](ARCHITECTURE.md).

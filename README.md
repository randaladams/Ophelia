# Ophelia

A one-deck solitaire card game, as taught by Grandma.

## How to play

- Ace counts 1, number cards count their face value, Jack 11, Queen 12, King 13.
- Turn over one card at a time. A King is 13 on its own and goes straight to the pile.
- If the card makes 13 with, or matches, the top or bottom card of the line, both go to the pile. Tap the two cards, or drag one onto the other. While you drag, every card you can drop on grows, and letting go near one is enough.
- Otherwise the card is laid at the bottom of the line. Cards are never laid at the top.
- Whenever the top and bottom cards, or the two cards side by side at either end, make 13 or a pair, they go to the pile too. This can chain.
- You win if the line is empty when the deck runs out. When the deck is empty and matches are still waiting at the ends, clear them, then tap **Finish game**.

When a match is waiting, the phone buzzes twice and the cards glow green. The game plays sideways or upright.

When a game ends, the results screen lists any matches you missed along the way.

Under the line, a **card count** chart shows every card: still in the deck, in the line, in your hand, or on the pile, with pile and left counts for each suit and a count of what's left of each rank.

Turn the phone upright and the table rearranges: the line fills the middle of the screen and shows many more cards at once, with your card and the buttons at the bottom.

The ⚙ button has these settings: **Real deck** turns off every buzz, glow and hint so you spot matches yourself; **Card count** shows or hides the chart; **Screen** chooses whether the table turns with the phone or stays sideways or upright.

## Getting the app

Every push to `main` builds the app on GitHub. This link always downloads the newest sideload build:

**https://github.com/randaladams/Ophelia/releases/latest/download/Ophelia.apk**

Open it on your Android phone or tablet, then open the downloaded file to install. Android will ask you to allow installing apps from your browser or file manager the first time.

## App details

- Package name: `com.adamselite.ophelia`
- Runs on Android 8.0 (API 26) and newer, phones and tablets.
- Targets API 36, as Google Play requires for new apps and updates since August 31, 2026. Compiled against API 37.
- No ads.

## Builds

`.github/workflows/build.yml` runs on every push and pull request:

1. Validates the Gradle wrapper.
2. Builds a release APK signed with the sideload test key and publishes it as a GitHub release. The version code is the GitHub run number, so each build installs over the last.
3. If the Play upload key secrets exist, also builds a signed `.aab` for Google Play and keeps it with the run as the `Ophelia-play-bundle` artifact.

Any Gradle deprecation warning, any deprecated Java or Android API, and any call too new for the minimum Android version (lint `NewApi`) fails the build.

`.github/workflows/refresh-wrapper.yml` regenerates the wrapper jar and scripts whenever the wrapper version changes, so they always match. `.github/dependabot.yml` proposes updates to Gradle, the Android plugin, libraries and GitHub Actions every week.

### Signing keys

The **Play upload key** is never committed. Add it in the repository's Settings > Secrets and variables > Actions:

| Secret | Value |
|---|---|
| `PLAY_UPLOAD_KEYSTORE_BASE64` | The upload keystore file, base64-encoded (`base64 -w0 upload.jks`) |
| `PLAY_UPLOAD_KEYSTORE_PASSWORD` | The keystore password |
| `PLAY_UPLOAD_KEY_ALIAS` | The key alias |
| `PLAY_UPLOAD_KEY_PASSWORD` | The key password |

Create the upload key once, on your own computer, and keep a backup:

```
keytool -genkeypair -keystore upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
```

The **sideload test key** (`app/sideload-test.p12`) is committed on purpose so every sideload build installs over the last one. It is only for test builds and is not used for Google Play. To replace it with a private one, add `SIDELOAD_KEYSTORE_BASE64`, `SIDELOAD_KEYSTORE_PASSWORD`, `SIDELOAD_KEY_ALIAS` and `SIDELOAD_KEY_PASSWORD` secrets.

## Project layout

- `app/src/main/assets/index.html` holds the whole game: the rules, the table and the cards.
- `app/src/main/java/com/adamselite/ophelia/MainActivity.java` shows the game full screen, handles Back, vibration and screen turning.
- `gradle/libs.versions.toml` lists every plugin and library version in one place.

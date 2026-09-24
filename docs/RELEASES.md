# Releases and APK Installation

## Current build

| Field | Value |
|---|---|
| App version | `1.1.1` |
| Android version code | `7` |
| Artifact | `JI-Product-Adviser-1.1.1.apk` |
| Size | `4,970,012 bytes` |
| SHA-256 | `BB97B1EA7AC560597552A3D0FBEC818B8D528F967FB176D9F51B28AD29A6D13A` |
| Signing | Android debug certificate; optimized/minified test build |
| Upgrade compatibility | Certificate matches the `1.0.4` test APK |

The APK is intentionally not committed to the source repository. Generated binaries are attached to GitHub Releases instead. This keeps source history small and separates reviewable code from distributable builds.

The `1.1.1` artifact is optimized and much smaller than the earlier debug package, but it still uses a debug certificate. It is suitable for testing and internal distribution, not Play Store production publishing.

## Installing the test APK

1. Copy or download the APK onto the Android tablet or phone.
2. Open the APK from Files or Downloads.
3. If Android asks, allow that file manager or browser to install unknown apps.
4. Select **Install**, then open **JI Product Adviser**.

An APK is the Android install file; there is no Windows `.exe` for this project.

## Verifying the download

Before distribution, compare the file's SHA-256 checksum with the value recorded above. If it differs, do not install the file until its origin is confirmed.

## Previous test build

| App version | Artifact | SHA-256 |
|---|---|---|
| `1.1.0` | `JI-Product-Adviser-1.1.0-test.apk` | `D8B82D1D20ACB75FF0370FFD2B16F5673355A8A41258111F11A2FB891263FC6B` |
| `1.0.4` | `JI-Product-Adviser-1.0.4-debug.apk` | `43ABE013C23EC798399114AB3BAC32A2DCF52D33601E2726C1F9304351419117` |

## Creating a production release

1. Create or select a JI Telecom-owned Android signing keystore.
2. Keep the keystore and its passwords outside this repository.
3. In Android Studio, select **Build > Generate Signed Bundle / APK**.
4. Choose APK or Android App Bundle, select the release variant, and sign it.
5. Test installation and core workflows on the target tablets.
6. Record the version, checksum, signing type, and release notes here.
7. Attach the signed artifact to a GitHub Release or distribute it through the approved company channel.

## Release history

See [`CHANGELOG.md`](../CHANGELOG.md) for the user-visible history from `1.0.1` through `1.1.1`.

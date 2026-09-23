# Releases and APK Installation

## Current build

| Field | Value |
|---|---|
| App version | `1.0.4` |
| Android version code | `5` |
| Artifact | `JI-Product-Adviser-1.0.4-debug.apk` |
| SHA-256 | `43ABE013C23EC798399114AB3BAC32A2DCF52D33601E2726C1F9304351419117` |
| Signing | Android debug certificate |

The APK is intentionally not committed to the source repository. Generated binaries are better attached to a GitHub Release or shared through an approved Drive folder. This keeps the repository small and avoids confusing source history with distributable builds.

## Installing the test APK

1. Copy or download the APK onto the Android tablet or phone.
2. Open the APK from Files or Downloads.
3. If Android asks, allow that file manager or browser to install unknown apps.
4. Select **Install**, then open **JI Product Adviser**.

An APK is the Android install file; there is no Windows `.exe` for this project.

## Verifying the download

Before distribution, compare the file's SHA-256 checksum with the value recorded above. If it differs, do not install the file until its origin is confirmed.

## Creating a production release

1. Create or select a JI Telecom-owned Android signing keystore.
2. Keep the keystore and its passwords outside this repository.
3. In Android Studio, select **Build > Generate Signed Bundle / APK**.
4. Choose APK or Android App Bundle, select the release variant, and sign it.
5. Test installation and core workflows on the target tablets.
6. Record the version, checksum, signing type, and release notes here.
7. Attach the signed artifact to a GitHub Release or distribute it through the approved company channel.

## Release history

See [`CHANGELOG.md`](../CHANGELOG.md) for the user-visible history from `1.0.1` through `1.0.4`.

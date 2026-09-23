# JI Product Adviser

JI Product Adviser is a tablet-first Android staff tool for browsing the JI Telecom product lineup, checking whether a device can run selected software, and finding suitable products using clear, deterministic rules. It is designed to work offline and remains usable on phones.

**Current documented release:** `1.1.0` (`versionCode 6`)

> The bundled product catalog comes from **Product-Line-up-x-July-2026.xlsx**. Prices and availability are a dated snapshot. Printers are intentionally excluded.

## What the app can do

- Browse and search the offline product catalog
- Filter products by category, brand, price, and availability
- View enriched laptop CPU and GPU information
- Check software compatibility with platform-aware Windows, macOS, Android, and iOS rules
- Distinguish minimum, recommended, unsupported-platform, and not-verified results
- Find best matches using budget, performance, capacity, preference, and software-fit scoring
- Search, filter, sort, favorite, revisit, and compare products without losing navigation state
- Explain when an app is unavailable for a platform separately from insufficient hardware
- Use a responsive interface optimized for landscape tablets and adaptable to phones
- Import, export, back up, and safely update the local catalog

## Project documentation

- [Development journal](docs/DEVELOPMENT-JOURNAL.md)
- [Version history](CHANGELOG.md)
- [Release and APK notes](docs/RELEASES.md)
- [Contributing and data-update guide](CONTRIBUTING.md)

## Technology and requirements

- Kotlin, Jetpack Compose, Material 3, Navigation Compose
- MVVM and clean `data` / `domain` / `ui` separation
- Room/SQLite, Coroutines, Flow/StateFlow, Hilt
- Retrofit/OkHttp, WorkManager, Preferences DataStore
- Minimum Android 9 (API 28); compile/target Android 16 (API 36)
- JDK 17 and Android SDK 36

## Build

Open the root folder in Android Studio, allow Gradle sync to finish, then run the `app` configuration. From a terminal:

```text
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

For a signed release APK, use **Build > Generate Signed Bundle / APK** in Android Studio, select APK, and use a JI Telecom-owned keystore. Signing credentials are intentionally not included in this repository.

## Architecture

- `data/local`: normalized Room entities, relations, DAOs and metadata
- `data/repository`: domain mapping and local repository implementations
- `data/importexport`: JSON/ZIP import, CSV validation, safety backups, and transactional replacement
- `data/remote`: configurable manifest client and constrained WorkManager updater
- `data/preferences`: theme, update source and local admin-PIN preferences
- `domain/compatibility`: pure Kotlin component evaluators and overall engine
- `domain/recommendation`: budget filtering, configurable weighted scoring and explanations
- `domain/repository`: UI-independent repository contracts
- `ui`: Compose screens and MVVM state holders

The core engines have no Android UI dependency and are covered by local unit tests.

## Database

Products refer to normalized processor and GPU tables. Software is version-aware; each software record can have one `MINIMUM` and one `RECOMMENDED` requirement record. Important records carry source and verification metadata. CPU/GPU tiers are explicitly internal classifications from 1–7 and are editable—not manufacturer ratings.

Unknown hardware, absent minimum requirements, or unverified source records result in `NOT_VERIFIED`; missing information never becomes a positive result. A below-minimum component always results in `BELOW_MINIMUM`.

### Add a product

Unlock **More > Admin**, then add the processor/GPU first if needed. Add the product with a unique SKU and select those normalized records. New manual records start unverified. Add official source and verification details through an import package when they have been checked.

### Add software and requirements

Create a version-specific software record, then import its minimum/recommended requirements with official source and verification metadata. Different versions remain separate records. Do not copy requirements between versions without checking the publisher’s official documentation.

## Compatibility methodology

Software entries declare the platforms on which they are actually available. The compatibility engine first checks platform support, then evaluates CPU tier/specific allow-list, GPU tier/specific allow-list, VRAM, RAM, storage, OS version, architecture, and required features. This prevents a macOS device from being failed merely because a Windows-only requirement says “Windows 10,” and it avoids implying that desktop-only apps exist on Android or iOS.

Component results are aggregated conservatively:

1. App is unavailable on the product platform → `UNSUPPORTED_PLATFORM`
2. Any stored minimum failure → `BELOW_MINIMUM`
3. Missing required facts or non-verified data → `NOT_VERIFIED`
4. Minimum passes but a recommendation is not met or is absent → `MEETS_MINIMUM`
5. Every stored recommended requirement passes → `MEETS_RECOMMENDED`

Results describe stored requirements and expected suitability; they never guarantee performance.

## Recommendation methodology

Candidates are first filtered by category, availability, brand and the absolute budget. A user-enabled allowance can include products up to the configured percentage above budget. Remaining products receive a configurable weighted score:

- Software compatibility 40%
- Performance tiers 20%
- Budget fit 20%
- RAM/storage 10%
- Preferences 10%

The displayed integer is labeled an **internal recommendation score**, not a probability. Results include strengths, limitations and per-software compatibility.

## Verification status

The current implementation has automated tests for the compatibility, platform, recommendation, catalog, and release-safety rules. The `1.1.0` build is checked with unit tests, Android lint, APK assembly, and Android signature verification. See [release notes](docs/RELEASES.md) for the recorded artifact checksum.

## Import, export and updates

Admin can import a full JSON file or a ZIP containing `metadata.json`, `products.json`, `processors.json`, `gpus.json`, `software.json`, and `requirements.json`. JSON exports use a single `DatabasePackage` object with those arrays plus local analytics. Required validations include duplicate SKU, duplicate software version, foreign-key references, missing RAM, invalid prices and invalid requirement values.

Imports follow this sequence: parse → preview validation → internal safety backup → single Room transaction → commit. Exceptions roll back the entire transaction. Admin exports omit the PIN and other credentials.

The optional remote URL points to a JSON manifest:

```json
{
  "databaseVersion": "2026.09.22.1",
  "minimumAppVersion": "1.0.0",
  "downloadUrl": "https://example.invalid/JI_PRODUCT_DATABASE_2026_09.zip",
  "checksum": "lowercase-sha256",
  "releaseNotes": "Verified catalog update",
  "publishedAt": "2026-09-22T00:00:00Z"
}
```

The updater is provider-neutral, checks network constraints and app version, verifies SHA-256, and uses the same safe import path. The local database continues to operate if the URL is blank, invalid, or offline.

The CSV template is in `app/src/main/assets/product_import_template.csv`. CSV preview validates catalog references; the versioned JSON/ZIP package is the recommended way to move the complete relational catalog.

## Backup, privacy and analytics

Exports contain catalog records and anonymous on-device usage counters only. No data is transmitted in V1. Availability is a manually maintained catalog label and is not live branch inventory. Local PIN protection is a convenience barrier and is not presented as enterprise security.

## Future integration

`AiQueryParser`, `InventoryProvider`, and `AnalyticsSink` are domain-facing extension interfaces. A future ASK JIPOL parser may convert natural language into a `CustomerRequest`, but it must never create specifications or decide compatibility. API keys must be held outside the APK and the deterministic local engines remain authoritative.

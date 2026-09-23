# Contributing

## Before making a change

- Keep the app useful without an internet connection.
- Do not add printers unless the product scope is explicitly changed.
- Do not commit APKs, signing files, passwords, API keys, or machine-specific Android SDK paths.
- Preserve the distinction between verified, unverified, and unknown product data.

## Product and software data

- Prefer official manufacturer or software-publisher sources.
- Record source and verification metadata for important specifications.
- Keep software versions separate when their requirements differ.
- Declare supported operating-system platforms before adding requirements.
- Never treat missing specifications as a successful compatibility result.

## Code changes

Run the following before opening a pull request:

```text
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Update automated tests when compatibility or recommendation rules change. Update `CHANGELOG.md` and the release documentation for user-visible changes.

## Commit guidance

Use short messages that describe the outcome, for example:

- `Fix tablet navigation state`
- `Add platform-aware software compatibility`
- `Document version 1.0.4 release`

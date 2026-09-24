# Development Journal

## Project goal

Build a production-ready, standalone Android application that helps JI Telecom staff answer three common customer questions quickly:

1. What products are available within the customer's needs and budget?
2. Can a selected product run a particular application?
3. Which products are the best matches, and why?

The app is offline-first because store connectivity cannot always be assumed. Compatibility and recommendations are deterministic so that staff can inspect the reasons behind every result.

## Source data and scope

The starting catalog was supplied as `Product-Line-up-x-July-2026.xlsx`. The workbook was cleaned and mapped into normalized product, processor, and graphics records. Printers were intentionally omitted from the application scope.

Prices and availability remain dated catalog values rather than live branch inventory. Specifications that cannot be confidently established should remain unverified instead of being guessed.

## Development timeline

### Phase 1 — Foundation and offline data

- Created the Kotlin and Jetpack Compose Android project.
- Separated data, domain, and user-interface responsibilities.
- Added a Room database for products, processors, GPUs, software, requirements, and metadata.
- Imported the supplied lineup and added safe JSON/ZIP import, export, preview validation, backup, and rollback behavior.
- Added a configurable remote update manifest without making the app dependent on a server.

### Phase 2 — Product discovery and navigation fixes

- Built the Home, Products, Can It Run, Recommend, and More destinations.
- Corrected bottom-navigation state so each item opens the expected screen.
- Corrected Home and search actions that previously appeared tappable but did not navigate.
- Connected product filters and search text to the catalog results.
- Repaired the Find Best Matches action and exposed recommendation explanations.

### Phase 3 — Laptop specification enrichment

- Added normalized CPU and GPU information for laptop models.
- Displayed the enriched information in product details.
- Reused the same normalized records in compatibility and recommendation calculations.
- Kept uncertain specifications reviewable rather than silently treating them as facts.

### Phase 4 — Platform-aware compatibility

The original operating-system check could incorrectly compare a Mac against a Windows-only rule. It also did not clearly represent applications unavailable on phones. The model was changed so software declares its supported platforms.

The engine now follows this order:

1. Check whether the application exists on the product's platform.
2. If supported, evaluate the platform-specific operating-system requirement.
3. Evaluate CPU, GPU, VRAM, memory, storage, architecture, and required features.
4. Aggregate the result conservatively and explain any missing information.

This separates “the hardware is too weak” from “this application is not offered for this operating system.”

### Phase 5 — Tablet-first interface

- Increased information density without crowding important actions.
- Improved card hierarchy, spacing, result colors, and compatibility explanations.
- Optimized the experience for landscape tablets used at a counter.
- Preserved adaptive layouts for narrower phone screens.

### Phase 6 — Store polish and reliability

- Added searchable selectors and catalog filters suitable for the full product lineup.
- Preserved screen state when staff switch between primary navigation destinations.
- Separated unavailable software platforms from below-minimum hardware results.
- Corrected recommendation settings and made every displayed priority affect scoring.
- Added favorites, recent products, comparison search, differences-only comparison, and customer-session reset actions.
- Hardened admin workflows with first-run PIN setup, attempt throttling, safer data editors, archive confirmation, and undo.
- Reduced local privacy exposure by recording only submitted searches and excluding DataStore preferences from Android backup.
- Expanded regression coverage and automated GitHub verification before release.

### Phase 7 — Compatibility confidence and admin resilience

- Separated the calculated compatibility verdict from the reliability of its source data.
- Preserved `NOT_VERIFIED` for missing decisive facts while allowing complete unverified records to show their calculated minimum/recommended result with a confidence warning.
- Applied confidence penalties during recommendation scoring so uncertain records do not outrank equivalent verified records.
- Distinguished iPhone and iPad platforms, stopped assuming unknown laptops run Windows, and normalized common CPU architecture aliases.
- Added editable and removable hardware/software records and protected Admin from duplicate-entry and file-operation crashes.
- Restricted remote catalog delivery to bounded HTTPS downloads.
- Added explicit catalog corrections only where the source names the capacity, leaving genuinely unknown specifications unset.

## Key design decisions

### Deterministic rules instead of generated answers

The app does not ask an AI service to invent specifications or decide compatibility. Stored evidence and explicit rules produce the result. A future natural-language parser may populate a customer request, but it must not override the local engines.

### Conservative unknown handling with separate confidence

Missing decisive information produces `NOT_VERIFIED`; it never becomes a pass. Source verification is a separate dimension: a complete unverified record retains its calculated result but carries a prominent confidence warning and a recommendation-score penalty. Confirmed minimum failures still override confidence and produce `BELOW_MINIMUM`.

### Transparent recommendation scoring

Recommendations are first restricted by hard constraints such as category, availability, brand, and budget. Remaining candidates receive an internal score based on software compatibility, performance, budget fit, capacity, and preferences. The score is not presented as a probability.

### Local-first administration

Catalog import and export work without a backend. Imports are validated before replacement, a safety backup is created, and the database update is transactional.

## Quality checks

For releases beginning with `1.0.4`, the project quality gate includes:

- Local unit tests for compatibility and recommendation rules
- Android lint checks
- Debug APK assembly
- Android APK signature verification
- Manual review of navigation, search, recommendation, and responsive layouts

## Known limitations and future work

- Availability is not synchronized with branch stock.
- Product and software records need periodic source verification.
- The distributed debug APK is suitable for testing, not a Play Store production release.
- A future signed release should use a JI Telecom-owned keystore kept outside the repository.
- Additional device testing can expand coverage across tablet sizes and Android versions.

## Documentation policy

Each meaningful release should update:

- `CHANGELOG.md` for user-visible changes
- `docs/RELEASES.md` for build and checksum information
- This journal when a design decision or major development phase changes
- Automated tests whenever compatibility or recommendation rules change

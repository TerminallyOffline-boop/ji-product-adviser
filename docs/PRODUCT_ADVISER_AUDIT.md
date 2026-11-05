# JI Product Adviser: Bug and Improvement Audit

**Review date:** 2026-11-05
**Reviewed version:** 1.4.5 (versionCode 18); local implementation prepared as 1.5.0 (versionCode 19)
**Scope:** Static review of navigation, product search, recommendations, compatibility, comparison, catalog data, imports, and backup flows. This report records findings and the local implementation status; it is not a published release.

## Findings

### P1 — Compatibility results do not reflect source-verification status

The compatibility engine marks a result's data status from missing values and platform ambiguity. It does not include the verification status of the product, software, or requirement records. As a result, records marked `NEEDS_REVIEW`, `UNVERIFIED`, or `OUTDATED` can still yield a result presented as meeting minimum or recommended requirements when their fields are populated. The existing unit test explicitly codifies this behavior.

**Impact:** A user can receive a decisive result based on specifications that are present but not reviewed. This is a confidence/provenance problem even when the numeric comparison itself is correct.

**Suggested fix:** Keep compatibility (meets/below/unavailable) separate from evidence confidence. Show the source, last-checked date, and a concise confidence note on the result; do not turn a numeric match into a hardware failure just because its source needs review.

**Evidence:** [`CompatibilityEngine.kt`](../app/src/main/java/com/jitelecom/productadviser/domain/compatibility/CompatibilityEngine.kt#L238), [`CompatibilityEngineTest.kt`](../app/src/test/java/com/jitelecom/productadviser/domain/CompatibilityEngineTest.kt#L27)

### P1 — Two laptop specifications are marked verified while model identity is unresolved

Assignments for `JUL26-LAP-007` and `JUL26-LAP-014` are both marked `VERIFIED`, but their notes say the supplied store model label is shortened and should be checked before verification. Their linked processor and graphics data may belong to a close retail model, not necessarily the exact unit in the JI lineup.

**Impact:** Can It Run and recommendations may use the wrong CPU/GPU for those laptops while treating the values as confirmed.

**Suggested fix:** Confirm the full model/product code against the physical label or exact manufacturer record, then correct the mapping. Until then, mark the mapping for review and avoid presenting its source confidence as verified.

**Evidence:** [`ji_official_specs_2026.json`](../app/src/main/assets/ji_official_specs_2026.json#L1261), [`ji_official_specs_2026.json`](../app/src/main/assets/ji_official_specs_2026.json#L1317)

### P1 — Inventory availability is a dated snapshot, not live stock

The bundled catalog contains 174 products. 159 have inventory snapshot values from July 2026, and their notes say the snapshot is not live branch inventory. The catalog stores `AVAILABLE`/`LIMITED` values that product filters and recommendations consume without an age/expiry check.

**Impact:** A product may still look available or be recommended after its stock status has changed.

**Suggested fix:** Show a visible “stock last updated” date, expire stale availability to `UNKNOWN`, and require a current stock refresh before using an “in stock” filter or making an availability claim.

**Evidence:** [`ji_catalog_july_2026.json`](../app/src/main/assets/ji_catalog_july_2026.json#L15), [`ProductsViewModel`](../app/src/main/java/com/jitelecom/productadviser/ui/AppViewModels.kt#L71), [`RecommendationEngine.kt`](../app/src/main/java/com/jitelecom/productadviser/domain/recommendation/RecommendationEngine.kt#L123)

### P2 — Online product lookup opens external sites; it does not update app results

The product screen’s Google and GSMArena buttons launch a browser. Search results are not imported into the app, compared with the local SKU, or passed into Can It Run. The Google button also appends “phone tablet laptop specifications” for every query, even when the query is clearly about only one product type.

**Impact:** A user can find an online spec page, but the app remains on its old local data; the generic Google suffix can make searches less precise. GSMArena is also a phone lookup destination, not a complete laptop/tablet source.

**Suggested fix:** Keep external links as references, but add a reviewed catalog-update flow that matches exact model codes, displays field-by-field changes and source URLs, and requires approval before saving. Tailor search terms/destinations to the detected product category.

**Evidence:** [`ProductScreens.kt`](../app/src/main/java/com/jitelecom/productadviser/ui/screens/ProductScreens.kt#L81), [`RemoteUpdate.kt`](../app/src/main/java/com/jitelecom/productadviser/data/remote/RemoteUpdate.kt#L45)

### P2 — Quick request recommendations silently ignore unrecognized constraints

The parser recognizes a limited list of categories, budget phrases, user profiles, and known app names. It does not translate common hardware requests such as “16 GB RAM”, “OLED”, “lightweight”, “good camera”, “5G”, or “large battery” into recommendation filters. If part of a sentence is recognized, the search proceeds using those recognized parts while other details are not called out as ignored.

**Impact:** A request such as “₱30,000 laptop with 16 GB RAM and OLED” may return results matching the budget and category but not the requested RAM or display.

**Suggested fix:** Show a request summary before or alongside results: recognized constraints, unsupported details, and any constraints that filtered out all products. Add structured filters for specs already in the database.

**Evidence:** [`RecommendationEngine.kt`](../app/src/main/java/com/jitelecom/productadviser/domain/recommendation/RecommendationEngine.kt#L139), [`RecommendationViewModel`](../app/src/main/java/com/jitelecom/productadviser/ui/AppViewModels.kt#L214)

### P2 — Automatic safety backups have no in-app restore flow

Before a package import, the app saves a private `before_import_*.json` file, then replaces all local product, hardware, software, requirement, analytics, and metadata rows in one transaction. I found backup creation and retention logic but no code path that lists or restores those private safety-backup files. The import preview reduces risk, but it does not let the user recover from importing the wrong valid package.

**Suggested fix:** Add an explicit restore screen for automatic backups, show the backup timestamp and record counts, and provide a second confirmation before restoring. Keep export-to-user-selected-location as a separate option.

**Evidence:** [`ImportExportManager.kt`](../app/src/main/java/com/jitelecom/productadviser/data/importexport/ImportExportManager.kt#L45), [`ImportExportManager.kt`](../app/src/main/java/com/jitelecom/productadviser/data/importexport/ImportExportManager.kt#L64), [`AdminSettingsScreens.kt`](../app/src/main/java/com/jitelecom/productadviser/ui/screens/AdminSettingsScreens.kt#L195)

### P2 — ZIP import has no decompressed-size limit

Manual imports are capped at 25 MB before parsing, but ZIP entries are read fully into memory with `readText()` and there is no per-entry or total expanded-size limit. A small compressed archive can expand substantially.

**Impact:** An unusually large or malicious archive can consume excessive memory and freeze or crash the app during preview/import.

**Suggested fix:** Enforce an expanded-size limit while streaming entries, cap entry count and individual file sizes, reject path/name collisions, and stop parsing immediately on overflow.

**Evidence:** [`ImportExportManager.kt`](../app/src/main/java/com/jitelecom/productadviser/data/importexport/ImportExportManager.kt#L197), [`AdminSettingsScreens.kt`](../app/src/main/java/com/jitelecom/productadviser/ui/screens/AdminSettingsScreens.kt#L202)

### P2 — CSV parser does not support quoted multi-line fields or reject malformed quotes

The parser processes one physical line at a time. It supports commas inside quoted values, but quoted fields spanning multiple lines cannot be represented, and an unclosed quote is not reported as a parse error.

**Impact:** Product notes containing line breaks can be split into multiple records; malformed CSV may produce confusing row-validation errors instead of identifying the parsing problem.

**Suggested fix:** Use a small RFC 4180-compliant parser or implement multiline quoted-record handling and explicit malformed-quote errors.

**Evidence:** [`ImportExportManager.kt`](../app/src/main/java/com/jitelecom/productadviser/data/importexport/ImportExportManager.kt#L213)

### P2 — CPU/GPU comparison percentages can imply more precision than tier scores support

The comparison screen calculates percentage deltas from internal 1–7 CPU/GPU tiers. It does disclose that tier differences are not benchmark claims, but “100% higher tier score” can still read as twice the real-world speed.

**Suggested fix:** For CPU/GPU, show the raw tier difference (for example, “Tier 5 vs Tier 4”) and keep percentages for measurable quantities such as listed RAM, storage, weight, and price. Show missing values as “Not listed” in every comparison row.

**Evidence:** [`CompareSoftwareMoreScreens.kt`](../app/src/main/java/com/jitelecom/productadviser/ui/screens/CompareSoftwareMoreScreens.kt#L139), [`CompareSoftwareMoreScreens.kt`](../app/src/main/java/com/jitelecom/productadviser/ui/screens/CompareSoftwareMoreScreens.kt#L150)

## UI quality-of-life improvements

1. **Use neutral recommendation defaults.** The form starts at a ₱35,000 budget and “Architecture Student,” which can bias the score toward high-tier CPU/RAM products before the salesperson chooses a profile. Start the profile at “Choose a use” and make the starting budget editable or visibly marked as an example.
2. **Add a “what I understood” summary.** Display parsed budget/category/apps/spec constraints and allow quick edits before accepting a result.
3. **Make stale data obvious.** Put “stock checked” and “specs checked” dates beside availability and technical details. Distinguish “online reference opened” from “updated in app.”
4. **Improve empty and blocked states.** When no match exists, show which filters excluded results and offer one-tap relaxations such as “include higher budget,” “remove brand,” or “show devices with incomplete data.”
5. **Make comparison scan-friendly.** Keep selected products in a pinned strip; group rows into performance, display, battery, storage, and price; use “more capacity”/“lighter” labels instead of ambiguous winners.
6. **Add import safety affordances.** Show current vs incoming record counts, duplicate SKUs, likely replacements, and a one-tap backup/restore path before final import.

## Functional improvements

1. **Versioned catalog feed:** Use a user-controlled, versioned JSON/CSV source (for example, a public GitHub-hosted catalog or a maintained sheet export) rather than scraping arbitrary search results. Preserve offline access and show the feed’s last successful sync.
2. **Exact identity matching:** Match manufacturer model code, JI SKU, and hardware variant separately. Do not promote confidence based only on a close retail model name.
3. **Evidence-aware compatibility:** Keep “meets minimum” as the technical comparison, but include evidence confidence and source freshness in the result. Add clear explanations when the exact OS version is unknown.
4. **Better request extraction:** Parse RAM, storage, display type/size, refresh rate, weight, battery, camera, network, and price constraints when those fields exist; report unsupported constraints instead of silently dropping them.
5. **Category-aware compare/search:** Use phone metrics for phones, laptop metrics for laptops, and avoid sending all model searches to one phone-oriented service.
6. **Update reconciliation:** Preview remote changes by SKU and field, retain local price/availability overrides where requested, and offer rollback after sync.

## Test and review status

### Implemented locally for 1.5.0

- Compatibility run labels remain based on the stored technical requirements, while a separate evidence-confidence line now reflects product, software, and requirement verification states.
- `JUL26-LAP-007` and `JUL26-LAP-014` are marked `NEEDS_REVIEW`; bundled specification version is bumped so existing installs can receive the revised assignments.
- Stock status expires to unknown after 14 days without a valid check date. Admin product editing and product CSV imports now accept an explicit check date.
- Admin backup settings lists automatic safety backups and offers a confirmation before restore. Restore creates a safety snapshot first.
- ZIP parsing enforces entry-count and expanded-size caps. CSV parsing supports quoted multiline fields and reports malformed quoting.
- Recommendation profile now starts at “Any use.” Quick requests apply parsed RAM/storage minimums and state supported-but-unapplied display/feature terms.
- CPU/GPU comparison reports tier spreads instead of percentage performance claims.

### Remaining work and limits

- No live inventory database or public feed URL has been supplied. The app remains offline-first; external product lookup does not automatically update local records. Stock refresh still requires staff input or a separately configured catalog feed.
- The model mapping caveats remain until someone confirms those two exact store model codes.
- Unsupported recommendation constraints such as OLED are disclosed but not applied until a corresponding structured field/filter exists.
- Restore UI lists filename, date, and file size; it does not yet show record counts before restore.
- The automatic backup and Room migration need a device/emulator exercise before relying on a production rollout.

- `testDebugUnitTest`, `lintDebug`, and `assembleDebug` passed for the local 1.5.0 changes. The unit report contains 69 tests with 0 failures, 0 errors, and 0 skipped. `git diff --check` reported no whitespace errors (only Git line-ending notices).
- GitHub Actions run [41750290809](https://github.com/TerminallyOffline-boop/ji-product-adviser/actions/runs/41750290809) passed `testDebugUnitTest`, `lintDebug`, and `assembleDebug` for the v1.4.5 release workflow.
- No emulator/device interaction, live inventory sync, or backup restore has yet been exercised for these local changes. Gradle reported one pre-existing deprecated admin icon warning.

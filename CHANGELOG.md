# Changelog

This file records the major development milestones of JI Product Adviser. Dates use the Asia/Manila working date.

## 1.2.1 - 2026-09-26

### Fixed

- Can It Run now bases a decisive result on the specifications required by that app instead of turning the result into “Needs verification” because of a separate source-audit label
- Integrated laptop graphics are evaluated by the reviewed GPU performance tier when they use shared system memory instead of being rejected for not having fixed dedicated VRAM
- The three incomplete HP/GIGABYTE laptop records now include the RAM, storage, operating system, and exact-model details needed for compatibility checks
- Existing v1.2.0 installations receive those improved bundled laptop specifications without overwriting store-maintained prices or admin-customized hardware

### Changed

- Customer product, software, comparison, recommendation, and Can It Run screens no longer show internal source-review badges
- A genuinely missing compatibility-critical field is now labeled “Cannot check — missing spec” instead of the vague “Needs verification”
- Automated coverage now rejects any bundled laptop that lacks CPU, GPU, RAM, storage, OS, or architecture data

## 1.2.0 - 2026-09-26 (local test build)

### Fixed

- Existing installations now receive updated official-source verification states, source labels, and dates without overwriting admin-maintained hardware or pricing
- ColorOS, MagicOS, HiOS, HyperOS, OriginOS, realme UI, and One UI are recognized as Android platforms
- Mobile compatibility no longer invents universal CPU/GPU minimums when an app publisher only documents platform or operating-system availability
- Obsolete bundled mobile comfort tiers are removed safely during upgrade while custom requirements remain untouched

### Added

- A traced specification record for every one of the 161 non-printer products in the July 2026 lineup
- Exact official-source verification for 101 configurations, including current HONOR, OPPO, realme, Samsung, TechLife, TECNO, vivo, Xiaomi, Apple, HP, and GIGABYTE products
- Explicit review reasons for the remaining 60 records when a manufacturer omits the chipset name, the official region differs, or the store variant cannot be matched exactly
- New processor and graphics records for current Snapdragon, Dimensity, Helio, Exynos, UNISOC, Adreno, and Mali platforms

### Changed

- Can It Run explanations now say they compare stored published or reviewed requirements and do not guarantee real-world performance or continued app availability
- Verified products show the official manufacturer source; retailer-only and incomplete records no longer claim manufacturer verification
- Mobile apps use publisher-backed platform/version requirements, including Android 8 or iOS/iPadOS 14 for Roblox Mobile

## 1.1.2 - 2026-09-25 (local test build)

### Fixed

- Operating-system checks now compare versions, so Windows 10 no longer satisfies Windows 11 and older Android or macOS versions no longer pass newer minimums
- An OS family without a stored version now returns Needs verification instead of an unsupported or falsely compatible answer
- Cross-platform apps can store different minimum and recommended requirements for Windows, macOS, Android, iOS, and iPadOS
- Imports reject invalid platform rules, empty requirements, unknown approved hardware IDs, non-positive values, and recommended values below the minimum
- Existing installations upgrade the requirements table safely without deleting local edits

### Added

- A complete Admin requirements manager with edit and delete actions, platform scope, approved CPU/GPU lists, source status, and specification notes
- Expanded product, CPU, GPU, and software editors for OS versions, architecture, tiers, VRAM, storage type, publisher sources, verification details, and other comparison fields
- A Data quality report that identifies missing decisive specifications, sources, verification, invalid platform scopes, and inconsistent requirement levels
- Clear Main blocker, Needs verification, and Main limitation guidance in Can It Run results
- Up to three compatible alternative devices ranked by compatibility and price
- Regression tests for Windows, Android, macOS, platform-specific rules, and data-quality reporting

### Changed

- Admin lists are searchable and show verification status and requirement counts
- Verified products, software, and requirements require their official HTTPS source details before saving

## 1.1.1 - 2026-09-24

### Fixed

- Admin controls no longer crash while their import/export and background-update services are initialized
- Duplicate processor, GPU, software, and requirement entries now show a useful error instead of closing the app
- Manual import/export failures are handled safely, large imports are rejected, and file work runs away from the interface thread
- CPU and GPU approved-model lists are enforced even when no performance tier is stored
- Architecture aliases such as x64, x86-64, AMD64, ARM64, and AArch64 now match correctly
- Unknown platforms are no longer treated as universally compatible, iPhone and iPad support remain distinct, and unknown laptop operating systems are not assumed to be Windows
- Missing required-feature data now produces an unknown result instead of a false minimum failure
- Search analytics no longer double-count product selections, and Home popularity is limited to recommendation events
- Remote catalog downloads now require HTTPS and reject oversized or insecurely redirected files

### Added

- Separate compatibility and data-confidence results, allowing “Meets minimum” and “Needs verification” to be shown together
- Edit and delete controls for processors, GPUs, and software in Admin
- Search and category filters for the software catalog
- Regression coverage for platform separation, approved hardware lists, architecture aliases, missing features, and data-confidence behavior

### Changed

- Unverified but complete records retain their calculated compatibility verdict while receiving a visible confidence warning and a recommendation-score penalty
- Missing decisive specifications still produce `NOT_VERIFIED`; confirmed failures still produce `BELOW_MINIMUM`
- Compare keeps the user's selection order and allows the optional software check to be removed
- Recommendation reset clears every local search and customer input
- Product details now show only technical specifications and specification notes
- Explicit catalog values were recovered for selected configurations without guessing unavailable specifications

## 1.1.0 - 2026-09-23

### Fixed

- The configured above-budget percentage now controls recommendation filtering instead of always using 10%
- Performance, RAM/storage, battery, portability, and customer-profile choices now affect recommendation scoring
- Recommendation results clear automatically after an input changes, preventing stale answers
- Required software that is unavailable or below minimum is no longer recommended as a valid match
- Platform availability now has a dedicated `NOT_AVAILABLE` result instead of being confused with weak hardware
- New products no longer silently receive the first CPU and GPU in the database
- New software is no longer hardcoded to Windows
- Remote update checks now report their actual completion or failure result
- Product search analytics are recorded only after a completed search, not after every typed character
- Android backup exclusions now protect the DataStore file containing the local admin PIN hash
- Safety backups are capped at the five newest files to prevent unbounded storage growth

### Added

- Searchable device and application pickers in Can It Run
- Optional viewing and explanation of apps unavailable on the selected platform
- Product category, brand, price, availability, and performance sorting filters
- Searchable, same-category product comparison with a differences-only view
- Favorites and recently viewed product shortcuts on Home
- Recommendation app search, labeled compatibility results, reset, and Start Another Customer actions
- First-run admin PIN setup, failed-attempt throttling, archive confirmation, and immediate undo
- Platform, operating-system, architecture, VRAM, and tier validation in admin editors
- Import progress feedback, condensed validation errors, and a local-history clearing control
- Additional regression tests for platform availability, custom budget allowances, required software, and priority scoring

### Changed

- Main navigation preserves screen state, filters, inputs, and scroll positions when switching sections
- Product details now distinguish loading from a missing or archived product
- Required catalog URLs must use HTTPS
- The admin default PIN is no longer displayed or accepted; each installation creates its own PIN

## 1.0.4 - 2026-09-23

### Added

- Platform-aware software availability for Windows, macOS, Android, and iOS
- A broader software catalog covering common desktop, creative, productivity, gaming, and mobile use cases
- Dedicated unsupported-platform results instead of misleading operating-system failures
- Responsive tablet layouts, denser information cards, improved spacing, and clearer status colors
- Better landscape behavior while retaining phone support

### Fixed

- macOS products no longer fail only because a stored Windows requirement names Windows 10 or 11
- Mobile devices no longer appear compatible with applications that are not available on Android or iOS
- Compatibility result explanations now separate platform availability from hardware capability

## 1.0.3 - 2026-09-23

### Added

- CPU and GPU details for the laptop catalog
- Normalized processor and graphics records used by compatibility and recommendation scoring

### Changed

- Product detail cards show more useful performance information
- Recommendations use the enriched laptop specifications instead of relying only on model names

## 1.0.2 - 2026-09-22

### Fixed

- Bottom navigation now changes between Home, Products, Can It Run, Recommend, and More correctly
- Home shortcuts and search entry points now open their intended screens
- Product search and filters update the visible catalog
- **Find Best Matches** now runs the recommendation workflow and displays results

### Changed

- Printers are excluded from the catalog as requested
- Product browsing behavior was simplified for in-store staff use

## 1.0.1 - 2026-09-22

### Added

- Initial production-oriented Android project
- Offline Room catalog imported from `Product-Line-up-x-July-2026.xlsx`
- Product browsing, compatibility checks, deterministic recommendations, and local administration
- JSON/ZIP import and export with validation, backup, checksum, and transactional replacement
- Optional provider-neutral remote catalog update workflow
- Unit tests for compatibility and recommendation logic

## Notes

- Catalog price and availability data are snapshots, not live branch inventory.
- Compatibility is an evidence-based guide and is not a performance guarantee.
- New or changed specifications should retain their source and verification status.

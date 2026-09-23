# Changelog

This file records the major development milestones of JI Product Adviser. Dates use the Asia/Manila working date.

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

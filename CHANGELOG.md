# Changelog

This file records the major development milestones of JI Product Adviser. Dates use the Asia/Manila working date.

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

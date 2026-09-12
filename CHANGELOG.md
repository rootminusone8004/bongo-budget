# Change log

All notable changes to this project are documented in this file.

## [1.0.0] - 12-09-2026

### Added
- Initial release of **Bongo Budget** (offline-first personal finance and category quota manager).
- Master Overall Budget pool with live shrinking available balance calculation.
- Daily Spendable Allowance mode with pace-aware daily calculations and one-touch "Toggle All" switch.
- Strict Category Quota constraints: Expense logging is restricted strictly to active budgeted categories.
- Custom Category creation with dynamic keyword-based icon and color palette assignment.
- Category drill-down bottom sheet to inspect individual category transactions and log expenses directly.
- Searchable and filterable transaction ledger with real-time text query and chip filters (All / Expense / Income).
- Complete offline-first Room SQLite persistence (no cloud dependency, no telemetry, zero tracking).
- Material Design 3 AMOLED pure pitch black theme (`#000000`) for high contrast and battery conservation.
- Bangladeshi Taka (`৳`) currency formatting throughout the app.
- Dual-mode JSON data backup and restore via Storage Access Framework (SAF), ShareSheet, and clipboard:
  - **Merge**: Non-destructive append of transactions and budgets.
  - **Replace**: Complete database state restoration.
- Built-in sample/demo data loader and factory data reset.
- Automated GitHub Actions CI/CD pipeline for signed release APK creation and GitHub Releases publishing.
- Comprehensive unit test suite covering budget allocation math, daily calculations, JSON export/import, and models.
- Fastlane integration with automated `test`, `build`, `release`, and `beta` lanes, including bilingual store metadata (English and Bengali).
- Debit-style dashboard balance calculation integrating Budget, Income, and Expense into unified available balance.

### Changed
- Rebranded application identity to **Bongo Budget** (`com.budjet.app`).
- Updated theme palette from blue to Deep Orange (`#E44919` / `#FF7043`) across all surfaces, progress indicators, FAB, and navigation tabs.
- Updated application launcher icon with official brand emblem across all screen densities (adaptive, round, and legacy mipmaps).
- Shortened date formats across the entire app (e.g., `Sep 26` and `Sep 12, 26`).
- Locked application screen orientation strictly to portrait mode to prevent motion-triggered rotation.
- Optimized release builds with R8 code shrinking and resource minification (~2.0 MB APK).

### Fixed
- Bottom navigation tab switching bug resolved using synchronous tag-based FragmentManager transactions.
- CI pipeline keystore decoding robustness across environment variables.

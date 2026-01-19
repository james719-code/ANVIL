# Changelog

All notable changes to the ANVIL project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Centralized Utilities**
  - `PrefsKeys.kt` - Centralized SharedPreferences key management
  - `DesignTokens.kt` - Consistent spacing, sizing, and animation tokens
  - `DateUtils.kt` - Comprehensive date formatting and manipulation utilities
  - `CurrencyUtils.kt` - Philippine Peso formatting utilities
  - `Categories.kt` - Centralized category constants
  - `Logger.kt` - Debug-only logging utility
  - `ShortcutProvider.kt` - Centralized app shortcut management
  - `WorkerScheduler.kt` - Centralized WorkManager scheduling with proper constraints

- **Reusable Components**
  - `PermissionDialog.kt` - Consolidated permission dialog component
  - `PermissionCheckManager.kt` - Single composable for all permission checks

- **Testing**
  - Added `DateUtilsTest.kt` with comprehensive date utility tests
  - Added `CurrencyUtilsTest.kt` with currency formatting tests
  - Fixed `DecisionEngineTest.kt` to use correct API methods and `runTest` for coroutines

- **CI/CD**
  - Added `ci.yml` workflow for lint, build, and test on PRs

- **Localization**
  - Expanded `strings.xml` with all UI strings for future translation support

### Changed
- Refactored `MainActivity.kt` to use centralized utilities
- Updated `TaskViewModel.kt` to use `PrefsKeys` constants
- Updated `DashboardScreen.kt` to use string resources for greetings
- Enabled `buildConfig` in build.gradle.kts for BuildConfig access

### Improved
- Added backoff policies and constraints to background workers
- Added content descriptions for accessibility
- Improved code organization with DesignTokens for consistent styling

## [1.0.0] - 2026-01-XX

### Added
- Initial release of ANVIL
- Task management with daily tasks and hardness levels
- Budget tracking with Cash/GCash dual balance
- Loan management with repayment tracking
- App and link blocking with Accessibility Service
- Bonus task system with grace day rewards
- Dashboard with contribution graph
- Material You theming with light/dark modes
- Home screen widget support
- Quick action shortcuts

---

## Legend

- **Added** for new features
- **Changed** for changes in existing functionality
- **Deprecated** for soon-to-be removed features
- **Removed** for now removed features
- **Fixed** for any bug fixes
- **Security** for vulnerability fixes

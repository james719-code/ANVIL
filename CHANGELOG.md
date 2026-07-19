# Changelog

All notable changes to the ANVIL project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.5.0] - 2026-07-20

### Added
- **Weekly Activity Chart Widget**
  - Designed and integrated a dynamic weekly task completions bar chart (`WeeklyActivityChart`) directly inside the home screen stats widget.
  - Replaced the large 4-card grid layout in the widget with a compact, modern horizontal `QuickStatsRow` for a more premium look.
  - Standardized all widget colors to follow the light/dark Material 3 themes.
  - Resolved `IllegalArgumentException` layout crashes in Android Glance by grouping components into nested columns (staying under the 10-child container limit).
- **Auto-Fill & Suggestion Recovery**
  - Passed `pastEntries` database states into `AddBudgetEntrySheet` and `EditBudgetEntrySheet` so suggestion chips and auto-fills render correctly when launching sheets from both the Budget and Vault Overview pages.

### Changed
- **Bottom Sheet Behavior**
  - Configured `skipPartiallyExpanded = true` on transaction and loan bottom sheets to prevent them from auto-dismissing when the software keyboard appears or when layout sizes change dynamically.
- **Vault FAB Positioning**
  - Removed custom bottom margin padding on the Vault overview FAB to align its location consistently with the Tasks page FAB.
  - Explicitly mapped the FAB colors to solid primary brand colors to prevent fading gray visual overlays.
- **Motivational Quotes Display**
  - Cleaned redundant surrounding double quotation marks wrapper from the daily quote text inside both the Hero Status Card and the Motivation Card.

## [1.4.0] - 2026-07-19

### Added
- **Dedicated About Screen**
  - Introduced a clean "About" screen detailing the application developer, open-source attributes, and project details.

### Changed
- **Overall UI & Navigation Upgrade**
  - Re-themed overall application colors to map to the new modern slate-blue and elevated surface neutral design tokens.
  - Refactored back-navigation behavior across all feature modules to support seamless page exits.

## [1.3.0] - 2026-02-01

### Added
- **VPN-Based Link Blocking**
  - New `AnvilVpnService` that intercepts DNS requests at the network level
  - Blocked domains receive NXDOMAIN response, causing immediate connection failure
  - Works in incognito mode and non-browser apps
  - Toggle in Settings > Blocking section
  - Does not slow down internet (local DNS proxy only)
  - `VpnHelper.kt` utility for easy VPN start/stop

### Changed
- **Selective Incognito Blocking**
  - Incognito mode is now only blocked when an "encrypted" link (`isEncrypted=true`) is active
  - Regular blocked links no longer trigger incognito detection
  - Reduces false positives when browsing in private mode

### Improved
- Settings screen now has a dedicated "Blocking" section for VPN control

## [1.2.0] - 2026-01-31

### Added
- **Streak Rescue System (Ice)**
  - Implemented automatic streak saving mechanism in `MidnightContributionWorker`.
  - If daily tasks are missed, 1 "Ice" is automatically consumed to maintain the streak.
  - Recorded as `streak_freeze` contribution type.

### Changed
- **Bonus Exchange Rates**
  - Increased cost: **5 Bonus Tasks** are now required for 1 Ice (previously 3 for 1 Grace Day).
  - Renamed "Grace Days" to "Ice" throughout the UI.
- **Dashboard UI**
  - Reward card now displays "Ice Available" with 🧊 emoji.
  - Exchange dialog clarified to "Streak Freeze".

## [1.1.0] - 2026-01-21

### Added
- **Schedule-Based Blocking**
  - Block apps/links based on schedule: Everyday, Weekdays, or Custom days
  - Time range support: Block during specific hours (e.g., 8 AM - 6 PM)
  - `BlockSchedule.kt` - Schedule types, day-of-week masks, and time ranges
  - `ScheduleEditDialog.kt` - UI for configuring blocking schedules
  - Schedule description shown on blocked items (clickable to edit)
  - Database migration preserves existing blocked items as "Everyday, All day"
- **Splash Screen**
  - New animated splash screen with horizontal progress bar
  - Smooth preloading of application resources
  - Material You branding with pulse animations
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
- **UI Enhancements**
  - Refined input field icons by removing background boxes for a cleaner look
- Added backoff policies and constraints to background workers
- Added content descriptions for accessibility
- Improved code organization with DesignTokens for consistent styling

## [1.0.3] - 2026-01-16
### Fixed
- Maintenance and deployment updates

## [1.0.2] - 2026-01-16
### Fixed
- Maintenance and deployment updates

## [1.0.1] - 2026-01-16
### Fixed
- Maintenance and deployment updates

## [1.0.0] - 2026-01-16
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


# ANVIL: Forge Your Will

**ANVIL** is a lightweight, high-performance productivity tool designed to help you reclaim your focus. Unlike mainstream blocking apps that are cluttered with ads and subscriptions, ANVIL provides a clean, "no-nonsense" environment to manage tasks, eliminate digital distractions, and build disciplined habits.

> **Current Version**: 1.3.0 · **Min SDK**: 24 (Android 7.0) · **Target SDK**: 36

## Why I Built This

I created ANVIL because I was tired of the "productivity tax."

I relied on apps like *StayFocused* to manage my digital habits, but I found myself increasingly frustrated. I wanted to focus, yet I was being interrupted by ads. I wanted to stay disciplined, but core features were locked behind monthly subscriptions. It felt counterproductive to use a tool meant for "freedom" that was cluttered with the very distractions it was supposed to prevent.

**I built ANVIL for three simple reasons:**

* **Ad-Free Zen**: Focus tools should be quiet. ANVIL has zero ads and zero pop-ups.
* **Total Ownership**: A powerful blocking tool shouldn't require a credit card to unlock basic functionality.
* **Simplicity Over Bloat**: I needed a "forge"—a simple, heavy-duty tool where I can set my tasks, lock my distractions, and get to work.

---

## Features

### Task Management

* **Quick Task Entry**: Create tasks with titles, deadlines, notes, and optional sub-steps
* **Daily Tasks**: Set up repeatable daily tasks that auto-reset every day
* **Hardness Rating**: Set urgency levels (1–5) that determine how many days before the deadline a task must be completed
* **Progress Tracking**: Visual progress indicators and completion stats
* **Category Organization**: Group and categorize your tasks for better organization
* **Smart Reminders**: Automatic notifications for upcoming task deadlines (every 15 minutes via WorkManager)
* **Bonus Tasks**: Track spontaneous completed tasks that weren't planned ahead of time

### Distraction Blocking

* **App Shield**: Toggle-based blocking for any installed application
* **Smart Categorization**: Organize blocked apps by custom categories
* **Link Warden**: Block specific URLs or domain patterns globally
* **VPN-Based DNS Blocking**: Local DNS proxy that returns NXDOMAIN for blocked domains—works in incognito mode and all apps without slowing your internet
* **Schedule-Based Blocking**: Block apps and links based on time ranges and specific days (Everyday, Weekdays, or Custom day bitmask)
* **Privacy Mode**: AES-256-GCM encrypted blocked URLs (hidden from display, triggers incognito blocking)
* **YouTube Shorts Blocking**: Intelligent detection and blocking of YouTube Shorts content
* **Hardness-Based Blocking**: Apps are blocked when tasks approach their hardness-adjusted deadlines
* **Bypass Detection**: Automatically detects and blocks VPN apps, Tor browsers, and private browsers used to circumvent restrictions
* **Real-time Enforcement**: Uses Android Accessibility Services to detect and intercept distracting habits
* **Lock Overlay**: Full-screen blocking overlay shown when a blocked app or link is opened

### Penalty & Anti-Tamper System

* **24-Hour Blocking Penalty**: Triggered when overdue tasks violate hardness thresholds, stored in `EncryptedSharedPreferences`
* **Clock Tamper Detection**: `TimeIntegrityGuard` compares `System.currentTimeMillis()` vs `SystemClock.elapsedRealtime()` to detect system clock manipulation
* **Device Admin**: Optional uninstall protection via Device Admin Receiver

### Focus Sessions (Pomodoro Timer)

* **Configurable Intervals**: Set custom work and break durations
* **Round Tracking**: Track completed rounds within a session
* **Session History**: View past focus sessions and total minutes focused
* **XP Rewards**: Earn experience points for completed focus sessions

### Budget Tracking

* **Dual Balance System**: Track both Cash and GCash balances separately
* **Income & Expenses**: Log all financial transactions with descriptions and categories (Necessity / Leisure)
* **Balance Overview**: Dashboard cards showing current balances at a glance
* **Transaction History**: View and filter your income and expense history
* **Quick Add Shortcut**: Add expenses or income directly from the home screen long-press menu

### Loan Management

* **Loan Tracking**: Record money loaned to others with borrower details and interest rates
* **Balance Type Support**: Track loans separately for Cash and GCash
* **Repayment Tracking**: Log partial or full repayments with automatic balance updates
* **Status Tracking**: Active, Partially Repaid, and Fully Repaid statuses
* **Due Date Reminders**: Track overdue loans with visual indicators
* **Balance Integration**: Loans automatically affect your budget balance calculations
* **Quick Add Shortcut**: Add loans directly from the home screen long-press menu

### Forge Leveling System

* **XP from Everything**: Earn experience from tasks, bonus tasks, streaks, budget entries, loan payoffs, and focus sessions
* **10 Forge Levels**: Progress from "Novice Smith" to "Legendary Artificer"
* **Theme Unlocks**: Unlock exclusive color themes at levels 4, 6, 9, and 10 (Ember, Frost, Amethyst, Golden Forge)
* **Forge Profile**: Dedicated profile screen showcasing your level, XP, and achievements

### Achievements

* **17+ Achievements**: Unlock milestones across tasks, streaks, levels, budget, focus sessions, bonus tasks, ice, and loans
* **Progress Tracking**: Track your progress toward each achievement

### Bonus & Ice System

* **Bonus Task Tracking**: Record extra tasks completed outside your planned list
* **Ice (Streak Freeze)**: Exchange completed bonus tasks for Ice that protects your streak (max 3, expires in 7 days)
* **Automatic Streak Save**: If daily tasks are missed, 1 Ice is automatically consumed at midnight to maintain your streak
* **Productivity Rewards**: Complete 5 bonus tasks to earn 1 Ice

### Dashboard & UX

* **Splash Screen**: Animated Lottie loading screen with progress tracking and branding
* **Contribution Graph**: GitHub-style activity heatmap showing completed tasks over 3 months
* **Consistency Chart**: Visual representation of your productivity consistency
* **Material You Design**: Built with Jetpack Compose and Material 3 for a modern, responsive feel
* **Adaptive Layout**: Responsive design with Material 3 Adaptive for different screen sizes
* **Adaptive Themes**: Full support for Light and Dark modes with unlockable color palettes
* **Motivational Quotes**: Daily motivational quotes to keep you inspired
* **Budget & Loan Summary**: Quick view of balances and outstanding loans from the dashboard
* **Home Screen Widget**: Jetpack Glance widget with stats, refreshed every 30 minutes
* **Onboarding Overlay**: Guided permission setup for first-time users
* **Haptic Feedback**: Tactile feedback on interactions
* **Shimmer Loading**: Polished loading states with shimmer animations

---

## Architecture & Stack

The app follows modern Android development practices to ensure performance and reliability:

| Layer | Technology |
|---|---|
| **Language** | Kotlin (100%) |
| **UI** | Jetpack Compose with Material 3 |
| **Architecture** | MVVM + Repository pattern |
| **Dependency Injection** | Dagger Hilt (KSP) |
| **Navigation** | Jetpack Navigation Compose with type-safe routes (kotlinx.serialization) |
| **Database** | Room (12 entities, 10 DAOs, 15 migrations) |
| **Async** | Kotlin Coroutines & Flow for reactive, non-blocking data streams |
| **Background Work** | WorkManager (6 periodic workers) |
| **Security** | EncryptedSharedPreferences + Android KeyStore (AES-256-GCM) |
| **Image Loading** | Coil for efficient rendering of application icons |
| **Animations** | Lottie Compose for rich animated content |
| **Widgets** | Jetpack Glance with Material 3 theming |
| **Responsive** | Material 3 Adaptive (window size classes) |
| **Serialization** | kotlinx-serialization-json |
| **Build** | Gradle KTS with version catalogs |
| **Minification** | R8 with ProGuard (minify + shrink resources) |

---

## CI/CD

GitHub Actions workflows automate quality checks and releases:

* **CI** (`ci.yml`) — Runs on push/PR to `main`/`develop`:
  * **Lint** → `lintDebug` with artifact upload
  * **Build** → `assembleDebug` with APK artifact
  * **Test** → `testDebugUnitTest` with test result artifacts
* **Release** (`release.yml`) — Triggers on `v*` tags:
  * Builds signed release APK using base64-decoded keystore from GitHub Secrets
  * Auto-generates release notes from git log
  * Creates GitHub Release with APK attached

---

## Setup & Installation

### Prerequisites

* Android Studio Ladybug or newer
* JDK 17+
* Physical device recommended (for testing Accessibility, VPN, and Battery persistence)

### Build

```bash
git clone https://github.com/yourusername/anvil
cd anvil
./gradlew assembleDebug
```

### Critical Permissions

To function as a system-wide blocker, ANVIL requires:

1. **Accessibility Service**: To detect when a blocked app is opened and monitor browser URLs
2. **Usage Access**: To track app statistics and improve blocking accuracy
3. **Ignore Battery Optimizations**: To prevent Android from killing the blocker in the background
4. **Notification Permission** (Android 13+): To send reminders about upcoming task deadlines
5. **VPN Service** *(optional)*: To enable network-level DNS blocking for incognito and non-browser apps
6. **Display Over Other Apps**: To show the full-screen lock overlay

---

## Troubleshooting

If you are sideloading the APK (installing via USB or browser), Android's security features may interfere:

### Accessibility Service Disabling on Restart

Android often kills background services to save power.

* **Fix**: Go to **App Info > Battery** and set it to **"Unrestricted."**

### "Restricted Setting" Error (Android 13+)

If the Accessibility toggle is greyed out:

1. Go to **Settings > Apps > ANVIL**.
2. Tap the **three dots (⋮)** in the top-right corner.
3. Select **"Allow restricted settings."**
4. Return to Accessibility settings to enable the service.

---

## Privacy Policy

ANVIL is built for personal use. **All data stays on your device.**

* No analytics
* No trackers
* No cloud syncing
* No ads
* No internet required (except local DNS proxy for VPN blocking)

Your focus is your business.

---

**Developed By**: James Ryan Gallego

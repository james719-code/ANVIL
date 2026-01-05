
# ANVIL: Forge Your Will

**ANVIL** is a lightweight, high-performance productivity tool designed to help you reclaim your focus. Unlike mainstream blocking apps that are cluttered with ads and subscriptions, ANVIL provides a clean, "no-nonsense" environment to manage tasks and eliminate digital distractions.

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

* **Quick Task Entry**: Create tasks with titles, deadlines, and optional sub-steps
* **Daily Tasks**: Set up repeatable daily tasks that auto-reset every day
* **Hardness Rating**: Set urgency levels (1-5) that determine how many days before the deadline a task must be completed
* **Progress Tracking**: Visual progress indicators and completion stats
* **Category Organization**: Group and categorize your tasks for better organization
* **Smart Reminders**: Automatic notifications for upcoming task deadlines
* **Bonus Tasks**: Track spontaneous completed tasks that weren't planned ahead of time

### Budget Tracking

* **Dual Balance System**: Track both Cash and GCash balances separately
* **Income & Expenses**: Log all financial transactions with descriptions and categories
* **Balance Overview**: Dashboard cards showing current balances at a glance
* **Transaction History**: View and filter your income and expense history

### Loan Management

* **Loan Tracking**: Record money loaned to others with borrower details
* **Balance Type Support**: Track loans separately for Cash and GCash
* **Repayment Tracking**: Log partial or full repayments with automatic balance updates
* **Due Date Reminders**: Track overdue loans with visual indicators
* **Balance Integration**: Loans automatically affect your budget balance calculations

### Distraction Blocking

* **App Shield**: Toggle-based blocking for any installed application
* **Smart Categorization**: Organize blocked apps by custom categories
* **Link Warden**: Block specific URLs or domain patterns globally
* **Privacy Mode**: Encrypt sensitive blocked URLs (hidden from display)
* **YouTube Shorts Blocking**: Intelligent detection and blocking of YouTube Shorts content
* **Hardness-Based Blocking**: Apps are blocked when tasks approach their hardness-adjusted deadlines
* **Real-time Enforcement**: Uses Android Accessibility Services to detect and intercept distracting habits

### Dashboard & Analytics

* **Contribution Graph**: GitHub-style activity chart showing completed tasks over 3 months
* **Material You Design**: Built with Jetpack Compose for a modern, responsive feel
* **Adaptive Themes**: Full support for Light and Dark modes with a subtle blue-green color palette
* **Motivational Quotes**: Daily motivational quotes to keep you inspired
* **Budget Summary**: Quick view of Cash and GCash balances from the dashboard
* **Loan Summary**: Outstanding loan amounts displayed prominently

### Bonus & Grace System

* **Bonus Task Tracking**: Record extra tasks completed outside your planned list
* **Grace Days**: Exchange completed bonus tasks for grace days that protect you from blocking
* **Productivity Rewards**: Complete 3 bonus tasks to earn 1 grace day (up to 3 max)


---

## Architecture & Stack

The app follows modern Android development practices to ensure performance and reliability:

* **UI**: Jetpack Compose (100% Kotlin)
* **Pattern**: MVVM (Model-View-ViewModel) for clean separation of concerns
* **Database**: Room for local persistence of tasks and blocklists
* **Asynchrony**: Kotlin Coroutines & Flow for reactive, non-blocking data streams
* **Background Work**: WorkManager for periodic task resets and reminder notifications
* **Image Loading**: Coil for efficient rendering of application icons

---

## Setup & Installation

### Prerequisites

* Android Studio Ladybug or newer
* JDK 17+
* Physical device recommended (for testing Accessibility/Battery persistence)

### Build

```bash
git clone https://github.com/yourusername/anvil
cd anvil
./gradlew assembleDebug
```

### Critical Permissions

To function as a system-wide blocker, ANVIL requires:

1. **Accessibility Service**: To detect when a blocked app is opened
2. **Usage Access**: To track app statistics and improve blocking accuracy
3. **Ignore Battery Optimizations**: To prevent Android from killing the blocker in the background
4. **Notification Permission** (Android 13+): To send reminders about upcoming task deadlines


---

## Troubleshooting

If you are sideloading the APK (installing via USB or browser), Android's security features may interfere:

### Accessibility Service Disabling on Restart

Android often kills background services to save power.

* **Fix**: Go to **App Info > Battery** and set it to **"Unrestricted."**

### 2. "Restricted Setting" Error (Android 13+)

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

Your focus is your business.

---

**Developed By**: James Ryan Gallego

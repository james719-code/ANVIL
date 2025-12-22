
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

### 🛠 Task Management

* **Direct Entry**: Create tasks with titles and deadlines quickly.
* **Progress Tracking**: A minimalist list view of pending responsibilities.
* **Reward System**: A satisfying "You are free" state when your queue is empty.

### 🚫 Distraction Blocking

* **App Shield**: Toggle-based blocking for any installed application.
* **Link Warden**: Add specific URLs or domain patterns to a global blocklist.
* **Real-time Enforcement**: Uses Android Accessibility Services to detect and intercept distracting habits.

### 🎨 Customization

* **Material You**: Built with Jetpack Compose for a modern, responsive feel.
* **Adaptive Themes**: Full support for Light and Dark modes.

---

## Architecture & Stack

The app follows modern Android development practices to ensure performance and reliability:

* **UI**: Jetpack Compose (100% Kotlin).
* **Pattern**: MVVM (Model-View-ViewModel) for clean separation of concerns.
* **Database**: Room for local persistence of tasks and blocklists.
* **Asynchrony**: Kotlin Coroutines & Flow for reactive, non-blocking data streams.
* **Image Loading**: Coil for efficient rendering of application icons.

---

## Setup & Installation

### Prerequisites

* Android Studio Ladybug or newer.
* JDK 17+.
* Physical device recommended (for testing Accessibility/Battery persistence).

### Build

```bash
git clone https://github.com/yourusername/anvil.git
cd anvil
./gradlew assembleDebug

```

### Critical Permissions

To function as a system-wide blocker, ANVIL requires:

1. **Accessibility Service**: To detect when a blocked app is opened.
2. **Usage Access**: To track app statistics and improve blocking accuracy.
3. **Request Ignore Battery Optimizations**: To prevent Android from killing the blocker in the background.

---

## Troubleshooting: Persistence & Restrictions

If you are sideloading the APK (installing via USB or browser), Android's security features may interfere:

### 1. Accessibility Disabling on Restart

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

ANVIL is built for personal use. **All data stays on your device.** There are no analytics, no trackers, and no cloud syncing. Your focus is your business.


---

Developed By: James Ryan Gallego

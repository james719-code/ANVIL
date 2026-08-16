# The Complete Guide to ANVIL Performance Testing, Profiling & Auto-Notifications

A practical engineering handbook on profiling ANVIL, eliminating UI jank, configuring Baseline & Startup Profiles, reading Perfetto traces, and managing intelligent background task deadline notifications.

---

## 📑 Table of Contents
1. [Core Concepts: Why Apps Lag](#1-core-concepts-why-apps-lag)
2. [The Profiling & Tracing Toolkit](#2-the-profiling--tracing-toolkit)
   - [A. Perfetto System Profiler](#a-perfetto-system-profiler)
   - [B. Compose Runtime Tracing](#b-compose-runtime-tracing)
   - [C. Custom Instrumentation (AnvilTrace.kt)](#c-custom-instrumentation-anviltracekt)
   - [D. Automated SQL Trace Processor](#d-automated-sql-trace-processor)
3. [The 4 Pillars of Android Performance](#3-the-4-pillars-of-android-performance)
   - [Pillar 1: Startup & Atomic State Management](#pillar-1-startup--atomic-state-management)
   - [Pillar 2: Baseline Profiles (AOT Compilation)](#pillar-2-baseline-profiles-aot-compilation)
   - [Pillar 3: Startup Profiles (DEX Layout Optimization)](#pillar-3-startup-profiles-dex-layout-optimization)
   - [Pillar 4: GPU RenderThread & Overdraw Elimination](#pillar-4-gpu-renderthread--overdraw-elimination)
4. [Intelligent Task Deadline Notifications](#4-intelligent-task-deadline-notifications)
   - [Multi-Stage Notification Architecture](#multi-stage-notification-architecture)
   - [WorkManager Execution Cycle](#workmanager-execution-cycle)
   - [State Tracking & Daily Task Resets](#state-tracking--daily-task-resets)
5. [Step-by-Step Workflow: How to Test & Profile ANVIL](#5-step-by-step-workflow-how-to-test--profile-anvil)

---

## 1. Core Concepts: Why Apps Lag

Android screens refresh at **60Hz (16.6ms budget per frame)** or **120Hz (8.3ms budget per frame)**. If the CPU or GPU takes longer than this budget to compute and draw a frame, the OS drops the frame—perceived by the user as stutter or **UI Jank**.

### The 3 Main Sources of Jank:
```
┌────────────────────────────────────────────────────────────────────────┐
│                        WHERE TIME IS LOST                              │
├───────────────────┬────────────────────────────┬───────────────────────┤
│ 1. Main Thread    │ 2. JIT Compiler Stalls     │ 3. GPU RenderThread   │
│ (CPU Stalls)      │ (ART Pauses)               │ (Overdraw & Blending) │
├───────────────────┼────────────────────────────┼───────────────────────┤
│ • Disk I/O on UI  │ • On-the-fly bytecode     │ • Nested transparent  │
│ • State churn &   │   compilation when opening │   alpha cards         │
│   recomposition   │   text fields or lists     │ • Repeated Canvas     │
│ • Heavy iteration │ • Missing Baseline Profile │   redraws during scroll│
└───────────────────┴────────────────────────────┴───────────────────────┘
```

---

## 2. The Profiling & Tracing Toolkit

We integrated a unified profiling stack into ANVIL:

### A. Perfetto System Profiler
- **What it captures**: Kernel CPU frequency, thread scheduling, Android SurfaceFlinger frames, binder IPC transactions, and app slices.
- **Config**: [`scripts/perfetto_config.pbtx`](../scripts/perfetto_config.pbtx) captures data sources with high buffer limits (128MB).

### B. Compose Runtime Tracing
- **Library**: `androidx.compose.runtime:runtime-tracing:1.0.0-beta01`.
- **Purpose**: Feeds composable method names (`TaskItemRow`, `AnimatedBottomBar`, `AnvilAppShell`) directly into the Perfetto timeline so you can see which Composable function took time to render or recompose.

### C. Custom Instrumentation (`AnvilTrace.kt`)
Lightweight, zero-allocation inline utilities:
```kotlin
import com.james.anvil.util.traceSection
import com.james.anvil.util.traceAsyncSection
import com.james.anvil.util.AnvilTrace

// Synchronous timing section:
val result = traceSection("MyCustomOperation") {
    computeTaskScores()
}

// Suspend / Coroutine asynchronous section:
val data = traceAsyncSection("MyAsyncDataLoader") {
    fetchHistoryFromDatabase()
}

// Custom numeric counter track in Perfetto UI:
AnvilTrace.setCounter("anvil.active_monsters", count.toLong())
```

### D. Automated SQL Trace Processor (`scripts/analyze_trace.py`)
Since `.perfetto-trace` is a binary format, we automated trace analysis using Perfetto's official `trace_processor` C++ engine to execute SQL queries on the trace and output a Markdown summary.

---

## 3. The 4 Pillars of Android Performance

### Pillar 1: Startup & Atomic State Management
* **Problem**: Having multiple uncoordinated coroutines writing to `StateFlow` or `mutableStateOf` in `ViewModel.init` forces the UI to recompose 3 to 15 times before the user sees the first frame.
* **Solution**: Combine initial data fetches and emit state atomically.

---

### Pillar 2: Baseline Profiles (AOT Compilation)
```
      Baseline Profile (src/main/baseline-prof.txt)
                        ↓
       "What CODE should Android compile?"
                        ↓
 [ART Ahead-Of-Time Compilation upon installation]
```
* **How it works**: By default in debug mode, Android uses the JIT (Just-In-Time) interpreter. When a user taps an uncompiled component (like `OutlinedTextField`), ART pauses the Main Thread for 30–70ms to compile bytecode to machine code.
* **Solution**: `baseline-prof.txt` tells Android's ART compiler to compile critical paths into native code immediately upon app installation.

---

### Pillar 3: Startup Profiles (DEX Layout Optimization)
```
       Startup Profile (src/main/startup-prof.txt)
                        ↓
    "Where should startup CODE be located inside DEX?"
                        ↓
      [Classes moved to primary classes.dex]
```
* **How it works**: Groups startup classes into consecutive pages on disk, avoiding random flash memory seeks on cold launch.

---

### Pillar 4: GPU RenderThread & Overdraw Elimination
* Avoid redundant alpha compositing passes where solid colors can be used.
* Cache static background drawings in RenderNodes via Compose's `Modifier.drawWithCache`.
* Add explicit `key` and `contentType` parameters to all `LazyColumn` items so Compose can reuse existing subtrees.

---

## 4. Intelligent Task Deadline Notifications

### Multi-Stage Notification Architecture
In ANVIL, `ReminderWorker` runs periodically to ensure users are proactively alerted when tasks are approaching their deadlines or when checkpoints are reached:

```
Task Created                                    Deadline
    ├─────────────────────┼────────────────────────┼───────────────► Time
                        50%                      -2h
                   [Halfway Point]       [Deadline Approaching] [Task Overdue]
```

1. **Halfway Checkpoint (`HALFWAY`)**:
   - For tasks scheduled for at least 2 hours.
   - Alerts user when 50% of the duration has passed, keeping momentum going.
   - Priority: `PRIORITY_DEFAULT`.

2. **Urgent Deadline Alert (`NEAR_DEADLINE`)**:
   - Triggered when the deadline is approaching within 2 hours (`timeLeft in 1..2 hours`).
   - Format: `"'Task Name' is due in 45m! Complete it to stay on track."`
   - Priority: `PRIORITY_HIGH` (Heads-Up Banner notification with Gold accent).

3. **Overdue Notice (`OVERDUE`)**:
   - Triggered immediately if the deadline has passed and the task remains incomplete.
   - Priority: `PRIORITY_HIGH`.

### WorkManager Execution Cycle
- `WorkerScheduler.scheduleReminderWorker` enqueues `ReminderWorker` via `PeriodicWorkRequestBuilder` with a 15-minute interval and battery constraints.
- Notification Channel: `"anvil_reminders"` ("Task Reminders"), configured with vibration patterns and gold LED lights (`#D4AF37`).
- Tap Action: Automatically launches `MainActivity` with clear-top flags.

### State Tracking & Daily Task Resets
- Multi-stage notification states are tracked in `anvil_task_reminder_tracker` SharedPreferences with stage-specific keys (`task_{id}_{STAGE}` and `daily_task_{id}_{DATE}_{STAGE}`).
- Daily tasks get fresh notification windows each day without requiring Room schema changes.

---

## 5. Step-by-Step Workflow: How to Test & Profile ANVIL

### Step 1: Record a Trace via Script
Ensure your Android device or emulator is connected via USB with USB Debugging enabled.

```powershell
# On Windows (PowerShell)
.\scripts\record-perfetto-trace.ps1

# On Windows (CMD)
.\scripts\record-perfetto-trace.bat

# On Linux / macOS (Bash)
./scripts/record-perfetto-trace.sh
```

### Step 2: Automated SQL Trace Analysis
Analyze the recorded trace file from the command line:

```bash
python scripts/analyze_trace.py traces/anvil-perfetto-<timestamp>.trace
```

### Step 3: Visual Timeline Inspection
Open [https://ui.perfetto.dev/](https://ui.perfetto.dev/) in Google Chrome or Edge, click **"Open trace file"**, and select your `.trace` file.
- Inspect the **`com.james.anvil`** process track.
- Look for `Compose:recompose`, `Record View#draw()`, and `AnvilApp` slices.
- Check the CPU scheduling track for core utilization and frequency scaling.

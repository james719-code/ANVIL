# ANVIL: Forge Your Will

ANVIL is a productivity application designed to help you focus by managing tasks and blocking distractions. It allows you to create tasks with deadlines and block distracting applications and websites.

## Features

### Task Management
*   **Add Tasks**: Create new tasks with titles and deadlines.
*   **Track Progress**: View your pending tasks in a clean list.
*   **Complete Tasks**: Mark tasks as done when you finish them.
*   **Empty State**: Enjoy a satisfying "You are free" message when all tasks are completed.

### Distraction Blocking
*   **Block Applications**: View a list of all installed applications on your device and toggle blocking for specific apps.
*   **Block Links**: Add specific URLs or domain patterns to a blocklist to prevent access to distracting websites.
*   **Management**: Easily add or remove apps and links from your blocklist.

### Settings & Customization
*   **Dark Mode**: Toggle between Light and Dark themes to suit your preference or lighting conditions.
*   **Modern UI**: Built with Jetpack Compose for a smooth and responsive user experience.
*   **Navigation**: Easy-to-use bottom navigation to switch between Tasks, Blocklist, and Settings.

## Architecture

The app follows modern Android development practices:
*   **Jetpack Compose**: For the UI.
*   **MVVM**: Model-View-ViewModel architecture for separation of concerns.
*   **Room Database**: For local data persistence of tasks and blocklists.
*   **Coroutines & Flow**: For asynchronous operations and reactive data streams.
*   **Navigation Compose**: For handling in-app navigation.
*   **Coil**: For efficient image loading (app icons).

## Setup & Build

1.  **Prerequisites**:
    *   Android Studio.
    *   JDK 11 or higher.

2.  **Build**:
    Open the project in Android Studio and sync Gradle files.
    Run the app on an emulator or physical device.

    ```bash
    ./gradlew assembleDebug
    ```

3.  **Permissions**:
    *   The app requires `QUERY_ALL_PACKAGES` permission to list installed applications for blocking.
    *   Accessibility Service permission is required for the core blocking functionality (handled by `AnvilAccessibilityService`).

## Usage

1.  **Tasks**: Tap the "+" FAB to add a task. Tap the checkmark on a task to complete it.
2.  **Blocklist**:
    *   **Apps Tab**: Scroll through the list of apps and toggle the switch to block/unblock.
    *   **Links Tab**: Tap the "+" FAB to add a URL to block. Tap the trash icon to remove it.
3.  **Settings**: Go to the Settings tab to toggle Dark Mode.

## License

[Add License Here]

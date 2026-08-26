# GPX Mock Location that works

GPX Mock Location is a lightweight Android application designed to simulate GPS movement using track points and waypoints extracted from standard `.gpx` files. The app replays recorded GPX routes using Android's system test provider APIs and runs the simulation inside a foreground service.

## Features

- **GPX File Parsing**: Parses track points (`<trkpt>`) and waypoints (`<wpt>`) alongside ISO 8601 `<time>` timestamps.
- **Dynamic Playback Timing**: Computes actual time deltas between points to replicate real movement speed (constrained between 100 ms and 10 seconds, falling back to 1 second if timestamps are missing).
- **Foreground Service Execution**: Runs the mock location engine inside `MockLocationService` with `location` foreground service type, ensuring uninterrupted playback when the app is in the background.
- **File Association**: Registered intent filters allow opening `.gpx` files directly from file managers and external applications.
- **Jetpack Compose UI**: Built with Material 3, dynamic theming (Android 12+), and reactive state management using Kotlin Coroutines and `StateFlow`.

## Requirements

- **Minimum Android Version**: Android 8.0 (API level 26)
- **Target Android Version**: Android 16 (API level 36)
- **Permissions**:
    - `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`
    - `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_LOCATION`
    - `POST_NOTIFICATIONS` (Android 13+)
    - Developer Options mock location access

## Setup and Configuration

Android restricts test location injection unless the application is explicitly designated as the mock location provider in system settings.

1. Enable **Developer Options** on your Android device (Go to **Settings** > **About phone**, then tap **Build number** 7 times).
2. Open **Settings** > **System** > **Developer options**.
3. Locate **Select mock location app** (or **Allow mock locations** on older versions).
4. Select **GPX Mock Location**.

## Usage

1. Launch the app.
2. Tap **Open GPX File** to pick a file via the system file picker, or open a `.gpx` file from any external file manager using GPX Mock Location.
3. Review the loaded coordinate count and status in the UI.
4. Tap **Start** to begin route injection.
5. Tap **Stop** to terminate route playback and deregister the test provider.
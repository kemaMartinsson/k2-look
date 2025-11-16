# Karoo2 ↔ ActiveLook Gateway

Real-time data gateway between Hammerhead Karoo2 cycling computer and ActiveLook smart glasses, enabling cyclists to view critical ride data on a heads-up display without looking down.

## Project Overview

This Karoo Extension streams cycling metrics (speed, heart rate, power, cadence, navigation, etc.) from the Karoo2 to ActiveLook smart glasses via Bluetooth Low Energy, providing hands-free ride data visualization.

**Key Features:**
- Real-time metric streaming (<500ms latency)
- Customizable display layouts
- Support for all Karoo2 SDK metrics
- Battery-efficient BLE communication
- Navigation prompts on HUD

## Repository Structure

```
/
├── app/                    # Main application code
├── docs/                   # Documentation (PRD, dev setup)
├── reference/              # Git submodules (upstream references)
│   ├── android-sdk/       # ActiveLook Android SDK
│   └── karoo-ext/         # Karoo Extensions SDK & samples
├── TODO.md                # Implementation progress tracker
└── [gradle files]
```

## Reference Submodules

The `reference/` directory contains **git submodules** pointing to upstream repositories. These are read-only references for development:

- **`reference/android-sdk`** → [ActiveLook Android SDK](https://github.com/ActiveLook/android-sdk) (v4.5.6)
- **`reference/karoo-ext`** → [Karoo Extensions](https://github.com/hammerheadnav/karoo-ext) (v1.1.7)

### Updating Reference Submodules

To pull the latest updates from upstream:

```bash
# Update ActiveLook SDK
cd reference/android-sdk
git pull origin main

# Update Karoo Extensions
cd reference/karoo-ext
git pull origin master

# Return to root and commit submodule updates
cd /workspaces/k2-look
git add reference/
git commit -m "Update reference submodules"
```

### Cloning This Repository

When cloning this repo, initialize submodules:

```bash
git clone git@github.com:kemaMartinsson/k2-look.git
cd k2-look
git submodule init
git submodule update
```

Or clone with submodules in one command:

```bash
git clone --recurse-submodules git@github.com:kemaMartinsson/k2-look.git
```

## Development Setup

### Install Android Studio

1. **Download Android Studio**
   - Visit [developer.android.com/studio](https://developer.android.com/studio)
   - Download the latest stable version for Windows

2. **Install Android Studio**
   - Run the installer
   - Follow the setup wizard (install Android SDK, Android Virtual Device)
   - Choose "Standard" installation type

3. **Configure Android SDK**
   - Open Android Studio
   - Go to **Settings → Appearance & Behavior → System Settings → Android SDK**
   - Ensure these are installed:
     - Android SDK Platform 30 (or higher)
     - Android SDK Build-Tools 30.0.3 (or higher)
     - Android SDK Platform-Tools

4. **Install GitHub Copilot (Optional)**
   - Go to **File → Settings → Plugins**
   - Search for "GitHub Copilot"
   - Click **Install** and restart
   - Sign in with your GitHub account

5. **Open the Project**
   - In Android Studio: **File → Open**
   - Navigate to `c:\Project\k2-look\src`
   - Let Gradle sync (may take several minutes on first run)

### GitHub Packages Authentication

Configure access to the Karoo Extensions library:

1. Create a GitHub Personal Access Token with `read:packages` scope
2. Edit `src/local.properties` (create if doesn't exist):

   ```properties
   gpr.user=your-github-username
   gpr.key=your-personal-access-token
   ```

3. This file is in `.gitignore` to protect your credentials

See [Dev Setup Guide](./docs/Karoo2-ActiveLook-Dev-Setup.md) for complete environment configuration and advanced topics.

## Building

```bash
./gradlew app:assembleDebug
```

## Installation

### Via ADB (USB)

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Via Karoo Companion App

Share the APK to the Hammerhead Companion App on your phone for wireless installation.

## Documentation

- [Product Requirements Document](./docs/Karoo2-ActiveLook-PRD.md)
- [Development Setup Guide](./docs/Karoo2-ActiveLook-Dev-Setup.md)
- [Implementation TODO](./TODO.md)

## External Resources

### Karoo

- [Karoo Extensions Documentation](https://hammerheadnav.github.io/karoo-ext/index.html)
- [Karoo Extensions GitHub](https://github.com/hammerheadnav/karoo-ext)
- [Developer Community](https://support.hammerhead.io/hc/en-us/community/topics/31298804001435-Hammerhead-Extensions-Developers)

### ActiveLook

- [ActiveLook Android SDK](https://github.com/ActiveLook/android-sdk)
- [ActiveLook API Documentation](https://github.com/ActiveLook/Activelook-API-Documentation)
- [Development Guide](https://www.activelook.net/news-blog/developing-with-activelook-getting-started)
- [Demo App](https://github.com/ActiveLook/demo-app)

## Contributing

This is a personal development project. If you have suggestions or find issues, feel free to open an issue or discussion.

## License

See [LICENSE](./LICENSE) file for details.

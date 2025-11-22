# Karoo2 ↔ ActiveLook Gateway (K2-Look)

**Status:** 🎉 **Phase 1 Complete - Ready for Field Testing!**

Real-time data gateway between Hammerhead Karoo2 cycling computer and ActiveLook smart glasses,
enabling cyclists to view critical ride data on a heads-up display without looking down.

## Project Overview

This Karoo Extension streams cycling metrics (speed, heart rate, power, cadence, distance, time)
from the Karoo2 to ActiveLook smart glasses via Bluetooth Low Energy, providing hands-free ride data
visualization.

### Current Features (Phase 1):

- ✅ Real-time Karoo data streaming (6 core metrics)
- ✅ ActiveLook BLE connection and display
- ✅ Data bridge with 1Hz throttling
- ✅ Hold/flush pattern for efficient updates
- ✅ Connection state management
- ✅ User-friendly Android UI

### Planned Features (Phase 2+):

- 🔄 Expanded metrics (HR zones, elevation, power averages)
- 🔄 Customizable display layouts
- 🔄 Navigation prompts on HUD
- 🔄 User preferences and settings persistence
- 🔄 Multiple layout presets

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

## Reference Projects

The `reference/` directory contains **local clones** of upstream repositories. These provide source
code for dependencies and serve as reference implementations:

- **`reference/android-sdk`
  ** → [ActiveLook Android SDK](https://github.com/ActiveLook/android-sdk) (v4.5.6)
- **`reference/karoo-ext`** → [Karoo Extensions](https://github.com/hammerheadnav/karoo-ext) (
  v1.1.7)
- **`reference/ki2`** → [Ki2 Reference Project](https://github.com/valterc/ki2) (working Karoo
  extension example)

### Setup Reference Projects

If you've cloned this repository without the reference projects, run:

**Windows (PowerShell):**

```powershell
.\setup-references.ps1
```

**Linux/Mac (Bash):**

```bash
chmod +x setup-references.sh
./setup-references.sh
```

This will clone all reference projects into the `reference/` directory.

### Updating Reference Projects

To pull the latest updates from upstream repositories:

**Windows (PowerShell):**

```powershell
.\update-references.ps1
```

**Linux/Mac (Bash):**

```bash
chmod +x update-references.sh
./update-references.sh
```

This script will:

- Fetch latest changes from all reference projects
- Stash any local modifications
- Pull updates for each repository
- **Automatically apply necessary build fixes** (Windows only)
- Show the latest commit information
- Report any errors

**Important:** The reference projects use version catalogs and configurations that don't work with
our local module setup. The `post-update-fix.ps1` script automatically applies the necessary fixes
after updates.

**After updating, rebuild the project:**

```bash
.\gradlew clean :app:assembleDebug
```

### Cloning This Repository

When cloning this repo for the first time:

```bash
git clone https://github.com/kemaMartinsson/k2-look.git
cd k2-look
.\setup-references.ps1  # or ./setup-references.sh on Linux/Mac
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

See [Dev Setup Guide](./docs/Karoo2-ActiveLook-Dev-Setup.md) for complete environment configuration
and advanced topics.

## Quick Start

### 1. Building

```bash
.\gradlew :app:assembleDebug
```

**Expected output:** APK in `app/build/outputs/apk/debug/app-debug.apk`

### 2. Installation on Karoo2

**Via ADB (recommended for development):**

```bash
adb devices  # Verify Karoo2 is connected
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n io.hammerhead.karooexttemplate/.MainActivity
```

**Via File Transfer:**
Share the APK to the Hammerhead Companion App on your phone for wireless installation.

### 3. First Run

1. Launch "Karoo Extension Template" on Karoo2
2. Grant Bluetooth and Location permissions
3. Tap **"Connect"** to connect to Karoo System
4. Turn on ActiveLook glasses
5. Tap **"Scan"** to find your glasses
6. Tap on discovered glasses to connect
7. Start a ride - data will stream to glasses automatically!

📖 **For detailed testing instructions, see:
** [Quick Start & Testing Guide](./docs/Quick-Start-Testing-Guide.md)

## Documentation

### Getting Started

- 📘 [Quick Start & Testing Guide](./docs/Quick-Start-Testing-Guide.md) - **Start here for
  deployment!**
- 📋 [Product Requirements Document](./docs/Karoo2-ActiveLook-PRD.md)
- 🛠️ [Development Setup Guide](./docs/Karoo2-ActiveLook-Dev-Setup.md)
- ✅ [Implementation TODO](./TODO.md)

### Phase 1 Completion Reports

- 🎉 [Phase 1 Complete Summary](./docs/Phase-1-Complete-Summary.md) - Overall Phase 1 achievement
- ✅ [Phase 1.1 Complete](./docs/Phase-1.1-Complete.md) - Karoo integration
- ✅ [Phase 1.2 Complete](./docs/Phase-1.2-Complete.md) - ActiveLook integration
- ✅ [Phase 1.3 Complete](./docs/Phase-1.3-Complete.md) - Data bridge

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

## Project Status

### ✅ Phase 1: Core Integration (COMPLETE)

- **Karoo System Integration** - Connect, stream 6 metrics, handle reconnection
- **ActiveLook BLE Integration** - Scan, connect, display text on glasses
- **Data Bridge** - Transform data, 1Hz throttling, hold/flush pattern
- **Build Status:** ✅ Successful
- **Field Testing:** ⏳ Pending

### 🔄 Phase 2: Metric Implementation (NEXT)

- Expand to 12+ metrics (HR zones, elevation, power averages)
- Improve data formatting and units
- Add navigation support
- Performance metrics (3s/10s/30s power)

### Current Metrics Display

The app currently displays these 6 metrics on ActiveLook glasses:

1. **Speed** (km/h)
2. **Heart Rate** (bpm)
3. **Power** (watts)
4. **Cadence** (rpm)
5. **Distance** (km)
6. **Time** (HH:MM:SS)

Updates at **1Hz** (1 update/second) for optimal BLE performance.

## Architecture

```
Karoo2 Sensors → KarooSystemService → KarooDataService
                                            ↓
                                  KarooActiveLookBridge
                                     (transformation)
                                            ↓
                              ActiveLookService → Glasses Display
```

**Key Components:**

- **KarooDataService** - Consumes Karoo data streams
- **ActiveLookService** - Manages BLE connection and display
- **KarooActiveLookBridge** - Coordinates both services, transforms data
- **MainViewModel** - UI state management
- **MainScreen** - Jetpack Compose UI

## Contributing

This is a personal development project. If you have suggestions or find issues, feel free to open an
issue or discussion.

**Bug Reports:** Please include:

- Karoo firmware version
- ActiveLook glasses model
- Steps to reproduce
- LogCat output (if available)

## License

See [LICENSE](./LICENSE) file for details.

---

**Made with ❤️ for the cycling community**


# Karoo2 ↔ ActiveLook (K2-Look)

## ⚠️ Disclaimer

This is a **personal project** shared with the community.  
The software is provided **"as is"** without warranty of any kind.  
Please use at your own risk.

**What this means:**

- ✅ Bug reports and issues are welcome and appreciated
- ✅ Compatibility issues can be reported
- ⚠️ Fixes are provided on a best-effort basis with no guarantees
- ⚠️ Support for different ActiveLook glasses brands can only be done if I have access to the
  hardware
- 💡 Community contributions, feature request and testing are encouraged!

This project is developed and tested
with [Engo2](https://engoeyewear.com/products/engo-2-photochromic).
Function may vary with different devices, but should generally work with any ActiveLook glasses.

---

Real-time data gateway between Hammerhead Karoo2 and (hopefully any) ActiveLook glasses, .

## Project Status

### Transmitted Metrics

K2Look transmits **6 core metrics** from your Karoo 2 to your ActiveLook glasses via Bluetooth:

**Real-time Data Stream:**

1. **Speed** (km/h) - Current speed
2. **Heart Rate** (bpm) - Current heart rate
3. **Power** (watts) - Current power output
4. **Cadence** (rpm) - Current pedaling cadence
5. **Distance** (km) - Total ride distance
6. **Time** (HH:MM:SS) - Ride elapsed time

**How it appears on your glasses:**  
The actual display layout and which metrics you see depends on your **ActiveLook glasses
configuration**. Configure your preferred display layout using the ActiveLook smartphone app or
glasses settings.

Updates transmitted at **1Hz** (1 update/second) for optimal BLE performance.

### Additional Metrics Available from Karoo

The following metrics are **available from Karoo** but not currently transmitted by K2Look:

**Statistical Metrics:**

- Average Speed, Max Speed
- Average Heart Rate, Max Heart Rate
- Average Cadence, Max Cadence
- Average Power, Max Power

**Advanced Metrics:**

- Heart Rate Zone (current training zone)
- 3s/10s/30s Smoothed Power (Normalized Power)
- VAM (Vertical Ascent Meters per hour)
- Average VAM

> **Future Enhancement:** Support for additional metrics and customizable data transmission may be
> added in future releases based on community feedback.

## Project Overview

This Karoo Extension streams cycling metrics (speed, heart rate, power, cadence, distance, time)
from the Karoo2 to ActiveLook smart glasses via Bluetooth Low Energy, providing hands-free ride data
visualization.

Currently, displays are selected in ActiveLook app.  
Depending on needs, K2Look configuration options may be added in future releases.

## Repository Structure

```
/
├── app/                    # Main application code
├── reference/              # Vendored dependencies (included in repo)
│   ├── android-sdk/       # ActiveLook Android SDK (v4.5.6)
│   └── karoo-ext/         # Karoo Extensions SDK (v1.1.7)
└── [gradle files]
```

## Reference Projects

The `reference/` directory contains **vendored copies** of upstream repositories that are **included
directly in this repository**. These provide source code for the ActiveLook SDK and Karoo Extensions
library that the app builds against.

**Why vendored instead of downloaded?**

- Ensures consistent builds across all environments
- CI/CD pipelines work without external dependencies
- Custom build fixes are maintained across updates
- No authentication required for building

**Included projects:**

- **`reference/android-sdk`
  ** → [ActiveLook Android SDK](https://github.com/ActiveLook/android-sdk) (v4.5.6)
- **`reference/karoo-ext`** → [Karoo Extensions](https://github.com/hammerheadnav/karoo-ext) (
  v1.1.7)

These are committed to the repository, so **no setup scripts are needed** - just clone and build!

> **Note:** The `setup-references.*` and `update-references.*` scripts in the repository root are *
*deprecated** and no longer needed since reference projects are now vendored directly in the repo.

### Cloning This Repository

```bash
git clone https://github.com/kemaMartinsson/k2-look.git
cd k2-look
.\gradlew :app:assembleDebug  # That's it! Reference projects are already included
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

4. **Open the Project**
    - In Android Studio: **File → Open**
    - Navigate to the cloned repository directory
    - Let Gradle sync (may take several minutes on first run)

### Alternative: VSCode with Dev Container

For developers who prefer Visual Studio Code, a devcontainer configuration is available (currently
disabled):

1. **Enable the Dev Container:**
    - Rename `.devcontainer.disabled/` to `.devcontainer/`
    - This folder contains Docker configuration for a complete Android development environment

2. **Prerequisites:**
    - Install [Docker Desktop](https://www.docker.com/products/docker-desktop)
    - Install [Visual Studio Code](https://code.visualstudio.com/)
    - Install the "Dev Containers" extension in VSCode

3. **Open in Container:**
    - Open VSCode
    - Press **F1** and select **"Dev Containers: Open Folder in Container..."**
    - Navigate to the cloned repository
    - VSCode will build and start the development container (first time takes several minutes)

4. **Build in Container:**
    - Once the container is running, open the integrated terminal
    - Run: `./gradlew :app:assembleDebug`

> **Note:** The devcontainer is disabled by default to avoid confusion for Android Studio users. It
> provides a consistent Linux-based development environment with all necessary tools pre-installed.

## Quick Start

### 1. Building

```bash
.\gradlew :app:assembleDebug
```

**Expected output:** APK in `app/build/outputs/apk/debug/`

### 2. Installation on Karoo2

**Via ADB (recommended for development):**

```bash
adb devices  # Verify Karoo2 is connected
adb install -r app/build/outputs/apk/debug/*.apk
```

**Via File Transfer:**
Transfer the APK to your Karoo2 and install it.

### 3. First Run

1. Launch "K2Look" on your Karoo2
2. Grant Bluetooth and Location permissions
3. Tap **"Connect"** to connect to Karoo System
4. Turn on your ActiveLook glasses
5. Tap **"Connect glasses"** to scan for glasses
6. Select your glasses from the list
7. Start a ride - data will stream to your glasses automatically!

## Documentation

### Getting Started

- 📘 [Quick Start & Testing Guide](./docs/Quick-Start-Testing-Guide.md) - **Start here for
  deployment!**
- 🛠️ [Development Setup Guide](./docs/Karoo2-ActiveLook-Dev-Setup.md)

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

This is a personal development project.  
If you have suggestions or find issues, feel free to open an
issue or discussion.

**Bug Reports:** Please include:

- Karoo firmware version
- ActiveLook glasses model
- Steps to reproduce
- LogCat output (if available)

## License

See [LICENSE](./LICENSE) file for details.

---

**Made with ❤️ for the K2 community**


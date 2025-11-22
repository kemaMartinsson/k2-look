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

## Reference Projects

The `reference/` directory contains **local clones** of upstream repositories. These provide source code for dependencies and serve as reference implementations:

- **`reference/android-sdk`** → [ActiveLook Android SDK](https://github.com/ActiveLook/android-sdk) (v4.5.6)
- **`reference/karoo-ext`** → [Karoo Extensions](https://github.com/hammerheadnav/karoo-ext) (v1.1.7)
- **`reference/ki2`** → [Ki2 Reference Project](https://github.com/valterc/ki2) (working Karoo extension example)

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

**Important:** The reference projects use version catalogs and configurations that don't work with our local module setup. The `post-update-fix.ps1` script automatically applies the necessary fixes after updates.

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

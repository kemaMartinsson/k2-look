# Dev Environment Setup — Option A (Native Karoo2 development)

**For:** Karoo2 ↔ ActiveLook (direct development on Karoo2)
**Version:** 1.0
**Date:** 2025-11-11

---

## Summary

This guide shows a step‑by‑step setup for developing a Karoo Extension that uses **karoo-ext** on a
Hammerhead Karoo2 (or Karoo) device and communicates with ActiveLook glasses over BLE. It focuses on
native Android development (Android Studio) and sideloading onto Karoo2 for testing.

Key upstream docs you'll use repeatedly:

- [Karoo Extensions (karoo-ext) documentation](https://hammerheadnav.github.io/karoo-ext/index.html) —
  comprehensive API docs and guides
- [Karoo Extensions GitHub repo](https://github.com/hammerheadnav/karoo-ext) — includes README,
  sample app, and latest releases
- [Karoo extension template](https://github.com/hammerheadnav/karoo-ext-template) — starter project
  repository
- [Hammerhead Extensions Developers Community](https://support.hammerhead.io/hc/en-us/community/topics/31298804001435-Hammerhead-Extensions-Developers) —
  get help from other developers
- [ActiveLook Android SDK](https://github.com/ActiveLook/android-sdk) — SDK for BLE communication
  with ActiveLook glasses
- [ActiveLook API Documentation](https://github.com/ActiveLook/Activelook-API-Documentation) — BTLE
  protocol and command reference
- [ActiveLook Development Guide](https://www.activelook.net/news-blog/developing-with-activelook-getting-started) —
  best practices and design guidelines
- [ActiveLook Demo App](https://github.com/ActiveLook/demo-app) — reference Android application

---

## Prerequisites (hardware & accounts)

- Hammerhead Karoo2 (device for rapid testing) with USB cable (data-capable USB‑C)
- ActiveLook glasses (fully charged) for BLE integration
  testing — [order development kit](https://shop.activelook.net/index.php?id_product=21)
- Windows laptop (you indicated) — with admin privileges to install software
- GitHub account (to clone/publish repos and to access GitHub Packages if needed)
- (Optional) Karoo Companion app on smartphone — useful for sideloading via companion app on newer
  Karoo firmware

Software prerequisites:

- Android Studio (latest stable) with Android SDK & platform-tools
- Java JDK 11 (recommended for modern Android Gradle plugin compatibility)
- Git (CLI)
- Android `adb` (comes with platform-tools)
- `nRF Connect` mobile app (or similar) for BLE debugging (recommended)

---

## High-level workflow

1. Create project from [karoo-ext-template](https://github.com/hammerheadnav/karoo-ext-template)
2. Add the `karoo-ext` Gradle dependency and configure access to the Hammerhead Maven package feed
3. Add the `ActiveLook Android SDK` from JitPack (handles BLE communication and display management)
4. Implement your Gateway logic: KarooSystemService consumer → format metrics → ActiveLook SDK
   commands
5. Build & sideload APK to Karoo2 for testing (via `adb install` or Companion sideload)
6. Test with real ActiveLook glasses and verify display updates during ride activities

---

## Step-by-step setup

### 1. Install core tools

1. Install **Android Studio** (download from developer.android.com). Open and complete the first-run
   SDK components install
2. Install **Java JDK 11** (Adoptium Temurin or similar). Configure `JAVA_HOME` if required
3. Install **Git** and configure your user.name / user.email
4. Install **Android platform-tools** (adb is included). Android Studio will manage this, but verify
   `adb` works:

```bash
adb version
```

### 2. Clone starter template & karoo-ext sample

```bash
# pick a working directory
git clone https://github.com/hammerheadnav/karoo-ext-template.git karoo-ext-skeleton
cd karoo-ext-skeleton
```

Open the cloned project in **Android Studio** (File → Open). Android Studio will prompt to install
any missing SDK platforms or build tools referenced by the project — let it install those
automatically. The template contains instructions to update package namespace and
`extension_info.xml`.

### 3. Add `karoo-ext` dependency (GitHub Packages)

The karoo-ext library is published via GitHub Packages and requires configuring the repository
credentials in your Gradle settings. The karoo-ext README shows the required snippet — add a `maven`
repository in your project settings like this (example in `settings.gradle.kts` or
`settings.gradle`):

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven {
            url = uri("https://maven.pkg.github.com/hammerheadnav/karoo-ext")
            credentials {
                username = providers.gradleProperty("gpr.user").getOrElse(System.getenv("USERNAME"))
                password = providers.gradleProperty("gpr.key").getOrElse(System.getenv("TOKEN"))
            }
        }
    }
}
```

**How to provide credentials:**

- Create a GitHub Personal Access Token (PAT) with `read:packages` (and `repo` if you need). Store
  the token in `local.properties` or export as an environment variable e.g. `TOKEN` and `USERNAME`.
  See [GitHub's documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package)
  for details

Then add the dependency in your module `build.gradle`/`build.gradle.kts`:

```kotlin
implementation("io.hammerhead:karoo-ext:1.1.6") // latest version as of Aug 2024
```

**Latest version:** Check [karoo-ext releases](https://github.com/hammerheadnav/karoo-ext/releases)
for the most recent version.

> Tip: If you prefer starting from the karoo-ext sample app (in the `karoo-ext` repo) you can open
> that project directly — it includes working examples of KarooSystemService and the sample Extension.
> Install with `./gradlew app:installDebug`.

### 4. Implement a minimal Karoo "hello" consumer (quick test)

From karoo-ext README, a minimal connection looks like this (Kotlin):

```kotlin
import io.hammerhead.karooext.KarooSystemService

class HelloActivity : Activity() {
    private val karooSystem by lazy { KarooSystemService(this) }

    override fun onStart() {
        super.onStart()
        karooSystem.connect {
            println("karoo system connected")
        }
    }

    override fun onStop() {
        karooSystem.disconnect()
        super.onStop()
    }
}
```

Register a consumer to receive `RideState` updates:

```kotlin
val consumerId = karooSystem.addConsumer { rideState: RideState ->
    // convert rideState to the metric messages you will send to ActiveLook
}
karooSystem.removeConsumer(consumerId)
```

This is the main hook for getting **all metrics** from Karoo to forward to the glasses. See
the [KarooSystemService documentation](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext/-karoo-system-service/index.html)
for details on available events and effects.

### 5. Add ActiveLook Android SDK

ActiveLook provides an Android SDK that simplifies BLE communication and display management. Add the
SDK to your project:

**Add JitPack repository** to your `settings.gradle` or `build.gradle`:

```groovy
dependencyResolutionManagement {
    repositories {
        // ... other repositories
        maven { url 'https://jitpack.io' }
    }
}
```

**Add the dependency** in your app `build.gradle`:

```groovy
dependencies {
    implementation 'com.github.activelook:android-sdk:4.5.6'  // latest version
}
```

**Request permissions** in your `AndroidManifest.xml`:

```xml

<uses-permission android:name="android.permission.BLUETOOTH" /><uses-permission
android:name="android.permission.BLUETOOTH_ADMIN" /><uses-permission
android:name="android.permission.ACCESS_FINE_LOCATION" /><uses-permission
android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### 6. Implement ActiveLook integration

**Initialize the SDK** (in your Application or Activity):

```kotlin
import com.activelook.activelooksdk.Sdk

Sdk.init(
    context,
    { update -> Log.d("GLASSES_UPDATE", "Starting update: $update") },
    { update -> Log.d("GLASSES_UPDATE", "Update available: $update"); true },
    { update -> Log.d("GLASSES_UPDATE", "Update progress: $update") },
    { update -> Log.d("GLASSES_UPDATE", "Update success: $update") },
    { update -> Log.e("GLASSES_UPDATE", "Update error: $update") }
)
```

**Scan for glasses:**

```kotlin
val sdk = Sdk.getInstance()
sdk.startScan { discoveredGlasses ->
    Log.d("SCAN", "Found: ${discoveredGlasses.name} at ${discoveredGlasses.address}")
    // Connect to glasses (see next step)
}
```

**Connect and display data:**

```kotlin
discoveredGlasses.connect(
    onConnected = { glasses ->
        Log.d("CONNECT", "Connected to ${glasses.deviceInformation.modelNumber}")

        // Display text on glasses
        glasses.txt(Point(50, 50), Rotation.TOP_LR, 0x00, 0xFF, "Hello Karoo!")

        // You can now forward Karoo metrics to the glasses
        // Use layouts and pages for efficient updates (see ActiveLook docs)
    },
    onConnectionFail = { discoveredGlasses ->
        Log.e("CONNECT", "Connection failed")
    },
    onDisconnected = { glasses ->
        Log.d("DISCONNECT", "Glasses disconnected")
    }
)
```

**Stop scanning when done:**

```kotlin
sdk.stopScan()
```

**ActiveLook best practices:**

- **Preload graphical resources:** Upload icons/layouts to glasses memory on first connection (phase
  1 setup)
- **Use layouts and pages:** Group display updates for efficiency instead of sending individual
  commands
- **Minimize updates:** Only refresh changed data (~1 update/second target). ActiveLook is a memory
  display
- **Hold & Flush pattern:** Wrap updates between `hold()` and `flush()` commands for smooth
  rendering
- **Battery optimization:** Limit update frequency and disable gesture sensor when not needed
- **Design for AR:** Use transparent backgrounds (black = transparent), prefer central display area
  with margins (30px horizontal, 25px vertical)

**Tools for BLE debugging:** `nRF Connect` (mobile) or `gatttool`/`bluetoothctl` on Linux; these
help you inspect GATT services and test commands

### 7. Bridge Karoo data to ActiveLook

Now connect the two systems: consume Karoo metrics and forward them to ActiveLook glasses:

```kotlin
class KarooActiveLookBridge : Activity() {
    private val karooSystem by lazy { KarooSystemService(this) }
    private val activeLookSdk by lazy { Sdk.getInstance() }
    private var connectedGlasses: Glasses? = null

    override fun onStart() {
        super.onStart()

        // Connect to Karoo system
        karooSystem.connect {
            Log.d("BRIDGE", "Karoo system connected")

            // Register consumer for ride data
            karooSystem.addConsumer { rideState: RideState ->
                // Forward metrics to ActiveLook
                connectedGlasses?.let { glasses ->
                    updateGlassesDisplay(glasses, rideState)
                }
            }
        }

        // Scan and connect to ActiveLook glasses
        activeLookSdk.startScan { discoveredGlasses ->
            discoveredGlasses.connect(
                onConnected = { glasses ->
                    connectedGlasses = glasses
                    setupGlassesLayout(glasses)
                },
                onConnectionFail = { /* handle error */ },
                onDisconnected = { connectedGlasses = null }
            )
        }
    }

    private fun setupGlassesLayout(glasses: Glasses) {
        // Upload layouts and configure display on first connection
        // (Implementation depends on your UI design)
    }

    private fun updateGlassesDisplay(glasses: Glasses, rideState: RideState) {
        // Extract metrics and update glasses display
        // Use hold/flush pattern for efficient updates
        glasses.hold()

        // Example: display speed
        rideState.speed?.let { speed ->
            glasses.txt(
                Point(100, 50), Rotation.TOP_LR, 0x00, 0xFF,
                String.format("%.1f km/h", speed)
            )
        }

        // Example: display heart rate
        rideState.heartRate?.let { hr ->
            glasses.txt(
                Point(100, 100), Rotation.TOP_LR, 0x00, 0xFF,
                String.format("%d bpm", hr)
            )
        }

        glasses.flush()
    }

    override fun onStop() {
        karooSystem.disconnect()
        connectedGlasses?.disconnect()
        super.onStop()
    }
}
```

### 8. Build & sideload to Karoo2

**Enable Developer Options on Karoo:** (Settings → About → tap Build Number 7× → Developer Options →
enable USB debugging)

**Sideload options:**

- **ADB install (classic)**: Connect Karoo via data USB cable, then run:

```bash
# from project root
./gradlew app:assembleDebug
adb devices          # verify Karoo shows up
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- **Companion App sideload (New Karoo / newer firmware)**: Share the APK URL or file to the
  Hammerhead Companion App on your phone — the Companion will transfer & prompt install on Karoo (
  requires firmware & companion versions). This is easier for many users

If `adb devices` doesn't list your Karoo: try a different data cable, enable USB Debugging on Karoo,
check Windows drivers, or use `adb kill-server` / `adb start-server`. Community tutorials and DC
Rainmaker have step‑by‑step sideload guides

### 9. Run & test

- Launch your app/extension on the Karoo. Use `adb logcat` to observe logs and verify
  KarooSystemService consumers are receiving `RideState` updates
- Confirm the BLE client can connect to ActiveLook and that simple commands render text/bitmaps. Use
  `nRF Connect` to manually test writes if needed

### 10. Mocks & unit testing locally

- Implement a **mock Karoo provider** inside your app (or as a separate debug build flavor) that
  emits synthetic `RideState` payloads, so you can develop ActiveLook rendering without having the
  Karoo connected.
- Use Android instrumentation/unit tests for formatting logic and small helpers. For end‑to‑end BLE
  tests you'll need a real device (ActiveLook).

### 11. CI / Docker (optional)

- Use Docker for headless Gradle builds in CI. Note: **BLE hardware cannot be tested inside a Docker
  container on Windows** — keep hardware integration tests on a physical machine. A typical CI job
  runs:

```yaml
# example: GitHub Actions (conceptual)
- uses: actions/checkout@v4
- name: Set up JDK 11
  uses: actions/setup-java@v4
  with:
    distribution: 'temurin'
    java-version: '11'
- name: Build
  run: ./gradlew assembleDebug --no-daemon
- name: Run Unit Tests
  run: ./gradlew test
```

If you prefer local container builds, use a Gradle-enabled Docker image and mount your Android SDK
inside it — but expect extra config effort for caching and license acceptance.

---

## Troubleshooting & tips

- **ADB not detecting Karoo:** try different USB cable (data-capable), enable USB debugging, try
  `adb devices` on a Linux or WSL2 host, check for Windows driver issues. Community posts and DC
  Rainmaker's sideload guide are helpful
- **Karoo-ext access:** If karoo-ext package access fails, ensure your GitHub token has
  `read:packages` or use the sample app in the karoo-ext repo to iterate locally
- **ActiveLook SDK issues:** Check that JitPack repository is properly configured and latest SDK
  version (4.5.6) is used
- **BLE throughput & latency:** Use preloaded layouts/pages on ActiveLook; only send small updates
  when values change (~1 update/second target)
- **ActiveLook display issues:** Remember black = transparent in AR. Use central display area with
  margins (30px horizontal, 25px vertical)
- **Battery optimization:** Wrap updates in hold/flush pattern, disable gesture sensor when not
  needed, minimize update frequency
- **Testing on the move:** test with real ride data and sensors (HR/power/cadence). The
  template/sample demonstrates scanning for sensors and streaming data
- **ActiveLook firmware:** Check firmware version on first connection and prompt users to update via
  ActiveLook Sports app if needed
- **Memory management:** ActiveLook has ~3MB flash for configurations. Preload your icons/layouts on
  first connection
- **Get help:** Visit
  the [Hammerhead Extensions Developers Community](https://support.hammerhead.io/hc/en-us/community/topics/31298804001435-Hammerhead-Extensions-Developers)
  to ask questions and share knowledge

---

## Useful commands (quick reference)

```bash
# clone template
git clone https://github.com/hammerheadnav/karoo-ext-template.git my-karoo-ext
# open in Android Studio, or build from CLI
./gradlew app:assembleDebug
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
./gradlew app:installDebug   # installs to any connected device
adb logcat | grep YourAppTag
```

---

---

## Links & references

- [Karoo Extensions comprehensive documentation](https://hammerheadnav.github.io/karoo-ext/index.html)
- [Karoo Extensions GitHub repository](https://github.com/hammerheadnav/karoo-ext)
- [Karoo extension template](https://github.com/hammerheadnav/karoo-ext-template)
- [Hammerhead Extensions Developers Community](https://support.hammerhead.io/hc/en-us/community/topics/31298804001435-Hammerhead-Extensions-Developers)
- [GitHub Packages with Gradle](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package)
- [ActiveLook Android SDK](https://github.com/ActiveLook/android-sdk) — SDK with examples and
  documentation
- [ActiveLook API Documentation](https://github.com/ActiveLook/Activelook-API-Documentation) —
  Complete BTLE protocol reference
- [ActiveLook Development Guide](https://www.activelook.net/news-blog/developing-with-activelook-getting-started) —
  Getting started and best practices
- [ActiveLook Demo App](https://github.com/ActiveLook/demo-app) — Reference implementation
- [ActiveLook Developer Kit](https://shop.activelook.net/index.php?id_product=21) — Hardware for
  testing
- [JitPack](https://jitpack.io/#ActiveLook/android-sdk) — Maven repository for ActiveLook SDK

---

If you'd like, I can also:

- Generate a ready‑to‑use Android Studio project stub (based on the Karoo template) with scaffolding
  for KarooSystemService consumers and a skeleton ActiveLook BLE client.
- Produce a small sample showing how to format & send a simple speed/HR update to ActiveLook (
  text-only), for quick end‑to‑end testing.

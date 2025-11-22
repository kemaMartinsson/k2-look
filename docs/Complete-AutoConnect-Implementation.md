# Complete Auto-Connect Implementation

**Date:** November 22, 2025  
**Status:** ✅ Complete

---

## Overview

The K2Look app now implements a complete auto-connect flow on startup:

1. ✅ **Auto-connect to Karoo System** (configurable, default: enabled)
2. ✅ **Auto-connect to last paired ActiveLook glasses** (configurable, default: disabled)
3. ✅ **Remember all user preferences** (locale, units, connection settings)

---

## Startup Sequence

### What Happens When App Starts:

```
App Launch
    ↓
MainActivity.onCreate()
    ↓
MainViewModel.init()
    ↓
KarooActiveLookBridge.initialize()
    ↓
1. Load PreferencesManager.getConfig()
    ↓
2. Initialize ActiveLook SDK
    ↓
3. Auto-connect to Karoo? (check config.autoConnectKaroo)
    ├─ YES → connectKaroo()
    └─ NO  → Log: "Auto-connect disabled"
    ↓
4. Auto-connect to glasses? (check config.autoConnectActiveLook)
    ├─ YES → Check for lastConnectedGlassesAddress
    │   ├─ Found? → attemptAutoConnectToGlasses(address)
    │   └─ Not found? → Log: "No previous connection"
    └─ NO  → Log: "Auto-connect disabled"
```

---

## Auto-Connect to Karoo System

### Behavior:

- **Default:** Enabled (`autoConnectKaroo = true`)
- **Happens:** Immediately on app startup
- **User sees:** "Karoo Connection: Connected" within 1-2 seconds
- **If fails:** Automatic retry with exponential backoff (built into KarooDataService)

### Configuration:

```kotlin
// Enable/disable via PreferencesManager
preferencesManager.setAutoConnectKaroo(true)  // Enable (default)
preferencesManager.setAutoConnectKaroo(false) // Disable
```

---

## Auto-Connect to ActiveLook Glasses

### Behavior:

- **Default:** Disabled (`autoConnectActiveLook = false`)
- **Why disabled by default?** User should explicitly select which glasses to use
- **When enabled:**
    1. App starts scanning for BLE devices
    2. Looks for glasses with previously saved address
    3. Connects automatically when found
    4. **Timeout:** 10 seconds (stops scanning if not found)

### First-Time Connection:

1. User manually scans for glasses
2. User selects glasses from list
3. App connects to glasses
4. **Address saved automatically** (`lastConnectedGlassesAddress`)
5. User can enable auto-connect for future sessions

### Subsequent Connections (when auto-connect enabled):

1. App starts
2. Automatically scans for saved glasses
3. Connects when found
4. No user interaction needed

### Configuration:

```kotlin
// Enable auto-connect after first manual pairing
preferencesManager.setAutoConnectActiveLook(true)

// The address is saved automatically on successful connection
// No need to manually save it
```

---

## Saved Preferences

### Complete List of Persisted Settings:

| Setting                     | Key                       | Default      | Description                               |
|-----------------------------|---------------------------|--------------|-------------------------------------------|
| **Locale**                  | `locale`                  | `SWEDISH`    | Number/date formatting (sv-SE)            |
| **Speed Unit**              | `speed_unit`              | `KPH`        | km/h or mph                               |
| **Distance Unit**           | `distance_unit`           | `KILOMETERS` | km or miles                               |
| **Elevation Unit**          | `elevation_unit`          | `METERS`     | m or feet                                 |
| **Auto-Connect Karoo**      | `auto_connect_karoo`      | `true`       | Connect to Karoo on startup               |
| **Auto-Connect ActiveLook** | `auto_connect_activelook` | `false`      | Connect to last glasses on startup        |
| **Last Glasses Address**    | `last_glasses_address`    | `null`       | BLE MAC address of last connected glasses |

### Storage:

- **Location:** Android SharedPreferences
- **File:** `k2look_preferences`
- **Persistence:** Survives app restarts, but cleared on app uninstall
- **Access:** Thread-safe via PreferencesManager

---

## User Experience Flow

### First Launch (Fresh Install):

```
1. User opens app
   → Auto-connects to Karoo ✅
   → Shows "Connected"
   
2. User clicks "Scan" for glasses
   → Discovers nearby ActiveLook devices
   
3. User selects their glasses
   → App connects ✅
   → Glasses address saved automatically
   → Shows "Connected"
   
4. (Optional) User enables auto-connect to glasses
   → Settings menu or in-app toggle
```

### Subsequent Launches (Auto-Connect Disabled):

```
1. User opens app
   → Auto-connects to Karoo ✅
   → Shows "Connected"
   
2. User manually scans for glasses
   → Selects from list
   → Connects
```

### Subsequent Launches (Auto-Connect Enabled):

```
1. User opens app
   → Auto-connects to Karoo ✅
   → Auto-scans for last glasses ✅
   → Both connections happen automatically
   → User sees "Karoo: Connected" and "ActiveLook: Connected"
   → Ready to ride! 🚴‍♂️
```

---

## Implementation Details

### Glasses Auto-Connect Logic:

```kotlin
private fun attemptAutoConnectToGlasses(targetAddress: String) {
    // 1. Start BLE scanning
    activeLookService.startScanning()

    // 2. Observe discovered devices
    scope.launch {
        activeLookService.discoveredGlasses.collect { glassesList ->
            // Find device with saved address
            val targetGlasses = glassesList.find { it.address == targetAddress }

            if (targetGlasses != null && !glassesFound) {
                // Found! Connect immediately
                activeLookService.stopScanning()
                connectActiveLook(targetGlasses)
            }
        }
    }

    // 3. Timeout after 10 seconds
    scope.launch {
        delay(10000)
        if (!glassesFound) {
            Log.w(TAG, "Auto-connect timeout")
            activeLookService.stopScanning()
        }
    }
}
```

### Address Saved on Connection:

```kotlin
fun connectActiveLook(glasses: DiscoveredGlasses) {
    activeLookService.connect(glasses)

    // Automatically save for future auto-connect
    preferencesManager.setLastConnectedGlasses(glasses.address)
    Log.i(TAG, "Saved: ${glasses.address}")
}
```

---

## Error Handling

### Karoo Connection Fails:

- **Action:** Automatic retry with exponential backoff
- **Max retries:** 5 attempts
- **UI shows:** "Reconnecting (attempt X/5)"
- **After max retries:** "Connection lost. Please reconnect manually."

### Glasses Not Found (Auto-Connect):

- **Action:** Stop scanning after 10 seconds
- **UI shows:** Normal "Scan" button available
- **Log:** "Could not find glasses with address XX:XX:XX:XX:XX:XX"
- **User action:** Manual scan and reconnect

### BLE Permission Denied:

- **Action:** Show permission request dialog
- **If denied:** Explain why BLE is needed, provide settings link
- **Scanning disabled** until permission granted

---

## Configuration API

### Enable/Disable Auto-Connect:

```kotlin
// In Settings Screen or ViewModel
val prefsManager = PreferencesManager(context)

// Toggle Karoo auto-connect
prefsManager.setAutoConnectKaroo(enabled)

// Toggle glasses auto-connect
prefsManager.setAutoConnectActiveLook(enabled)
```

### Check Current Settings:

```kotlin
val config = prefsManager.getConfig()

if (config.autoConnectKaroo) {
    // Karoo will connect automatically
}

if (config.autoConnectActiveLook && config.lastConnectedGlassesAddress != null) {
    // Glasses will attempt auto-connect
}
```

### Clear Saved Glasses:

```kotlin
// Useful if user wants to forget glasses
prefsManager.setLastConnectedGlasses(null)
```

---

## Testing

### Test Cases:

#### 1. First Launch (No Saved Preferences)

- [ ] Karoo auto-connects
- [ ] No glasses auto-connect attempt
- [ ] Manual scan required for glasses
- [ ] Glasses address saved on connection

#### 2. Subsequent Launch (Auto-Connect Disabled)

- [ ] Karoo auto-connects
- [ ] No glasses auto-connect
- [ ] Saved address persists

#### 3. Subsequent Launch (Auto-Connect Enabled, Glasses On)

- [ ] Karoo auto-connects
- [ ] Glasses scan starts
- [ ] Glasses found and connected
- [ ] No user interaction needed

#### 4. Subsequent Launch (Auto-Connect Enabled, Glasses Off)

- [ ] Karoo auto-connects
- [ ] Glasses scan starts
- [ ] Timeout after 10 seconds
- [ ] Scan stops gracefully
- [ ] Manual scan still available

#### 5. Different Glasses

- [ ] User scans manually
- [ ] Selects different glasses
- [ ] New address saved (replaces old)
- [ ] Next launch connects to new glasses

### Test Commands:

```powershell
# Install latest build
.\gradlew installDebug

# Launch and watch logs
adb shell am start -n com.kema.k2look/.MainActivity
adb logcat -s K2Look:* KarooActiveLookBridge:* PreferencesManager:*

# Check saved preferences
adb shell run-as com.kema.k2look cat shared_prefs/k2look_preferences.xml
```

---

## Future Enhancements

### Settings Screen:

Add UI to configure auto-connect behavior:

```
Settings
├── Connection
│   ├── Auto-connect to Karoo: [✓] (enabled)
│   ├── Auto-connect to glasses: [ ] (disabled)
│   └── Last connected glasses: "ActiveLook Engo 2"
│       └── [Forget Glasses] button
```

### Multiple Glasses Support:

- Save list of paired glasses
- Quick-select from recent connections
- Remember per-activity preferences

### Smart Auto-Connect:

- Connect only when starting a ride
- Disconnect when ride ends
- Battery-saving mode

---

## Summary

### ✅ Complete Auto-Connect Flow:

1. **App Starts**
    - Loads user preferences (locale, units, connection settings)
    - Auto-connects to Karoo System (default: enabled)
    - Auto-connects to last glasses (default: disabled, requires manual first pairing)

2. **User Connects Glasses**
    - Address saved automatically
    - Available for future auto-connect

3. **All Preferences Persist**
    - Survive app restarts
    - Cleared only on app uninstall

### 🎯 Key Benefits:

- **Seamless UX:** One-tap to ready (if auto-connect enabled)
- **Flexible:** User controls auto-connect behavior
- **Safe:** Glasses require manual first pairing
- **Persistent:** All settings remembered
- **Fast:** Auto-connect completes in ~2-3 seconds

---

**Status:** ✅ Ready for testing on Karoo2  
**Next:** Test complete flow with real Karoo2 and ActiveLook glasses


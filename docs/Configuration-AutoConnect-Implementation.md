# Configuration & Auto-Connect Implementation

**Date:** November 22, 2025  
**Status:** Complete ✅

---

## Changes Made

### 1. Auto-Connect to Karoo System ✅

**Problem:** The app required users to manually click "Connect" to connect to the Karoo System.

**Solution:** Implemented automatic connection on app startup.

#### Files Modified:

**`KarooActiveLookBridge.kt`:**

- Updated `initialize()` method to automatically call `connectKaroo()` on startup
- Added log message: "Auto-connecting to Karoo System..."

**`MainScreen.kt`:**

- Removed Connect/Disconnect buttons for Karoo
- Show only connection status
- Added explanatory text: "Auto-connects on startup"

#### Behavior:

1. App starts → `MainViewModel` initializes → `KarooActiveLookBridge.initialize()` called
2. Bridge automatically calls `connectKaroo()`
3. UI shows connection status (Connecting → Connected)
4. No user interaction needed for Karoo connection

---

### 2. Configuration System ✅

**Problem:** No way to configure user preferences (locale, units, auto-connect settings).

**Solution:** Created comprehensive configuration system with persistent storage.

#### New Files Created:

**`model/AppConfig.kt`:**

- `AppConfig` data class - Main configuration container
- `AppLocale` enum - Supported locales (Swedish default)
- `SpeedUnit` enum - KPH / MPH
- `DistanceUnit` enum - Kilometers / Miles
- `ElevationUnit` enum - Meters / Feet

**`util/PreferencesManager.kt`:**

- SharedPreferences-based persistence
- Get/Set methods for all configuration
- Individual setting accessors
- KTX-ready (warnings about edit() usage)

#### Configuration Options:

| Setting                       | Type          | Default         | Description                      |
|-------------------------------|---------------|-----------------|----------------------------------|
| `locale`                      | AppLocale     | SWEDISH (sv-SE) | Number/date formatting           |
| `speedUnit`                   | SpeedUnit     | KPH             | km/h or mph                      |
| `distanceUnit`                | DistanceUnit  | KILOMETERS      | km or miles                      |
| `elevationUnit`               | ElevationUnit | METERS          | m or feet                        |
| `autoConnectKaroo`            | Boolean       | true            | Auto-connect to Karoo on startup |
| `autoConnectActiveLook`       | Boolean       | false           | Auto-connect to last glasses     |
| `lastConnectedGlassesAddress` | String?       | null            | Last paired glasses MAC address  |

---

### 3. Locale Support in MetricFormatter ✅

**Problem:** MetricFormatter used hardcoded US locale for formatting.

**Solution:** Updated to use Swedish locale by default, with configuration support.

#### Files Modified:

**`MetricFormatter.kt`:**

- Added `defaultLocale = Locale("sv", "SE")`
- Added config-aware formatting methods:
    - `formatSpeed(metricData, config)`
    - `formatDistance(metricData, config)`
    - `formatElevation(metricData, config)`
- Updated all numeric formatting to accept `locale` parameter
- Time formatting uses Swedish locale

#### Swedish Format Characteristics:

- **Decimal Separator:** Comma (`,`) instead of period (`.`)
    - Example: `25,5 km/h` (Swedish) vs `25.5 km/h` (US)
- **Time Format:** 24-hour clock (HH:MM:SS)
- **Date Format:** YYYY-MM-DD (ISO 8601)

#### Usage Examples:

```kotlin
// Using default Swedish locale
val speed = formatSpeed(speedData)  // "25,5 km/h"

// Using config
val config = prefsManager.getConfig()
val speed = formatSpeed(speedData, config)  // Uses user's preferred locale & unit

// Using explicit locale and unit
val speed = formatSpeed(speedData, useMiles = false, locale = Locale("sv", "SE"))
```

---

## Architecture

### Data Flow for Configuration:

```
App Startup
    ↓
PreferencesManager.getConfig()
    ↓
AppConfig (loaded from SharedPreferences)
    ↓
Passed to formatters/services
    ↓
Applied to metric display
```

### Auto-Connect Flow:

```
App Startup
    ↓
MainActivity.onCreate()
    ↓
MainViewModel.init()
    ↓
KarooActiveLookBridge.initialize()
    ↓
connectKaroo() [automatic]
    ↓
KarooDataService.connect()
    ↓
UI shows "Connected"
```

---

## Default Configuration

### Out-of-Box Settings:

```kotlin
AppConfig(
    locale = AppLocale.SWEDISH,           // sv-SE
    speedUnit = SpeedUnit.KPH,            // km/h
    distanceUnit = DistanceUnit.KILOMETERS, // km
    elevationUnit = ElevationUnit.METERS,  // m
    autoConnectKaroo = true,              // Auto-connect enabled
    autoConnectActiveLook = false,        // Manual glass selection
    lastConnectedGlassesAddress = null    // No previous connection
)
```

### Why These Defaults?

1. **Swedish Locale:** Requested by user, matches target market
2. **Metric Units:** European cycling standard
3. **Auto-Connect Karoo:** Simplifies user experience
4. **Manual ActiveLook:** User must select specific glasses

---

## User Experience Improvements

### Before:

1. User opens app
2. User clicks "Connect" to connect to Karoo
3. Manual step required every time

### After:

1. User opens app
2. ✅ Automatically connects to Karoo
3. Shows "Karoo Connection: Connected"
4. User can immediately scan for glasses

---

## Future Enhancements

### Settings Screen (TODO)

A settings screen could be added to allow users to configure:

- [ ] Language/Locale selection
- [ ] Unit preferences (metric/imperial)
- [ ] Auto-connect toggles
- [ ] Display preferences
- [ ] Debug options

**Mockup:**

```
Settings
├── Units
│   ├── Speed: [KPH | MPH]
│   ├── Distance: [Kilometers | Miles]
│   └── Elevation: [Meters | Feet]
├── Language
│   └── Locale: [Swedish | English | ...]
├── Connection
│   ├── Auto-connect Karoo: [✓]
│   └── Auto-connect ActiveLook: [ ]
└── About
    ├── Version: 1.0
    └── Licenses
```

### Implementation:

```kotlin
// SettingsScreen.kt
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val config by viewModel.config.collectAsState()

    // UI with switches, dropdowns for each setting
    // Save button calls: prefsManager.saveConfig(config)
}
```

---

## Testing

### Manual Testing Checklist:

- [x] App builds successfully
- [ ] App auto-connects to Karoo on startup
- [ ] Connection status displays correctly
- [ ] Swedish locale formats numbers with commas
- [ ] Speed shows in km/h by default
- [ ] Distance shows in km by default
- [ ] Time format is HH:MM:SS
- [ ] Preferences persist across app restarts

### Testing on Karoo2:

```powershell
# Install latest build
.\gradlew installDebug

# Launch app
adb shell am start -n com.kema.k2look/.MainActivity

# Check logs for auto-connect
adb logcat -s K2Look:* KarooActiveLookBridge:*

# Expected log:
# I/KarooActiveLookBridge: Initializing KarooActiveLookBridge...
# I/KarooActiveLookBridge: Auto-connecting to Karoo System...
# I/KarooActiveLookBridge: Connecting to Karoo System...
# I/KarooDataService: Successfully connected to KarooSystem
```

---

## Configuration API Reference

### PreferencesManager

```kotlin
class PreferencesManager(context: Context) {
    // Get full configuration
    fun getConfig(): AppConfig

    // Save full configuration
    fun saveConfig(config: AppConfig)

    // Individual setters
    fun setLocale(locale: AppLocale)
    fun setSpeedUnit(unit: SpeedUnit)
    fun setDistanceUnit(unit: DistanceUnit)
    fun setElevationUnit(unit: ElevationUnit)
    fun setAutoConnectKaroo(enabled: Boolean)
    fun setAutoConnectActiveLook(enabled: Boolean)
    fun setLastConnectedGlasses(address: String?)

    // Individual getters
    fun getLocale(): AppLocale
    fun getSpeedUnit(): SpeedUnit
    fun getDistanceUnit(): DistanceUnit
    fun getElevationUnit(): ElevationUnit
    fun isAutoConnectKarooEnabled(): Boolean
    fun isAutoConnectActiveLookEnabled(): Boolean
    fun getLastConnectedGlassesAddress(): String?
}
```

### Usage Example:

```kotlin
// In ViewModel or Activity
val prefsManager = PreferencesManager(context)

// Load configuration
val config = prefsManager.getConfig()

// Use in formatter
val formattedSpeed = MetricFormatter.formatSpeed(speedData, config)

// Update setting
prefsManager.setSpeedUnit(SpeedUnit.MPH)

// Or update full config
val newConfig = config.copy(speedUnit = SpeedUnit.MPH, locale = AppLocale.ENGLISH)
prefsManager.saveConfig(newConfig)
```

---

## Files Modified/Created

### New Files:

- ✅ `model/AppConfig.kt` - Configuration models
- ✅ `util/PreferencesManager.kt` - Persistence layer

### Modified Files:

- ✅ `service/KarooActiveLookBridge.kt` - Auto-connect on init
- ✅ `screens/MainScreen.kt` - Removed Karoo connect buttons
- ✅ `util/MetricFormatter.kt` - Swedish locale, config support

---

## Summary

### ✅ Completed

1. **Auto-Connect to Karoo**
    - Connects automatically on app startup
    - No manual user action required
    - UI shows status only (no connect button)

2. **Configuration System**
    - AppConfig model with all settings
    - PreferencesManager for persistence
    - Default to Swedish locale and metric units
    - Ready for future settings screen

3. **Locale Support**
    - Swedish locale (sv-SE) as default
    - Decimal comma separator (25,5 instead of 25.5)
    - 24-hour time format
    - Config-aware formatting methods

### 🎯 Benefits

- **Simpler UX:** One less step for users
- **Localized:** Proper Swedish formatting
- **Configurable:** Ready for user preferences
- **Persistent:** Settings survive app restarts
- **Extensible:** Easy to add new settings

---

**Status:** ✅ Ready for testing on Karoo2  
**Next:** Test auto-connect behavior and Swedish formatting on real device


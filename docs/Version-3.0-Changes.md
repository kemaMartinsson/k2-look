# Version 3.0 - Karoo UserProfile Integration

**Release Date:** 2025-01-23

## Overview

Version 3.0 removes custom locale and unit system implementations in favor of using Karoo's built-in
UserProfile settings. This ensures the app automatically respects the user's Karoo configuration for
units (metric/imperial) and removes unnecessary duplication.

## Major Changes

### 1. **UserProfile Integration**

- Added observation of Karoo's `UserProfile` event
- App now automatically detects if user has configured Karoo for imperial or metric units
- Unit preferences are automatically applied from Karoo settings:
    - `preferredUnit.distance` - km/h vs mph
    - `preferredUnit.elevation` - meters vs feet
    - `preferredUnit.temperature` - Celsius vs Fahrenheit
    - `preferredUnit.weight` - kg vs lbs

### 2. **Removed Custom Implementations**

The following custom implementations were removed since they're now provided by Karoo:

#### Deleted:

- `AppLocale` enum (Swedish, English, German, French, Italian, Spanish)
- `SpeedUnit` enum (KPH, MPH)
- `DistanceUnit` enum (Kilometers, Miles)
- `ElevationUnit` enum (Meters, Feet)
- `AppConfig` data class

#### Simplified:

- **PreferencesManager** - Now only handles:
    - Auto-connect settings (Karoo & glasses)
    - Last connected glasses address
    - All unit/locale preferences removed

### 3. **Updated Files**

#### Modified:

- `MainViewModel.kt`
    - Added `userProfile: UserProfile?` to UiState
    - Added `useImperialUnits: Boolean` to UiState
    - Added `observeUserProfile()` method
    - Observes Karoo UserProfile and extracts unit preferences

- `KarooDataService.kt`
    - Added `getKarooSystem()` method for direct access to KarooSystemService

- `KarooActiveLookBridge.kt`
    - Updated `initialize()` to use simplified PreferencesManager methods
    - Removed config object loading

- `PreferencesManager.kt`
    - Removed all locale and unit-related methods
    - Simplified to only handle connection preferences
    - Updated javadoc to "Version 3"

- `BuildConfig.kt` (temporary placeholder)
    - Updated to version 3.0

- `build.gradle.kts`
    - Updated versionCode to 3
    - Updated versionName to "3.0"

## Benefits

### For Users:

✅ **Consistent Experience** - Units match what's shown on Karoo device
✅ **No Duplicate Settings** - Configure once in Karoo, applies everywhere
✅ **Automatic Updates** - Changing Karoo settings instantly updates the app

### For Developers:

✅ **Less Code** - Removed ~200 lines of custom unit/locale handling
✅ **Single Source of Truth** - Karoo UserProfile is the authority
✅ **Future-Proof** - New Karoo unit types automatically supported

## Migration Notes

### Breaking Changes:

- `AppConfig` class removed - use `UserProfile` from `MainViewModel.UiState`
- Custom unit enums removed - use `UserProfile.PreferredUnit.UnitType`
- PreferencesManager no longer has `getConfig()` or `saveConfig()` methods

### How to Access Unit Preferences:

```kotlin
// Old way (removed):
val config = preferencesManager.getConfig()
val useImperial = config.speedUnit.useMiles

// New way (v3.0):
val uiState = viewModel.uiState.collectAsState()
val useImperial = uiState.value.useImperialUnits
val userProfile = uiState.value.userProfile
```

## Technical Details

### UserProfile Event

The app now consumes the `UserProfile` event from KarooSystemService:

```kotlin
karooDataService.getKarooSystem().addConsumer<UserProfile> { profile ->
    val useImperial = profile.preferredUnit.distance ==
            UserProfile.PreferredUnit.UnitType.IMPERIAL
    // Update UI state
}
```

### Available Unit Types:

- `METRIC` - km, m, kg, °C
- `IMPERIAL` - mi, ft, lbs, °F

## Testing

To test unit switching:

1. Open Karoo Settings → User Profile
2. Change "Units" between Metric and Imperial
3. K2-Look will automatically detect and apply the new setting
4. No app restart required

## Future Enhancements

Now that we're integrated with Karoo's UserProfile, we can also access:

- User's weight
- Max heart rate & resting heart rate
- Heart rate zones
- FTP (Functional Threshold Power)
- Power zones

These could be used for future features like:

- Personalized training zone displays
- Power-to-weight calculations
- HR zone warnings

## Version History

- **v1.0** - Initial release with custom locale/unit system
- **v2.0** - (skipped)
- **v3.0** - Integrated with Karoo UserProfile, removed custom implementations

---

**Note:** This version requires a connected Karoo device to function properly. The UserProfile data
is provided by Karoo System and will not be available when testing without a Karoo connection.


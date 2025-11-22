# Feature Implementation Status

**Date:** 2024-11-22  
**Project:** K2-Look (Karoo2 ↔ ActiveLook Gateway)

## Summary

This document provides the implementation status of requested features and what's available from the
Karoo2 SDK.

---

## ✅ AVAILABLE & CAN BE IMPLEMENTED

### 1. HR Zones

**Status:** ✅ Available from Karoo2  
**DataType:** `DataType.Type.HR_ZONE`  
**Field:** `Field.HR_ZONE`  
**Implementation:** NOT YET IMPLEMENTED

**Description:**

- Karoo2 provides current HR zone (typically 1-5 based on user's HR zone configuration)
- Zones are configured in Karoo2 settings (user profile)
- Returns integer value representing the zone

**To Implement:**

```kotlin
// Add to KarooDataService.kt
private val _hrZoneData = MutableStateFlow<StreamState?>(null)
val hrZoneData: StateFlow<StreamState?> = _hrZoneData.asStateFlow()

// In registerConsumers()
val hrZoneId = karooSystem.addConsumer(
    OnStreamState.StartStreaming(DataType.Type.HR_ZONE),
    onError = { error -> Log.e(TAG, "HR Zone stream error: $error") }
) { event: OnStreamState ->
    _hrZoneData.value = event.state
}
consumerIds.add(hrZoneId)
```

---

### 2. Smoothed Power (3s/10s/30s)

**Status:** ✅ Available from Karoo2  
**DataTypes:**

- `DataType.Type.SMOOTHED_3S_AVERAGE_POWER`
- `DataType.Type.SMOOTHED_10S_AVERAGE_POWER`
- `DataType.Type.SMOOTHED_30S_AVERAGE_POWER`

**Fields:**

- `Field.SMOOTHED_3S_AVERAGE_POWER`
- `Field.SMOOTHED_10S_AVERAGE_POWER`
- `Field.SMOOTHED_30S_AVERAGE_POWER`

**Implementation:** NOT YET IMPLEMENTED

**Description:**

- Rolling average power over 3, 10, or 30 second windows
- Useful for smoothing out power spikes
- Very common in cycling training (especially 3s and 30s)

**To Implement:**

```kotlin
// Add to KarooDataService.kt
private val _smoothed3sPowerData = MutableStateFlow<StreamState?>(null)
val smoothed3sPowerData: StateFlow<StreamState?> = _smoothed3sPowerData.asStateFlow()

private val _smoothed10sPowerData = MutableStateFlow<StreamState?>(null)
val smoothed10sPowerData: StateFlow<StreamState?> = _smoothed10sPowerData.asStateFlow()

private val _smoothed30sPowerData = MutableStateFlow<StreamState?>(null)
val smoothed30sPowerData: StateFlow<StreamState?> = _smoothed30sPowerData.asStateFlow()

// Register consumers for each
```

**Additional Available:**

- `SMOOTHED_5S_AVERAGE_POWER` (5 second)
- `SMOOTHED_20M_AVERAGE_POWER` (20 minute)
- `SMOOTHED_1HR_AVERAGE_POWER` (1 hour)

---

### 3. VAM (Vertical Ascent Meters)

**Status:** ✅ Available from Karoo2  
**DataTypes:**

- `DataType.Type.VERTICAL_SPEED` - Current VAM
- `DataType.Type.AVERAGE_VERTICAL_SPEED` - Average VAM this ride
- `DataType.Type.AVERAGE_VERTICAL_SPEED_30S` - 30s rolling average

**Field:** `Field.VERTICAL_SPEED`  
**Implementation:** NOT YET IMPLEMENTED

**Description:**

- Rate of vertical ascent in meters/hour
- Popular metric for climbing performance
- Example: 1000 m/h means climbing 1000 meters of elevation in 1 hour

**To Implement:**

```kotlin
// Add to KarooDataService.kt
private val _vamData = MutableStateFlow<StreamState?>(null)
val vamData: StateFlow<StreamState?> = _vamData.asStateFlow()

private val _avgVamData = MutableStateFlow<StreamState?>(null)
val avgVamData: StateFlow<StreamState?> = _avgVamData.asStateFlow()

// Register consumers
val vamId = karooSystem.addConsumer(
    OnStreamState.StartStreaming(DataType.Type.VERTICAL_SPEED),
    onError = { error -> Log.e(TAG, "VAM stream error: $error") }
) { event: OnStreamState ->
    _vamData.value = event.state
}
```

**Related Elevation Metrics Also Available:**

- `PRESSURE_ELEVATION_CORRECTION` - Current altitude
- `ELEVATION_GAIN` - Total elevation gain
- `ELEVATION_LOSS` - Total descent
- `ELEVATION_GRADE` - Current grade percentage
- `MIN_ELEVATION` / `MAX_ELEVATION` / `AVERAGE_ELEVATION`

---

## ❌ NOT AVAILABLE FROM KAROO-EXT

### 4. Turn-by-Turn Navigation

**Status:** ❌ NOT AVAILABLE  
**Reason:** Navigation data is not exposed through karoo-ext library

**Findings:**

- No navigation-related DataTypes in `DataType.kt`
- No navigation models or services in karoo-ext source code
- Searched for: "Navigation", "Route", "Turn", "Waypoint", "Direction" - no results

**Alternative Approaches:**

1. **Request Feature from Hammerhead:** Post
   in [Extensions Developers Community](https://support.hammerhead.io/hc/en-us/community/topics/31298804001435-Hammerhead-Extensions-Developers)
2. **Wait for Future API:** Navigation access may be added in future karoo-ext releases
3. **Manual Implementation:** User manually configures turn prompts (not practical)

**Current Status:** CANNOT BE IMPLEMENTED with current karoo-ext SDK

---

### 5. Distance to Next Turn

**Status:** ❌ NOT AVAILABLE  
**Reason:** Same as Turn-by-Turn - navigation data not exposed

**Workaround:** None available with current SDK

---

## Summary Table

| Feature          | Available? | DataType                     | Implemented? | Priority |
|------------------|------------|------------------------------|--------------|----------|
| HR Zones         | ✅ Yes      | `HR_ZONE`                    | ❌ No         | Medium   |
| 3s Power         | ✅ Yes      | `SMOOTHED_3S_AVERAGE_POWER`  | ❌ No         | High     |
| 10s Power        | ✅ Yes      | `SMOOTHED_10S_AVERAGE_POWER` | ❌ No         | High     |
| 30s Power        | ✅ Yes      | `SMOOTHED_30S_AVERAGE_POWER` | ❌ No         | High     |
| VAM              | ✅ Yes      | `VERTICAL_SPEED`             | ❌ No         | Medium   |
| Turn-by-Turn     | ❌ No       | N/A                          | ❌ No         | Low      |
| Distance to Turn | ❌ No       | N/A                          | ❌ No         | Low      |

---

## Recommendations

### High Priority (Should Implement Soon)

1. **Smoothed Power (3s/10s/30s)** - Very popular training metrics
    - Essential for serious cyclists with power meters
    - Easy to implement (same pattern as other metrics)

### Medium Priority (Nice to Have)

2. **HR Zones** - Useful for training guidance
    - Requires displaying zone number (1-5) or colored indicator
    - Consider ActiveLook display constraints

3. **VAM** - Great for climbing
    - Particularly useful for mountainous rides
    - Could be added to elevation metrics section

### Low Priority / Blocked

4. **Navigation Features** - Not currently possible
    - Track karoo-ext releases for future support
    - Consider submitting feature request to Hammerhead

---

## Implementation Effort Estimate

| Feature          | Effort      | Files to Modify                                        |
|------------------|-------------|--------------------------------------------------------|
| HR Zones         | 1 hour      | KarooDataService, MainViewModel, MainScreen            |
| 3s/10s/30s Power | 2 hours     | KarooDataService, MainViewModel, MainScreen, UI layout |
| VAM              | 1 hour      | KarooDataService, MainViewModel, MainScreen            |
| **Total**        | **4 hours** | Same files across all features                         |

---

## Next Steps

### Option 1: Implement All Available Features

Add HR Zones, Smoothed Power, and VAM to complete Phase 2.3 (Advanced Metrics)

### Option 2: Move to Phase 3

Focus on ActiveLook display implementation, add these metrics later

### Option 3: Hybrid Approach (Recommended)

1. Implement 3s/10s/30s Power (most valuable for serious cyclists)
2. Move to Phase 3 (ActiveLook display)
3. Add HR Zones and VAM later as enhancements

---

## References

- **karoo-ext DataType.kt:**
  `reference/karoo-ext/lib/src/main/kotlin/io/hammerhead/karooext/models/DataType.kt`
- **karoo-ext Documentation:** https://hammerheadnav.github.io/karoo-ext/index.html
- **Community Forum:
  ** https://support.hammerhead.io/hc/en-us/community/topics/31298804001435-Hammerhead-Extensions-Developers


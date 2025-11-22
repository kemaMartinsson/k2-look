# Phase 2: Metric Implementation - Progress Report

**Date:** November 22, 2025  
**Status:** In Progress

---

## Overview

Phase 2 focuses on implementing comprehensive metric display and formatting for cycling data from
the Karoo2, preparing it for display on ActiveLook glasses.

---

## Completed Work

### 2.1 Foundation Classes ✅

#### MetricData Model (`model/MetricData.kt`)

**Purpose:** Store comprehensive metric information including current, average, and max values

**Features:**

- `MetricData` data class with current/average/max values
- `MetricState` enum (IDLE, SEARCHING, STREAMING, NOT_AVAILABLE)
- `RideMetrics` comprehensive data class for all ride metrics
- Extension function to convert `StreamState` to `MetricState`

**Benefits:**

- Unified metric representation across the app
- Easy to extend with additional statistics
- Type-safe metric states

#### MetricFormatter Utility (`util/MetricFormatter.kt`)

**Purpose:** Format metrics for display with proper units and precision

**Supported Metrics:**

1. **Speed**
    - km/h or mph support
    - 1 decimal place precision
    - Current/average/max formatting

2. **Heart Rate**
    - BPM (no decimals)
    - Current/average/max formatting

3. **Cadence**
    - RPM (no decimals)
    - Current/average/max formatting

4. **Power**
    - Watts (no decimals)
    - Current/average/max formatting

5. **Distance**
    - km or miles support
    - 2 decimal places precision
    - Current value only (no average/max)

6. **Elevation**
    - Meters or feet support
    - No decimals
    - Current value only

7. **Time**
    - HH:MM:SS format
    - Compact format (MM:SS for < 1 hour)
    - Millisecond input

**Features:**

- Unit conversion (metric/imperial)
- Configurable decimal precision
- State-aware display strings
- Detailed display with avg/max
- Simple display with current only

---

## Current Implementation Status

### 2.1 Basic Metrics Display

| Metric       | Current | Average | Max | Units     | Status   |
|--------------|---------|---------|-----|-----------|----------|
| **Speed**    | ✅       | ⏳       | ⏳   | km/h, mph | Partial  |
| **Distance** | ✅       | N/A     | N/A | km, mi    | Complete |
| **Time**     | ✅       | N/A     | N/A | HH:MM:SS  | Complete |

### 2.2 Performance Metrics

| Metric         | Current | Average | Max | Units | Status  |
|----------------|---------|---------|-----|-------|---------|
| **Heart Rate** | ✅       | ⏳       | ⏳   | bpm   | Partial |
| **Cadence**    | ✅       | ⏳       | ⏳   | rpm   | Partial |
| **Power**      | ✅       | ⏳       | ⏳   | watts | Partial |

### 2.3 Advanced Metrics

| Metric         | Current | Units   | Status      |
|----------------|---------|---------|-------------|
| **Elevation**  | ⏳       | m, ft   | Not Started |
| **Battery**    | ⏳       | %       | Not Started |
| **Navigation** | ⏳       | various | Not Started |

---

## Next Steps

### Immediate Tasks (Phase 2.1 Completion)

1. **Implement Average/Max Tracking**
    - [ ] Create `MetricAccumulator` class to track running averages
    - [ ] Track max values per metric
    - [ ] Update `KarooDataService` to accumulate metrics
    - [ ] Persist metrics across app restarts (optional)

2. **Update ViewModel**
    - [ ] Use new `MetricData` model
    - [ ] Apply `MetricFormatter` for all displays
    - [ ] Add average/max fields to UI state
    - [ ] Handle metric resets (on ride start)

3. **Enhance UI**
    - [ ] Display average values
    - [ ] Display max values
    - [ ] Add toggle between simple/detailed view
    - [ ] Show state indicators (searching, N/A, etc.)

### Phase 2.2 Tasks

1. **Advanced Power Metrics**
    - [ ] Implement 3-second power average
    - [ ] Implement 10-second power average
    - [ ] Implement 30-second power average
    - [ ] Add normalized power calculation

2. **Heart Rate Zones** (Optional)
    - [ ] Define HR zones (Z1-Z5)
    - [ ] Calculate current zone
    - [ ] Track time in each zone

### Phase 2.3 Tasks

1. **Elevation Metrics**
    - [ ] Add elevation data stream
    - [ ] Track elevation gain
    - [ ] Track elevation loss
    - [ ] Format with m/ft support

2. **Battery Monitoring**
    - [ ] Karoo2 battery level
    - [ ] Sensor battery levels
    - [ ] Display warnings for low battery

---

## Architecture

### Data Flow

```
KarooSystemService (Karoo2)
    ↓
StreamState (raw sensor data)
    ↓
KarooDataService (data streams)
    ↓
MetricAccumulator (calculate avg/max)
    ↓
MetricData (current/avg/max)
    ↓
MetricFormatter (format for display)
    ↓
FormattedMetric (display strings)
    ↓
UI (MainScreen)
```

### Key Classes

1. **Model Layer**
    - `MetricData` - Metric with statistics
    - `MetricState` - State enum
    - `RideMetrics` - Complete metric collection

2. **Utility Layer**
    - `MetricFormatter` - Display formatting
    - `FormattedMetric` - Formatted output
    - `MetricAccumulator` - Statistics calculator (TODO)

3. **Service Layer**
    - `KarooDataService` - Data stream management
    - `ActiveLookService` - Glasses communication
    - `KarooActiveLookBridge` - Data pipeline

4. **UI Layer**
    - `MainViewModel` - State management
    - `MainScreen` - UI display
    - `MetricCard` - Metric display component

---

## Testing Plan

### Unit Tests (TODO)

1. **MetricFormatter Tests**
    - [ ] Speed conversion (km/h ↔ mph)
    - [ ] Distance conversion (km ↔ miles)
    - [ ] Elevation conversion (m ↔ feet)
    - [ ] Time formatting (edge cases)
    - [ ] Precision/rounding

2. **MetricAccumulator Tests**
    - [ ] Running average calculation
    - [ ] Max value tracking
    - [ ] Reset functionality
    - [ ] Edge cases (null values, zeros)

3. **MetricData Tests**
    - [ ] State conversions
    - [ ] Data validation

### Integration Tests (TODO)

1. **End-to-End Metric Flow**
    - [ ] Karoo → Service → Format → UI
    - [ ] Verify all metrics update
    - [ ] Test state transitions
    - [ ] Test error handling

### Field Tests (TODO)

1. **Real Ride Testing**
    - [ ] Start ride on Karoo2
    - [ ] Verify all metrics populate
    - [ ] Check average/max calculations
    - [ ] Monitor update frequency
    - [ ] Test for extended duration (1+ hour)

---

## Code Examples

### Using MetricData

```kotlin
val speedMetric = MetricData(
    current = 25.5,
    average = 22.3,
    max = 35.2,
    unit = "km/h",
    state = MetricState.STREAMING
)
```

### Using MetricFormatter

```kotlin
// Format speed in mph
val formattedSpeed = MetricFormatter.formatSpeed(speedMetric, useMiles = true)
// FormattedMetric(current="15.9", average="13.9", max="21.9", unit="mph")

// Get display string
val display = MetricFormatter.getDisplayString(formattedSpeed)
// "15.9 mph"

// Get detailed display
val detailed = MetricFormatter.getDetailedDisplayString(formattedSpeed)
// "15.9 mph | avg: 13.9 | max: 21.9"
```

### Using MetricFormatter for Time

```kotlin
val milliseconds = 3665000L // 1 hour, 1 minute, 5 seconds
val timeStr = MetricFormatter.formatTime(milliseconds)
// "01:01:05"

val compactStr = MetricFormatter.formatTimeCompact(milliseconds)
// "1:01"
```

---

## Configuration Options

### Future User Preferences

- [ ] Unit system (metric/imperial)
- [ ] Decimal precision per metric
- [ ] Show/hide average values
- [ ] Show/hide max values
- [ ] Metric order/layout
- [ ] Auto-reset on ride start

---

## Performance Considerations

### Metric Update Frequency

- **Current Values:** Real-time (as received from Karoo)
- **Average Calculation:** Per data point received
- **Max Tracking:** Per data point received
- **UI Updates:** Throttled to 1 Hz (via KarooActiveLookBridge)

### Memory Usage

- Minimal: Only storing current/avg/max per metric
- Future: May add sliding window buffers for power averages

### CPU Usage

- Formatting is lightweight (string operations)
- Average calculation is O(1) using running average
- Max tracking is O(1) comparison

---

## Known Limitations

1. **No Historical Data**
    - Currently only tracking current ride
    - No ride history/database
    - No lap tracking

2. **No Lap Support**
    - Can't split metrics by lap
    - No lap average/max

3. **No Custom Zones**
    - HR zones not configurable
    - No power zones (FTP-based)

4. **No Unit Persistence**
    - Unit preference not saved
    - Defaults to metric

---

## Success Criteria

### Phase 2.1 Complete When:

- [ ] All basic metrics show current, average, and max
- [ ] Proper unit support (metric/imperial)
- [ ] UI displays all values correctly
- [ ] Metrics reset properly on new ride

### Phase 2.2 Complete When:

- [ ] All performance metrics implemented
- [ ] Power averages (3s/10s/30s) working
- [ ] HR zones functional (if implemented)
- [ ] Field tested for accuracy

### Phase 2.3 Complete When:

- [ ] Elevation metrics working
- [ ] Battery monitoring functional
- [ ] All advanced features tested

---

## File Structure

```
app/src/main/kotlin/com/kema/k2look/
├── model/
│   └── MetricData.kt          ✅ Complete
├── util/
│   └── MetricFormatter.kt     ✅ Complete
│   └── MetricAccumulator.kt   ⏳ TODO
├── service/
│   ├── KarooDataService.kt    ✅ Data streams working
│   ├── ActiveLookService.kt   ✅ BLE working
│   └── KarooActiveLookBridge.kt ✅ Bridge working
├── viewmodel/
│   └── MainViewModel.kt       ⏳ Needs MetricData integration
└── screens/
    └── MainScreen.kt          ⏳ Needs avg/max display
```

---

## Documentation

- [MetricData.kt](../app/src/main/kotlin/com/kema/k2look/model/MetricData.kt) - Data models
- [MetricFormatter.kt](../app/src/main/kotlin/com/kema/k2look/util/MetricFormatter.kt) - Formatting
  utility
- [TODO.md](../TODO.md) - Overall project status

---

**Status:** Foundation Complete ✅ | Integration In Progress 🔄 | Testing Pending ⏳

**Next Milestone:** Complete Phase 2.1 with average/max tracking and UI integration


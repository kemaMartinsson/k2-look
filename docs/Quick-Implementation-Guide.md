# Quick Implementation Guide - Available Features

This guide shows exactly how to add the available features to K2-Look.

## 1. HR Zones (15 minutes)

### Step 1: Add to KarooDataService.kt

```kotlin
// Add after other metric StateFlows (around line 67)
private val _hrZoneData = MutableStateFlow<StreamState?>(null)
val hrZoneData: StateFlow<StreamState?> = _hrZoneData.asStateFlow()

// Add in registerConsumers() after maxHeartRateData (around line 220)
// Register HR Zone stream
val hrZoneId = karooSystem.addConsumer(
    OnStreamState.StartStreaming(DataType.Type.HR_ZONE),
    onError = { error ->
        Log.e(TAG, "HR Zone stream error: $error")
    }
) { event: OnStreamState ->
    _hrZoneData.value = event.state
}
consumerIds.add(hrZoneId)

// Add in clearMetricData() (around line 340)
_hrZoneData.value = null
```

### Step 2: Add to MainViewModel.kt

```kotlin
// Add to UiState data class (around line 40)
val hrZone: String = "--",

// Add observer in observeKarooData() after maxHeartRate (around line 195)
// Observe HR zone data
viewModelScope.launch {
    karooDataService.hrZoneData.collect { streamState ->
        val zoneStr =
            when (val value = streamState?.dataPoint?.values?.get("hr_zone")?.toIntOrNull()) {
                null -> "--"
                else -> "Z$value"
            }
        _uiState.value = _uiState.value.copy(hrZone = zoneStr)
    }
}
```

### Step 3: Update UI in MainScreen.kt

```kotlin
// Update Heart Rate MetricCard (around line 230)
MetricCard(
    label = "Heart Rate",
    value = uiState.heartRate,
    avgValue = uiState.avgHeartRate,
    maxValue = uiState.maxHeartRate,
    zone = uiState.hrZone,  // Add this
    modifier = Modifier.weight(1f)
)

// Update MetricCard composable to support zone (around line 290)
@Composable
fun MetricCard(
    label: String,
    value: String,
    avgValue: String? = null,
    maxValue: String? = null,
    zone: String? = null,  // Add this parameter
    modifier: Modifier = Modifier
) {
    // ... existing code ...

    // Add after avg/max display:
    if (zone != null && zone != "--") {
        Text(
            text = zone,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
    }
}
```

---

## 2. Smoothed Power (3s/10s/30s) (30 minutes)

### Step 1: Add to KarooDataService.kt

```kotlin
// Add after maxPowerData (around line 73)
private val _smoothed3sPowerData = MutableStateFlow<StreamState?>(null)
val smoothed3sPowerData: StateFlow<StreamState?> = _smoothed3sPowerData.asStateFlow()

private val _smoothed10sPowerData = MutableStateFlow<StreamState?>(null)
val smoothed10sPowerData: StateFlow<StreamState?> = _smoothed10sPowerData.asStateFlow()

private val _smoothed30sPowerData = MutableStateFlow<StreamState?>(null)
val smoothed30sPowerData: StateFlow<StreamState?> = _smoothed30sPowerData.asStateFlow()

// Add in registerConsumers() after maxPowerId (around line 260)
// Register 3s Power stream
val power3sId = karooSystem.addConsumer(
    OnStreamState.StartStreaming(DataType.Type.SMOOTHED_3S_AVERAGE_POWER),
    onError = { error ->
        Log.e(TAG, "3s Power stream error: $error")
    }
) { event: OnStreamState ->
    _smoothed3sPowerData.value = event.state
}
consumerIds.add(power3sId)

// Register 10s Power stream
val power10sId = karooSystem.addConsumer(
    OnStreamState.StartStreaming(DataType.Type.SMOOTHED_10S_AVERAGE_POWER),
    onError = { error ->
        Log.e(TAG, "10s Power stream error: $error")
    }
) { event: OnStreamState ->
    _smoothed10sPowerData.value = event.state
}
consumerIds.add(power10sId)

// Register 30s Power stream
val power30sId = karooSystem.addConsumer(
    OnStreamState.StartStreaming(DataType.Type.SMOOTHED_30S_AVERAGE_POWER),
    onError = { error ->
        Log.e(TAG, "30s Power stream error: $error")
    }
) { event: OnStreamState ->
    _smoothed30sPowerData.value = event.state
}
consumerIds.add(power30sId)

// Add in clearMetricData()
_smoothed3sPowerData.value = null
_smoothed10sPowerData.value = null
_smoothed30sPowerData.value = null
```

### Step 2: Add to MainViewModel.kt

```kotlin
// Add to UiState data class
val power3s: String = "--",
val power10s: String = "--",
val power30s: String = "--",

// Add observers in observeKarooData()
// Observe 3s power data
viewModelScope.launch {
    karooDataService.smoothed3sPowerData.collect { streamState ->
        val power3sStr = formatStreamData(streamState, "w")
        _uiState.value = _uiState.value.copy(power3s = power3sStr)
    }
}

// Observe 10s power data
viewModelScope.launch {
    karooDataService.smoothed10sPowerData.collect { streamState ->
        val power10sStr = formatStreamData(streamState, "w")
        _uiState.value = _uiState.value.copy(power10s = power10sStr)
    }
}

// Observe 30s power data
viewModelScope.launch {
    karooDataService.smoothed30sPowerData.collect { streamState ->
        val power30sStr = formatStreamData(streamState, "w")
        _uiState.value = _uiState.value.copy(power30s = power30sStr)
    }
}
```

### Step 3: Update UI - Option A (Replace Power Card)

Show 3s/10s/30s instead of current/avg/max:

```kotlin
MetricCard(
    label = "Power (3s/10s/30s)",
    value = uiState.power3s,
    avgValue = uiState.power10s,
    maxValue = uiState.power30s,
    modifier = Modifier.weight(1f)
)
```

### Step 3: Update UI - Option B (Add New Row)

Add a new row showing all power metrics:

```kotlin
// After existing Row 2 (Cadence, Power)
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    MetricCard(
        label = "Power 3s",
        value = uiState.power3s,
        modifier = Modifier.weight(1f)
    )
    MetricCard(
        label = "Power 10s",
        value = uiState.power10s,
        modifier = Modifier.weight(1f)
    )
    MetricCard(
        label = "Power 30s",
        value = uiState.power30s,
        modifier = Modifier.weight(1f)
    )
}
```

---

## 3. VAM (Vertical Ascent Meters) (15 minutes)

### Step 1: Add to KarooDataService.kt

```kotlin
// Add after timeData (around line 79)
private val _vamData = MutableStateFlow<StreamState?>(null)
val vamData: StateFlow<StreamState?> = _vamData.asStateFlow()

private val _avgVamData = MutableStateFlow<StreamState?>(null)
val avgVamData: StateFlow<StreamState?> = _avgVamData.asStateFlow()

// Add in registerConsumers() after time stream
// Register VAM stream
val vamId = karooSystem.addConsumer(
    OnStreamState.StartStreaming(DataType.Type.VERTICAL_SPEED),
    onError = { error ->
        Log.e(TAG, "VAM stream error: $error")
    }
) { event: OnStreamState ->
    _vamData.value = event.state
}
consumerIds.add(vamId)

// Register Average VAM stream
val avgVamId = karooSystem.addConsumer(
    OnStreamState.StartStreaming(DataType.Type.AVERAGE_VERTICAL_SPEED),
    onError = { error ->
        Log.e(TAG, "Average VAM stream error: $error")
    }
) { event: OnStreamState ->
    _avgVamData.value = event.state
}
consumerIds.add(avgVamId)

// Add in clearMetricData()
_vamData.value = null
_avgVamData.value = null
```

### Step 2: Add to MainViewModel.kt

```kotlin
// Add to UiState data class
val vam: String = "--",
val avgVam: String = "--",

// Add observers in observeKarooData()
// Observe VAM data
viewModelScope.launch {
    karooDataService.vamData.collect { streamState ->
        val vamStr = formatStreamData(streamState, "m/h")
        _uiState.value = _uiState.value.copy(vam = vamStr)
    }
}

// Observe average VAM data
viewModelScope.launch {
    karooDataService.avgVamData.collect { streamState ->
        val avgVamStr = formatStreamData(streamState, "m/h")
        _uiState.value = _uiState.value.copy(avgVam = avgVamStr)
    }
}
```

### Step 3: Update UI in MainScreen.kt

Add a new row for climbing metrics:

```kotlin
// Add after Distance/Time row
Text(
    text = "Climbing",
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold,
    modifier = Modifier.padding(vertical = 8.dp)
)

Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    MetricCard(
        label = "VAM",
        value = uiState.vam,
        avgValue = uiState.avgVam,
        modifier = Modifier.weight(1f)
    )
    // Could add Grade % here later
}
```

---

## Testing Checklist

After implementing each feature:

- [ ] Build succeeds (`.\gradlew assembleDebug`)
- [ ] No compilation errors
- [ ] App installs on Karoo2
- [ ] Connect to Karoo system
- [ ] Start a ride
- [ ] Verify new metrics display
- [ ] Verify metrics update during ride
- [ ] Test with/without sensors (HR zones need HR monitor, Power needs power meter, VAM needs GPS)

---

## Notes

1. **HR Zones:** Will only show if user has HR zones configured in Karoo2 settings
2. **Smoothed Power:** Requires power meter connected to Karoo2
3. **VAM:** Requires GPS data; most accurate when climbing

---

## Estimated Total Implementation Time

- HR Zones: 15 minutes
- Smoothed Power: 30 minutes
- VAM: 15 minutes
- **Total: 1 hour**

All three features can be implemented in parallel if desired.


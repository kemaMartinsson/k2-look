# Phase 1.1 Developer Quick Reference

## Quick Start

### Running the App

```bash
# From project root
./gradlew :app:installDebug
```

### Key Classes

| Class | Purpose | Location |
|-------|---------|----------|
| `KarooDataService` | Manages Karoo connection & data streams | `service/KarooDataService.kt` |
| `MainViewModel` | UI state management & data formatting | `viewmodel/MainViewModel.kt` |
| `MainScreen` | UI display | `screens/MainScreen.kt` |

---

## Using KarooDataService

### Basic Usage

```kotlin
// Create service instance
val service = KarooDataService(context)

// Connect to Karoo
service.connect()

// Observe connection state
lifecycleScope.launch {
    service.connectionState.collect { state ->
        when (state) {
            is ConnectionState.Connected -> {
                // Connected successfully
            }
            is ConnectionState.Error -> {
                // Handle error: state.message
            }
            // ... other states
        }
    }
}

// Observe ride state
lifecycleScope.launch {
    service.rideState.collect { rideState ->
        when (rideState) {
            is RideState.Recording -> {
                // Ride is recording
            }
            is RideState.Paused -> {
                // Ride is paused
            }
            RideState.Idle -> {
                // No active ride
            }
        }
    }
}

// Observe metric data
lifecycleScope.launch {
    service.speedData.collect { streamState ->
        when (streamState) {
            is StreamState.Streaming -> {
                val speed = streamState.dataPoint.singleValue
                // Use speed value
            }
            StreamState.Searching -> {
                // Waiting for sensor data
            }
            // ... other states
        }
    }
}

// Disconnect when done
service.disconnect()
```

---

## Available Data Streams

### Connection State
```kotlin
service.connectionState: StateFlow<ConnectionState>
```

States:
- `Disconnected` - Not connected
- `Connecting` - Connection in progress
- `Connected` - Connected and ready
- `Reconnecting(attempt)` - Auto-reconnecting
- `Error(message)` - Connection error

### Ride State
```kotlin
service.rideState: StateFlow<RideState>
```

States:
- `RideState.Idle` - No active ride
- `RideState.Recording` - Ride recording
- `RideState.Paused(auto: Boolean)` - Ride paused

### Metric Streams
```kotlin
service.speedData: StateFlow<StreamState?>       // km/h
service.heartRateData: StateFlow<StreamState?>   // bpm
service.cadenceData: StateFlow<StreamState?>     // rpm
service.powerData: StateFlow<StreamState?>       // watts
service.distanceData: StateFlow<StreamState?>    // km
service.timeData: StateFlow<StreamState?>        // milliseconds
```

---

## StreamState Types

All metric streams use `StreamState`:

```kotlin
sealed class StreamState {
    object Idle              // No data yet
    object Searching         // Looking for sensor
    object NotAvailable      // Sensor not available
    data class Streaming(
        val dataPoint: DataPoint
    )                        // Receiving data
}
```

### Accessing Values

```kotlin
// Single value helper
val value: Double? = streamState.dataPoint.singleValue

// All values (for multi-field data types)
val values: Map<String, Double> = streamState.dataPoint.values
```

---

## MainViewModel Integration

### In Your Activity/Fragment

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AppTheme {
                MainScreen()  // ViewModel injected automatically
            }
        }
    }
}
```

### Custom ViewModel Usage

```kotlin
@Composable
fun MyScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Use uiState.speed, uiState.heartRate, etc.
    Text("Speed: ${uiState.speed}")
    Text("HR: ${uiState.heartRate}")
    
    // Control connection
    Button(onClick = { viewModel.connect() }) {
        Text("Connect")
    }
}
```

---

## Data Formatting

### Numeric Values

The ViewModel formats values automatically:

```kotlin
// Raw: 123.456789
// Formatted: "123 km/h"   (value >= 100)

// Raw: 45.678
// Formatted: "45.7 km/h"  (value >= 10)

// Raw: 5.123
// Formatted: "5.12 km/h"  (value < 10)
```

### Time Values

```kotlin
// Input: 3725000 milliseconds
// Output: "01:02:05" (HH:MM:SS)
```

### Special States

```kotlin
"Searching..."  // StreamState.Searching
"N/A"          // StreamState.NotAvailable
"-- unit"      // Idle or null
```

---

## Error Handling

### Connection Errors

The service automatically handles:
1. Connection failures → Auto-reconnect (up to 5 attempts)
2. Disconnections → Auto-reconnect with exponential backoff
3. Consumer errors → Logged and consumer removed

### Manual Error Handling

```kotlin
lifecycleScope.launch {
    service.connectionState.collect { state ->
        if (state is ConnectionState.Error) {
            // Show error to user
            Toast.makeText(
                context,
                "Connection error: ${state.message}",
                Toast.LENGTH_LONG
            ).show()
            
            // Optionally retry
            delay(5000)
            service.connect()
        }
    }
}
```

---

## Logging

All classes use Android Log with tags:
- `KarooDataService` - Service operations
- `MainViewModel` - ViewModel operations

### Enable Debug Logging

```kotlin
// In logcat filter:
tag:KarooDataService OR tag:MainViewModel
```

### Log Levels

- `Log.i()` - Normal operations (connect, disconnect, state changes)
- `Log.d()` - Debug info (data updates)
- `Log.w()` - Warnings (already connected, etc.)
- `Log.e()` - Errors (connection failures, exceptions)

---

## Testing

### Manual Testing

```kotlin
val test = KarooDataServiceTest(context)

// Run tests
test.testConnection()       // Returns true if connected
test.testDisconnection()    // Returns true if disconnected
test.testRideStateConsumer() // Returns true if consumer works
test.testMetricStreams()    // Returns true if streams initialized

// Cleanup
test.cleanup()
```

### Integration Testing

```kotlin
@Test
fun testKarooConnection() = runTest {
    val service = KarooDataService(context)
    service.connect()
    
    // Wait for connection
    service.connectionState.first { 
        it is ConnectionState.Connected 
    }
    
    assertTrue(service.isConnected)
    service.disconnect()
}
```

---

## Common Issues

### Issue: No Data Showing

**Solution:** Check these in order:
1. Is Karoo System running?
2. Is connection state `Connected`?
3. Is ride state `Recording` or `Paused`?
4. Are sensors paired and active?
5. Check logcat for errors

### Issue: Connection Keeps Dropping

**Solution:**
1. Check Karoo System version compatibility
2. Verify no other apps consuming KarooSystem
3. Review logcat for specific errors
4. May need to increase reconnection attempts

### Issue: Metrics Show "N/A"

**Solution:**
- `StreamState.NotAvailable` means:
  - Sensor not paired
  - Sensor not connected
  - Sensor battery dead
  - Data type not supported by sensor

---

## Performance Notes

### Update Frequency

Karoo streams update at different rates:
- Speed: ~1-4 Hz
- Heart Rate: ~1 Hz
- Cadence: ~1-4 Hz
- Power: ~1-4 Hz
- Distance: ~1 Hz
- Time: ~1 Hz

### Memory Usage

StateFlows are memory-efficient:
- Only latest value stored
- Automatic cleanup on ViewModel clear
- No memory leaks if properly lifecycle-aware

### Battery Impact

Current implementation:
- Always-on data streaming
- Minimal processing (just formatting)
- Estimated impact: <2% additional battery drain

---

## Next Steps

After Phase 1.1, you can:

1. **Add ActiveLook integration** (Phase 1.2)
   - Display metrics on AR glasses
   
2. **Add user preferences** (Phase 4.1)
   - Choose which metrics to display
   - Unit preferences (metric/imperial)
   
3. **Add data persistence** (Phase 5.2)
   - Save connection settings
   - Log ride data

---

## Support

For issues or questions:
1. Check logcat output
2. Review Phase-1.1-Summary.md
3. Consult Karoo Extensions docs
4. Ask in Hammerhead developer community


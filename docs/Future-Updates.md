# K2Look - Future Updates & Enhancements

**Last Updated**: 2025-12-29  
**Status**: Ideas & Enhancements for Future Releases

---

## Overview

This document outlines potential future enhancements for K2Look. The current implementation is *
*fully functional and production-ready**. These updates are optional improvements that could be
added based on user feedback and needs.

---

## 1. Preview Button - Test Layout on Glasses

**Status**: Deferred to Future Release  
**Priority**: Medium  
**Complexity**: Low-Medium  
**Benefit**: Users can test layouts before riding

### What It Does

Adds a "Preview" button in the Datafields tab that sends the current profile layout to the glasses
for testing, without starting a ride.

### Current Behavior

Users must:

1. Configure their profile
2. Start a ride in Karoo
3. Check if layout looks good on glasses
4. Stop ride, go back, adjust if needed

### With Preview Button

Users can:

1. Configure their profile
2. **Click "Preview"** → Layout instantly displayed on glasses
3. View on glasses, make adjustments
4. Click "Preview" again to test changes
5. When satisfied, start ride

### Implementation Plan

```kotlin
// In DataFieldBuilderTab.kt
Button(
    onClick = {
        viewModel.previewProfile()
    },
    modifier = Modifier.weight(1f),
    enabled = uiState.canPreview && !uiState.isPreviewing
) {
    if (uiState.isPreviewing) {
        Text("Stop Preview")
    } else {
        Text("Preview")
    }
}

// In LayoutBuilderViewModel.kt
fun previewProfile() {
    viewModelScope.launch {
        val profile = _uiState.value.activeProfile ?: return@launch

        // Send current profile to glasses
        bridge.previewProfile(profile)

        // Update state
        _uiState.update { it.copy(isPreviewing = true) }

        // Auto-stop preview after 30 seconds
        delay(30_000)
        stopPreview()
    }
}

fun stopPreview() {
    viewModelScope.launch {
        bridge.clearGlassesDisplay()
        _uiState.update { it.copy(isPreviewing = false) }
    }
}

// In KarooActiveLookBridge.kt
suspend fun previewProfile(profile: DataFieldProfile) {
    // Save layouts to glasses
    layoutService.saveProfileLayouts(profile)

    // Send sample data for preview
    val sampleData = mapOf(
        "speed" to "32.5",
        "hr" to "145",
        "power" to "245"
    )

    // Display with sample values
    profile.screens.firstOrNull()?.let { screen ->
        screen.dataFields.forEach { field ->
            val value = sampleData[field.dataField.id] ?: "---"
            layoutService.displayFieldValue(field.zoneId, value)
        }
    }
}
```

### UI Flow

```
Before Preview:
┌─────────────────────┐
│ [   Preview   ]     │ ← Enabled when glasses connected
│ [ Build & Send ]    │ ← Disabled (future feature)
└─────────────────────┘

During Preview:
┌─────────────────────┐
│ [ Stop Preview ]    │ ← Click to clear glasses
│ [ Build & Send ]    │ ← Still disabled
└─────────────────────┘

Glasses show layout with sample data
User can adjust config and click Preview again
```

### Requirements

- ✅ Glasses must be connected
- ✅ Profile must have at least one configured field
- ✅ Uses sample/dummy data (not real sensor data)
- ✅ Auto-stops after 30 seconds
- ✅ Manual stop available

### Benefits

- Test layouts without starting a ride
- Iterate quickly on design
- See font sizes and positioning
- Verify icon display
- Check readability

### Estimated Effort

- **Time**: 2-3 hours
- **Files to modify**: 2 (DataFieldBuilderTab.kt, LayoutBuilderViewModel.kt)
- **New methods**: 3 (previewProfile, stopPreview, sample data generation)
- **Testing**: 30 minutes

---

## 2. Gauges and Progress Bars - Visual Metric Display

**Status**: Analyzed & Planned  
**Priority**: Medium-High  
**Complexity**: Medium  
**Benefit**: Professional visual feedback, reduced cognitive load

### What It Does

Adds **visual representations** of metrics using circular gauges (arc progress) and rectangular
progress bars instead of just text numbers.

### Reference

See `docs/Gauge_Bar.png` for visual reference showing:

- Circular gauge for power/HR zones
- Horizontal bar for HR zone visualization
- Combined gauge + text displays

### ActiveLook API Support

✅ **Fully Supported!**

**Gauges**: Native API support via gauge commands

```
0x70 - gaugeDisplay: Display value (0-100%)
0x71 - gaugeSave: Save gauge parameters
0x72 - gaugeDelete: Delete gauge(s)
```

**Bars**: Implemented using rectangle commands

```
0x33 - rect: Draw empty rectangle
0x34 - rectf: Draw filled rectangle
0x30 - color: Set grey level
```

### Current Behavior

Users see metrics as **text only**:

```
Power
  245 W

HR
  165 bpm
```

### With Gauges and Bars

Users see metrics **visually**:

```
┌─────────────────────┐
│     ╭───╮           │
│   ╭─────╮  245 W    │ ← Power gauge (0-400W)
│   │█████│           │
│   ╰─────╯           │
│                     │
│ HR: 165 bpm         │
│ ████████████▓▓▒▒    │ ← HR zone bar
│ Z1│Z2│Z3│Z4│ Z5     │
└─────────────────────┘
```

### Features

#### Gauges (Circular/Arc)

- ✅ 16 portions in full circle
- ✅ Customizable start/end angles
- ✅ Adjustable thickness (inner/outer radius)
- ✅ Clockwise or counter-clockwise
- ✅ Color gradient (16 grey levels)
- ✅ Perfect for: Power zones, HR zones, cadence ranges

#### Progress Bars (Rectangular)

- ✅ Horizontal or vertical orientation
- ✅ Customizable size and position
- ✅ Optional border
- ✅ Multi-zone support (e.g., HR zones with different colors)
- ✅ Perfect for: Battery, progress to goal, zone indicators

### Use Cases

**Power Gauge**:

- Display power as circular gauge (0-400W)
- Text value in center
- Easy to glance at percentage of FTP

**HR Zone Bar**:

- Horizontal bar showing HR zones (Z1-Z5)
- Different grey levels for each zone
- Current HR highlighted
- Instant zone awareness

**Speed Progress**:

- Bar showing current speed vs. target
- Visual feedback for pacing
- Less mental math required

**Multi-Metric Dashboard**:

```
┌─────────────────────┐
│ Power ███████▓▓▒    │ 245W/400W (61%)
│ HR    ████████▓▓▓   │ 165 bpm (Z4)
│ Cad   █████▓▓▒▒▒    │ 85 rpm (71%)
└─────────────────────┘
```

### Implementation Plan

**See**: `docs/Gauge-Bar-Implementation-Plan.md` for complete details

**Phases**:

1. Data models (Gauge, ProgressBar) - 2 hours
2. ActiveLook service integration - 3 hours
3. Bridge updates - 2 hours
4. UI components (config dialogs) - 4 hours
5. ViewModel updates - 2 hours
6. Default configurations - 1 hour

**Total Effort**: ~14 hours (2 days) + 4 hours testing = **18 hours**

### Configuration

Users would configure visualization type per zone:

**Field Configuration Dialog**:

```
┌─────────────────────────┐
│ Display Style:          │
│ [ Text ] [Gauge] [ Bar ]│ ← New selector
│                         │
│ [Gauge Settings...]     │ ← When gauge selected
│ - Style: 3/4 circle     │
│ - Min: 0                │
│ - Max: 400              │
│ - Preview: ╭──╮         │
│            │██│         │
│            ╰──╯         │
└─────────────────────────┘
```

### Pre-configured Options

**Default Gauges**:

- Power gauge (0-400W, 3/4 circle)
- HR gauge (40-200 bpm, 3/4 circle)
- Cadence gauge (0-120 rpm, full circle)

**Default Bars**:

- HR zone bar (5 zones with colors)
- Speed bar (0-60 km/h)
- Power bar (0-400W)
- Battery bar (0-100%)

### Benefits

**User Benefits**:

- ✅ **Instant visual feedback** - No need to read numbers
- ✅ **Zone awareness** - See if in target range at a glance
- ✅ **Reduced cognitive load** - Colors and shapes vs. numbers
- ✅ **Professional look** - Like high-end cycling computers

**Technical Benefits**:

- ✅ **Native API support** - Gauges are built-in
- ✅ **Efficient updates** - 1Hz same as text
- ✅ **Works with zones** - Each zone can have different visualization
- ✅ **Backward compatible** - Text remains default

### Performance

**Gauges**:

- Efficient (native API)
- Single command to update: `gaugeDisplay(id, percentage)`
- No flickering issues

**Bars**:

- 2-3 rectangle commands per update
- Use `holdFlush` to prevent flickering
- Atomic updates ensure smooth visuals

### Estimated Timeline

**Phase 1** (Week 1): Gauge implementation

- Data models
- Service integration
- Basic UI

**Phase 2** (Week 2): Bar implementation

- Rectangle-based drawing
- Zone coloring
- Multi-zone bars

**Phase 3** (Week 3): Polish & Test

- Hardware testing on glasses
- Configuration UI polish
- Documentation

**Release Target**: v0.8 or v0.9

### Design Examples

**Example 1: Power-Focused Layout**

```
Template: 2D (Two Data)
┌─────────────────────┐
│     ╭───────╮       │
│   ╭───────────╮     │
│   │    245    │     │ ← Top: Power gauge + text
│   │     W     │     │
│   ╰───────────╯     │
│                     │
│   165 bpm  •  Z4   │ ← Bottom: HR text
└─────────────────────┘
```

**Example 2: Zone-Focused Layout**

```
Template: 3D Full
┌─────────────────────┐
│ 32.5 km/h           │ ← Top: Speed (text)
├─────────────────────┤
│ ████████████▓▓▒▒    │ ← Middle: HR bar
│ Z1 │Z2│Z3│Z4│  Z5   │
├─────────────────────┤
│ Power: 245W (61%)   │ ← Bottom: Power text
└─────────────────────┘
```

**Example 3: Dashboard Layout**

```
Template: 6D (Six Data)
┌─────────────────────┐
│ ╭─╮  │ 165  │       │ ← Top: Gauge + HR
│ │█│  │ bpm  │       │
│ ╰─╯  ├──────┤       │
│ 85   │ ████ │       │ ← Middle: Cad + Power bar
│ rpm  │ 61%  │       │
├──────┼──────┤       │
│ 32.5 │ 1:45 │       │ ← Bottom: Speed + Time
│ km/h │      │       │
└─────────────────────┘
```

### Alternatives Considered

**Option 1**: Text Only (Current)

- ✅ Simple, reliable
- ❌ Requires reading numbers
- ❌ Less glanceable

**Option 2**: Icons Only

- ✅ Very minimal
- ❌ No range indication
- ❌ Limited information

**Option 3**: Gauges + Bars (Recommended)

- ✅ Visual feedback
- ✅ Range indication
- ✅ Professional look
- ✅ API supported

### Recommendation

**IMPLEMENT THIS!**

**Reasons**:

1. ✅ Fully supported by ActiveLook API (native gauges!)
2. ✅ Medium effort (2-3 days)
3. ✅ High user value (visual feedback highly requested)
4. ✅ Professional feature (differentiates K2Look)
5. ✅ Works with existing zone system
6. ✅ Matches reference perfectly

**Priority**: Implement after Phase 6 (testing & polish) is complete.

**See**: `docs/Gauge-Bar-Implementation-Plan.md` for complete implementation details.

---

## 3. Persistent Layout Storage (cfgWrite/cfgSet)

**Status**: Optional Enhancement  
**Priority**: Medium  
**Complexity**: Medium  
**Benefit**: Faster reconnection, survives power cycles

### What It Does

Saves compiled layouts to the glasses' **flash memory** instead of just RAM, making them persist
across power cycles and disconnections.

### Current Behavior (Phase 4.2)

```
Day 1:
- Select profile → Send layouts to glasses RAM (450ms)
- Ride with profile ✅
- Power off glasses 💤

Day 2:
- Power on glasses → Layouts LOST (RAM cleared)
- App reconnects → Must re-send layouts (450ms)
- Ride with profile ✅
```

**Overhead**: 450ms every time glasses reconnect

### With cfgWrite/cfgSet

```
Day 1:
- Select profile → Send layouts to RAM (450ms)
- Save to flash: cfgWrite("K2LOOK_RoadBike") (50ms)
- Ride with profile ✅
- Power off glasses 💤

Day 2:
- Power on glasses → Layouts in FLASH ✅
- App reconnects → cfgSet("K2LOOK_RoadBike") (50ms)
- Ride with profile ✅ (instant!)
```

**Overhead**: 500ms first time, then 50ms forever

### Implementation

```kotlin
// Save configuration to flash
suspend fun saveConfiguration(profile: DataFieldProfile): Boolean {
    try {
        // Save layouts to RAM first
        saveProfileLayouts(profile)

        // Write entire configuration to flash
        val configName = "K2LOOK_${profile.id}"
        glasses.cfgWrite(configName)

        Log.i(TAG, "✅ Config saved to flash")
        return true
    } catch (e: Exception) {
        Log.e(TAG, "Flash save failed", e)
        return false
    }
}

// Load configuration from flash
suspend fun loadConfiguration(profile: DataFieldProfile): Boolean {
    try {
        val configName = "K2LOOK_${profile.id}"
        glasses.cfgSet(configName)

        Log.i(TAG, "✅ Config loaded from flash")
        return true
    } catch (e: Exception) {
        // Fallback: save layouts normally
        return saveProfileLayouts(profile)
    }
}
```

### Storage Capacity

- **Glasses flash storage**: ~10-20 KB total
- **Per configuration**: ~500-1000 bytes
- **Max configurations**: ~10-20 profiles

**Strategy:**

- Save most-used profiles to flash
- Delete old configs when space is low
- Always keep "Default" profile in flash

### When Is This Useful?

**NOT needed for:**

- ✅ Single-session rides (glasses stay on)
- ✅ Starting fresh next day (new session)
- ✅ Rare profile switching

**Useful for:**

- 🔄 Multi-day tours (power off at night)
- 🔄 Frequent profile switching during rides
- 🔄 Unreliable BLE connections (many reconnects)
- 🔄 Instant resume after brief power loss

### User Setting

```kotlin
// In Settings/Preferences
"Save Layouts to Glasses Flash"
[X] Enabled (faster reconnection, uses glasses storage)
[] Disabled (re - send layouts each time, saves storage)
```

**Default**: Disabled (current behavior is fine for most users)

---

## 2. Multi-Screen Support with Screen Switching

**Status**: Optional Enhancement  
**Priority**: Low  
**Complexity**: High  
**Benefit**: More metrics available during rides

### What It Does

Allows users to configure multiple screens (e.g., "Main", "Climbing", "Sprint") and switch between
them during rides using buttons or gestures.

### Current Behavior

- Single screen with 3 fields (TOP/MIDDLE/BOTTOM)
- Fixed layout throughout ride
- If you need different metrics, must change profile before ride

### With Multi-Screen Support

```
Profile "Road Bike" has 3 screens:

Screen 1 "Main":
├── TOP: Speed
├── MIDDLE: Heart Rate  
└── BOTTOM: Power

Screen 2 "Climbing":
├── TOP: Gradient
├── MIDDLE: VAM (vertical speed)
└── BOTTOM: Power

Screen 3 "Navigation":
├── TOP: Distance to next turn
├── MIDDLE: ETA
└── BOTTOM: Speed
```

### Screen Switching Methods

**Option A: Karoo Button**

- Add "Next Screen" button to Karoo app
- Click to cycle through screens
- Screen change takes ~50ms (just switch layout IDs)

**Option B: Time-Based Auto-Switch**

- Screen 1 for 5 seconds
- Screen 2 for 5 seconds
- Screen 3 for 5 seconds
- Loop

**Option C: Event-Based**

- Gradient > 5% → Switch to "Climbing" screen
- Speed > 40 km/h → Switch to "Sprint" screen
- Turn approaching → Switch to "Navigation" screen

### Implementation

```kotlin
class ActiveLookLayoutService {

    // Track current screen
    private var currentScreenIndex = 0

    /**
     * Save all screens from a profile
     */
    suspend fun saveAllScreens(profile: DataFieldProfile): Boolean {
        profile.screens.forEachIndexed { index, screen ->
            val baseLayoutId = 10 + (index * 3)

            // Save 3 layouts per screen (TOP/MIDDLE/BOTTOM)
            saveLayout(baseLayoutId + 0, screen, Position.TOP)
            saveLayout(baseLayoutId + 1, screen, Position.MIDDLE)
            saveLayout(baseLayoutId + 2, screen, Position.BOTTOM)
        }
    }

    /**
     * Switch to different screen
     */
    fun switchToScreen(screenIndex: Int) {
        currentScreenIndex = screenIndex

        // Update which layout IDs to use
        val baseLayoutId = 10 + (screenIndex * 3)
        // Next update will use new layout IDs
    }
}
```

### Layout ID Allocation

```
Screen 1: Layout IDs 10, 11, 12 (TOP/MIDDLE/BOTTOM)
Screen 2: Layout IDs 13, 14, 15 (TOP/MIDDLE/BOTTOM)
Screen 3: Layout IDs 16, 17, 18 (TOP/MIDDLE/BOTTOM)
Screen 4: Layout IDs 19, 20, 21 (TOP/MIDDLE/BOTTOM)
...
Max: ~30 screens (layout IDs 10-99 available)
```

### UI Changes Needed

**Builder Tab:**

- Add "Add Screen" button (already exists! ✅)
- Show screen switcher tabs (already exists! ✅)
- Configure screen switching behavior

**Status Tab:**

- Add "Next Screen" / "Prev Screen" buttons
- Show current screen indicator (1/3)
- Optional: Auto-advance timer

### When Is This Useful?

**NOT needed for:**

- ✅ Simple rides with consistent metrics
- ✅ Users happy with 3 metrics
- ✅ Minimal distraction preferred

**Useful for:**

- 📊 Data enthusiasts wanting many metrics
- ⛰️ Varied terrain (flat/climbing need different data)
- 🗺️ Rides with navigation + performance data
- 🔬 Training rides with detailed metrics

---

## 3. Automatic Profile Switching Based on Karoo Profile

**Status**: Optional Enhancement  
**Priority**: Medium  
**Complexity**: Medium  
**Benefit**: Seamless experience, no manual switching

### What It Does

Automatically switches K2Look display profile when user selects a different ride profile in Karoo
launcher.

### Scenario

```
User in Karoo Launcher:
- Selects "Road Bike" profile
- Karoo knows: road bike, power meter, HR sensor

K2Look automatically:
- Detects Karoo profile = "Road Bike"
- Switches to K2Look profile = "Road Bike"
- Displays: Speed, HR, Power

Next ride:
- User selects "Gravel Bike" profile in Karoo
- K2Look detects change
- Switches to K2Look profile = "Gravel Bike"  
- Displays: Speed, HR, Cadence (no power meter)
```

### Implementation

```kotlin
class KarooActiveLookBridge {

    /**
     * Monitor Karoo active ride profile changes
     */
    private fun observeKarooProfile() {
        scope.launch {
            karooDataService.activeRideProfile.collect { karooProfile ->
                if (karooProfile != null) {
                    Log.i(TAG, "Karoo profile: ${karooProfile.name}")

                    // Find matching K2Look profile
                    val k2LookProfile = findMatchingProfile(karooProfile.name)

                    if (k2LookProfile != null) {
                        Log.i(TAG, "Auto-switching to: ${k2LookProfile.name}")
                        setActiveProfile(k2LookProfile)
                    }
                }
            }
        }
    }

    /**
     * Find K2Look profile matching Karoo profile name
     */
    private fun findMatchingProfile(karooProfileName: String): DataFieldProfile? {
        // Try exact match first
        profileRepository.findProfileByName(karooProfileName)

        // Or use mapping table:
        // Karoo "Road Bike" → K2Look "Road Performance"
        // Karoo "Gravel" → K2Look "Gravel Adventure"
    }
}
```

### Configuration

**Settings screen:**

```
Auto-Switch Profile
[X] Enabled

Profile Mapping:
┌────────────────────┬─────────────────────┐
│ Karoo Profile      │ K2Look Profile      │
├────────────────────┼─────────────────────┤
│ Road Bike          │ → Road Performance  │
│ Gravel Adventure   │ → Gravel            │
│ Indoor Training    │ → Training          │
│ Commute            │ → Simple            │
└────────────────────┴─────────────────────┘

[Add Mapping]
```

### When Is This Useful?

**NOT needed for:**

- ✅ Single bike/ride type
- ✅ Users who manually manage profiles
- ✅ Simple setup

**Useful for:**

- 🚴 Multiple bikes (road, gravel, MTB)
- 🔄 Different ride types (training, commute, race)
- 🎯 Set-and-forget users
- 📱 Minimal interaction during rides

---

## 4. Advanced Metric Support

**Status**: Future Enhancement  
**Priority**: Medium  
**Complexity**: Low-Medium  
**Benefit**: More cycling data available

### Heart Rate Zones

✅ **IMPLEMENTED** - HR Zones are now available in Phase 5!

Karoo provides HR zone data (Z1-Z5) directly through the data streams. The zone is calculated by
Karoo based on the user's configured HR zones in their profile.

**Usage:**

- Select "HR Zone" metric in Builder tab
- Displays as "Z1", "Z2", "Z3", "Z4", or "Z5"
- Updates in real-time based on current heart rate

### VAM (Vertical Ascent Meters)

✅ **AVAILABLE IN KAROO** - Ready to implement!

Karoo provides VAM data streams:

- Current VAM (vertical speed in m/h)
- Average VAM

**Implementation:**

```kotlin
// Already defined in DataFieldRegistry.kt
DataField(24, "VAM", "m/h", CLIMBING, ...)

// Connect data stream in KarooActiveLookBridge
scope.launch {
    karooDataService.vamData.collect { streamState ->
        currentData.vam = formatStreamData(streamState, "m/h")
        currentData.isDirty = true
    }
}
```

**When Is This Useful?**

- ⛰️ Climbing performance tracking
- 🏔️ Mountain rides
- 📊 Comparing climbing efforts

### Additional Power Smoothing

⚡ **AVAILABLE IN KAROO** - 10s and 30s smoothing ready to implement!

Currently implemented:

- ✅ Power 3s (smoothed)

Available from Karoo:

- 📊 Power 10s (smoothed)
- 📊 Power 30s (smoothed)

**Implementation:**

```kotlin
// Add to DataFieldRegistry.kt
DataField(11, "Power 10s", "w", POWER, ...),
DataField(12, "Power 30s", "w", POWER, ...),

// Connect streams (already exist in KarooDataService)
currentData.power10s = formatStreamData(smoothed10sPowerData, "w")
currentData.power30s = formatStreamData(smoothed30sPowerData, "w")
```

### NOT Available from Karoo

The following metrics are **NOT provided by Karoo data streams** and would require custom
implementation:

**Elevation Data:**

- ❌ Altitude (current)
- ❌ Total Ascent (cumulative)
- ❌ Total Descent (cumulative)
- ❌ Gradient (current %)

**Energy:**

- ❌ Calories burned
- ❌ TSS (Training Stress Score)
- ❌ IF (Intensity Factor)
- ❌ NP (Normalized Power)

**Advanced Power:**

- ❌ Left/Right balance
- ❌ Torque effectiveness
- ❌ FTP percentage

**Note:** These would require:

- Custom calculation from raw data
- Local storage/tracking
- Significant development effort
- May not be worth complexity vs benefit

---

## 5. Custom Color Themes

**Status**: Future Enhancement  
**Priority**: Low  
**Complexity**: Low  
**Benefit**: Personalization

### What It Does

Allows users to customize display colors for better visibility in different conditions.

### Current Behavior

- Fixed colors: White (15) on Black (0)
- Good contrast, works in most conditions

### With Custom Themes

**Theme Options:**

```
Default (White on Black)
├── Foreground: 15 (white)
└── Background: 0 (black)

High Contrast (Black on White)
├── Foreground: 0 (black)
└── Background: 15 (white)

Night Mode (Red on Black)
├── Foreground: 1 (red)
└── Background: 0 (black)

Amber (Orange on Black)
├── Foreground: 12 (orange)
└── Background: 0 (black)
```

### Implementation

```kotlin
data class ColorTheme(
    val name: String,
    val foreground: Int,
    val background: Int
)

// In profile configuration
val profile = DataFieldProfile(
    name = "Road Bike",
    theme = ColorTheme("Default", 15, 0),
    screens = [...]
)
```

### When Is This Useful?

**NOT needed for:**

- ✅ Default colors work well
- ✅ Daytime riding only

**Useful for:**

- 🌙 Night rides (red preserves night vision)
- ☀️ Bright sunlight (high contrast)
- 👓 Individual preferences
- 🎨 Personalization

---

## 6. Gesture Control Integration

**Status**: Future Enhancement  
**Priority**: Low  
**Complexity**: High  
**Benefit**: Hands-free operation

### What It Does

Use head gestures or taps on glasses to control display (if supported by glasses hardware).

### Possible Gestures

```
Double Tap → Next screen
Tap-Hold → Toggle display on/off
Nod Up → Scroll up
Nod Down → Scroll down
```

### Implementation

Depends on ActiveLook SDK gesture support.

### When Is This Useful?

**NOT needed for:**

- ✅ Glasses don't support gestures
- ✅ Simple static display sufficient

**Useful for:**

- 🖐️ Hands-free operation
- 🚴 Aero position (can't reach Karoo)
- 🎮 Interactive displays
- 🔀 Screen switching without phone

---

## 7. Historical Data Display

**Status**: Future Enhancement  
**Priority**: Low  
**Complexity**: High  
**Benefit**: Training insights

### What It Does

Shows historical averages, personal bests, or comparison data on glasses.

### Examples

```
Screen Layout:
┌─────────────────────────────────┐
│ Current:  250w                  │
│ 20min Avg: 235w                │
│ PR: 290w (2024-11-15)           │
└─────────────────────────────────┘
```

### Implementation

Requires:

- Local database of past rides
- Segment detection
- Real-time comparison logic

### When Is This Useful?

**NOT needed for:**

- ✅ Live data sufficient
- ✅ Post-ride analysis preferred

**Useful for:**

- 🏆 Personal records tracking
- 📈 Training progress
- 🎯 Goal pacing
- 🔥 Motivation

---

## 8. Voice Alerts

**Status**: Future Enhancement  
**Priority**: Low  
**Complexity**: Medium  
**Benefit**: Audio feedback

### What It Does

Plays audio alerts for important events (if glasses support audio).

### Examples

```
Power drops below threshold:
→ "Power low"

Heart rate zone change:
→ "Zone 4"

Segment start:
→ "Segment started"
```

### Implementation

Depends on glasses audio capabilities.

### When Is This Useful?

**NOT needed for:**

- ✅ Glasses don't have audio
- ✅ Visual display sufficient

**Useful for:**

- 🔊 Training cues
- ⚠️ Alerts
- 🎧 No need to look at display

---

## 9. Export/Import Profiles

**Status**: Future Enhancement  
**Priority**: Medium  
**Complexity**: Low  
**Benefit**: Profile sharing

### What It Does

Allows users to export profiles as JSON files and share with others.

### Implementation

```kotlin
// Export profile
fun exportProfile(profile: DataFieldProfile): File {
    val json = Json.encodeToString(profile)
    val file = File(exportDir, "${profile.name}.k2look")
    file.writeText(json)
    return file
}

// Import profile
fun importProfile(file: File): DataFieldProfile {
    val json = file.readText()
    return Json.decodeFromString<DataFieldProfile>(json)
}
```

### UI

```
Profile Management Screen:

[Road Bike Profile]
├── Edit
├── Duplicate
├── Export → Share via email/Drive
└── Delete

[Import Profile from File]
```

### When Is This Useful?

**NOT needed for:**

- ✅ Single user
- ✅ Default profiles sufficient

**Useful for:**

- 👥 Team setup (same config for everyone)
- 🔄 Backup/restore profiles
- 📱 Multiple devices
- 🌐 Community sharing

---

## 10. Configuration Backup to Cloud

**Status**: Future Enhancement  
**Priority**: Low  
**Complexity**: High  
**Benefit**: Device migration

### What It Does

Automatically backs up K2Look profiles to cloud (Google Drive, Dropbox, etc.).

### Features

- Auto-sync profiles across devices
- Restore after factory reset
- Version history
- Conflict resolution

### Implementation

Requires:

- Google Drive API integration
- Authentication
- Sync logic
- Conflict handling

### When Is This Useful?

**NOT needed for:**

- ✅ Single device
- ✅ Rare profile changes

**Useful for:**

- 📱 Multiple Karoo devices
- 🔄 Backup/restore
- 👥 Team synchronization
- ☁️ Peace of mind

---

## 11. Smart Workout Integration

**Status**: Future Enhancement  
**Priority**: Medium  
**Complexity**: High  
**Benefit**: Structured training

### What It Does

Displays workout intervals and targets on glasses during structured workouts.

### Example

```
Workout: FTP Test

Current Interval:
├── 5 min @ 250w (FTP 90%)
├── Time remaining: 3:24
├── Current: 248w ✅
└── Next: 5 min @ 280w

Progress: [████████░░] 60%
```

### Implementation

Requires:

- Karoo workout API access
- Interval detection
- Target zone calculations
- Dynamic display updates

### When Is This Useful?

**NOT needed for:**

- ✅ Unstructured rides
- ✅ Outdoor rides only

**Useful for:**

- 🎯 Structured training
- 📊 Interval workouts
- 🏋️ Indoor training
- 🔢 Target zones

---

## Priority Summary

### High Priority

- None currently (Phase 4.2 is complete!)

### Medium Priority

- cfgWrite/cfgSet (faster reconnection)
- Auto profile switching
- Export/import profiles

### Low Priority

- Multi-screen support
- Advanced metrics
- Custom themes
- Gestures
- Historical data
- Voice alerts
- Cloud backup
- Workout integration

---

## Implementation Strategy

### For Each Enhancement

1. **Validate Need**
    - User feedback
    - Usage patterns
    - Benefit vs complexity

2. **Prototype**
    - Quick proof-of-concept
    - Test with real users
    - Measure impact

3. **Implement**
    - Full feature development
    - Testing
    - Documentation

4. **Monitor**
    - Usage analytics
    - User feedback
    - Battery/performance impact

---

## Conclusion

The current K2Look implementation (Phase 4.2) is **fully functional and production-ready**. These
future updates are optional enhancements that can be added based on:

- ✅ User demand
- ✅ Hardware capabilities
- ✅ Development time available
- ✅ Real-world testing feedback

**For most users, the current implementation provides:**

- ✅ Customizable layouts
- ✅ Efficient battery usage (50% improvement)
- ✅ Real-time metrics
- ✅ Reliable operation
- ✅ Easy configuration

**Future updates are ideas for specific use cases, not requirements for a complete product.**

---

**Current status: K2Look is ready for daily use! 🚀**

**Future updates: Nice-to-have enhancements based on feedback! 🎁**


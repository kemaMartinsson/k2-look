# ActiveLook Icon Reference for K2Look

Quick reference guide for using pre-loaded icons from ActiveLook Visual Assets in K2Look layouts.

---

## Icon Mapping Table

### General Metrics

| Metric           | Icon 28×28 | Icon 40×40 | Filename                                              |
|------------------|------------|------------|-------------------------------------------------------|
| **Elapsed Time** | 8          | 40         | chrono_28x28.png / chrono_40x40.png                   |
| **Distance**     | 9          | 41         | distance_28x28.png / distance_40x40.png               |
| **Calories**     | 7          | 39         | calories-burned_28x28.png / calories-burned_40x40.png |

### Heart Rate

| Metric             | Icon 28×28 | Icon 40×40 | Filename                                            |
|--------------------|------------|------------|-----------------------------------------------------|
| **Heart Rate**     | 12         | 44         | heart-beat_28x28.png / heart-beat_40x40.png         |
| **Avg Heart Rate** | 13         | 45         | heart-beat-avg_28x28.png / heart-beat-avg_40x40.png |
| **Max Heart Rate** | 14         | 46         | heart-beat-max_28x28.png / heart-beat-max_40x40.png |

### Power

| Metric               | Icon 28×28 | Icon 40×40 | Filename                                                |
|----------------------|------------|------------|---------------------------------------------------------|
| **Power**            | 19         | 51         | power_28x28.png / power_40x40.png                       |
| **Power 3s**         | 20         | 52         | power-3s_28x28.png / power-3s_40x40.png                 |
| **Avg Power**        | 21         | 53         | power-avg_28x28.png / power-avg_40x40.png               |
| **Max Power**        | 22         | 54         | power-max_28x28.png / power-max_40x40.png               |
| **Normalized Power** | 15         | 47         | normalized-power_28x28.png / normalized-power_40x40.png |

### Speed & Pace

| Metric        | Icon 28×28 | Icon 40×40 | Filename                                  |
|---------------|------------|------------|-------------------------------------------|
| **Speed**     | 26         | 58         | speed_28x28.png / speed_40x40.png         |
| **Avg Speed** | 27         | 59         | speed-avg_28x28.png / speed-avg_40x40.png |
| **Max Speed** | 28         | 60         | speed-max_28x28.png / speed-max_40x40.png |
| **Pace**      | 16         | 48         | pace_28x28.png / pace_40x40.png           |
| **Avg Pace**  | 17         | 49         | pace-avg_28x28.png / pace-avg_40x40.png   |
| **Max Pace**  | 18         | 50         | pace-max_28x28.png / pace-max_40x40.png   |

### Cadence

#### Cycling Cadence

| Metric          | Icon 28×28 | Icon 40×40 | Filename                                      |
|-----------------|------------|------------|-----------------------------------------------|
| **Cadence**     | 4          | 36         | cadence_28x28.png / cadence_40x40.png         |
| **Avg Cadence** | 5          | 37         | cadence-avg_28x28.png / cadence-avg_40x40.png |
| **Max Cadence** | 6          | 38         | cadence-max_28x28.png / cadence-max_40x40.png |

#### Running Cadence

| Metric                  | Icon 28×28 | Icon 40×40 | Filename                                                      |
|-------------------------|------------|------------|---------------------------------------------------------------|
| **Running Cadence**     | 23         | 55         | running-cadence_28x28.png / running-cadence_40x40.png         |
| **Avg Running Cadence** | 24         | 56         | running-cadence-avg_28x28.png / running-cadence-avg_40x40.png |
| **Max Running Cadence** | 25         | 57         | running-cadence-max_28x28.png / running-cadence-max_40x40.png |

### Elevation

| Metric            | Icon 28×28 | Icon 40×40 | Filename                                              |
|-------------------|------------|------------|-------------------------------------------------------|
| **Altitude**      | 2          | 34         | altitude_28x28.png / altitude_40x40.png               |
| **Total Ascent**  | 30         | 62         | total-ascent_28x28.png / total-ascent_40x40.png       |
| **Total Descent** | 31         | 63         | total-descent_28x28.png / total-descent_40x40.png     |
| **Ascent Speed**  | 29         | 61         | speed-ascension_28x28.png / speed-ascension_40x40.png |

---

## Usage in Kotlin

### Basic Icon Display

```kotlin
// In layout building
val iconId = when (iconSize) {
    IconSize.SMALL -> field.dataField.icon28  // 28×28 icon
    IconSize.LARGE -> field.dataField.icon40  // 40×40 icon
}

commands.add(
    GraphicCommand.Image(
        id = iconId,
        x = 20,  // Left margin
        y = 30   // Vertical position
    )
)
```

### DataField with Icons

```kotlin
DataField(
    id = 12,
    name = "Speed",
    unit = "km/h",
    category = SPEED_PACE,
    karooStreamType = KarooStreamType.SPEED,
    icon28 = 26,  // Small icon
    icon40 = 58   // Large icon
)
```

### Icon Selection Helper

```kotlin
object IconHelper {
    fun getIconForDataField(field: DataField, size: IconSize): Int? {
        return when (size) {
            IconSize.SMALL -> field.icon28
            IconSize.LARGE -> field.icon40
        }
    }
}
```

---

## Layout Examples

### With Icon (28×28)

```
┌─────────────────────────────┐
│ [⚡]  PWR      250 w        │
└─────────────────────────────┘
  ↑      ↑        ↑
  Icon  Label   Value
```

### With Icon (40×40) - Prominent

```
┌─────────────────────────────┐
│ [❤️]                        │
│      HR       145 bpm       │
└─────────────────────────────┘
  ↑     ↑         ↑
 Large  Label   Value
 Icon
```

### Without Icon

```
┌─────────────────────────────┐
│      SPEED     22.5 km/h    │
└─────────────────────────────┘
```

---

## Icon Positioning Guidelines

### Recommended Positions (304×256 display)

#### Small Icons (28×28)

- **X Position**: 10-20 (left margin)
- **Y Position**: Center vertically in section: `(sectionHeight - 28) / 2`
- **Label Offset**: Icon X + 28 + 10 (10px gap)

#### Large Icons (40×40)

- **X Position**: 10-20 (left margin)
- **Y Position**: Center vertically: `(sectionHeight - 40) / 2`
- **Label Offset**: Icon X + 40 + 10

### Section Heights

For 3-field layout (304×256):

- **Section Height**: 85px each
- **Small Icon Centered**: Y = (85 - 28) / 2 = 28-29px
- **Large Icon Centered**: Y = (85 - 40) / 2 = 22-23px

---

## Configuration Setup

### Ensure ALooK Config is Active

```kotlin
class ActiveLookLayoutService(private val activeLookService: ActiveLookService) {

    fun initialize() {
        // Select ALooK system configuration to access pre-loaded icons
        activeLookService.cfgSet("ALooK")
        Log.i(TAG, "Selected ALooK configuration for icon access")
    }

    fun buildAndSendLayout(layoutId: Int, screen: LayoutScreen) {
        // Build layout with icons
        val layout = layoutBuilder.buildLayout(layoutId, screen)

        // Encode and send
        val bytes = encoder.encodeLayout(layout)
        activeLookService.layoutSave(layoutId, bytes)
    }
}
```

---

## Icon Display Order

In layout `additionalCommands`:

1. **Image** (icon) - displays first
2. **Text** (label) - positioned after icon
3. **Line** (separator) - drawn last

```kotlin
private fun buildAdditionalCommands(field: LayoutDataField): List<GraphicCommand> {
    return listOf(
        // 1. Icon (if enabled)
        GraphicCommand.Image(id = iconId, x = iconX, y = iconY),

        // 2. Label text (if enabled)
        GraphicCommand.Text(x = labelX, y = labelY, text = label),

        // 3. Separator line
        GraphicCommand.Line(x0 = 10, y0 = 84, x1 = 294, y1 = 84)
    )
}
```

---

## Sport-Specific Icons

### Cycling vs Running

For **cadence**, choose the appropriate icon based on sport:

```kotlin
fun getCadenceIcon(sport: Sport, size: IconSize): Int {
    return when (sport) {
        Sport.CYCLING -> if (size == IconSize.SMALL) 4 else 36
        Sport.RUNNING -> if (size == IconSize.SMALL) 23 else 55
    }
}
```

### Auto-Detection (Future)

```kotlin
// Detect sport from Karoo ride profile
val sport = when (karooSystem.rideProfile) {
    RideProfile.CYCLING, RideProfile.MOUNTAIN_BIKE -> Sport.CYCLING
    RideProfile.RUNNING, RideProfile.TRAIL_RUNNING -> Sport.RUNNING
    else -> Sport.CYCLING // Default to cycling
}
```

---

## Testing Icon Display

### Simulator Mode

```kotlin
// Test icon rendering in simulator
viewModel.startSimulator()  // Generates sample data
// Icons should appear in layouts alongside values
```

### Manual Testing Checklist

- [ ] Icon displays correctly at 28×28 size
- [ ] Icon displays correctly at 40×40 size
- [ ] Icon aligns vertically with text
- [ ] Label positioned correctly after icon
- [ ] Icon doesn't overlap with value text
- [ ] Multiple icons in different sections don't overlap
- [ ] Icons persist when switching screens
- [ ] Icons remain visible during ride data updates

---

## Troubleshooting

### Icons Not Showing

1. **Check ALooK config is selected**:
   ```kotlin
   activeLookService.cfgSet("ALooK")
   ```

2. **Verify icon ID is valid** (0-80 for system icons)

3. **Check icon command is in additionalCommands**:
   ```kotlin
   layout.additionalCommands.any { it is GraphicCommand.Image }
   ```

4. **Ensure icon is within clipping region**:
    - Icon X + width < clippingRegion.width
    - Icon Y + height < clippingRegion.height

### Icon Position Issues

- **Too far left**: Increase icon X position (try 20 instead of 10)
- **Overlapping text**: Increase label X offset after icon
- **Not centered**: Recalculate Y: `(sectionHeight - iconHeight) / 2`

---

## Future Enhancements

### Custom Icon Upload

For advanced users who want custom icons:

```kotlin
// Upload custom icon (future feature)
activeLookService.imgSave(
    id = 100,  // User icon IDs: 100+
    size = imageBytes.size,
    width = 28,
    format = ImageFormat.BPP_4,
    data = imageBytes
)
```

### Icon Library UI

Future UI enhancement to browse and preview all available icons:

```
┌─────────────────────────────────────┐
│  Icon Library                        │
├─────────────────────────────────────┤
│  [❤️] [⚡] [🏃] [🚴] [🏔️] [⏱️]     │
│  HR   PWR  RUN  CYC  ALT  TIME       │
│                                      │
│  Size: ○ 28×28  ● 40×40             │
│  [Preview on Glasses]               │
└─────────────────────────────────────┘
```

---

## References

- **Visual Assets Repository**: `docs/Activelook-Visual-Assets/`
- **Icon List**: `docs/Activelook-Visual-Assets/README.md`
- **API Documentation**: `docs/Activelook-API-Documentation/ActiveLook_API.md`
- **Implementation Plan**: `docs/DataField-Builder-Implementation-Plan.md`

---

**Last Updated**: 2025-01-27  
**ALooK Config Version**: 11  
**Compatible Firmware**: >= 4.2.X


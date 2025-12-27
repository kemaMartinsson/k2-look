# DataField Builder Tab - Implementation Plan

## Executive Summary

Create a visual DataField Builder tab that allows users to design custom layouts for their
ActiveLook glasses by selecting metrics, positions, fonts, and visual elements - similar to how
Garmin's ActiveLook app works but tailored for Karoo 2 data.

---

## 1. Architecture Overview

### 1.1 Core Components

```
DataFieldBuilderTab (UI)
    ↓
LayoutBuilderViewModel (State Management)
    ↓
LayoutBuilder (Business Logic)
    ↓
ActiveLookLayoutService (API Wrapper)
    ↓
ActiveLookService (Low-level BLE Communication)
```

### 1.2 Data Models

#### DataField Model

#### DataField Model

```kotlin
data class DataField(
    val id: Int,                    // Metric ID (from Garmin datafields list)
    val name: String,               // Display name (e.g., "Speed", "Heart Rate")
    val unit: String,               // Unit (e.g., "km/h", "bpm")
    val category: DataFieldCategory,
    val karooStreamType: KarooStreamType, // Maps to Karoo data stream
    val icon28: Int? = null,        // 28×28 icon ID from ActiveLook Visual Assets
    val icon40: Int? = null         // 40×40 icon ID from ActiveLook Visual Assets
)

enum class DataFieldCategory {
    GENERAL, HEART_RATE, POWER, SPEED_PACE,
    CADENCE, RUNNING_DYNAMICS, ELEVATION, ENERGY
}
```

#### LayoutScreen Model

```kotlin
data class LayoutScreen(
    val id: Int,                    // Screen number (1-N)
    val dataFields: List<LayoutDataField>, // Max 3 per screen
    val name: String = "Screen $id"
)

data class LayoutDataField(
    val dataField: DataField,
    val position: Position,         // TOP, MIDDLE, BOTTOM
    val fontSize: FontSize,         // SMALL, MEDIUM, LARGE
    val showLabel: Boolean = true,
    val showUnit: Boolean = true,
    val showIcon: Boolean = true,   // Display icon next to label
    val iconSize: IconSize = IconSize.SMALL // Icon size (28×28 or 40×40)
)

enum class Position { TOP, MIDDLE, BOTTOM }
enum class FontSize(val fontId: Int, val height: Int) {
    SMALL(1, 24), MEDIUM(2, 35), LARGE(3, 49)
}
enum class IconSize(val pixels: Int) {
    SMALL(28), LARGE(40)
}
```

#### ActiveLook Layout Model

```kotlin
data class ActiveLookLayout(
    val layoutId: Int,              // Layout number in glasses
    val clippingRegion: ClippingRegion,
    val foreColor: Int = 15,        // White
    val backColor: Int = 0,         // Black
    val font: Int = 2,              // Default medium font
    val textConfig: TextConfig,
    val additionalCommands: List<GraphicCommand> = emptyList()
)

data class ClippingRegion(
    val x: Int, val y: Int,
    val width: Int, val height: Int
)

data class TextConfig(
    val x: Int, val y: Int,
    val rotation: Int = 4,          // TOP_LR
    val opacity: Boolean = true
)

sealed class GraphicCommand {
    data class Image(val id: Int, val x: Int, val y: Int) : GraphicCommand()
    data class Circle(val x: Int, val y: Int, val radius: Int) : GraphicCommand()
    data class Line(val x0: Int, val y0: Int, val x1: Int, val y1: Int) : GraphicCommand()
    data class Rect(val x0: Int, val y0: Int, val x1: Int, val y1: Int) : GraphicCommand()
    data class Text(val x: Int, val y: Int, val text: String) : GraphicCommand()
}
```

#### Configuration Profile Model

```kotlin
data class DataFieldProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isDefault: Boolean = false,
    val isReadOnly: Boolean = false,
    val screens: List<LayoutScreen>,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)
```

#### Default Profile Factory

```kotlin
object DefaultProfiles {
    /**
     * Default read-only profile with essential metrics:
     * - Speed
     * - Distance
     * - Elapsed Time
     */
    fun getDefaultProfile(): DataFieldProfile {
        return DataFieldProfile(
            id = "default",
            name = "Default",
            isDefault = true,
            isReadOnly = true,
            screens = listOf(
                LayoutScreen(
                    id = 1,
                    name = "Main",
                    dataFields = listOf(
                        LayoutDataField(
                            dataField = DataFieldRegistry.getById(12), // Speed
                            position = Position.TOP,
                            fontSize = FontSize.LARGE,
                            showIcon = true,
                            iconSize = IconSize.LARGE
                        ),
                        LayoutDataField(
                            dataField = DataFieldRegistry.getById(2), // Distance
                            position = Position.MIDDLE,
                            fontSize = FontSize.LARGE,
                            showIcon = true,
                            iconSize = IconSize.LARGE
                        ),
                        LayoutDataField(
                            dataField = DataFieldRegistry.getById(1), // Elapsed Time
                            position = Position.BOTTOM,
                            fontSize = FontSize.LARGE,
                            showIcon = true,
                            iconSize = IconSize.LARGE
                        )
                    )
                )
            )
        )
    }

    /**
     * Pre-built template for road bikes with power meter
     */
    fun getRoadBikeProfile(): DataFieldProfile {
        return DataFieldProfile(
            id = "template_road",
            name = "Road Bike (with Power)",
            isDefault = false,
            isReadOnly = false,
            screens = listOf(
                LayoutScreen(
                    id = 1,
                    name = "Primary",
                    dataFields = listOf(
                        LayoutDataField(
                            dataField = DataFieldRegistry.getById(12), // Speed
                            position = Position.TOP,
                            fontSize = FontSize.LARGE,
                            showIcon = true
                        ),
                        LayoutDataField(
                            dataField = DataFieldRegistry.getById(7), // Power
                            position = Position.MIDDLE,
                            fontSize = FontSize.LARGE,
                            showIcon = true
                        ),
                        LayoutDataField(
                            dataField = DataFieldRegistry.getById(4), // Heart Rate
                            position = Position.BOTTOM,
                            fontSize = FontSize.MEDIUM,
                            showIcon = true
                        )
                    )
                )
            )
        )
    }

    /**
     * Pre-built template for bikes without power meter
     */
    fun getGravelBikeProfile(): DataFieldProfile {
        return DataFieldProfile(
            id = "template_gravel",
            name = "Gravel Bike (no Power)",
            isDefault = false,
            isReadOnly = false,
            screens = listOf(
                LayoutScreen(
                    id = 1,
                    name = "Primary",
                    dataFields = listOf(
                        LayoutDataField(
                            dataField = DataFieldRegistry.getById(12), // Speed
                            position = Position.TOP,
                            fontSize = FontSize.LARGE,
                            showIcon = true
                        ),
                        LayoutDataField(
                            dataField = DataFieldRegistry.getById(4), // Heart Rate
                            position = Position.MIDDLE,
                            fontSize = FontSize.LARGE,
                            showIcon = true
                        ),
                        LayoutDataField(
                            dataField = DataFieldRegistry.getById(2), // Distance
                            position = Position.BOTTOM,
                            fontSize = FontSize.MEDIUM,
                            showIcon = true
                        )
                    )
                )
            )
        )
    }
}
```

---

## 2. UI Design (DataFieldBuilderTab)

### 2.1 Screen Layout Structure

```
┌─────────────────────────────────────┐
│  DataField Builder                   │
├─────────────────────────────────────┤
│  Active Profile: [Road Bike ▼]      │
│  [Manage Profiles]                  │
├─────────────────────────────────────┤
│                                      │
│  ┌────────────────────────────────┐ │
│  │ Screen 1: [Active]             │ │
│  │  ┌──────────────────────────┐  │ │
│  │  │ TOP: Speed (Large)       │  │ │
│  │  │      22.5 km/h           │  │ │
│  │  ├──────────────────────────┤  │ │
│  │  │ MIDDLE: Heart Rate (Med) │  │ │
│  │  │         145 bpm          │  │ │
│  │  ├──────────────────────────┤  │ │
│  │  │ BOTTOM: Power (Med)      │  │ │
│  │  │         250 w            │  │ │
│  │  └──────────────────────────┘  │ │
│  │  [+ Add Field] [Preview]     │ │
│  └────────────────────────────────┘ │
│                                      │
│  Screen Tabs: [1] [2] [3] [+]       │
│                                      │
│  ┌────────────────────────────────┐ │
│  │ Available Metrics              │ │
│  │  ▼ General                     │ │
│  │    □ Elapsed Time              │ │
│  │    □ Distance                  │ │
│  │  ▼ Heart Rate                  │ │
│  │    □ Heart Rate                │ │
│  │    □ HR Zone Widget            │ │
│  │  ▼ Power                       │ │
│  │    □ Power                     │ │
│  │    □ Power 3s                  │ │
│  └────────────────────────────────┘ │
│                                      │
│  [Build & Send to Glasses]          │
│  [Save Configuration]               │
└─────────────────────────────────────┘
```

### 2.2 Profile Management

**Multiple Configuration Profiles** allow users to create different setups for different
bikes/scenarios:

**Example Use Cases**:

- **Bike 1** (Road with Power): Speed, Power, Heart Rate
- **Bike 2** (Gravel no Power): Speed, Heart Rate, Distance
- **Training Mode**: HR Zones, Power 3s, Cadence
- **Touring Mode**: Speed, Distance, Time

**Default Profile** (Read-only):

- Speed
- Distance
- Elapsed Time
- Cannot be edited/deleted
- Always available as fallback

### 2.2.1 Automatic Profile Switching

**K2Look can automatically switch profiles based on the active Karoo ride profile!**

The Karoo System Service provides `ActiveRideProfile` events that notify when the user selects a
different profile on the Karoo launcher. K2Look monitors these changes and automatically switches to
a matching datafield profile by name.

**How It Works**:

1. User selects "Road Bike" profile on Karoo launcher
2. K2Look receives `ActiveRideProfile` event with profile name "Road Bike"
3. K2Look searches for a datafield profile named "Road Bike"
4. If found, automatically switches to that profile
5. Display updates on glasses with the new profile's datafields

**Example Workflow**:

```
Karoo Profile: "Road Bike"  →  K2Look Profile: "Road Bike" (with power metrics)
Karoo Profile: "Gravel"     →  K2Look Profile: "Gravel" (no power metrics)
Karoo Profile: "Indoor"     →  K2Look Profile: "Indoor" (trainer metrics)
```

**Benefits**:

- ✅ Seamless experience - one profile switch updates everything
- ✅ No manual profile selection needed in K2Look
- ✅ Consistent naming between Karoo and glasses display
- ✅ Can be toggled on/off in settings

**User Control**:

- Auto-switch can be enabled/disabled via toggle
- Manual override always available
- Profile name matching is case-insensitive
- Falls back to current profile if no match found

### 2.3 Interaction Flow

1. **Select Screen**: User taps screen tab (1, 2, 3, or +)
2. **Add DataField**:
    - Tap position slot (TOP/MIDDLE/BOTTOM)
    - Select metric from expandable category list
    - Configure font size, labels
3. **Preview**: Live preview showing layout on simulated glasses display
4. **Build**: Generate ActiveLook layouts and send to glasses
5. **Test**: Switch between screens to verify display

### 2.4 Key UI Components

```kotlin
@Composable
fun DataFieldBuilderTab(
    viewModel: LayoutBuilderViewModel,
    uiState: LayoutBuilderViewModel.UiState
)

@Composable
fun ProfileSelectorCard(
    profiles: List<DataFieldProfile>,
    activeProfile: DataFieldProfile?,
    onProfileSelected: (String) -> Unit,
    onCreateProfile: () -> Unit,
    onManageProfiles: () -> Unit
)

@Composable
fun ProfileManagementScreen(
    profiles: List<DataFieldProfile>,
    onBack: () -> Unit,
    onCreateProfile: (name: String, template: String?) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onDuplicateProfile: (String, String) -> Unit
)

@Composable
fun ScreenEditor(
    screen: LayoutScreen,
    onFieldAdd: (Position, DataField) -> Unit,
    onFieldEdit: (LayoutDataField) -> Unit,
    onFieldRemove: (Position) -> Unit
)

@Composable
fun DataFieldSlot(
    position: Position,
    field: LayoutDataField?,
    onTap: () -> Unit
)

@Composable
fun MetricSelector(
    categories: List<DataFieldCategory>,
    dataFields: List<DataField>,
    onSelect: (DataField) -> Unit
)

@Composable
fun GlassesPreview(
    screen: LayoutScreen,
    showGrid: Boolean = true
)
```

---

## 3. ActiveLook Visual Assets Integration

### 3.1 Pre-loaded Icons

The ActiveLook glasses come with a **system configuration called "ALooK"** that includes:

- **80+ pre-loaded icons** in two sizes (28×28 and 40×40 pixels)
- **5 custom fonts** (24px to 82px)
- **Pre-defined layouts** for common metrics
- **Animations** (splash screens, bluetooth, battery, etc.)

**Key Advantage**: No need to upload icons - they're already in the glasses! 🎉

### 3.2 Icon Mapping

| Icon Category     | 28×28 IDs  | 40×40 IDs    | Examples                      |
|-------------------|------------|--------------|-------------------------------|
| **Time/Distance** | 8-10       | 40-42        | Chrono, Distance, Destination |
| **Heart Rate**    | 12-14      | 44-46        | HR, HR Avg, HR Max            |
| **Power**         | 19-22      | 51-54        | Power, Power 3s, Avg, Max     |
| **Speed**         | 26-28      | 58-60        | Speed, Avg Speed, Max Speed   |
| **Cadence**       | 4-6, 23-25 | 36-38, 55-57 | Cycling & Running variants    |
| **Elevation**     | 2, 29-31   | 34, 61-63    | Altitude, Ascent, Descent     |
| **Energy**        | 7, 11      | 39, 43       | Calories, Energy Expenditure  |

**Reference**: See `docs/Activelook-Visual-Assets/README.md` for complete icon list

### 3.3 Configuration Setup

```kotlin
// Ensure ALooK system config is active to access icons
activeLookService.cfgSet("ALooK")  // Load system configuration
```

**Important**: The ALooK configuration must be selected before displaying layouts that use icons.
Our implementation will ensure this during initialization.

---

## 4. Data Mapping

### 3.1 Karoo to DataField Mapping

Based on Garmin datafields.md, current Karoo streams, and **ActiveLook Visual Assets**:

| ID | Garmin Metric  | Karoo Stream         | Unit     | Icon (28×28) | Icon (40×40) | Available |
|----|----------------|----------------------|----------|--------------|--------------|-----------|
| 1  | Elapsed Time   | timeData             | HH:MM:SS | 8            | 40           | ✅         |
| 2  | Distance       | distanceData         | km       | 9            | 41           | ✅         |
| 4  | Heart Rate     | heartRateData        | bpm      | 12           | 44           | ✅         |
| 5  | Max Heart Rate | maxHeartRateData     | bpm      | 14           | 46           | ✅         |
| 6  | Avg Heart Rate | averageHeartRateData | bpm      | 13           | 45           | ✅         |
| 7  | Power          | powerData            | w        | 19           | 51           | ✅         |
| 8  | Max Power      | maxPowerData         | w        | 22           | 54           | ✅         |
| 9  | Avg Power      | averagePowerData     | w        | 21           | 53           | ✅         |
| 10 | Power 3s       | smoothed3sPowerData  | w        | 20           | 52           | ✅         |
| 11 | Normalized Pwr | normalizedPowerData  | w        | 15           | 47           | ⚠️        |
| 12 | Speed          | speedData            | km/h     | 26           | 58           | ✅         |
| 13 | Max Speed      | maxSpeedData         | km/h     | 28           | 60           | ✅         |
| 14 | Avg Speed      | averageSpeedData     | km/h     | 27           | 59           | ✅         |
| 18 | Cadence        | cadenceData          | rpm      | 4/23         | 36/55        | ✅         |
| 19 | Max Cadence    | maxCadenceData       | rpm      | 6/25         | 38/57        | ✅         |
| 20 | Avg Cadence    | averageCadenceData   | rpm      | 5/24         | 37/56        | ✅         |
| 21 | Altitude       | altitudeData         | m        | 2            | 34           | ⚠️        |
| 22 | Total Ascent   | totalAscentData      | m        | 30           | 62           | ⚠️        |
| 23 | Total Descent  | totalDescentData     | m        | 31           | 63           | ⚠️        |
| 24 | Ascent Speed   | ascentRateData       | m/h      | 29           | 61           | ⚠️        |
| 25 | Calories       | caloriesData         | kcal     | 7            | 39           | ⚠️        |
| 47 | HR Zone Widget | hrZoneData           | zone     | -            | -            | ✅         |

**Notes**:

- Icon IDs from ActiveLook Visual Assets (ALooK config v11)
- 28×28 icons for compact layouts, 40×40 for prominent display
- Cadence has separate icons for cycling (4/36) and running (23/55)
- Icons pre-loaded in glasses' default configuration - no upload needed!

### 3.2 Default DataField List

```kotlin
object DataFieldRegistry {
    val ALL_FIELDS = listOf(
        // General
        DataField(
            1, "Elapsed Time", "HH:MM:SS", GENERAL, KarooStreamType.TIME,
            icon28 = 8, icon40 = 40
        ),
        DataField(
            2, "Distance", "km", GENERAL, KarooStreamType.DISTANCE,
            icon28 = 9, icon40 = 41
        ),

        // Heart Rate
        DataField(
            4, "Heart Rate", "bpm", HEART_RATE, KarooStreamType.HEART_RATE,
            icon28 = 12, icon40 = 44
        ),
        DataField(
            5, "Max Heart Rate", "bpm", HEART_RATE, KarooStreamType.MAX_HEART_RATE,
            icon28 = 14, icon40 = 46
        ),
        DataField(
            6, "Avg Heart Rate", "bpm", HEART_RATE, KarooStreamType.AVG_HEART_RATE,
            icon28 = 13, icon40 = 45
        ),
        DataField(47, "HR Zone", "Z", HEART_RATE, KarooStreamType.HR_ZONE),

        // Power
        DataField(
            7, "Power", "w", POWER, KarooStreamType.POWER,
            icon28 = 19, icon40 = 51
        ),
        DataField(
            8, "Max Power", "w", POWER, KarooStreamType.MAX_POWER,
            icon28 = 22, icon40 = 54
        ),
        DataField(
            9, "Avg Power", "w", POWER, KarooStreamType.AVG_POWER,
            icon28 = 21, icon40 = 53
        ),
        DataField(
            10, "Power 3s", "w", POWER, KarooStreamType.POWER_3S,
            icon28 = 20, icon40 = 52
        ),

        // Speed
        DataField(
            12, "Speed", "km/h", SPEED_PACE, KarooStreamType.SPEED,
            icon28 = 26, icon40 = 58
        ),
        DataField(
            13, "Max Speed", "km/h", SPEED_PACE, KarooStreamType.MAX_SPEED,
            icon28 = 28, icon40 = 60
        ),
        DataField(
            14, "Avg Speed", "km/h", SPEED_PACE, KarooStreamType.AVG_SPEED,
            icon28 = 27, icon40 = 59
        ),

        // Cadence (cycling)
        DataField(
            18, "Cadence", "rpm", CADENCE, KarooStreamType.CADENCE,
            icon28 = 4, icon40 = 36
        ),
        DataField(
            19, "Max Cadence", "rpm", CADENCE, KarooStreamType.MAX_CADENCE,
            icon28 = 6, icon40 = 38
        ),
        DataField(
            20, "Avg Cadence", "rpm", CADENCE, KarooStreamType.AVG_CADENCE,
            icon28 = 5, icon40 = 37
        ),

        // Elevation (future implementation)
        // DataField(21, "Altitude", "m", ELEVATION, KarooStreamType.ALTITUDE, 
        //     icon28 = 2, icon40 = 34),
        // DataField(22, "Total Ascent", "m", ELEVATION, KarooStreamType.TOTAL_ASCENT, 
        //     icon28 = 30, icon40 = 62),
        // DataField(23, "Total Descent", "m", ELEVATION, KarooStreamType.TOTAL_DESCENT, 
        //     icon28 = 31, icon40 = 63),
        // DataField(24, "Ascent Speed", "m/h", ELEVATION, KarooStreamType.ASCENT_RATE, 
        //     icon28 = 29, icon40 = 61),

        // Energy (future implementation)
        // DataField(25, "Calories", "kcal", ENERGY, KarooStreamType.CALORIES, 
        //     icon28 = 7, icon40 = 39),
    )

    fun getByCategory(category: DataFieldCategory) =
        ALL_FIELDS.filter { it.category == category }

    fun getIconId(dataField: DataField, size: IconSize): Int? {
        return when (size) {
            IconSize.SMALL -> dataField.icon28
            IconSize.LARGE -> dataField.icon40
        }
    }
}
```

---

## 4. Layout Generation Algorithm

### 4.1 Screen Layout Calculations

ActiveLook display: **304 x 256 pixels**

**3-Field Layout** (TOP, MIDDLE, BOTTOM):

```
┌─────────────────────┐  0
│  Label: SPEED      │  
│  Value: 22.5 km/h  │  
├─────────────────────┤  85  (256/3 ≈ 85px per section)
│  Label: HR         │
│  Value: 145 bpm    │
├─────────────────────┤  170
│  Label: POWER      │
│  Value: 250 w      │
└─────────────────────┘  256
```

### 4.2 Layout Builder Logic

```kotlin
class LayoutBuilder {
    companion object {
        const val DISPLAY_WIDTH = 304
        const val DISPLAY_HEIGHT = 256
        const val SECTION_HEIGHT = 85  // 256 / 3
        const val MARGIN_X = 10
        const val LABEL_OFFSET_Y = 15
        const val VALUE_OFFSET_Y = 40
    }

    fun buildLayout(
        layoutId: Int,
        screen: LayoutScreen,
        position: Position
    ): ActiveLookLayout {
        val sectionY = when (position) {
            Position.TOP -> 0
            Position.MIDDLE -> SECTION_HEIGHT
            Position.BOTTOM -> SECTION_HEIGHT * 2
        }

        val field = screen.dataFields.find { it.position == position }
            ?: return createEmptyLayout(layoutId, sectionY)

        return ActiveLookLayout(
            layoutId = layoutId,
            clippingRegion = ClippingRegion(
                x = 0,
                y = sectionY,
                width = DISPLAY_WIDTH,
                height = SECTION_HEIGHT
            ),
            foreColor = 15,  // White
            backColor = 0,   // Black
            font = field.fontSize.fontId,
            textConfig = TextConfig(
                x = DISPLAY_WIDTH / 2,  // Center
                y = VALUE_OFFSET_Y,
                rotation = 4,  // TOP_LR centered
                opacity = true
            ),
            additionalCommands = buildAdditionalCommands(field, sectionY)
        )
    }

    private fun buildAdditionalCommands(
        field: LayoutDataField,
        sectionY: Int
    ): List<GraphicCommand> {
        val commands = mutableListOf<GraphicCommand>()

        var labelX = DISPLAY_WIDTH / 2  // Default centered

        // Add icon if enabled (from ActiveLook Visual Assets)
        if (field.showIcon) {
            val iconId = DataFieldRegistry.getIconId(field.dataField, field.iconSize)
            if (iconId != null) {
                val iconSize = field.iconSize.pixels
                val iconX = MARGIN_X + 10  // Left side with margin
                val iconY = (SECTION_HEIGHT - iconSize) / 2  // Vertically centered

                commands.add(
                    GraphicCommand.Image(
                        id = iconId,
                        x = iconX,
                        y = iconY
                    )
                )

                // Shift label right if icon is shown
                labelX = iconX + iconSize + 10
            }
        }

        // Add label if enabled
        if (field.showLabel) {
            commands.add(
                GraphicCommand.Text(
                    x = labelX,
                    y = LABEL_OFFSET_Y,
                    text = field.dataField.name.uppercase()
                )
            )
        }

        // Add separator line between sections
        commands.add(
            GraphicCommand.Line(
                x0 = MARGIN_X,
                y0 = SECTION_HEIGHT - 1,
                x1 = DISPLAY_WIDTH - MARGIN_X,
                y1 = SECTION_HEIGHT - 1
            )
        )

        return commands
    }
}
```

**Icon Layout Example:**

```
┌─────────────────────────────┐
│ [❤️] HR     145 bpm        │  ← Icon (28×28) + Label + Value
└─────────────────────────────┘
```

### 4.3 Layout Encoding

Following ActiveLook protocol (see ActiveLook_API.md section 5.10):

```kotlin
class ActiveLookLayoutEncoder {
    fun encodeLayout(layout: ActiveLookLayout): ByteArray {
        val buffer = ByteBuffer.allocate(126)

        // Layout header (17 bytes)
        buffer.put(layout.layoutId.toByte())
        buffer.put(calculateAdditionalCommandsSize(layout).toByte())
        buffer.putShort(layout.clippingRegion.x.toShort())
        buffer.put(layout.clippingRegion.y.toByte())
        buffer.putShort(layout.clippingRegion.width.toShort())
        buffer.put(layout.clippingRegion.height.toByte())
        buffer.put(layout.foreColor.toByte())
        buffer.put(layout.backColor.toByte())
        buffer.put(layout.font.toByte())
        buffer.put(1) // TextValid = true
        buffer.putShort(layout.textConfig.x.toShort())
        buffer.put(layout.textConfig.y.toByte())
        buffer.put(layout.textConfig.rotation.toByte())
        buffer.put(if (layout.textConfig.opacity) 1 else 0)

        // Additional commands
        layout.additionalCommands.forEach { cmd ->
            encodeCommand(buffer, cmd)
        }

        return buffer.array().copyOf(buffer.position())
    }

    private fun encodeCommand(buffer: ByteBuffer, cmd: GraphicCommand) {
        when (cmd) {
            is GraphicCommand.Text -> {
                buffer.put(9) // Text command ID
                buffer.putShort(cmd.x.toShort())
                buffer.put(cmd.y.toByte())
                buffer.put(cmd.text.length.toByte())
                buffer.put(cmd.text.toByteArray(Charsets.US_ASCII))
            }
            is GraphicCommand.Line -> {
                buffer.put(5) // Line command ID
                buffer.putShort(cmd.x0.toShort())
                buffer.putShort(cmd.y0.toShort())
                buffer.putShort(cmd.x1.toShort())
                buffer.putShort(cmd.y1.toShort())
            }
            // ... other commands
        }
    }
}
```

### 4.4 ViewModel Profile Management

```kotlin
class LayoutBuilderViewModel(
    private val profileRepository: ProfileRepository,
    private val layoutBuilder: LayoutBuilder,
    private val activeLookService: ActiveLookService
) : ViewModel() {

    // Profile management
    private val _profiles = MutableStateFlow<List<DataFieldProfile>>(emptyList())
    val profiles: StateFlow<List<DataFieldProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<DataFieldProfile?>(null)
    val activeProfile: StateFlow<DataFieldProfile?> = _activeProfile.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            val saved = profileRepository.loadProfiles()
            val default = DefaultProfiles.getDefaultProfile()

            // Always include default profile first
            _profiles.value = listOf(default) + saved

            // Set active profile (first saved, or default)
            _activeProfile.value = saved.firstOrNull() ?: default
        }
    }

    /**
     * Create a new profile from scratch or from a template
     */
    fun createProfile(name: String, templateId: String? = null) {
        val newProfile = when (templateId) {
            "road" -> DefaultProfiles.getRoadBikeProfile().copy(
                id = UUID.randomUUID().toString(),
                name = name
            )
            "gravel" -> DefaultProfiles.getGravelBikeProfile().copy(
                id = UUID.randomUUID().toString(),
                name = name
            )
            else -> DataFieldProfile(
                name = name,
                screens = listOf(LayoutScreen(id = 1, name = "Screen 1", dataFields = emptyList()))
            )
        }

        viewModelScope.launch {
            profileRepository.saveProfile(newProfile)
            _profiles.value = _profiles.value + newProfile
            _activeProfile.value = newProfile // Auto-switch to new profile
        }
    }

    /**
     * Switch to a different profile
     */
    fun switchProfile(profileId: String) {
        val profile = _profiles.value.find { it.id == profileId }
        if (profile != null) {
            _activeProfile.value = profile
            Log.i(TAG, "Switched to profile: ${profile.name}")
        }
    }

    /**
     * Delete a user profile (cannot delete default)
     */
    fun deleteProfile(profileId: String) {
        val profile = _profiles.value.find { it.id == profileId }

        if (profile?.isReadOnly == true) {
            Log.w(TAG, "Cannot delete read-only default profile")
            return
        }

        viewModelScope.launch {
            profileRepository.deleteProfile(profileId)
            _profiles.value = _profiles.value.filter { it.id != profileId }

            // Switch to default if deleted profile was active
            if (_activeProfile.value?.id == profileId) {
                _activeProfile.value = _profiles.value.first { it.isDefault }
                Log.i(TAG, "Switched to default profile after deletion")
            }
        }
    }

    /**
     * Duplicate an existing profile
     */
    fun duplicateProfile(profileId: String, newName: String) {
        val original = _profiles.value.find { it.id == profileId } ?: return
        val duplicate = original.copy(
            id = UUID.randomUUID().toString(),
            name = newName,
            isReadOnly = false,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            profileRepository.saveProfile(duplicate)
            _profiles.value = _profiles.value + duplicate
            Log.i(TAG, "Duplicated profile: ${original.name} → $newName")
        }
    }

    /**
     * Update active profile (save changes)
     */
    fun updateActiveProfile(updatedProfile: DataFieldProfile) {
        if (updatedProfile.isReadOnly) {
            Log.w(TAG, "Cannot update read-only profile")
            return
        }

        viewModelScope.launch {
            val updated = updatedProfile.copy(modifiedAt = System.currentTimeMillis())
            profileRepository.saveProfile(updated)

            // Update in list
            _profiles.value = _profiles.value.map {
                if (it.id == updated.id) updated else it
            }
            _activeProfile.value = updated
        }
    }

    /**
     * Enable/disable automatic profile switching based on Karoo ride profile
     */
    private val _autoSwitchEnabled = MutableStateFlow(true)
    val autoSwitchEnabled: StateFlow<Boolean> = _autoSwitchEnabled.asStateFlow()

    fun setAutoSwitch(enabled: Boolean) {
        _autoSwitchEnabled.value = enabled
        if (enabled) {
            Log.i(TAG, "Auto-switch enabled: will match K2Look profiles to Karoo profiles by name")
        } else {
            Log.i(TAG, "Auto-switch disabled: manual profile selection only")
        }
    }

    /**
     * Monitor Karoo ride profile changes and auto-switch K2Look profiles
     */
    fun observeKarooProfile(karooDataService: KarooDataService) {
        viewModelScope.launch {
            karooDataService.activeRideProfile.collect { karooProfile ->
                if (karooProfile != null && _autoSwitchEnabled.value) {
                    // Try to find a K2Look profile with matching name
                    val matchingProfile = _profiles.value.find {
                        it.name.equals(karooProfile.name, ignoreCase = true)
                    }

                    if (matchingProfile != null) {
                        Log.i(
                            TAG, "Auto-switching to profile '${matchingProfile.name}' " +
                                    "to match Karoo profile '${karooProfile.name}'"
                        )
                        _activeProfile.value = matchingProfile
                    } else {
                        Log.d(
                            TAG,
                            "No K2Look profile found matching Karoo profile '${karooProfile.name}'"
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "LayoutBuilderViewModel"
    }
}
```

### 4.5 Profile Repository

```kotlin
class ProfileRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("k2look_profiles", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadProfiles(): List<DataFieldProfile> {
        val json = prefs.getString("user_profiles", "[]") ?: "[]"
        val type = object : TypeToken<List<DataFieldProfile>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profiles", e)
            emptyList()
        }
    }

    fun saveProfile(profile: DataFieldProfile) {
        val profiles = loadProfiles().toMutableList()
        val index = profiles.indexOfFirst { it.id == profile.id }

        if (index >= 0) {
            // Update existing profile
            profiles[index] = profile.copy(modifiedAt = System.currentTimeMillis())
        } else {
            // Add new profile
            profiles.add(profile)
        }

        val json = gson.toJson(profiles)
        prefs.edit().putString("user_profiles", json).apply()
        Log.i(TAG, "Saved profile: ${profile.name}")
    }

    fun deleteProfile(profileId: String) {
        val profiles = loadProfiles().filter { it.id != profileId }
        val json = gson.toJson(profiles)
        prefs.edit().putString("user_profiles", json).apply()
        Log.i(TAG, "Deleted profile: $profileId")
    }

    companion object {
        private const val TAG = "ProfileRepository"
    }
}
```

---

## 5. Implementation Phases

### Phase 1: Foundation (Week 1) ✅ COMPLETE

- [x] Create data models (DataField, LayoutScreen, LayoutDataField, etc.)
- [x] **Create DataFieldProfile model with read-only flag**
- [x] **Implement DefaultProfiles factory (Default, Road Bike, Gravel Bike)**
- [x] Implement DataFieldRegistry with Karoo stream mapping
- [x] **Add icon28/icon40 fields to DataField model**
- [x] **Add ProfileRepository for configuration persistence**
- [x] **Add Gson dependency to build.gradle.kts**
- [x] **Fix deprecated exec() warnings in build.gradle.kts**

**Status**: ✅ All data models created and compiling successfully!

**Files Created**:

- `model/DataFieldCategory.kt` - Metric categories enum
- `model/IconSize.kt` - Icon size enum (28×28, 40×40)
- `model/FontSize.kt` - Font size enum (Small, Medium, Large)
- `model/Position.kt` - Position enum (TOP, MIDDLE, BOTTOM)
- `model/DataField.kt` - Metric definition with icon IDs
- `model/LayoutDataField.kt` - Configured field with display settings
- `model/LayoutScreen.kt` - Screen with up to 3 fields
- `model/DataFieldProfile.kt` - Profile with multiple screens
- `data/DataFieldRegistry.kt` - Registry of 19 metrics with icons
- `data/DefaultProfiles.kt` - Default, Road Bike, Gravel templates
- `data/ProfileRepository.kt` - Persistence with Gson

### Phase 2: UI Components (Week 1-2)

- [ ] **Implement ProfileSelectorCard composable**
- [ ] **Build ProfileManagementScreen (create/delete/duplicate)**
- [ ] Implement ScreenEditor composable
- [ ] Create DataFieldSlot composable (TOP/MIDDLE/BOTTOM)
- [ ] Build MetricSelector with categories
- [ ] **Add icon display toggle in field configuration**
- [ ] Add screen tab navigation
- [ ] Implement field configuration dialog (font, labels, icons, etc.)

### Phase 3: Layout Engine (Week 2)

- [ ] Implement LayoutBuilder class
- [ ] Create layout calculation logic (positions, fonts, sizes)
- [ ] **Add icon positioning logic (use pre-loaded ActiveLook icons)**
- [ ] Build ActiveLookLayoutEncoder
- [ ] Test layout encoding with manual bytes verification
- [ ] **Verify ALooK configuration is selected (cfgSet("ALooK"))**

### Phase 4: ActiveLook Integration (Week 2-3)

- [ ] Create ActiveLookLayoutService wrapper
- [ ] Implement cfgWrite/cfgSelect flow
- [ ] **Ensure ALooK system config is active for icon access**
- [ ] Add layoutSave command support in ActiveLookService
- [ ] Implement layoutDisplay command
- [ ] **Test icon display in layouts**
- [ ] Test layouts on real glasses

### Phase 5: Data Binding (Week 3)

- [ ] Connect Karoo streams to layout updates
- [ ] Implement real-time value formatting
- [ ] Add unit conversion (imperial/metric)
- [ ] Handle missing/unavailable data streams
- [ ] **Add ActiveRideProfile monitoring in KarooDataService**
- [ ] **Implement auto-switch logic in ViewModel**
- [ ] **Add auto-switch toggle in settings**
- [ ] **Test profile switching on Karoo launcher**

### Phase 6: Persistence (Week 3-4)

- [ ] **Implement ProfileRepository with SharedPreferences/JSON**
- [ ] **Add profile CRUD operations (create, read, update, delete)**
- [ ] **Implement profile switching logic**
- [ ] **Add profile duplication feature**
- [ ] **Protect default profile from modification**
- [ ] Add configuration export/import (JSON file)
- [ ] **Create template profiles (Road Bike, Gravel Bike)**
- [ ] Test profile persistence across app restarts

### Phase 7: Polish & Testing (Week 4)

- [ ] Add GlassesPreview composable
- [ ] Implement preview mode with sample data
- [ ] Add validation (max 3 fields per screen, etc.)
- [ ] Write unit tests for LayoutBuilder
- [ ] Create integration tests
- [ ] Add error handling and user feedback

### Phase 8: Advanced Features (Future)

- [ ] Custom colors/themes
- [ ] **Icon selection UI (28×28 vs 40×40)**
- [ ] **Sport-specific icons (running vs cycling cadence)**
- [ ] Custom layouts with drag-and-drop
- [ ] Layout templates gallery
- [ ] Community layout sharing
- [ ] Custom font upload (advanced users)
- [ ] Custom image upload (advanced users)

---

## 6. File Structure

```
app/src/main/kotlin/com/kema/k2look/
├── model/
│   ├── DataField.kt
│   ├── DataFieldCategory.kt
│   ├── LayoutScreen.kt
│   ├── LayoutDataField.kt
│   ├── ActiveLookLayout.kt
│   ├── DataFieldProfile.kt          # NEW: Profile model
│   ├── IconSize.kt                  # NEW: Icon size enum
│   ├── FontSize.kt
│   └── Position.kt
│
├── data/
│   ├── DataFieldRegistry.kt
│   ├── DefaultProfiles.kt           # NEW: Default profile factory
│   └── ProfileRepository.kt         # NEW: Profile persistence
│
├── service/
│   ├── ActiveLookLayoutService.kt
│   └── LayoutDataBridge.kt
│
├── viewmodel/
│   └── LayoutBuilderViewModel.kt    # UPDATED: Profile management
│
├── screens/
│   ├── DataFieldBuilderTab.kt
│   ├── ProfileSelectorCard.kt       # NEW: Profile selection UI
│   ├── ProfileManagementScreen.kt   # NEW: Profile CRUD UI
│   ├── ScreenEditor.kt
│   ├── DataFieldSlot.kt
│   ├── MetricSelector.kt
│   └── GlassesPreview.kt
│
└── util/
    ├── LayoutBuilder.kt
    ├── ActiveLookLayoutEncoder.kt
    └── DataFormatter.kt
```

---

## 7. Key Technical Decisions

### 7.1 Layout ID Management

- **System layouts**: 0-9 (reserved by ActiveLook)
- **User layouts**: 10-99 (available for custom screens)
- **Mapping**: Screen 1 → layouts 10, 11, 12 (TOP, MIDDLE, BOTTOM)
- **Mapping**: Screen 2 → layouts 13, 14, 15
- **Mapping**: Screen 3 → layouts 16, 17, 18

### 7.2 Configuration Format

- **Write config**: Each custom configuration gets unique name
- **Format**: JSON file stored in app private storage
- **Example**:

```json
{
  "name": "My Cycling Layout",
  "version": "1.0",
  "screens": [
    {
      "id": 1,
      "name": "Speed Screen",
      "dataFields": [
        {
          "dataFieldId": 12,
          "position": "TOP",
          "fontSize": "LARGE",
          "showLabel": true,
          "showUnit": true
        }
      ]
    }
  ]
}
```

### 7.3 Real-time Updates

- Subscribe to Karoo streams in LayoutDataBridge
- Format values using DataFormatter
- Call layoutDisplay with formatted string
- Update at 1Hz (same as current implementation)

### 7.4 Error Handling

- Validate layout before sending (max size, valid IDs, etc.)
- Check glasses connection before cfgWrite
- Provide clear error messages to user
- Fall back to default layouts on failure

---

## 8. Testing Strategy

### 8.1 Unit Tests

```kotlin
class LayoutBuilderTest {
    @Test
    fun `test 3-field layout calculations`()

    @Test
    fun `test layout encoding matches protocol`()

    @Test
    fun `test datafield to karoo stream mapping`()

    @Test
    fun `test icon positioning in layouts`()
}

class DefaultProfilesTest {
    @Test
    fun `test default profile is read-only`()

    @Test
    fun `test default profile has speed distance time`()

    @Test
    fun `test template profiles are not read-only`()
}

class ProfileRepositoryTest {
    @Test
    fun `test save and load profiles`()

    @Test
    fun `test cannot save over default profile`()

    @Test
    fun `test profile deletion`()
}
```

### 8.2 Integration Tests

```kotlin
class LayoutIntegrationTest {
    @Test
    fun `test full layout build and send flow`()

    @Test
    fun `test layout switching between screens`()

    @Test
    fun `test profile switching updates display`()

    @Test
    fun `test default profile cannot be edited`()

    @Test
    fun `test real-time data updates`()
}
```

### 8.3 Manual Testing Checklist

**Layout Building**:

- [ ] Create layout with 1 field
- [ ] Create layout with 2 fields
- [ ] Create layout with 3 fields
- [ ] Add icons to fields (28×28 and 40×40)
- [ ] Toggle icons on/off

**Profile Management**:

- [ ] View default profile (read-only)
- [ ] Create new profile from scratch
- [ ] Create profile from Road Bike template
- [ ] Create profile from Gravel Bike template
- [ ] Switch between profiles
- [ ] Duplicate existing profile
- [ ] Try to edit default profile (should fail)
- [ ] Try to delete default profile (should fail)
- [ ] Delete user profile
- [ ] Profile persists after app restart

**Automatic Profile Switching**:

- [ ] Enable auto-switch in settings
- [ ] Create K2Look profile named "Road Bike"
- [ ] Switch to "Road Bike" on Karoo launcher
- [ ] Verify K2Look auto-switches to "Road Bike" profile
- [ ] Verify glasses display updates with correct metrics
- [ ] Test with profile name mismatch (should keep current profile)
- [ ] Disable auto-switch and verify manual control works
- [ ] Test case-insensitive matching ("road bike" matches "Road Bike")

**Data Display**:

- [ ] Switch between screens
- [ ] Test with real ride data
- [ ] Test with simulator data
- [ ] Verify glasses display matches preview
- [ ] Test with glasses disconnected
- [ ] Icons display correctly on glasses

---

## 9. Dependencies

### New Dependencies (if needed)

```kotlin
// build.gradle.kts
dependencies {
    // JSON serialization for config storage
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Optional: Preview rendering
    // implementation("androidx.compose.ui:ui-tooling-preview")
}
```

### Existing Dependencies

- Karoo Extension SDK (already integrated)
- ActiveLook SDK (already integrated)
- Jetpack Compose (already used for UI)
- Kotlin Coroutines & Flow (already used)

---

## 10. Success Criteria

### Minimum Viable Product (MVP)

✅ User can create 3 screens with up to 3 fields each  
✅ User can select from available Karoo metrics  
✅ **Default profile (Speed/Distance/Time) always available**  
✅ **Icons display correctly with metrics (28×28 and 40×40)**  
✅ Layouts are sent to glasses and displayed correctly  
✅ Real-time data updates work during rides  
✅ **Multiple profiles can be created and switched**  
✅ **Profiles persist across app restarts**

### Full Feature Set

✅ Preview shows accurate layout representation  
✅ All available Karoo metrics are mappable  
✅ Font sizes are configurable  
✅ Labels, units, and icons are toggleable  
✅ **Default profile is protected (read-only)**  
✅ **Template profiles available (Road Bike, Gravel Bike)**  
✅ **Profile duplication works correctly**  
✅ **Icons align properly with text**  
✅ **Automatic profile switching based on Karoo profile**  
✅ **Auto-switch can be toggled on/off**  
✅ **Case-insensitive profile name matching**  
✅ Error handling is robust  
✅ UI is intuitive and responsive

### Key Features

✅ **Default Datafields**: Speed, Distance, Time (always available)  
✅ **Multiple Configurations**: Different profiles for different bikes  
✅ **Read-only Protection**: Default profile cannot be edited/deleted  
✅ **Icon Support**: Pre-loaded ActiveLook icons in two sizes  
✅ **Profile Templates**: Quick-start options for common setups  
✅ **Profile Management**: Create, duplicate, switch, delete  
✅ **Automatic Profile Switching**: K2Look profiles auto-match Karoo profiles by name

---

## 11. Future Enhancements

1. **Advanced Layouts**
    - Drag-and-drop positioning
    - Custom sizes and colors
    - Multi-line text fields
    - Graphical widgets (gauges, charts)

2. **Template Gallery**
    - Pre-built layouts for different sports
    - Community-shared configurations
    - One-tap import

3. **Conditional Display**
    - Show/hide fields based on data availability
    - Auto-switch screens based on ride state
    - Alerts and notifications

4. **Visual Customization**
    - Custom fonts (upload to glasses)
    - Background images
    - Color themes (when supported by glasses)
    - Icons and symbols

---

## 12. Risk Analysis & Mitigation

| Risk                       | Probability | Impact | Mitigation                                       |
|----------------------------|-------------|--------|--------------------------------------------------|
| ActiveLook API complexity  | High        | High   | Thorough protocol testing, use SDK when possible |
| Layout encoding errors     | Medium      | High   | Extensive unit tests, byte-level verification    |
| Glasses memory limits      | Medium      | Medium | Monitor cfgFreeSpace, implement cleanup          |
| BLE communication failures | Low         | Medium | Retry logic, flow control handling               |
| UI complexity              | Medium      | Low    | Iterative design, user testing                   |

---

## Summary

This implementation plan provides a complete roadmap for building a DataField Builder tab that
allows Karoo 2 users to create custom layouts for their ActiveLook glasses. The design follows the
ActiveLook protocol specifications, leverages existing Karoo data streams, and provides an intuitive
UI for layout creation.

**Key Advantages**:

- ✅ **Default datafields always available** (Speed, Distance, Time - read-only)
- ✅ **Multiple configuration profiles** (different setups for different bikes)
- ✅ **Automatic profile switching** - K2Look profiles auto-match Karoo profiles by name!
- ✅ **80+ pre-loaded icons** in glasses (no upload needed!)
- ✅ **5 custom fonts** included (24px to 82px)
- ✅ **Profile templates** (Road Bike with power, Gravel Bike without)
- ✅ **Binary protocol** for efficient BLE communication
- ✅ **Real-time updates** at 1Hz during rides
- ✅ **Configuration persistence** (save/load profiles)
- ✅ **Icon support** in two sizes (28×28 and 40×40)
- ✅ **Protected default profile** (cannot be edited/deleted)

**Seamless User Experience**:

When you switch profiles on your Karoo launcher (e.g., "Road Bike" → "Gravel"), K2Look automatically
switches to the matching datafield profile on your glasses. No manual intervention needed!

**Example Use Cases**:

1. **Bike 1** (Road with Power Meter): Speed, Power, Heart Rate
2. **Bike 2** (Gravel no Power): Speed, Heart Rate, Distance
3. **Training Mode**: HR Zones, Power 3s, Cadence
4. **Touring Mode**: Speed, Distance, Time (default profile)

**Estimated Timeline**: 3-4 weeks for MVP, 4-6 weeks for full feature set.

**Next Steps**:

1. Review and approve plan
2. Start Phase 1 implementation (data models + default profiles)
3. Create prototype for early testing
4. Iterate based on feedback

---

## Documentation References

### Primary Documents

- **[DataField Builder Implementation Plan](DataField-Builder-Implementation-Plan.md)** - This
  document (complete implementation guide)
- **[ActiveLook Icon Reference](ActiveLook-Icon-Reference.md)** - Quick icon lookup & usage examples
- **[ActiveLook API Documentation](Activelook-API-Documentation/ActiveLook_API.md)** - Full BLE
  protocol specification
- **[ActiveLook Visual Assets](Activelook-Visual-Assets/README.md)** - Complete icon/font/layout
  catalog
- **[Garmin Datafields](Garmin%20Datafields.md)** - Metric ID reference & mapping guide

### Related Project Documents

- `INSTALLATION.md` - K2Look installation guide for users
- `README.md` - Project overview and features
- `Karoo2-Hardware-Profile.md` - Device specifications & capabilities
- `Quick-Start-Testing-Guide.md` - Testing procedures

### External Resources

- [ActiveLook Visual Assets Repository](https://github.com/ActiveLook/Activelook-Visual-Assets) -
  Icon source files
- [Karoo SDK Documentation](https://github.com/hammerheadnav/karoo-ext) - Extension framework
  reference
- [ActiveLook SDK for Android](https://github.com/ActiveLook/android-sdk) - BLE communication
  library


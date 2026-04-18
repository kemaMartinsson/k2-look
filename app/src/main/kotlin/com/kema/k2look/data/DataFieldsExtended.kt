package com.kema.k2look.data

import com.kema.k2look.model.DataField
import com.kema.k2look.model.DataFieldCategory.EBIKE
import com.kema.k2look.model.DataFieldCategory.ELEVATION
import com.kema.k2look.model.DataFieldCategory.LAP
import com.kema.k2look.model.DataFieldCategory.LAST_LAP
import com.kema.k2look.model.DataFieldCategory.NAVIGATION
import com.kema.k2look.model.DataFieldCategory.RADAR
import com.kema.k2look.model.DataFieldCategory.SHIFTING
import io.hammerhead.karooext.models.DataType

/** Extended metrics: Radar, Elevation, Lap, Last Lap, Shifting, Navigation, eBike */
internal fun extendedDataFields(): List<DataField> =
        listOf(
                // ── Radar (requires a compatible rear radar, e.g. Garmin Varia) ──
                DataField(
                        id = 50,
                        name = "Radar Threat",
                        unit = "",
                        category = RADAR,
                        karooStreamType = DataType.Type.RADAR
                ),
                DataField(
                        id = 51,
                        name = "Radar Targets",
                        unit = "",
                        category = RADAR,
                        karooStreamType = DataType.Type.RADAR
                ),
                DataField(
                        id = 52,
                        name = "Radar Range",
                        unit = "m",
                        category = RADAR,
                        karooStreamType = DataType.Type.RADAR
                ),

                // ── Elevation ──
                DataField(
                        id = 72,
                        name = "Grade",
                        unit = "%",
                        category = ELEVATION,
                        karooStreamType = DataType.Type.ELEVATION_GRADE
                ),
                DataField(
                        id = 73,
                        name = "Ascent",
                        unit = "m",
                        category = ELEVATION,
                        karooStreamType = DataType.Type.ELEVATION_GAIN,
                        icon28 = 30,
                        icon40 = 62
                ),
                DataField(
                        id = 74,
                        name = "Descent",
                        unit = "m",
                        category = ELEVATION,
                        karooStreamType = DataType.Type.ELEVATION_LOSS,
                        icon28 = 31,
                        icon40 = 63
                ),
                DataField(
                        id = 75,
                        name = "Altitude",
                        unit = "m",
                        category = ELEVATION,
                        karooStreamType = DataType.Type.PRESSURE_ELEVATION_CORRECTION,
                        icon28 = 2,
                        icon40 = 34
                ),
                DataField(
                        id = 76,
                        name = "VAM 30s",
                        unit = "m/h",
                        category = ELEVATION,
                        karooStreamType = DataType.Type.AVERAGE_VERTICAL_SPEED_30S,
                        icon28 = 29,
                        icon40 = 61
                ),

                // ── Lap ──
                DataField(
                        id = 77,
                        name = "Lap #",
                        unit = "",
                        category = LAP,
                        karooStreamType = DataType.Type.LAP_NUMBER
                ),
                DataField(
                        id = 78,
                        name = "Lap Time",
                        unit = "MM:SS",
                        category = LAP,
                        karooStreamType = DataType.Type.ELAPSED_TIME_LAP,
                        icon28 = 8,
                        icon40 = 40
                ),
                DataField(
                        id = 79,
                        name = "Lap Dist",
                        unit = "km",
                        category = LAP,
                        karooStreamType = DataType.Type.DISTANCE_LAP,
                        icon28 = 9,
                        icon40 = 41
                ),
                DataField(
                        id = 80,
                        name = "Lap Speed",
                        unit = "km/h",
                        category = LAP,
                        karooStreamType = DataType.Type.AVERAGE_SPEED_LAP,
                        icon28 = 26,
                        icon40 = 58
                ),
                DataField(
                        id = 81,
                        name = "Lap HR",
                        unit = "bpm",
                        category = LAP,
                        karooStreamType = DataType.Type.AVERAGE_LAP_HR,
                        icon28 = 12,
                        icon40 = 44
                ),
                DataField(
                        id = 82,
                        name = "Lap Power",
                        unit = "w",
                        category = LAP,
                        karooStreamType = DataType.Type.POWER_LAP,
                        icon28 = 19,
                        icon40 = 51
                ),
                DataField(
                        id = 83,
                        name = "Lap NP",
                        unit = "w",
                        category = LAP,
                        karooStreamType = DataType.Type.NORMALIZED_POWER_LAP,
                        icon28 = 15,
                        icon40 = 47
                ),
                DataField(
                        id = 84,
                        name = "Lap Cadence",
                        unit = "rpm",
                        category = LAP,
                        karooStreamType = DataType.Type.CADENCE_LAP,
                        icon28 = 4,
                        icon40 = 36
                ),
                DataField(
                        id = 85,
                        name = "Lap Ascent",
                        unit = "m",
                        category = LAP,
                        karooStreamType = DataType.Type.ELEVATION_GAIN_LAP,
                        icon28 = 30,
                        icon40 = 62
                ),

                // ── Last Lap ──
                DataField(
                        id = 86,
                        name = "L.Lap Time",
                        unit = "MM:SS",
                        category = LAST_LAP,
                        karooStreamType = DataType.Type.ELAPSED_TIME_LAST_LAP,
                        icon28 = 8,
                        icon40 = 40
                ),
                DataField(
                        id = 87,
                        name = "L.Lap Dist",
                        unit = "km",
                        category = LAST_LAP,
                        karooStreamType = DataType.Type.DISTANCE_LAP_LAST_LAP,
                        icon28 = 9,
                        icon40 = 41
                ),
                DataField(
                        id = 88,
                        name = "L.Lap Speed",
                        unit = "km/h",
                        category = LAST_LAP,
                        karooStreamType = DataType.Type.AVERAGE_SPEED_LAST_LAP,
                        icon28 = 26,
                        icon40 = 58
                ),
                DataField(
                        id = 89,
                        name = "L.Lap HR",
                        unit = "bpm",
                        category = LAST_LAP,
                        karooStreamType = DataType.Type.AVERAGE_HR_LAST_LAP,
                        icon28 = 12,
                        icon40 = 44
                ),
                DataField(
                        id = 90,
                        name = "L.Lap Power",
                        unit = "w",
                        category = LAST_LAP,
                        karooStreamType = DataType.Type.AVERAGE_POWER_LAST_LAP,
                        icon28 = 19,
                        icon40 = 51
                ),
                DataField(
                        id = 91,
                        name = "L.Lap NP",
                        unit = "w",
                        category = LAST_LAP,
                        karooStreamType = DataType.Type.NORMALIZED_POWER_LAST_LAP,
                        icon28 = 15,
                        icon40 = 47
                ),

                // ── Shifting (requires electronic groupset e.g. Di2, AXS, eTap) ──
                DataField(
                        id = 92,
                        name = "Front Gear",
                        unit = "",
                        category = SHIFTING,
                        karooStreamType = DataType.Type.SHIFTING_FRONT_GEAR
                ),
                DataField(
                        id = 93,
                        name = "Rear Gear",
                        unit = "",
                        category = SHIFTING,
                        karooStreamType = DataType.Type.SHIFTING_REAR_GEAR
                ),
                DataField(
                        id = 94,
                        name = "Drive Battery",
                        unit = "",
                        category = SHIFTING,
                        karooStreamType = DataType.Type.SHIFTING_BATTERY
                ),
                DataField(
                        id = 95,
                        name = "Shift Count",
                        unit = "",
                        category = SHIFTING,
                        karooStreamType = DataType.Type.SHIFTING_COUNT
                ),

                // ── Navigation (requires active route) ──
                DataField(
                        id = 96,
                        name = "To Turn",
                        unit = "m",
                        category = NAVIGATION,
                        karooStreamType = DataType.Type.DISTANCE_TO_NEXT_TURN,
                        icon28 = 10,
                        icon40 = 42
                ),
                DataField(
                        id = 97,
                        name = "To Finish",
                        unit = "km",
                        category = NAVIGATION,
                        karooStreamType = DataType.Type.DISTANCE_TO_DESTINATION,
                        icon28 = 10,
                        icon40 = 42
                ),
                DataField(
                        id = 98,
                        name = "ETA",
                        unit = "",
                        category = NAVIGATION,
                        karooStreamType = DataType.Type.TIME_OF_ARRIVAL
                ),
                DataField(
                        id = 99,
                        name = "Time to End",
                        unit = "",
                        category = NAVIGATION,
                        karooStreamType = DataType.Type.TIME_TO_DESTINATION
                ),
                DataField(
                        id = 100,
                        name = "Heading",
                        unit = "",
                        category = NAVIGATION,
                        karooStreamType = DataType.Type.HEADING
                ),

                // ── eBike (requires LEV/eBike sensor) ──
                DataField(
                        id = 101,
                        name = "Bike Battery",
                        unit = "%",
                        category = EBIKE,
                        karooStreamType = DataType.Type.LEV_BATTERY_STATUS
                ),
                DataField(
                        id = 102,
                        name = "Est. Range",
                        unit = "km",
                        category = EBIKE,
                        karooStreamType = DataType.Type.LEV_ESTIMATED_RANGE,
                        icon28 = 9,
                        icon40 = 41
                ),
                DataField(
                        id = 103,
                        name = "Assist Mode",
                        unit = "",
                        category = EBIKE,
                        karooStreamType = DataType.Type.LEV_ASSIST_MODE
                ),
                DataField(
                        id = 104,
                        name = "Motor Power",
                        unit = "w",
                        category = EBIKE,
                        karooStreamType = DataType.Type.LEV_MOTOR_POWER,
                        icon28 = 19,
                        icon40 = 51
                ),
        )

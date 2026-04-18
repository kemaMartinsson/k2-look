package com.kema.k2look.data

import com.kema.k2look.model.DataField
import com.kema.k2look.model.DataFieldCategory
import com.kema.k2look.model.DataFieldCategory.CADENCE
import com.kema.k2look.model.DataFieldCategory.CLIMBING
import com.kema.k2look.model.DataFieldCategory.EBIKE
import com.kema.k2look.model.DataFieldCategory.ELEVATION
import com.kema.k2look.model.DataFieldCategory.ENERGY
import com.kema.k2look.model.DataFieldCategory.GENERAL
import com.kema.k2look.model.DataFieldCategory.HEART_RATE
import com.kema.k2look.model.DataFieldCategory.LAP
import com.kema.k2look.model.DataFieldCategory.LAST_LAP
import com.kema.k2look.model.DataFieldCategory.NAVIGATION
import com.kema.k2look.model.DataFieldCategory.POWER
import com.kema.k2look.model.DataFieldCategory.RADAR
import com.kema.k2look.model.DataFieldCategory.SHIFTING
import com.kema.k2look.model.DataFieldCategory.SPEED_PACE
import com.kema.k2look.model.IconSize
import io.hammerhead.karooext.models.DataType

/** Registry of all available data fields that can be displayed on glasses */
object DataFieldRegistry {

        val ALL_FIELDS =
                listOf(
                        // General
                        DataField(
                                id = 1,
                                name = "Elapsed Time",
                                unit = "HH:MM:SS",
                                category = GENERAL,
                                karooStreamType = DataType.Type.ELAPSED_TIME,
                                icon28 = 8,
                                icon40 = 40
                        ),
                        DataField(
                                id = 2,
                                name = "Distance",
                                unit = "km",
                                category = GENERAL,
                                karooStreamType = DataType.Type.DISTANCE,
                                icon28 = 9,
                                icon40 = 41
                        ),

                        // Heart Rate
                        DataField(
                                id = 4,
                                name = "Heart Rate",
                                unit = "bpm",
                                category = HEART_RATE,
                                karooStreamType = DataType.Type.HEART_RATE,
                                icon28 = 12,
                                icon40 = 44
                        ),
                        DataField(
                                id = 5,
                                name = "Max Heart Rate",
                                unit = "bpm",
                                category = HEART_RATE,
                                karooStreamType = DataType.Type.MAX_HR,
                                icon28 = 14,
                                icon40 = 46
                        ),
                        DataField(
                                id = 6,
                                name = "Avg Heart Rate",
                                unit = "bpm",
                                category = HEART_RATE,
                                karooStreamType = DataType.Type.AVERAGE_HR,
                                icon28 = 13,
                                icon40 = 45
                        ),
                        DataField(
                                id = 47,
                                name = "HR Zone",
                                unit = "Z",
                                category = HEART_RATE,
                                karooStreamType = DataType.Type.HR_ZONE
                        ),

                        // Power
                        DataField(
                                id = 7,
                                name = "Power",
                                unit = "w",
                                category = POWER,
                                karooStreamType = DataType.Type.POWER,
                                icon28 = 19,
                                icon40 = 51
                        ),
                        DataField(
                                id = 8,
                                name = "Max Power",
                                unit = "w",
                                category = POWER,
                                karooStreamType = DataType.Type.MAX_POWER,
                                icon28 = 22,
                                icon40 = 54
                        ),
                        DataField(
                                id = 9,
                                name = "Avg Power",
                                unit = "w",
                                category = POWER,
                                karooStreamType = DataType.Type.AVERAGE_POWER,
                                icon28 = 21,
                                icon40 = 53
                        ),
                        DataField(
                                id = 10,
                                name = "Power 3s",
                                unit = "w",
                                category = POWER,
                                karooStreamType = DataType.Type.SMOOTHED_3S_AVERAGE_POWER,
                                icon28 = 20,
                                icon40 = 52
                        ),
                        DataField(
                                id = 48,
                                name = "Power Zone",
                                unit = "Z",
                                category = POWER,
                                karooStreamType = DataType.Type.POWER_ZONE
                        ),

                        // Speed
                        DataField(
                                id = 12,
                                name = "Speed",
                                unit = "km/h",
                                category = SPEED_PACE,
                                karooStreamType = DataType.Type.SPEED,
                                icon28 = 26,
                                icon40 = 58
                        ),
                        DataField(
                                id = 13,
                                name = "Max Speed",
                                unit = "km/h",
                                category = SPEED_PACE,
                                karooStreamType = DataType.Type.MAX_SPEED,
                                icon28 = 28,
                                icon40 = 60
                        ),
                        DataField(
                                id = 14,
                                name = "Avg Speed",
                                unit = "km/h",
                                category = SPEED_PACE,
                                karooStreamType = DataType.Type.AVERAGE_SPEED,
                                icon28 = 27,
                                icon40 = 59
                        ),

                        // Cadence (cycling)
                        DataField(
                                id = 18,
                                name = "Cadence",
                                unit = "rpm",
                                category = CADENCE,
                                karooStreamType = DataType.Type.CADENCE,
                                icon28 = 4,
                                icon40 = 36
                        ),
                        DataField(
                                id = 19,
                                name = "Max Cadence",
                                unit = "rpm",
                                category = CADENCE,
                                karooStreamType = DataType.Type.MAX_CADENCE,
                                icon28 = 6,
                                icon40 = 38
                        ),
                        DataField(
                                id = 20,
                                name = "Avg Cadence",
                                unit = "rpm",
                                category = CADENCE,
                                karooStreamType = DataType.Type.AVERAGE_CADENCE,
                                icon28 = 5,
                                icon40 = 37
                        ),

                        // Climbing
                        DataField(
                                id = 24,
                                name = "VAM",
                                unit = "m/h",
                                category = CLIMBING,
                                karooStreamType = DataType.Type.VERTICAL_SPEED,
                                icon28 =
                                        29, // 29=speed-ascension (rate of ascent — correct for VAM)
                                icon40 = 61
                        ),
                        DataField(
                                id = 25,
                                name = "Avg VAM",
                                unit = "m/h",
                                category = CLIMBING,
                                karooStreamType = DataType.Type.AVERAGE_VERTICAL_SPEED,
                                icon28 = 29, // 29=speed-ascension (same; no avg-specific icon
                                // exists)
                                icon40 = 61
                        ),

                        // Radar (requires a compatible rear radar, e.g. Garmin Varia)
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

                        // General additions
                        DataField(
                                id = 53,
                                name = "Clock",
                                unit = "HH:MM",
                                category = GENERAL,
                                karooStreamType = DataType.Type.CLOCK_TIME,
                                icon28 = 8,
                                icon40 = 40
                        ), // 8=chrono_28x28, 40=chrono_40x40
                        DataField(
                                id = 54,
                                name = "Temperature",
                                unit = "°C",
                                category = GENERAL,
                                karooStreamType = DataType.Type.TEMPERATURE
                        ),
                        DataField(
                                id = 55,
                                name = "Karoo Battery",
                                unit = "%",
                                category = GENERAL,
                                karooStreamType = DataType.Type.BATTERY_PERCENT
                        ),
                        DataField(
                                id = 56,
                                name = "Ride Time",
                                unit = "HH:MM:SS",
                                category = GENERAL,
                                karooStreamType = DataType.Type.RIDE_TIME,
                                icon28 = 8,
                                icon40 = 40
                        ), // 8=chrono_28x28, 40=chrono_40x40

                        // Heart Rate additions
                        DataField(
                                id = 57,
                                name = "% Max HR",
                                unit = "%",
                                category = HEART_RATE,
                                karooStreamType = DataType.Type.PERCENT_MAX_HR
                        ),
                        DataField(
                                id = 58,
                                name = "% HR Reserve",
                                unit = "%",
                                category = HEART_RATE,
                                karooStreamType = DataType.Type.PERCENT_HRR
                        ),

                        // Power additions
                        DataField(
                                id = 59,
                                name = "Power 5s",
                                unit = "w",
                                category = POWER,
                                karooStreamType = DataType.Type.SMOOTHED_5S_AVERAGE_POWER,
                                icon28 = 21,
                                icon40 = 53
                        ), // 21=power-avg_28x28 (smoothed = averaged power)
                        DataField(
                                id = 60,
                                name = "Power 10s",
                                unit = "w",
                                category = POWER,
                                karooStreamType = DataType.Type.SMOOTHED_10S_AVERAGE_POWER,
                                icon28 = 21,
                                icon40 = 53
                        ), // 21=power-avg_28x28
                        DataField(
                                id = 61,
                                name = "Power 30s",
                                unit = "w",
                                category = POWER,
                                karooStreamType = DataType.Type.SMOOTHED_30S_AVERAGE_POWER,
                                icon28 = 21,
                                icon40 = 53
                        ), // 21=power-avg_28x28
                        DataField(
                                id = 62,
                                name = "Norm. Power",
                                unit = "w",
                                category = POWER,
                                karooStreamType = DataType.Type.NORMALIZED_POWER,
                                icon28 = 15,
                                icon40 = 47
                        ), // 15=normalized-power_28x28, 47=normalized-power_40x40
                        DataField(
                                id = 63,
                                name = "% FTP",
                                unit = "%",
                                category = POWER,
                                karooStreamType = DataType.Type.PERCENT_MAX_FTP
                        ),
                        DataField(
                                id = 64,
                                name = "Int. Factor",
                                unit = "",
                                category = POWER,
                                karooStreamType = DataType.Type.INTENSITY_FACTOR
                        ),
                        DataField(
                                id = 65,
                                name = "TSS",
                                unit = "",
                                category = POWER,
                                karooStreamType = DataType.Type.TRAINING_STRESS_SCORE
                        ),
                        DataField(
                                id = 66,
                                name = "W/kg",
                                unit = "w/kg",
                                category = POWER,
                                karooStreamType = DataType.Type.POWER_TO_WEIGHT,
                                icon28 = 19,
                                icon40 = 51
                        ), // 19=power_28x28

                        // Energy (fill existing empty category)
                        DataField(
                                id = 67,
                                name = "Energy",
                                unit = "kJ",
                                category = ENERGY,
                                karooStreamType = DataType.Type.ENERGY_OUTPUT,
                                icon28 = 11,
                                icon40 = 43
                        ), // 11=energy-expenditure_28x28, 43=energy-expenditure_40x40
                        DataField(
                                id = 68,
                                name = "Calories",
                                unit = "kcal",
                                category = ENERGY,
                                karooStreamType = DataType.Type.CALORIES,
                                icon28 = 7,
                                icon40 = 39
                        ), // 7=calories-burned_28x28, 39=calories-burned_40x40
                        DataField(
                                id = 69,
                                name = "Cal/hr",
                                unit = "kcal/h",
                                category = ENERGY,
                                karooStreamType = DataType.Type.CALORIES_PER_HOUR,
                                icon28 = 7,
                                icon40 = 39
                        ), // 7=calories-burned_28x28

                        // Speed additions
                        DataField(
                                id = 70,
                                name = "Speed 3s",
                                unit = "km/h",
                                category = SPEED_PACE,
                                karooStreamType = DataType.Type.SMOOTHED_3S_AVERAGE_SPEED,
                                icon28 = 27,
                                icon40 = 59
                        ), // 27=speed-avg_28x28 (3s smoothed = averaged speed)

                        // Cadence additions
                        DataField(
                                id = 71,
                                name = "Cadence 3s",
                                unit = "rpm",
                                category = CADENCE,
                                karooStreamType = DataType.Type.SMOOTHED_3S_AVERAGE_CADENCE,
                                icon28 = 5,
                                icon40 = 37
                        ), // 5=cadence-avg_28x28 (3s smoothed = averaged cadence)

                        // Elevation (fill existing empty category)
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
                        ), // 30=total-ascent_28x28, 62=total-ascent_40x40
                        DataField(
                                id = 74,
                                name = "Descent",
                                unit = "m",
                                category = ELEVATION,
                                karooStreamType = DataType.Type.ELEVATION_LOSS,
                                icon28 = 31,
                                icon40 = 63
                        ), // 31=total-descent_28x28, 63=total-descent_40x40
                        DataField(
                                id = 75,
                                name = "Altitude",
                                unit = "m",
                                category = ELEVATION,
                                karooStreamType = DataType.Type.PRESSURE_ELEVATION_CORRECTION,
                                icon28 = 2,
                                icon40 = 34
                        ), // 2=altitude_28x28, 34=altitude_40x40
                        DataField(
                                id = 76,
                                name = "VAM 30s",
                                unit = "m/h",
                                category = ELEVATION,
                                karooStreamType = DataType.Type.AVERAGE_VERTICAL_SPEED_30S,
                                icon28 = 29,
                                icon40 = 61
                        ), // 29=speed-ascension_28x28 (same as VAM)

                        // Lap (new category)
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
                        ), // 8=chrono_28x28
                        DataField(
                                id = 79,
                                name = "Lap Dist",
                                unit = "km",
                                category = LAP,
                                karooStreamType = DataType.Type.DISTANCE_LAP,
                                icon28 = 9,
                                icon40 = 41
                        ), // 9=distance_28x28
                        DataField(
                                id = 80,
                                name = "Lap Speed",
                                unit = "km/h",
                                category = LAP,
                                karooStreamType = DataType.Type.AVERAGE_SPEED_LAP,
                                icon28 = 26,
                                icon40 = 58
                        ), // 26=speed_28x28
                        DataField(
                                id = 81,
                                name = "Lap HR",
                                unit = "bpm",
                                category = LAP,
                                karooStreamType = DataType.Type.AVERAGE_LAP_HR,
                                icon28 = 12,
                                icon40 = 44
                        ), // 12=heart-beat_28x28
                        DataField(
                                id = 82,
                                name = "Lap Power",
                                unit = "w",
                                category = LAP,
                                karooStreamType = DataType.Type.POWER_LAP,
                                icon28 = 19,
                                icon40 = 51
                        ), // 19=power_28x28
                        DataField(
                                id = 83,
                                name = "Lap NP",
                                unit = "w",
                                category = LAP,
                                karooStreamType = DataType.Type.NORMALIZED_POWER_LAP,
                                icon28 = 15,
                                icon40 = 47
                        ), // 15=normalized-power_28x28
                        DataField(
                                id = 84,
                                name = "Lap Cadence",
                                unit = "rpm",
                                category = LAP,
                                karooStreamType = DataType.Type.CADENCE_LAP,
                                icon28 = 4,
                                icon40 = 36
                        ), // 4=cadence_28x28
                        DataField(
                                id = 85,
                                name = "Lap Ascent",
                                unit = "m",
                                category = LAP,
                                karooStreamType = DataType.Type.ELEVATION_GAIN_LAP,
                                icon28 = 30,
                                icon40 = 62
                        ), // 30=total-ascent_28x28

                        // Last Lap (new category)
                        DataField(
                                id = 86,
                                name = "L.Lap Time",
                                unit = "MM:SS",
                                category = LAST_LAP,
                                karooStreamType = DataType.Type.ELAPSED_TIME_LAST_LAP,
                                icon28 = 8,
                                icon40 = 40
                        ), // 8=chrono_28x28
                        DataField(
                                id = 87,
                                name = "L.Lap Dist",
                                unit = "km",
                                category = LAST_LAP,
                                karooStreamType = DataType.Type.DISTANCE_LAP_LAST_LAP,
                                icon28 = 9,
                                icon40 = 41
                        ), // 9=distance_28x28
                        DataField(
                                id = 88,
                                name = "L.Lap Speed",
                                unit = "km/h",
                                category = LAST_LAP,
                                karooStreamType = DataType.Type.AVERAGE_SPEED_LAST_LAP,
                                icon28 = 26,
                                icon40 = 58
                        ), // 26=speed_28x28
                        DataField(
                                id = 89,
                                name = "L.Lap HR",
                                unit = "bpm",
                                category = LAST_LAP,
                                karooStreamType = DataType.Type.AVERAGE_HR_LAST_LAP,
                                icon28 = 12,
                                icon40 = 44
                        ), // 12=heart-beat_28x28
                        DataField(
                                id = 90,
                                name = "L.Lap Power",
                                unit = "w",
                                category = LAST_LAP,
                                karooStreamType = DataType.Type.AVERAGE_POWER_LAST_LAP,
                                icon28 = 19,
                                icon40 = 51
                        ), // 19=power_28x28
                        DataField(
                                id = 91,
                                name = "L.Lap NP",
                                unit = "w",
                                category = LAST_LAP,
                                karooStreamType = DataType.Type.NORMALIZED_POWER_LAST_LAP,
                                icon28 = 15,
                                icon40 = 47
                        ), // 15=normalized-power_28x28

                        // Shifting (new category — requires electronic groupset e.g. Di2, AXS,
                        // eTap)
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

                        // Navigation (new category — requires active route)
                        DataField(
                                id = 96,
                                name = "To Turn",
                                unit = "m",
                                category = NAVIGATION,
                                karooStreamType = DataType.Type.DISTANCE_TO_NEXT_TURN,
                                icon28 = 10,
                                icon40 = 42
                        ), // 10=distance-to-destination_28x28
                        DataField(
                                id = 97,
                                name = "To Finish",
                                unit = "km",
                                category = NAVIGATION,
                                karooStreamType = DataType.Type.DISTANCE_TO_DESTINATION,
                                icon28 = 10,
                                icon40 = 42
                        ), // 10=distance-to-destination_28x28
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

                        // eBike (new category — requires LEV/eBike sensor)
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
                        ), // 9=distance_28x28
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
                        ) // 19=power_28x28
                )

        /** Get data fields by category */
        fun getByCategory(category: DataFieldCategory): List<DataField> {
                return ALL_FIELDS.filter { it.category == category }
        }

        /** Get data field by ID */
        fun getById(id: Int): DataField? {
                return ALL_FIELDS.find { it.id == id }
        }

        /** Get icon ID for a data field based on size */
        fun getIconId(dataField: DataField, size: IconSize): Int? {
                return when (size) {
                        IconSize.SMALL -> dataField.icon28
                        IconSize.LARGE -> dataField.icon40
                }
        }
}

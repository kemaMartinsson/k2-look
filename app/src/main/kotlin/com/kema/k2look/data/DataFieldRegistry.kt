package com.kema.k2look.data

import com.kema.k2look.model.DataField
import com.kema.k2look.model.DataFieldCategory
import com.kema.k2look.model.DataFieldCategory.CADENCE
import com.kema.k2look.model.DataFieldCategory.CLIMBING
import com.kema.k2look.model.DataFieldCategory.GENERAL
import com.kema.k2look.model.DataFieldCategory.HEART_RATE
import com.kema.k2look.model.DataFieldCategory.POWER
import com.kema.k2look.model.DataFieldCategory.SPEED_PACE
import com.kema.k2look.model.IconSize
import io.hammerhead.karooext.models.DataType

/**
 * Registry of all available data fields that can be displayed on glasses
 */
object DataFieldRegistry {

    val ALL_FIELDS = listOf(
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
            icon28 = 30,
            icon40 = 62
        ),
        DataField(
            id = 25,
            name = "Avg VAM",
            unit = "m/h",
            category = CLIMBING,
            karooStreamType = DataType.Type.AVERAGE_VERTICAL_SPEED,
            icon28 = 29,
            icon40 = 61
        )
    )

    /**
     * Get data fields by category
     */
    fun getByCategory(category: DataFieldCategory): List<DataField> {
        return ALL_FIELDS.filter { it.category == category }
    }

    /**
     * Get data field by ID
     */
    fun getById(id: Int): DataField? {
        return ALL_FIELDS.find { it.id == id }
    }

    /**
     * Get icon ID for a data field based on size
     */
    fun getIconId(dataField: DataField, size: IconSize): Int? {
        return when (size) {
            IconSize.SMALL -> dataField.icon28
            IconSize.LARGE -> dataField.icon40
        }
    }
}



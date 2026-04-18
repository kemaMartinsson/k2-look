package com.kema.k2look.data

import com.kema.k2look.model.DataField
import com.kema.k2look.model.DataFieldCategory.ENERGY
import com.kema.k2look.model.DataFieldCategory.POWER
import io.hammerhead.karooext.models.DataType

/** Power and energy metrics */
internal fun powerEnergyFields(): List<DataField> =
        listOf(
                // ── Power ──
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
                DataField(
                        id = 59,
                        name = "Power 5s",
                        unit = "w",
                        category = POWER,
                        karooStreamType = DataType.Type.SMOOTHED_5S_AVERAGE_POWER,
                        icon28 = 21,
                        icon40 = 53
                ),
                DataField(
                        id = 60,
                        name = "Power 10s",
                        unit = "w",
                        category = POWER,
                        karooStreamType = DataType.Type.SMOOTHED_10S_AVERAGE_POWER,
                        icon28 = 21,
                        icon40 = 53
                ),
                DataField(
                        id = 61,
                        name = "Power 30s",
                        unit = "w",
                        category = POWER,
                        karooStreamType = DataType.Type.SMOOTHED_30S_AVERAGE_POWER,
                        icon28 = 21,
                        icon40 = 53
                ),
                DataField(
                        id = 62,
                        name = "Norm. Power",
                        unit = "w",
                        category = POWER,
                        karooStreamType = DataType.Type.NORMALIZED_POWER,
                        icon28 = 15,
                        icon40 = 47
                ),
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
                ),

                // ── Energy ──
                DataField(
                        id = 67,
                        name = "Energy",
                        unit = "kJ",
                        category = ENERGY,
                        karooStreamType = DataType.Type.ENERGY_OUTPUT,
                        icon28 = 11,
                        icon40 = 43
                ),
                DataField(
                        id = 68,
                        name = "Calories",
                        unit = "kcal",
                        category = ENERGY,
                        karooStreamType = DataType.Type.CALORIES,
                        icon28 = 7,
                        icon40 = 39
                ),
                DataField(
                        id = 69,
                        name = "Cal/hr",
                        unit = "kcal/h",
                        category = ENERGY,
                        karooStreamType = DataType.Type.CALORIES_PER_HOUR,
                        icon28 = 7,
                        icon40 = 39
                ),
        )

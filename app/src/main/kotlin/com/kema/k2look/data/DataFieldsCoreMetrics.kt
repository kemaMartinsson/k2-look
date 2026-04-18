package com.kema.k2look.data

import com.kema.k2look.model.DataField
import com.kema.k2look.model.DataFieldCategory.CADENCE
import com.kema.k2look.model.DataFieldCategory.CLIMBING
import com.kema.k2look.model.DataFieldCategory.GENERAL
import com.kema.k2look.model.DataFieldCategory.HEART_RATE
import com.kema.k2look.model.DataFieldCategory.SPEED_PACE
import io.hammerhead.karooext.models.DataType

/** Core cycling metrics: General, Heart Rate, Speed/Pace, Cadence, Climbing */
internal fun coreCyclingFields(): List<DataField> =
        listOf(
                // ── General ──
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
                DataField(
                        id = 53,
                        name = "Clock",
                        unit = "HH:MM",
                        category = GENERAL,
                        karooStreamType = DataType.Type.CLOCK_TIME,
                        icon28 = 8,
                        icon40 = 40
                ),
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
                ),

                // ── Heart Rate ──
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

                // ── Speed / Pace ──
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
                DataField(
                        id = 70,
                        name = "Speed 3s",
                        unit = "km/h",
                        category = SPEED_PACE,
                        karooStreamType = DataType.Type.SMOOTHED_3S_AVERAGE_SPEED,
                        icon28 = 27,
                        icon40 = 59
                ),

                // ── Cadence ──
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
                DataField(
                        id = 71,
                        name = "Cadence 3s",
                        unit = "rpm",
                        category = CADENCE,
                        karooStreamType = DataType.Type.SMOOTHED_3S_AVERAGE_CADENCE,
                        icon28 = 5,
                        icon40 = 37
                ),

                // ── Climbing ──
                DataField(
                        id = 24,
                        name = "VAM",
                        unit = "m/h",
                        category = CLIMBING,
                        karooStreamType = DataType.Type.VERTICAL_SPEED,
                        icon28 = 29,
                        icon40 = 61
                ),
                DataField(
                        id = 25,
                        name = "Avg VAM",
                        unit = "m/h",
                        category = CLIMBING,
                        karooStreamType = DataType.Type.AVERAGE_VERTICAL_SPEED,
                        icon28 = 29,
                        icon40 = 61
                ),
        )

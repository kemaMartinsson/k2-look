package com.kema.k2look.data

import com.kema.k2look.model.DataField
import com.kema.k2look.model.DataFieldCategory
import com.kema.k2look.model.IconSize

/**
 * Registry of all available data fields that can be displayed on glasses.
 *
 * Field definitions are split by domain:
 * - [coreCyclingFields] — General, Heart Rate, Speed/Pace, Cadence, Climbing
 * - [powerEnergyFields] — Power, Energy
 * - [extendedDataFields] — Radar, Elevation, Lap, Last Lap, Shifting, Navigation, eBike
 */
object DataFieldRegistry {

        val ALL_FIELDS: List<DataField> =
                coreCyclingFields() + powerEnergyFields() + extendedDataFields()

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

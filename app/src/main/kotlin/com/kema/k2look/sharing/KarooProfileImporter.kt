package com.kema.k2look.sharing

import com.kema.k2look.service.AppLog as Log
import com.kema.k2look.data.DataFieldRegistry
import com.kema.k2look.layout.LayoutTemplateRegistry
import com.kema.k2look.model.DataFieldProfile
import com.kema.k2look.model.IconSize
import com.kema.k2look.model.LayoutDataField
import com.kema.k2look.model.LayoutScreen
import com.kema.k2look.model.VisualizationType
import io.hammerhead.karooext.models.RideProfile
import java.util.UUID

/**
 * Maps a Karoo [RideProfile] to a K2Look [DataFieldProfile].
 *
 * Each non-map Karoo page becomes one [LayoutScreen]. The template is chosen by the number
 * of recognisable data fields on each page. Unknown third-party extension fields are skipped
 * with a log warning.
 */
object KarooProfileImporter {

    private const val TAG = "KarooProfileImporter"

    fun import(rideProfile: RideProfile): DataFieldProfile {
        val screens = mutableListOf<LayoutScreen>()
        var screenIndex = 1

        for (page in rideProfile.pages.filter { !it.mapPage }) {
            val knownFields = page.elements.mapNotNull { element ->
                val dataField = DataFieldRegistry.ALL_FIELDS
                    .find { it.karooStreamType == element.dataTypeId }
                if (dataField == null) {
                    Log.w(TAG, "Unknown dataTypeId '${element.dataTypeId}', skipping")
                }
                dataField
            }

            val fieldCount = minOf(knownFields.size, 6)
            if (fieldCount == 0) {
                Log.d(TAG, "Page skipped: no recognisable fields")
                continue
            }

            val templateId = templateIdFor(fieldCount)
            val template = LayoutTemplateRegistry.getTemplate(templateId)

            val dataFields = knownFields.take(fieldCount).mapIndexed { index, dataField ->
                LayoutDataField(
                    dataField = dataField,
                    zoneId = template.zones[index].id,
                    visualizationType = VisualizationType.TEXT,
                    showLabel = true,
                    showUnit = true,
                    showIcon = dataField.icon28 != null || dataField.icon40 != null,
                    iconSize = IconSize.SMALL
                )
            }

            screens.add(
                LayoutScreen(
                    id = screenIndex,
                    name = "Screen $screenIndex",
                    templateId = templateId,
                    dataFields = dataFields
                )
            )
            screenIndex++
        }

        if (screens.isEmpty()) {
            Log.w(TAG, "All Karoo pages were skipped — using empty fallback screen")
            screens.add(LayoutScreen(id = 1, name = "Screen 1", templateId = "3D_FULL"))
        }

        Log.i(TAG, "Imported '${rideProfile.name}': ${screens.size} screen(s)")
        return DataFieldProfile(
            id = UUID.randomUUID().toString(),
            name = rideProfile.name,
            screens = screens,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )
    }

    private fun templateIdFor(fieldCount: Int): String = when (fieldCount) {
        1 -> "1D"
        2 -> "2D"
        3 -> "3D_FULL"
        4 -> "4D"
        5 -> "5D"
        else -> "6D"
    }
}


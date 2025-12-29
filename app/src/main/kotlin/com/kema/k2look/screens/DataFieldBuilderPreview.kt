package com.kema.k2look.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kema.k2look.data.DataFieldRegistry
import com.kema.k2look.model.DataFieldProfile
import com.kema.k2look.model.IconSize
import com.kema.k2look.model.LayoutDataField
import com.kema.k2look.model.LayoutScreen

/**
 * Preview helpers for DataField Builder components
 */

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun PreviewZoneBasedScreenEditor() {
    val sampleScreen = LayoutScreen(
        id = 1,
        name = "Main Screen",
        templateId = "3D_FULL", // Three rows layout
        dataFields = listOf(
            LayoutDataField(
                dataField = DataFieldRegistry.getById(12)!!, // Speed
                zoneId = "3D_FULL_H", // Top zone
                showLabel = true,
                showUnit = true,
                showIcon = true,
                iconSize = IconSize.LARGE
            ),
            LayoutDataField(
                dataField = DataFieldRegistry.getById(7)!!, // Power
                zoneId = "3D_FULL_M", // Middle zone
                showLabel = true,
                showUnit = true,
                showIcon = true,
                iconSize = IconSize.LARGE
            )
        )
    )

    ZoneBasedScreenEditor(
        screen = sampleScreen,
        onTemplateChange = { },
        onFieldAdd = { _, _ -> },
        onFieldEdit = { },
        onFieldRemove = { }
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 200)
@Composable
fun PreviewZoneDataFieldSlotEmpty() {
    val template = com.kema.k2look.layout.LayoutTemplateRegistry.getTemplate("3D_FULL")
    val zone = template.zones.first()

    ZoneDataFieldSlot(
        zone = zone,
        field = null,
        onAdd = { },
        onEdit = { },
        onRemove = { }
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 200)
@Composable
fun PreviewZoneDataFieldSlotFilled() {
    val template = com.kema.k2look.layout.LayoutTemplateRegistry.getTemplate("3D_FULL")
    val zone = template.zones[1] // Middle zone

    ZoneDataFieldSlot(
        zone = zone,
        field = LayoutDataField(
            dataField = DataFieldRegistry.getById(4)!!, // Heart Rate
            zoneId = zone.id,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.SMALL
        ),
        onAdd = { },
        onEdit = { },
        onRemove = { }
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 600)
@Composable
fun PreviewProfileSelectorCard() {
    val profiles = listOf(
        DataFieldProfile(
            id = "default",
            name = "Default",
            isDefault = true,
            isReadOnly = true,
            screens = listOf(
                LayoutScreen(
                    id = 1,
                    name = "Main",
                    templateId = "3D_FULL",
                    dataFields = listOf()
                )
            )
        ),
        DataFieldProfile(
            id = "road",
            name = "Road Bike",
            isDefault = false,
            isReadOnly = false,
            screens = listOf(
                LayoutScreen(
                    id = 1,
                    name = "Main",
                    templateId = "3D_FULL",
                    dataFields = listOf()
                )
            )
        )
    )

    ProfileSelectorCard(
        profiles = profiles,
        activeProfile = profiles[1],
        onProfileSelected = { },
        onManageProfiles = { }
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 600)
@Composable
fun PreviewLayoutTemplateSelector() {
    LayoutTemplateSelectorDialog(
        currentTemplateId = "3D_FULL",
        onTemplateSelected = { },
        onDismiss = { }
    )
}




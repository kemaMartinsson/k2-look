package com.kema.k2look.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kema.k2look.data.DataFieldRegistry
import com.kema.k2look.model.DataFieldProfile
import com.kema.k2look.model.FontSize
import com.kema.k2look.model.IconSize
import com.kema.k2look.model.LayoutDataField
import com.kema.k2look.model.LayoutScreen
import com.kema.k2look.model.Position

/**
 * Preview helpers for DataField Builder components
 */

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun PreviewScreenEditor() {
    val sampleScreen = LayoutScreen(
        id = 1,
        name = "Main Screen",
        dataFields = listOf(
            LayoutDataField(
                dataField = DataFieldRegistry.getById(12)!!, // Speed
                position = Position.TOP,
                fontSize = FontSize.LARGE,
                showLabel = true,
                showUnit = true,
                showIcon = true,
                iconSize = IconSize.LARGE
            ),
            LayoutDataField(
                dataField = DataFieldRegistry.getById(7)!!, // Power
                position = Position.MIDDLE,
                fontSize = FontSize.LARGE,
                showLabel = true,
                showUnit = true,
                showIcon = true,
                iconSize = IconSize.LARGE
            )
        )
    )

    ScreenEditor(
        screen = sampleScreen,
        onFieldAdd = { _, _ -> },
        onFieldEdit = { },
        onFieldRemove = { }
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 200)
@Composable
fun PreviewDataFieldSlotEmpty() {
    DataFieldSlot(
        position = Position.TOP,
        field = null,
        onAdd = { },
        onEdit = { },
        onRemove = { }
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 200)
@Composable
fun PreviewDataFieldSlotFilled() {
    DataFieldSlot(
        position = Position.MIDDLE,
        field = LayoutDataField(
            dataField = DataFieldRegistry.getById(4)!!, // Heart Rate
            position = Position.MIDDLE,
            fontSize = FontSize.MEDIUM,
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




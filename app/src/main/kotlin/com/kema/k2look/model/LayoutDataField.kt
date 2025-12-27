package com.kema.k2look.model

/**
 * Represents a configured data field in a layout with display settings
 *
 * @param dataField The metric to display
 * @param position Position in the screen section (TOP/MIDDLE/BOTTOM)
 * @param fontSize Font size for the value
 * @param showLabel Whether to show the metric label
 * @param showUnit Whether to show the unit
 * @param showIcon Whether to display icon next to label
 * @param iconSize Icon size (28×28 or 40×40)
 */
data class LayoutDataField(
    val dataField: DataField,
    val position: Position,
    val fontSize: FontSize = FontSize.MEDIUM,
    val showLabel: Boolean = true,
    val showUnit: Boolean = true,
    val showIcon: Boolean = true,
    val iconSize: IconSize = IconSize.SMALL
)


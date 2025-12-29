package com.kema.k2look.layout

import android.util.Log
import com.kema.k2look.R
import com.kema.k2look.model.FontSize
import com.kema.k2look.model.LayoutTemplate
import com.kema.k2look.model.LayoutZone

/**
 * Registry for all official ActiveLook layout templates
 *
 * Provides access to 6 pre-defined layout templates (1D through 6D) based on
 * ActiveLook's official layout positions documented in the Visual Assets README.
 */
object LayoutTemplateRegistry {
    private const val TAG = "LayoutTemplateRegistry"

    private val templates = mutableMapOf<String, LayoutTemplate>()

    init {
        registerAllTemplates()
        Log.i(TAG, "Registered ${templates.size} layout templates")
    }

    private fun registerAllTemplates() {
        // 1D - Single Large Data
        register(
            LayoutTemplate(
                id = "1D",
                name = "Single Data",
                zones = listOf(
                    LayoutZone(
                        id = "1D",
                        displayName = "Center",
                        x = 59, y = 41, width = 187, height = 163,
                        font = 5, fontSize = FontSize.LARGE,
                        isChrono = true,
                        chronoHourX = 239, chronoHourY = 121
                    )
                ),
                maxFields = 1,
                preview = R.drawable.layout_preview_1d
            )
        )

        // 2D - Two Data Fields
        register(
            LayoutTemplate(
                id = "2D",
                name = "Two Data",
                zones = listOf(
                    LayoutZone(
                        id = "2D_H",
                        displayName = "Top",
                        x = 30, y = 129, width = 244, height = 60,
                        font = 4, fontSize = FontSize.LARGE,
                        isChrono = true,
                        chronoHourX = 203, chronoHourY = 154
                    ),
                    LayoutZone(
                        id = "2D_L",
                        displayName = "Bottom",
                        x = 30, y = 25, width = 244, height = 60,
                        font = 4, fontSize = FontSize.LARGE,
                        isChrono = true,
                        chronoHourX = 203, chronoHourY = 50
                    )
                ),
                maxFields = 2,
                preview = R.drawable.layout_preview_2d
            )
        )

        // 3D_TRIANGLE - Triangle Layout
        register(
            LayoutTemplate(
                id = "3D_TRIANGLE",
                name = "Triangle Layout",
                zones = listOf(
                    LayoutZone(
                        id = "3D_TRIANGLE_H",
                        displayName = "Top Center",
                        x = 30, y = 129, width = 244, height = 60,
                        font = 4, fontSize = FontSize.LARGE,
                        isChrono = true,
                        chronoHourX = 203, chronoHourY = 154
                    ),
                    LayoutZone(
                        id = "3D_HALF_L1",
                        displayName = "Bottom Right",
                        x = 157, y = 33, width = 117, height = 35,
                        font = 2, fontSize = FontSize.MEDIUM,
                        isChrono = true,
                        chronoHourX = 257, chronoHourY = 45
                    ),
                    LayoutZone(
                        id = "3D_HALF_L2",
                        displayName = "Bottom Left",
                        x = 30, y = 33, width = 117, height = 35,
                        font = 2, fontSize = FontSize.MEDIUM,
                        isChrono = true,
                        chronoHourX = 130, chronoHourY = 45
                    )
                ),
                maxFields = 3,
                preview = R.drawable.layout_preview_3d_triangle
            )
        )

        // 3D_FULL - Three Full Width (Default)
        register(
            LayoutTemplate(
                id = "3D_FULL",
                name = "Three Rows",
                zones = listOf(
                    LayoutZone(
                        id = "3D_FULL_H",
                        displayName = "Top",
                        x = 30, y = 153, width = 244, height = 50,
                        font = 3, fontSize = FontSize.MEDIUM,
                        isChrono = true,
                        chronoHourX = 211, chronoHourY = 170
                    ),
                    LayoutZone(
                        id = "3D_FULL_M",
                        displayName = "Middle",
                        x = 30, y = 89, width = 244, height = 50,
                        font = 3, fontSize = FontSize.MEDIUM,
                        isChrono = true,
                        chronoHourX = 211, chronoHourY = 106
                    ),
                    LayoutZone(
                        id = "3D_FULL_L",
                        displayName = "Bottom",
                        x = 30, y = 25, width = 244, height = 50,
                        font = 3, fontSize = FontSize.MEDIUM,
                        isChrono = true,
                        chronoHourX = 211, chronoHourY = 42
                    )
                ),
                maxFields = 3,
                preview = R.drawable.layout_preview_3d_full
            )
        )

        // 4D - Four Data Fields (2 full + 2 half)
        register(
            LayoutTemplate(
                id = "4D",
                name = "Four Data",
                zones = listOf(
                    LayoutZone(
                        id = "4D_FULL_H",
                        displayName = "Top",
                        x = 30, y = 149, width = 244, height = 60,
                        font = 4, fontSize = FontSize.LARGE,
                        isChrono = true,
                        chronoHourX = 203, chronoHourY = 174
                    ),
                    LayoutZone(
                        id = "4D_FULL_L",
                        displayName = "Second Row",
                        x = 30, y = 80, width = 244, height = 60,
                        font = 4, fontSize = FontSize.LARGE,
                        isChrono = true,
                        chronoHourX = 203, chronoHourY = 105
                    ),
                    LayoutZone(
                        id = "3D_HALF_L1",
                        displayName = "Bottom Right",
                        x = 157, y = 33, width = 117, height = 35,
                        font = 2, fontSize = FontSize.MEDIUM,
                        isChrono = true,
                        chronoHourX = 257, chronoHourY = 45
                    ),
                    LayoutZone(
                        id = "3D_HALF_L2",
                        displayName = "Bottom Left",
                        x = 30, y = 33, width = 117, height = 35,
                        font = 2, fontSize = FontSize.MEDIUM,
                        isChrono = true,
                        chronoHourX = 130, chronoHourY = 45
                    )
                ),
                maxFields = 4,
                preview = R.drawable.layout_preview_4d
            )
        )

        // 5D - Five Data Fields
        register(
            LayoutTemplate(
                id = "5D",
                name = "Five Data",
                zones = listOf(
                    LayoutZone(
                        id = "4D_FULL_H",
                        displayName = "Top",
                        x = 30, y = 149, width = 244, height = 60,
                        font = 4, fontSize = FontSize.LARGE,
                        isChrono = true,
                        chronoHourX = 203, chronoHourY = 174
                    ),
                    LayoutZone(
                        id = "3D_HALF_M1",
                        displayName = "Middle Right",
                        x = 157, y = 95, width = 117, height = 35,
                        font = 2, fontSize = FontSize.MEDIUM,
                        isChrono = true,
                        chronoHourX = 257, chronoHourY = 105
                    ),
                    LayoutZone(
                        id = "3D_HALF_M2",
                        displayName = "Middle Left",
                        x = 30, y = 95, width = 117, height = 35,
                        font = 2, fontSize = FontSize.MEDIUM,
                        isChrono = true,
                        chronoHourX = 130, chronoHourY = 105
                    ),
                    LayoutZone(
                        id = "3D_HALF_L1",
                        displayName = "Bottom Right",
                        x = 157, y = 33, width = 117, height = 35,
                        font = 2, fontSize = FontSize.MEDIUM,
                        isChrono = true,
                        chronoHourX = 257, chronoHourY = 45
                    ),
                    LayoutZone(
                        id = "3D_HALF_L2",
                        displayName = "Bottom Left",
                        x = 30, y = 33, width = 117, height = 35,
                        font = 2, fontSize = FontSize.MEDIUM,
                        isChrono = true,
                        chronoHourX = 130, chronoHourY = 45
                    )
                ),
                maxFields = 5,
                preview = R.drawable.layout_preview_5d
            )
        )

        // 6D - Six Half-Width Fields
        register(
            LayoutTemplate(
                id = "6D",
                name = "Six Data",
                zones = listOf(
                    LayoutZone(
                        id = "3D_HALF_H1",
                        displayName = "Top Right",
                        x = 157, y = 157, width = 117, height = 35,
                        font = 2, fontSize = FontSize.MEDIUM,
                        isChrono = true,
                        chronoHourX = 257, chronoHourY = 166
                    ),
                    LayoutZone(
                        id = "3D_HALF_H2",
                        displayName = "Top Left",
                        x = 30, y = 157, width = 117, height = 35,
                        font = 2, fontSize = FontSize.MEDIUM,
                        isChrono = true,
                        chronoHourX = 130, chronoHourY = 166
                    ),
                    LayoutZone(
                        id = "3D_HALF_M1",
                        displayName = "Middle Right",
                        x = 157, y = 95, width = 117, height = 35,
                        font = 2, fontSize = FontSize.MEDIUM,
                        isChrono = true,
                        chronoHourX = 257, chronoHourY = 105
                    ),
                    LayoutZone(
                        id = "3D_HALF_M2",
                        displayName = "Middle Left",
                        x = 30, y = 95, width = 117, height = 35,
                        font = 2, fontSize = FontSize.MEDIUM,
                        isChrono = true,
                        chronoHourX = 130, chronoHourY = 105
                    ),
                    LayoutZone(
                        id = "3D_HALF_L1",
                        displayName = "Bottom Right",
                        x = 157, y = 33, width = 117, height = 35,
                        font = 2, fontSize = FontSize.MEDIUM,
                        isChrono = true,
                        chronoHourX = 257, chronoHourY = 45
                    ),
                    LayoutZone(
                        id = "3D_HALF_L2",
                        displayName = "Bottom Left",
                        x = 30, y = 33, width = 117, height = 35,
                        font = 2, fontSize = FontSize.MEDIUM,
                        isChrono = true,
                        chronoHourX = 130, chronoHourY = 45
                    )
                ),
                maxFields = 6,
                preview = R.drawable.layout_preview_6d
            )
        )
    }

    private fun register(template: LayoutTemplate) {
        templates[template.id] = template
        Log.d(
            TAG,
            "Registered template: ${template.id} (${template.name}) with ${template.maxFields} fields"
        )
    }

    /**
     * Get template by ID, returns default if not found
     */
    fun getTemplate(id: String): LayoutTemplate {
        return templates[id] ?: run {
            Log.w(TAG, "Template '$id' not found, returning default")
            getDefaultTemplate()
        }
    }

    /**
     * Get the default template (3D_FULL)
     */
    fun getDefaultTemplate(): LayoutTemplate {
        return templates["3D_FULL"] ?: throw IllegalStateException("Default template not found")
    }

    /**
     * Get all available templates
     */
    fun getAllTemplates(): List<LayoutTemplate> {
        return templates.values.toList().sortedBy { it.maxFields }
    }

    /**
     * Get templates sorted by number of fields
     */
    fun getTemplatesByFieldCount(): Map<Int, List<LayoutTemplate>> {
        return templates.values.groupBy { it.maxFields }
    }
}


package com.kema.k2look.model

import com.kema.k2look.layout.LayoutTemplateRegistry

/**
 * Represents a screen with data fields in a profile
 *
 * @param id Screen identifier
 * @param name Screen name
 * @param templateId Layout template identifier (nullable for backward compatibility)
 * @param dataFields List of configured data fields (max 6)
 */
data class LayoutScreen(
    val id: Int,
    val name: String,
    val templateId: String? = null,
    val dataFields: List<LayoutDataField> = emptyList()
) {
    /**
     * Get the layout template for this screen
     * Returns default "3D_FULL" template if templateId is null (backward compatibility)
     */
    fun getTemplate(): LayoutTemplate {
        val actualTemplateId = templateId ?: "3D_FULL"
        return LayoutTemplateRegistry.getTemplate(actualTemplateId)
    }

    init {
        val template = getTemplate()
        require(dataFields.size <= template.maxFields) {
            "Maximum ${template.maxFields} data fields for template ${templateId ?: "3D_FULL"}"
        }
    }
}


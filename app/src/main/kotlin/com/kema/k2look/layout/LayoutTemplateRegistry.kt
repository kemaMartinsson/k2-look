package com.kema.k2look.layout

import android.util.Log
import com.kema.k2look.R
import com.kema.k2look.model.LayoutTemplate

/**
 * Registry for all official ActiveLook layout templates
 *
 * Provides access to 6 pre-defined layout templates (1D through 6D) based on ActiveLook's official
 * layout positions documented in the Visual Assets README.
 *
 * Template definitions (registerAllTemplates / registerAllTemplatesWithoutPreviews) live in
 * LayoutTemplateDefinitions.kt.
 */
object LayoutTemplateRegistry {
    private const val TAG = "LayoutTemplateRegistry"

    internal val templates = mutableMapOf<String, LayoutTemplate>()
    private var initialized = false

    private fun ensureInitialized() {
        if (initialized) return

        try {
            // Try to access R class to see if we're in an Android environment
            @Suppress("UNUSED_VARIABLE") val testR = R.drawable::class.java
            registerAllTemplates()
            Log.i(TAG, "Registered ${templates.size} layout templates with preview images")
        } catch (_: Throwable) {
            // R class not available in unit tests, register templates without preview images
            registerAllTemplatesWithoutPreviews()
        }
        initialized = true
    }

    // registerAllTemplates / registerAllTemplatesWithoutPreviews → LayoutTemplateDefinitions.kt

    /** Get template by ID, returns default if not found */
    fun getTemplate(id: String): LayoutTemplate {
        ensureInitialized()
        return templates[id]
                ?: run {
                    Log.w(TAG, "Template '$id' not found, returning default")
                    getDefaultTemplate()
                }
    }

    /** Get the default template (3D_FULL) */
    fun getDefaultTemplate(): LayoutTemplate {
        ensureInitialized()
        return templates["3D_FULL"] ?: throw IllegalStateException("Default template not found")
    }

    /** Get all available templates */
    fun getAllTemplates(): List<LayoutTemplate> {
        ensureInitialized()
        return templates.values.toList().sortedBy { it.maxFields }
    }

    /** Get templates sorted by number of fields */
    fun getTemplatesByFieldCount(): Map<Int, List<LayoutTemplate>> {
        ensureInitialized()
        return templates.values.groupBy { it.maxFields }
    }

    /**
     * Get the height of the zone with [zoneId] across all templates. Returns 0 if the zone is not
     * found.
     */
    fun getZoneHeight(zoneId: String): Int {
        ensureInitialized()
        return templates.values.flatMap { it.zones }.find { it.id == zoneId }?.height ?: 0
    }
}
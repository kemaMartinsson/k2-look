package com.kema.k2look.layout

/**
 * ActiveLook layout structure
 * Represents a configured display layout ready to be sent to the glasses
 */
data class ActiveLookLayout(
    val layoutId: Int,                              // Layout number (1-15)
    val clippingRegion: ClippingRegion,            // Display area
    val foreColor: Int = 15,                        // Foreground color (15=white)
    val backColor: Int = 0,                         // Background color (0=black)
    val font: Int = 2,                              // Font ID (1=SMALL, 2=MEDIUM, 3=LARGE)
    val textConfig: TextConfig,                     // Main text configuration
    val additionalCommands: List<GraphicCommand> = emptyList()  // Icons, labels, lines, etc.
)

/**
 * Clipping region defines the display area for this layout
 */
data class ClippingRegion(
    val x: Int,         // X position (pixels from left)
    val y: Int,         // Y position (pixels from top)
    val width: Int,     // Width in pixels
    val height: Int     // Height in pixels
)

/**
 * Text configuration for the main value display
 */
data class TextConfig(
    val x: Int,             // X position
    val y: Int,             // Y position
    val rotation: Int = 4,  // Text rotation (0=left, 4=centered, etc.)
    val opacity: Boolean = true  // Text opacity
)


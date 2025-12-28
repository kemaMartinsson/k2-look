package com.kema.k2look.layout

/**
 * Graphic commands for ActiveLook displays
 * These commands are encoded and sent to the glasses as part of layouts
 */
sealed class GraphicCommand {

    /**
     * Display an image (icon) at specified position
     * @param id Icon ID from ActiveLook Visual Assets (pre-loaded in glasses)
     * @param x X position (pixels from left)
     * @param y Y position (pixels from top)
     */
    data class Image(
        val id: Int,
        val x: Int,
        val y: Int
    ) : GraphicCommand()

    /**
     * Display text at specified position
     * @param x X position
     * @param y Y position
     * @param rotation Text rotation (0=left-aligned, 4=centered)
     * @param font Font ID (1=SMALL, 2=MEDIUM, 3=LARGE)
     * @param text Text to display
     */
    data class Text(
        val x: Int,
        val y: Int,
        val rotation: Int = 0,
        val font: Int = 2,
        val text: String
    ) : GraphicCommand()

    /**
     * Draw a line
     * @param x0 Start X
     * @param y0 Start Y
     * @param x1 End X
     * @param y1 End Y
     */
    data class Line(
        val x0: Int,
        val y0: Int,
        val x1: Int,
        val y1: Int
    ) : GraphicCommand()

    /**
     * Draw a circle
     * @param x Center X
     * @param y Center Y
     * @param radius Circle radius
     */
    data class Circle(
        val x: Int,
        val y: Int,
        val radius: Int
    ) : GraphicCommand()

    /**
     * Draw a rectangle
     * @param x0 Top-left X
     * @param y0 Top-left Y
     * @param x1 Bottom-right X
     * @param y1 Bottom-right Y
     */
    data class Rect(
        val x0: Int,
        val y0: Int,
        val x1: Int,
        val y1: Int
    ) : GraphicCommand()
}


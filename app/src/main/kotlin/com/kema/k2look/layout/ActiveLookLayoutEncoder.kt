package com.kema.k2look.layout

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Encodes ActiveLookLayout objects into binary format for transmission to glasses
 * Format follows ActiveLook API specification (section 5.10)
 */
class ActiveLookLayoutEncoder {

    companion object {
        private const val TAG = "ActiveLookLayoutEncoder"
        private const val MAX_LAYOUT_SIZE = 126  // bytes

        // Command IDs (from ActiveLook API)
        private const val CMD_TEXT = 9
        private const val CMD_LINE = 5
        private const val CMD_CIRCLE = 3
        private const val CMD_RECT = 7
        private const val CMD_IMAGE = 10
    }

    /**
     * Encode a layout to bytes ready for transmission
     * @return ByteArray in ActiveLook format
     */
    fun encodeLayout(layout: ActiveLookLayout): ByteArray {
        Log.d(TAG, "Encoding layout ${layout.layoutId}")

        val buffer = ByteBuffer.allocate(MAX_LAYOUT_SIZE)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        try {
            // Layout header (17 bytes)
            encodeHeader(buffer, layout)

            // Additional commands (icons, labels, lines, etc.)
            layout.additionalCommands.forEach { cmd ->
                encodeCommand(buffer, cmd)
            }

            val result = buffer.array().copyOf(buffer.position())
            Log.d(TAG, "Encoded layout ${layout.layoutId}: ${result.size} bytes")
            return result

        } catch (e: Exception) {
            Log.e(TAG, "Error encoding layout ${layout.layoutId}", e)
            throw e
        }
    }

    /**
     * Encode layout header (17 bytes)
     */
    private fun encodeHeader(buffer: ByteBuffer, layout: ActiveLookLayout) {
        buffer.put(layout.layoutId.toByte())                                    // 1: Layout ID
        buffer.put(calculateAdditionalCommandsSize(layout).toByte())           // 2: Additional commands size
        buffer.putShort(layout.clippingRegion.x.toShort())                     // 3-4: Clip X
        buffer.put(layout.clippingRegion.y.toByte())                           // 5: Clip Y
        buffer.putShort(layout.clippingRegion.width.toShort())                 // 6-7: Clip Width
        buffer.put(layout.clippingRegion.height.toByte())                      // 8: Clip Height
        buffer.put(layout.foreColor.toByte())                                   // 9: Foreground color
        buffer.put(layout.backColor.toByte())                                   // 10: Background color
        buffer.put(layout.font.toByte())                                        // 11: Font ID
        buffer.put(1)                                                           // 12: TextValid = true
        buffer.putShort(layout.textConfig.x.toShort())                         // 13-14: Text X
        buffer.put(layout.textConfig.y.toByte())                               // 15: Text Y
        buffer.put(layout.textConfig.rotation.toByte())                        // 16: Text rotation
        buffer.put(if (layout.textConfig.opacity) 1 else 0)                    // 17: Text opacity
    }

    /**
     * Calculate total size of additional commands
     */
    private fun calculateAdditionalCommandsSize(layout: ActiveLookLayout): Int {
        var size = 0
        layout.additionalCommands.forEach { cmd ->
            size += when (cmd) {
                is GraphicCommand.Text -> 5 + cmd.text.length  // CMD + x + y + rotation + font + len + text
                is GraphicCommand.Line -> 9                      // CMD + x0 + y0 + x1 + y1
                is GraphicCommand.Circle -> 6                    // CMD + x + y + radius
                is GraphicCommand.Rect -> 9                      // CMD + x0 + y0 + x1 + y1
                is GraphicCommand.Image -> 6                     // CMD + id + x + y
            }
        }
        return size
    }

    /**
     * Encode a single graphic command
     */
    private fun encodeCommand(buffer: ByteBuffer, cmd: GraphicCommand) {
        when (cmd) {
            is GraphicCommand.Text -> {
                buffer.put(CMD_TEXT.toByte())
                buffer.putShort(cmd.x.toShort())
                buffer.put(cmd.y.toByte())
                buffer.put(cmd.rotation.toByte())
                buffer.put(cmd.font.toByte())
                buffer.put(cmd.text.length.toByte())
                buffer.put(cmd.text.toByteArray(Charsets.US_ASCII))
                Log.v(TAG, "  Text: '${cmd.text}' at (${cmd.x}, ${cmd.y})")
            }

            is GraphicCommand.Line -> {
                buffer.put(CMD_LINE.toByte())
                buffer.putShort(cmd.x0.toShort())
                buffer.putShort(cmd.y0.toShort())
                buffer.putShort(cmd.x1.toShort())
                buffer.putShort(cmd.y1.toShort())
                Log.v(TAG, "  Line: (${cmd.x0}, ${cmd.y0}) to (${cmd.x1}, ${cmd.y1})")
            }

            is GraphicCommand.Circle -> {
                buffer.put(CMD_CIRCLE.toByte())
                buffer.putShort(cmd.x.toShort())
                buffer.putShort(cmd.y.toShort())
                buffer.put(cmd.radius.toByte())
                Log.v(TAG, "  Circle: center (${cmd.x}, ${cmd.y}), radius ${cmd.radius}")
            }

            is GraphicCommand.Rect -> {
                buffer.put(CMD_RECT.toByte())
                buffer.putShort(cmd.x0.toShort())
                buffer.putShort(cmd.y0.toShort())
                buffer.putShort(cmd.x1.toShort())
                buffer.putShort(cmd.y1.toShort())
                Log.v(TAG, "  Rect: (${cmd.x0}, ${cmd.y0}) to (${cmd.x1}, ${cmd.y1})")
            }

            is GraphicCommand.Image -> {
                buffer.put(CMD_IMAGE.toByte())
                buffer.put(cmd.id.toByte())
                buffer.putShort(cmd.x.toShort())
                buffer.putShort(cmd.y.toShort())
                Log.v(TAG, "  Image: ID ${cmd.id} at (${cmd.x}, ${cmd.y})")
            }
        }
    }

    /**
     * Encode multiple layouts at once
     * Useful for sending a complete screen configuration
     */
    fun encodeLayouts(layouts: List<ActiveLookLayout>): List<ByteArray> {
        Log.i(TAG, "Encoding ${layouts.size} layouts")
        return layouts.map { encodeLayout(it) }
    }
}


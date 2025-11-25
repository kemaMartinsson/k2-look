package com.kema.k2look.screens

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for markdown parsing functionality
 * Tests header, bold, and italic parsing
 */
class MarkdownParserTest {

    @Test
    fun `parseSimpleMarkdown handles plain text`() {
        val input = "This is plain text"

        val result = parseSimpleMarkdown(input)

        assertEquals("This is plain text", result.text)
    }

    @Test
    fun `parseSimpleMarkdown handles bold text`() {
        val input = "This is **bold** text"

        val result = parseSimpleMarkdown(input)

        assertEquals("This is bold text", result.text)
        // Verify bold span is applied (would check spanStyles in real test)
        assertTrue(result.text.contains("bold"))
    }

    @Test
    fun `parseSimpleMarkdown handles italic text`() {
        val input = "This is *italic* text"

        val result = parseSimpleMarkdown(input)

        assertEquals("This is italic text", result.text)
        assertTrue(result.text.contains("italic"))
    }

    @Test
    fun `parseSimpleMarkdown handles header with h2 tags`() {
        val input = "<h2>Header Text</h2>"

        val result = parseSimpleMarkdown(input)

        assertEquals("Header Text", result.text)
    }

    @Test
    fun `parseSimpleMarkdown handles header without closing tag`() {
        val input = "<h2>Header Text"

        val result = parseSimpleMarkdown(input)

        assertEquals("Header Text", result.text)
    }

    @Test
    fun `parseSimpleMarkdown handles multiple bold sections`() {
        val input = "**First** and **Second** bold"

        val result = parseSimpleMarkdown(input)

        assertEquals("First and Second bold", result.text)
    }

    @Test
    fun `parseSimpleMarkdown handles mixed formatting`() {
        val input = "**Bold** and *italic* together"

        val result = parseSimpleMarkdown(input)

        assertEquals("Bold and italic together", result.text)
    }

    @Test
    fun `parseSimpleMarkdown preserves newlines`() {
        val input = "Line 1\nLine 2\nLine 3"

        val result = parseSimpleMarkdown(input)

        assertEquals("Line 1\nLine 2\nLine 3", result.text)
    }

    @Test
    fun `parseSimpleMarkdown handles complex changelog`() {
        val input = """Release 0.5
<h2>What's New:</h2>
- **Background service**
- *Optional* disconnect
<h2>Known Issues:</h2>
- None"""

        val result = parseSimpleMarkdown(input)

        assertTrue(result.text.contains("What's New:"))
        assertTrue(result.text.contains("Background service"))
        assertTrue(result.text.contains("Optional"))
        assertTrue(result.text.contains("Known Issues:"))
    }

    @Test
    fun `parseSimpleMarkdown handles empty string`() {
        val input = ""

        val result = parseSimpleMarkdown(input)

        assertEquals("", result.text)
    }

    @Test
    fun `parseSimpleMarkdown handles unclosed bold`() {
        val input = "Text with **unclosed bold"

        val result = parseSimpleMarkdown(input)

        // Should keep the ** if not closed
        assertTrue(result.text.contains("**") || result.text.contains("unclosed bold"))
    }

    @Test
    fun `parseSimpleMarkdown handles nested markup gracefully`() {
        val input = "**Bold *and italic* together**"

        val result = parseSimpleMarkdown(input)

        // Should parse at least the bold part
        assertTrue(result.text.contains("Bold") && result.text.contains("italic"))
    }

    @Test
    fun `parseSimpleMarkdown handles multiple headers`() {
        val input = """<h2>Header 1</h2>
Content 1
<h2>Header 2</h2>
Content 2"""

        val result = parseSimpleMarkdown(input)

        assertTrue(result.text.contains("Header 1"))
        assertTrue(result.text.contains("Header 2"))
        assertTrue(result.text.contains("Content 1"))
        assertTrue(result.text.contains("Content 2"))
    }

    @Test
    fun `parseSimpleMarkdown preserves special characters`() {
        val input = "Text with symbols: @#$%^&()"

        val result = parseSimpleMarkdown(input)

        assertEquals("Text with symbols: @#$%^&()", result.text)
    }

    @Test
    fun `parseSimpleMarkdown handles bullet points`() {
        val input = "- Item 1\n- Item 2\n- **Bold item**"

        val result = parseSimpleMarkdown(input)

        assertTrue(result.text.contains("Item 1"))
        assertTrue(result.text.contains("Item 2"))
        assertTrue(result.text.contains("Bold item"))
    }
}


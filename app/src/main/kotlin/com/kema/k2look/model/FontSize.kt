package com.kema.k2look.model

/**
 * Font size for data field values on glasses display
 */
enum class FontSize(val fontId: Int, val height: Int) {
    SMALL(1, 24),    // Font 1: SourceSansPro SemiBold 24px (full ASCII)
    MEDIUM(2, 38),   // Font 2: SourceSansPro SemiBold 38px (full ASCII)
    LARGE(3, 64)     // Font 3: SourceSansPro SemiBold 64px (full ASCII)
    // Font 4 (75px) and Font 5 (82px) have limited charset (Space to ';')
    // and are referenced directly by font ID in zone definitions
}


// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}

// Disable all test tasks for reference modules — they are third-party sources we don't own.
val referenceModules = setOf(":activelook-sdk", ":karoo-ext")
configure(subprojects.filter { it.path in referenceModules }) {
    afterEvaluate {
        tasks.matching { it.name.contains("test", ignoreCase = true) || it.name.contains("Test") }
            .configureEach { enabled = false }
    }
}

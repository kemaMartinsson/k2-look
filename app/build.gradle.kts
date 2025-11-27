import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date

// Function to get the latest git tag, or fallback to "0.1" if no tags exist
fun getGitTag(): String {
    return try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "describe", "--tags", "--abbrev=0")
            standardOutput = stdout
            isIgnoreExitValue = true
        }
        val tag = stdout.toString().trim()
        if (tag.isNotEmpty()) tag else "0.1"
    } catch (e: Exception) {
        "0.1"
    }
}

// Function to get version code from git tag count
fun getGitVersionCode(): Int {
    return try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            standardOutput = stdout
            isIgnoreExitValue = true
        }
        val count = stdout.toString().trim()
        if (count.isNotEmpty()) count.toInt() else 1
    } catch (e: Exception) {
        1
    }
}

// Function to get git tag message (changelog)
fun getGitTagMessage(): String {
    return try {
        // Get the latest tag first
        var stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "tag", "--sort=-version:refname")
            standardOutput = stdout
            isIgnoreExitValue = true
        }
        val tags = stdout.toString().trim().split("\n")
        val latestTag = if (tags.isNotEmpty() && tags[0].isNotBlank()) tags[0] else ""

        if (latestTag.isEmpty()) {
            return "Initial release"
        }

        // Get the message for that tag
        stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "tag", "-l", "--format=%(contents)", latestTag)
            standardOutput = stdout
            isIgnoreExitValue = true
        }
        val message = stdout.toString().trim()
        if (message.isNotEmpty()) message else "Release $latestTag"
    } catch (e: Exception) {
        println("Warning: Could not get git tag message")
        "Initial release"
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.kema.k2look"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kema.k2look"
        minSdk = 23
        targetSdk = 34
        versionCode = getGitVersionCode()
        versionName = getGitTag()

        buildConfigField(
            "String",
            "BUILD_DATE",
            "\"${SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date())}\""
        )

        buildConfigField(
            "String",
            "CHANGELOG",
            "\"${getGitTagMessage().replace("\"", "\\\"").replace("\n", "\\n")}\""
        )

        // Inject version name into strings.xml
        resValue("string", "app_version", getGitTag())
    }

    signingConfigs {
        create("release") {
            // For local builds, use local keystore if it exists
            // For CI builds, use environment variables
            val keystorePath = System.getenv("KEYSTORE_FILE") ?: "keystore.jks"
            val keystoreFile = file(keystorePath)

            if (keystoreFile.exists() || System.getenv("KEYSTORE_FILE") != null) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                    ?: project.findProperty("KEYSTORE_PASSWORD") as String?
                keyAlias =
                    System.getenv("KEY_ALIAS") ?: project.findProperty("KEY_ALIAS") as String?
                keyPassword =
                    System.getenv("KEY_PASSWORD") ?: project.findProperty("KEY_PASSWORD") as String?
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Only sign if signing config is properly configured
            val releaseSigningConfig = signingConfigs.getByName("release")
            if (releaseSigningConfig.storeFile?.exists() == true) {
                signingConfig = releaseSigningConfig
            }
        }
        debug {
            // Also inject for debug builds
            resValue("string", "app_version", getGitTag())
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.all {
            it.reports.junitXml.required.set(true)
        }
    }
}

dependencies {
    implementation(project(":karoo-ext"))
    implementation(project(":activelook-sdk"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.androidx.lifeycle)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.compose.ui)

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.google.truth:truth:1.1.5")

    // Android Instrumented Tests
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")

    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
}

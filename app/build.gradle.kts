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
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":karoo-ext"))
    implementation(project(":activelook-sdk"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.androidx.lifeycle)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.compose.ui)
}

import java.text.SimpleDateFormat
import java.util.Date

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
        versionCode = 3
        versionName = "3.0"

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

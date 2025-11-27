// Commented out dokka imports - not needed for local development
// import org.jetbrains.dokka.base.DokkaBase
// import org.jetbrains.dokka.base.DokkaBaseConfiguration
import java.net.URL
import java.time.LocalDateTime

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android") version "2.0.0"
    // id("org.jetbrains.dokka") version "1.9.20"  // Commented out - not needed for local dev
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
    // `maven-publish`  // Commented out - not publishing locally
}

val moduleName = "karoo-ext"
val libVersion = "1.1.7"

// buildscript {
//     dependencies {
//         classpath("org.jetbrains.dokka:android-documentation-plugin:1.9.20")
//     }
// }

android {
    namespace = "io.hammerhead.karooext"
    compileSdk = 34

    defaultConfig {
        minSdk = 23

        buildConfigField("String", "LIB_VERSION", "\"$libVersion\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        buildConfig = true
        aidl = true
    }

    // Commented out publishing config - not needed for local development
    // publishing {
    //     singleVariant("release") {
    //         withSourcesJar()
    //     }
    // }
}

// Commented out dokka documentation generation - not needed for local development
// tasks.dokkaHtml.configure {
//     moduleName = "karoo-ext"
//     moduleVersion = libVersion
//     outputDirectory.set(rootDir.resolve("docs"))
//     suppressInheritedMembers = true
//
//     pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
//         val assetsDir = rootDir.resolve("assets")
//         homepageLink = "https://github.com/hammerheadnav/karoo-ext"
//
//         footerMessage = "© ${LocalDateTime.now().year} SRAM LLC."
//         customAssets = listOf(assetsDir.resolve("logo-icon.svg"))
//         customStyleSheets = listOf(assetsDir.resolve("hammerhead-style.css"))
//     }
//
//     dokkaSourceSets {
//         configureEach {
//             // A bug exists in dokka for Android libraries that prevents this from being generated
//             // https://github.com/Kotlin/dokka/issues/2876
//             sourceLink {
//                 localDirectory.set(projectDir.resolve("lib/src/main/kotlin"))
//                 remoteUrl.set(URL("https://github.com/hammerheadnav/karoo-ext/blob/${libVersion}/lib"))
//                 remoteLineSuffix.set("#L")
//             }
//             skipEmptyPackages.set(true)
//             includeNonPublic.set(false)
//             includes.from("Module.md")
//             samples.from("src/test/kotlin/Samples.kt")
//         }
//     }
// }

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.jakewharton.timber:timber:5.0.1")

    // dokkaPlugin("org.jetbrains.dokka:android-documentation-plugin:1.9.20")  // Commented out

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}

// Commented out publishing - not needed for local development
// publishing {
//     repositories {
//         maven {
//             name = "GitHubPackages"
//             url = uri("https://maven.pkg.github.com/hammerheadnav/karoo-ext")
//             credentials {
//                 username = System.getenv("GITHUB_USERNAME")
//                 password = System.getenv("GITHUB_TOKEN")
//             }
//         }
//     }
//     publications {
//         register<MavenPublication>("karoo-ext") {
//             artifactId = moduleName
//             groupId = "io.hammerhead"
//             version = libVersion
//
//             afterEvaluate {
//                 from(components["release"])
//             }
//         }
//     }
// }

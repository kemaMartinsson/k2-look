pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Karoo Extension Template"
include("app")

// Include local reference modules
include(":activelook-sdk")
project(":activelook-sdk").projectDir = file("reference/android-sdk/ActiveLookSDK")

include(":karoo-ext")
project(":karoo-ext").projectDir = file("reference/karoo-ext/lib")


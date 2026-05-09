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
        maven { setUrl("https://jitpack.io") }
        mavenLocal()
    }
}
include(":app")
include(":commons")

// Point specifically to the 'commons' subfolder
project(":commons").projectDir = file("../Fossify/Common/commons")

plugins {
    alias(libs.plugins.android).apply(false)
    alias(libs.plugins.kotlinAndroid).apply(false)
    alias(libs.plugins.detekt).apply(false)
    id("com.google.devtools.ksp") version "2.2.0-2.0.2"

}

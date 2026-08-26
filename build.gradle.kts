// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.installerx.spotless)
    alias(libs.plugins.agp.lib) apply false
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.aboutLibraries) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
}

tasks.register("Delete", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

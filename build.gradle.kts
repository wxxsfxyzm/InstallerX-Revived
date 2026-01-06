// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.agp.lib) apply false
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.aboutLibraries) apply false
    alias(libs.plugins.aboutLibraries.android) apply false

}

tasks.register("Delete", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

val baseVersion: String by project
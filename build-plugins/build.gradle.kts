plugins {
    `kotlin-dsl`
}

dependencies {
    // Provide DSL resolution for AGP and Kotlin
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.spotless.plugin.gradle)
}

plugins {
    `kotlin-dsl`
}

dependencies {
    // Provide DSL resolution for AGP and Kotlin
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.spotless.plugin.gradle)
    // The root build puts the Compose compiler plugin on the classpath; only its API is needed here.
    compileOnly(libs.compose.compiler.gradle.plugin)
}

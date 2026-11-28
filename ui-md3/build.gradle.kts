plugins {
    alias(libs.plugins.installerx.compose.library)
    alias(libs.plugins.installerx.publication)
}

installerxPublication {
    description.set("Material 3 Expressive widgets and theme from InstallerX")
}

android {
    namespace = "com.rosan.installer.ui.md3"
}

dependencies {
    api(projects.uiCore)
    api(projects.uiEffects)
    api(libs.compose.material3)

    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.materialIcons)
    implementation(libs.miuix.material)

    testImplementation(libs.kotlin.test.junit)
}

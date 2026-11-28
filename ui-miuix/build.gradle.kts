plugins {
    alias(libs.plugins.installerx.compose.library)
    alias(libs.plugins.installerx.publication)
}

installerxPublication {
    description.set("Miuix widgets, OS4 glass navigation, and theme from InstallerX")
}

android {
    namespace = "com.rosan.installer.ui.miuix"
}

dependencies {
    api(projects.uiColor)
    api(projects.uiCore)
    api(projects.uiEffects)
    api(libs.miuix.core)
    api(libs.miuix.ui)

    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.materialIcons)
    implementation(libs.miuix.glass)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.material)
    implementation(libs.miuix.preference)
}

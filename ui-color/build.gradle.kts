plugins {
    alias(libs.plugins.installerx.compose.library)
    alias(libs.plugins.installerx.publication)
}

installerxPublication {
    description.set("Dynamic Material color schemes shared by InstallerX UI families")
}

android {
    namespace = "com.rosan.installer.ui.color"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.compose.ui)
    // ColorScheme is the color contract shared by every UI family.
    api(libs.compose.material3)

    implementation(libs.materialKolor)
}

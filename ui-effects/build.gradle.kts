plugins {
    alias(libs.plugins.installerx.compose.library)
    alias(libs.plugins.installerx.publication)
}

installerxPublication {
    description.set("Floating navigation bar and glass effects built on Miuix blur")
}

android {
    namespace = "com.rosan.installer.ui.effects"
}

dependencies {
    api(projects.uiCore)

    api(libs.miuix.blur)

    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.miuix.material)
}

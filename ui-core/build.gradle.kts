plugins {
    alias(libs.plugins.installerx.compose.library)
    alias(libs.plugins.installerx.publication)
}

installerxPublication {
    description.set("Design-system-independent Compose primitives from InstallerX")
}

android {
    namespace = "com.rosan.installer.ui.core"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.compose.foundation)
    api(libs.compose.ui)
}

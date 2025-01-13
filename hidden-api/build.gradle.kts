@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.agp.lib)
}

android {
    namespace = "com.rosan.hidden_api"
    compileSdk = 35

    defaultConfig {
        minSdk = 34
    }
}

dependencies {
}
plugins {
    // Applied custom convention plugin instead of raw library plugin
    alias(libs.plugins.installerx.library)
}

android {
    namespace = "com.rosan.hidden_api"

    defaultConfig {
        minSdk = BuildConfig.MIN_SDK
    }
}

dependencies {
    // Add specific dependencies for hidden api stubs if needed
}

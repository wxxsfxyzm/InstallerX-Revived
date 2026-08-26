// Apply necessary base plugins
plugins {
    id("com.android.library")
}

android {
    compileSdk = BuildConfig.COMPILE_SDK
    compileSdkMinor = BuildConfig.COMPILE_SDK_MINOR

    defaultConfig {
        minSdk = BuildConfig.MIN_SDK
    }

    compileOptions {
        val javaVersion = JavaVersion.toVersion(BuildConfig.JDK_VERSION)
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

kotlin {
    jvmToolchain(BuildConfig.JDK_VERSION)
}

plugins {
    id("com.android.test")
}

android {
    compileSdk = BuildConfig.COMPILE_SDK
    compileSdkMinor = BuildConfig.COMPILE_SDK_MINOR

    defaultConfig {
        minSdk = 28
        targetSdk = BuildConfig.TARGET_SDK
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

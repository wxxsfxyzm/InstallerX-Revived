// Apply necessary base plugins
plugins {
    id("com.android.application")
}

android {
    compileSdk = BuildConfig.COMPILE_SDK
    compileSdkMinor = BuildConfig.COMPILE_SDK_MINOR

    defaultConfig {
        minSdk = BuildConfig.MIN_SDK
        targetSdk = BuildConfig.TARGET_SDK
    }

    compileOptions {
        val javaVersion = JavaVersion.toVersion(BuildConfig.JDK_VERSION)
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

kotlin {
    jvmToolchain(BuildConfig.JDK_VERSION)
}

plugins {
    alias(libs.plugins.installerx.library)
}

android {
    namespace = "com.rosan.app_process"

    buildFeatures {
        aidl = true
        buildConfig = true
    }
}

dependencies {
    compileOnly(project(":hidden-api"))
    compileOnly(libs.androidx.annotation)

    implementation(libs.commons.cli)
    implementation(libs.hiddenapibypass)
}

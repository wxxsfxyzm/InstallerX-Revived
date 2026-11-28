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
    compileOnly(projects.hiddenApi)
    compileOnly(libs.androidx.annotation)

    implementation(libs.commons.cli)
    implementation(libs.hiddenapibypass)
}

plugins {
    alias(libs.plugins.installerx.library)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

android {
    namespace = "com.rosan.installer.ui.components"

    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.compose.foundation)
    api(libs.compose.material3)
    api(libs.compose.ui)
    api(libs.miuix.blur)
    api(libs.miuix.ui)

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.materialIcons)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.miuix.core)
    implementation(libs.miuix.glass)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.material)
    implementation(libs.miuix.preference)

    testImplementation(libs.kotlin.test.junit)
}

val publicationVersion = providers.gradleProperty("UI_COMPONENTS_VERSION").orElse("0.1.0-SNAPSHOT")

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.rosan.installer"
            artifactId = "ui-components"
            version = publicationVersion.get()
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

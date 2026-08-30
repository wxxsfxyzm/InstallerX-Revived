import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.DuplicateRule
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.installerx.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.aboutLibraries)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.rosan.installer"

    val properties = Properties()
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        try {
            properties.load(keystorePropertiesFile.inputStream())
        } catch (e: Exception) {
            println("Warning: Could not load keystore.properties file: ${e.message}")
        }
    }
    val storeFile = properties.getProperty("storeFile") ?: System.getenv("KEYSTORE_FILE")
    val storePassword = properties.getProperty("storePassword") ?: System.getenv("KEYSTORE_PASSWORD")
    val keyAlias = properties.getProperty("keyAlias") ?: System.getenv("KEY_ALIAS")
    val keyPassword = properties.getProperty("keyPassword") ?: System.getenv("KEY_PASSWORD")
    val hasCustomSigning = storeFile != null && storePassword != null && keyAlias != null && keyPassword != null

    defaultConfig {
        // If you use InstallerX source code, package it into apk or other installation package format
        // Please change the applicationId to one that does not conflict with any official release.
        applicationId = project.findProperty("APP_ID") as String? ?: "com.rosan.installer.x.revived"

        // Version control retrieved from git, with a build-plugin fallback when git is unavailable.
        versionCode = project.getGitCommitCount()
        versionName = project.getBaseVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasCustomSigning) {
            register("releaseCustom") {
                this.storeFile = rootProject.file(storeFile)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = if (hasCustomSigning) {
                println("Applying custom signing to debug build.")
                signingConfigs.getByName("releaseCustom")
            } else {
                println("No custom signing info. Debug build will use the default debug keystore.")
                signingConfigs.getByName("debug")
            }
            optimization.enable = false
        }
        getByName("release") {
            signingConfig = if (hasCustomSigning) {
                println("Applying custom signing to release build.")
                signingConfigs.getByName("releaseCustom")
            } else {
                println("No custom signing info. Release build will use the default debug keystore.")
                signingConfigs.getByName("debug")
            }
            vcsInfo.include = false
            optimization.enable = true
        }
        create("nonMinifiedRelease") {
            signingConfig = if (hasCustomSigning) {
                signingConfigs.getByName("releaseCustom")
            } else {
                signingConfigs.getByName("debug")
            }
        }

        create("benchmarkRelease") {
            isDebuggable = true
            signingConfig = if (hasCustomSigning) {
                signingConfigs.getByName("releaseCustom")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    flavorDimensions.add("level")

    productFlavors {
        create("Unstable") {
            dimension = "level"
            isDefault = true
            versionNameSuffix = ".${project.getGitHash()}"
            buildConfigField("int", "BUILD_LEVEL", "0")
        }

        create("Preview") {
            dimension = "level"
            versionNameSuffix = ".${project.getGitHash()}"
            buildConfigField("int", "BUILD_LEVEL", "1")
        }

        create("Stable") {
            dimension = "level"
            buildConfigField("int", "BUILD_LEVEL", "2")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        aidl = true
    }

    packaging {
        jniLibs {
            // Module-specific exclusions
            excludes += setOf(
                "lib/*/libandroidx.graphics.path.so",
                "lib/*/libdatastore_shared_counter.so",
            )
        }
    }

    androidResources {
        generateLocaleConfig = true
    }
}

/*configurations.all {
    exclude(group = "androidx.navigationevent", module = "navigationevent-compose")
}*/

aboutLibraries {
    library {
        // Enable the duplication mode, allows to merge, or link dependencies which relate
        duplicationMode = DuplicateMode.MERGE
        // Configure the duplication rule, to match "duplicates" with
        duplicationRule = DuplicateRule.SIMPLE
    }
}

// Use the apksig implementation bundled with the lowest installed Build Tools version that
// supports the signature schemes handled by the app. Newer Build Tools retain this support.
private val minApksignerBuildTools = "37.0.0"

private fun buildToolsVersionParts(version: String): List<Int> = version
    .substringBefore('-')
    .split('.')
    .map { it.toIntOrNull() ?: 0 }
    .let { parts -> parts + List((3 - parts.size).coerceAtLeast(0)) { 0 } }

private fun compareBuildToolsVersions(left: String, right: String): Int = buildToolsVersionParts(left).zip(buildToolsVersionParts(right))
    .firstOrNull { (leftPart, rightPart) -> leftPart != rightPart }
    ?.let { (leftPart, rightPart) -> leftPart.compareTo(rightPart) }
    ?: 0

private fun selectApksignerJar(sdkDirectory: File): File {
    val buildToolsDirectory = sdkDirectory.resolve("build-tools")
    val selectedDirectory = buildToolsDirectory
        .listFiles()
        .orEmpty()
        .asSequence()
        .filter { it.isDirectory }
        .filter { it.resolve("lib/apksigner.jar").isFile }
        .filter { compareBuildToolsVersions(it.name, minApksignerBuildTools) >= 0 }
        .minWithOrNull { left, right -> compareBuildToolsVersions(left.name, right.name) }
        ?: error(
            "Build Tools >= $minApksignerBuildTools with lib/apksigner.jar are required",
        )
    return selectedDirectory.resolve("lib/apksigner.jar")
}

val apksignerJar = androidComponents.sdkComponents.sdkDirectory.map { sdkDirectory ->
    selectApksignerJar(sdkDirectory.asFile)
}

room3 {
    // Specify the schema directory
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    implementation(libs.commons.compress)
    implementation(libs.androidx.profileinstaller)
    implementation(files(apksignerJar))
    "baselineProfile"(project(":baselineprofile"))
    compileOnly(project(":hidden-api"))
    implementation(libs.androidx.core)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.splashscreen)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // implementation(libs.androidx.navigation3.runtime)
    /*    implementation(libs.androidx.navigationevent) {
            exclude(group = "androidx.navigation", module = "navigationevent-compose")
        }*/

    implementation(libs.compose.materialIcons)
    // Preview support only for debug builds
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    implementation(libs.ktx.serializationJson)

    implementation(libs.hiddenapibypass)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)

    implementation(libs.appiconloader)

    implementation(libs.iamr0s.dhizuku.api)

    implementation(project(":app-process"))

    // aboutlibraries
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.m3)

    // log
    implementation(libs.timber)

    // miuix
    implementation(libs.miuix.core)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.shader)
    implementation(libs.miuix.blur)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.navigation)

    // okhttp
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)

    // monetcompat
    implementation(libs.monetcompat)
    implementation(libs.androidx.palette)

    implementation(libs.focus.api)

    implementation(libs.materialKolor)
}

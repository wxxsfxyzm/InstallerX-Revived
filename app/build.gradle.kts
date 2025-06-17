import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    // id("kotlinx-serialization")
    alias(libs.plugins.aboutLibraries)
}

android {
    compileSdk = 36
    // 加载 keystore.properties
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties().apply {
        load(FileInputStream(keystorePropertiesFile))
    }
    defaultConfig {
        // 你如果根据InstallerX的源码进行打包成apk或其他安装包格式
        // 请换一个applicationId，不要和官方的任何发布版本产生冲突。
        // If you use InstallerX source code, package it into apk or other installation package format
        // Please change the applicationId to one that does not conflict with any official release.
        applicationId = "com.rosan.installer.x.revived"
        namespace = "com.rosan.installer"
        minSdk = 30
        targetSdk = 36
        // Version control
        // Github Actions will automatically use versionName A.B.C+1 when building preview releases
        // update versionCode and versionName before manually trigger a stable release
        versionCode = 35
        versionName = "2.2.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        javaCompileOptions {
            annotationProcessorOptions {
                compilerArgumentProviders(
                )
            }
        }
    }

    signingConfigs {
        getByName("debug") {
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
            storeFile = rootProject.file(keystoreProps["storeFile"] as String)
            storePassword = keystoreProps.getProperty("storePassword")
            enableV1Signing = true
            enableV2Signing = true
        }

        create("release") {
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
            storeFile = rootProject.file(keystoreProps["storeFile"] as String)
            storePassword = keystoreProps.getProperty("storePassword")
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "level"

    productFlavors {
        create("Unstable") {
            dimension = "level"
            isDefault = true
        }

        create("Preview") {
            dimension = "level"
        }

        create("Stable") {
            dimension = "level"
        }
    }

    val isReleaseBuild = gradle.startParameter.taskNames.any {
        it.contains("Release", ignoreCase = true)
    }

    splits {
        abi {
            isEnable = isReleaseBuild
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = false
        }
    }

    applicationVariants.all {
        val level = when (flavorName) {
            "Unstable" -> 0
            "Preview" -> 1
            "Stable" -> 2
            else -> 0
        }.toString()
        buildConfigField("int", "BUILD_LEVEL", level)
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_21
        sourceCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        buildConfig = true
        compose = true
        aidl = true
    }

    /*    composeOptions {
            //kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
        }*/

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

aboutLibraries {
    library {
        // Enable the duplication mode, allows to merge, or link dependencies which relate
        duplicationMode = com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
        // Configure the duplication rule, to match "duplicates" with
        duplicationRule = com.mikepenz.aboutlibraries.plugin.DuplicateRule.SIMPLE
    }
}

room {
    // Specify the schema directory
    schemaDirectory("$projectDir/schemas")
}

/*class RoomSchemaArgProvider(
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val schemaDir: File
) : CommandLineArgumentProvider {

    override fun asArguments(): Iterable<String> {
        return listOf("room.schemaLocation=${schemaDir.path}")
    }
}*/

dependencies {
    compileOnly(project(":hidden-api"))

    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    //implementation(libs.androidx.foundation)
    implementation(libs.compose.ui)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.compose.material)
    implementation(libs.compose.material3)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.compose.navigation)
    implementation(libs.compose.materialIcons)
    implementation(libs.material)

    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    implementation(libs.work.runtime.ktx)

    implementation(libs.ktx.serializationJson)

    implementation(libs.lsposed.hiddenapibypass)

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    implementation(libs.lottie.compose)

    implementation(libs.accompanist.navigationAnimation)
    implementation(libs.accompanist.flowlayout)
    implementation(libs.accompanist.drawablepainter)

    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)

    implementation(libs.appiconloader)
    implementation(libs.compose.coil)

    implementation(libs.iamr0s.dhizuku.api)

    implementation(libs.iamr0s.androidAppProcess)

    implementation(libs.okhttp)

    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.m3)
}

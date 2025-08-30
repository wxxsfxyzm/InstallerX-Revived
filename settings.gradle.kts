pluginManagement {
    repositories {
        mavenLocal()
//        maven { setUrl("https://maven.aliyun.com/repository/public/") }
//        maven { setUrl("https://repo.huaweicloud.com/repository/maven/") }
//        maven { setUrl("https://jitpack.io") }
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { setUrl("https://maven.scijava.org/content/repositories/public/") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
//        maven { setUrl("https://maven.aliyun.com/repository/public/") }
//        maven { setUrl("https://repo.huaweicloud.com/repository/maven/") }
//        maven { setUrl("https://jitpack.io") }
        google()
        mavenCentral()
        maven { setUrl("https://maven.scijava.org/content/repositories/public/") }
    }
}

rootProject.name = "InstallerX"
include(
    ":app",
    ":hidden-api"
)

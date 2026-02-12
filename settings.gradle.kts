pluginManagement {
    repositories {
        mavenLocal()
        // maven { setUrl("https://maven.aliyun.com/repository/public/") }
        // maven { setUrl("https://repo.huaweicloud.com/repository/maven/") }
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        // maven { setUrl("https://maven.scijava.org/content/repositories/public/") }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        // maven { setUrl("https://maven.aliyun.com/repository/public/") }
        // maven { setUrl("https://repo.huaweicloud.com/repository/maven/") }
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        // maven { setUrl("https://maven.scijava.org/content/repositories/public/") }
        // GitHub Packages (compose-miuix-ui/miuix)
        //
        // NOTE:
        // GitHub Packages does NOT support anonymous access.
        // Even if the repository is public or you are a member of the organization,
        // authentication is still required to resolve dependencies.
        //
        // Required environment variables (recommended):
        //   - GITHUB_ACTOR : your GitHub username
        //   - GITHUB_TOKEN : a Personal Access Token (classic) with `read:packages` scope
        //
        // Alternative:
        //   - Define `gpr.user` and `gpr.key` in ~/.gradle/gradle.properties (NOT in this repo)
        //
        // This configuration is intentionally placed in settings.gradle.kts
        // to work with RepositoriesMode.FAIL_ON_PROJECT_REPOS.
        maven {
            url = uri("https://maven.pkg.github.com/compose-miuix-ui/miuix")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(System.getenv("GITHUB_ACTOR"))
                    .get()
                password = providers.gradleProperty("gpr.key")
                    .orElse(System.getenv("GITHUB_TOKEN"))
                    .get()
            }
        }
    }
}

rootProject.name = "InstallerX-Revived"
include(
    ":app",
    ":hidden-api"
)

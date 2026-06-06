pluginManagement {
    includeBuild("build-plugins")
    repositories {
        // maven { setUrl("https://maven.aliyun.com/repository/public/") }
        // maven { setUrl("https://repo.huaweicloud.com/repository/maven/") }
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        // maven { setUrl("https://maven.scijava.org/content/repositories/public/") }
        mavenLocal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
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
        //   - GITHUB_TOKEN : GitHub Actions token, or a PAT classic with `read:packages`
        //
        // Alternative:
        //   - Define `gpr.user` and `gpr.key` in ~/.gradle/gradle.properties (NOT in this repo)
        //
        // This configuration is intentionally placed in settings.gradle.kts
        // to work with RepositoriesMode.FAIL_ON_PROJECT_REPOS.
        val gprUser = providers.gradleProperty("gpr.user")
            .orElse(providers.environmentVariable("GITHUB_ACTOR"))

        val gprKey = providers.gradleProperty("gpr.key")
            .orElse(providers.environmentVariable("GITHUB_TOKEN"))

        maven {
            name = "GitHubPackagesMiuix"
            url = uri("https://maven.pkg.github.com/compose-miuix-ui/miuix")

            if (gprUser.isPresent && gprKey.isPresent) {
                credentials {
                    username = gprUser.get()
                    password = gprKey.get()
                }
            }
        }

        mavenLocal()
    }
}

rootProject.name = "InstallerX-Revived"
include(
    ":app",
    ":app-process",
    ":hidden-api"
)
include(":baselineprofile")

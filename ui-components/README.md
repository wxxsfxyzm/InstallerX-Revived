# UI components

Reusable Material 3 and Miuix Compose widgets, navigation surfaces, and rendering effects. Pages, application navigation, ViewModels, and localized application copy belong to `:app`.

## Use in another Android project

Publish the release AAR, sources, and dependency metadata to your local Maven repository.

```sh
./gradlew :ui-components:publishReleasePublicationToMavenLocal
```

In the consuming project's `settings.gradle.kts`, add `mavenLocal()` to `dependencyResolutionManagement.repositories`. The consumer also needs the repositories that resolve Compose and Miuix. See [the project build instructions](../docs/README.md#github-packages-authentication) for Miuix repository authentication.

Add the library to the consuming module.

```kotlin
dependencies {
    implementation("com.rosan.installer:ui-components:0.1.0-SNAPSHOT")
}
```

Use `-PUI_COMPONENTS_VERSION=<version>` to publish under a different version. The consumer should use a compatible Android Gradle Plugin, Kotlin and Compose toolchain, and the appropriate theme for the widgets it uses.

The Miuix blur and glass dependencies declare API 33 in their manifests. InstallerX supports lower API levels through manifest overrides and runtime capability checks. A separate consumer can use `minSdk = 33` to consume the library without those overrides.

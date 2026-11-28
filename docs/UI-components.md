# Reusable Compose components

InstallerX keeps application screens, navigation, theme switching, and ViewModels in `app`. Other projects can use the libraries separately.

| Artifact | Components |
| --- | --- |
| `ui-color` | Dynamic Material color schemes, preset seed colors, and the color input shared by every UI family |
| `ui-core` | Compose layout shapes, lifecycle hooks, and reusable UI primitives |
| `ui-effects` | Floating navigation bar, glass and blur effects, and their gesture animations |
| `ui-md3` | Material 3 Expressive widgets and the `Material3InstallerTheme` entry point |
| `ui-miuix` | Miuix widgets, OS4 glass navigation, and the Miuix theme controller built from `ui-color` input |

Each library has its own package root: `com.rosan.installer.ui.color`, `ui.core`, `ui.effects`, `ui.md3`, and `ui.miuix`. Material 3 widgets use subpackages such as `ui.md3.widgets.card` and `ui.md3.widgets.setting`.

Every UI family uses the Material `ColorScheme` from `ui-color` as its shared color contract, because Android dynamic color follows the Material color system. A family maps that scheme, or the `ColorThemeConfig` input, to its own theme. Both UI libraries depend on `ui-core` and `ui-effects`, and `ui-miuix` also depends on `ui-color`. The effects library uses Miuix's blur and rendering artifacts. The Miuix UI library also brings in the upstream window size class API.

## Use in another Android project

Publish the release AARs, sources, and dependency metadata to Maven Local.

```sh
./gradlew :ui-color:publishReleasePublicationToMavenLocal \
          :ui-core:publishReleasePublicationToMavenLocal \
          :ui-effects:publishReleasePublicationToMavenLocal \
          :ui-md3:publishReleasePublicationToMavenLocal \
          :ui-miuix:publishReleasePublicationToMavenLocal
```

Add `mavenLocal()` to the consuming project's `dependencyResolutionManagement.repositories`. The consumer also needs the repositories used to resolve Compose and the selected rendering dependencies. See the [repository configuration](../settings.gradle.kts) for authentication when using artifacts from GitHub Packages.

Declare the library for the UI you use.

```kotlin
dependencies {
    implementation("com.rosan.installer:ui-md3:0.1.0-SNAPSHOT")
}
```

Use `com.rosan.installer:ui-miuix:0.1.0-SNAPSHOT` for Miuix, or declare `ui-core` for the reusable primitives alone. Depend directly on `ui-effects` if you want to use the floating navigation bar with your own theme. Its icon and label slots receive the current content color, and you can pass your own `FloatingBottomBarColors` or use the theme defaults provided by either UI library.

To migrate from the former `ui-components` artifact, replace it with the selected library or declare both if the project uses both families. Use `-PUI_COMPONENTS_VERSION=<version>` to publish the libraries with a different version.

The glass and blur dependencies declare API 33 in their manifests. Consumers of either themed library or `ui-effects` can use `minSdk = 33`. InstallerX uses manifest overrides and runtime capability checks to support earlier versions.

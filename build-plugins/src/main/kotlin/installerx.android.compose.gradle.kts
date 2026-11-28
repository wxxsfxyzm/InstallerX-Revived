import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

// Apply after an Android application or library plugin.
apply(plugin = "org.jetbrains.kotlin.plugin.compose")

plugins.withId("com.android.application") {
    extensions.configure<ApplicationExtension> { buildFeatures.compose = true }
}
plugins.withId("com.android.library") {
    extensions.configure<LibraryExtension> { buildFeatures.compose = true }
}

// Pass -PcomposeReports=true to write Compose compiler metrics and reports to build/compose_compiler.
if (providers.gradleProperty("composeReports").map(String::toBoolean).getOrElse(false)) {
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        val outputDir = layout.buildDirectory.dir("compose_compiler")
        reportsDestination.set(outputDir)
        metricsDestination.set(outputDir)
    }
}

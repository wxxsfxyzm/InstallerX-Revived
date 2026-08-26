import com.diffplug.spotless.LineEnding
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

plugins {
    id("com.diffplug.spotless")
}

val versions = extensions.getByType<VersionCatalogsExtension>().named("libs")
val ktlintVersion = versions.findLibrary("ktlint-cli").get().get().versionConstraint.requiredVersion
val composeKtlintRules = versions.findLibrary("compose-ktlint-rules").get().get().let {
    "${it.module.group}:${it.module.name}:${it.versionConstraint.requiredVersion}"
}
spotless {
    lineEndings = LineEnding.UNIX

    kotlin {
        target("**/src/**/*.kt")
        targetExclude("**/build/**", "**/generated/**")
        ktlint(ktlintVersion)
            .customRuleSets(listOf(composeKtlintRules))
            .editorConfigOverride(
                mapOf(
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                    "ktlint_compose_modifier-missing-check" to "disabled",
                    "ktlint_compose_compositionlocal-allowlist" to "disabled",
                    "ktlint_compose_mutable-state-param-check" to "disabled",
                    "ktlint_compose_parameter-naming" to "disabled",
                    "ktlint_compose_modifier-naming" to "disabled",
                ),
            )
    }

    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**", "**/.gradle/**")
        ktlint(ktlintVersion)
    }
}

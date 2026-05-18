import org.gradle.api.Project
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object BuildConfig {
    const val COMPILE_SDK = 37
    const val COMPILE_SDK_MINOR = 0
    const val TARGET_SDK = 37
    const val MIN_SDK = 26
    const val JDK_VERSION = 25

    const val VERSION_CODE = 52
}

// Get git commit hash safely, compatible with configuration cache
fun Project.getGitHash(): String {
    return try {
        providers.exec {
            commandLine("git", "rev-parse", "--short=7", "HEAD")
        }.standardOutput.asText.get().trim()
    } catch (_: Exception) {
        "unknown"
    }
}

// Get the date of the latest commit directly formatted as yy.MM
fun Project.getGitDate(): String {
    return try {
        providers.exec {
            commandLine("git", "log", "-1", "--format=%cd", "--date=format:%y.%m")
        }.standardOutput.asText.get().trim()
    } catch (_: Exception) {
        // Fallback to current date if git command fails
        LocalDate.now().format(DateTimeFormatter.ofPattern("yy.MM"))
    }
}

// Combine the manual version name or dynamic git date
fun Project.getBaseVersionName(): String {
    val manualVersionName = findProperty("VERSION_NAME") as String?
    return manualVersionName ?: getGitDate()
}
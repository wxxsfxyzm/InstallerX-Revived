package com.rosan.installer.util

/**
 * Converts legacy Android language codes to their modern equivalents.
 * This ensures compatibility with older and non-standard APK splits.
 *
 * Logic is from the PackageUtil.kt file in the PackageInstaller project.
 * Under Apache License 2.0.
 *
 * @param this The legacy language code to be converted.
 * @return The modern language code.
 * @see <a href="https://developer.android.com/reference/java/util/Locale#legacy-language-codes">java.util.Locale#legacy-language-codes</a>
 *  * @see <a href="https://github.com/vvb2060/PackageInstaller/blob/master/app/src/main/java/io/github/vvb2060/packageinstaller/model/PackageUtil.kt">PackageUtil</a>
 * @see <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>
 */
fun String.convertLegacyLanguageCode(): String {
    return when (this) {
        "in" -> "id" // Indonesian
        "iw" -> "he" // Hebrew
        "ji" -> "yi" // Yiddish
        else -> this
    }
}
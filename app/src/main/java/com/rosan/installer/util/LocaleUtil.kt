package com.rosan.installer.util

/**
 * Converts legacy Android language codes to their modern equivalents.
 * This logic is migrated from the original PackageUtil to ensure compatibility.
 *
 * @param this The legacy language code to be converted.
 * @return The modern language code.
 * @see <a href="https://developer.android.com/reference/java/util/Locale#legacy-language-codes">java.util.Locale#legacy-language-codes</a>
 */
fun String.convertLegacyLanguageCode(): String {
    return when (this) {
        "in" -> "id" // Indonesian
        "iw" -> "he" // Hebrew
        "ji" -> "yi" // Yiddish
        else -> this
    }
}
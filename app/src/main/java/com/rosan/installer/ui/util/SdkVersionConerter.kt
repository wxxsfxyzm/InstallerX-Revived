package com.rosan.installer.ui.util

/**
 * Converts a string representing an Android SDK version code (API level)
 * to its corresponding Android marketing version name.
 *
 * For example, "23" will be converted to "6", and "32" to "12.1".
 *
 * If the input string is not a valid integer or the API level is unknown,
 * the original string is returned.
 *
 * @return The Android version name as a String, or the original string if conversion fails.
 */
fun String.toAndroidVersionName(): String {
    // Attempt to convert the string to an integer. If it fails (e.g., "abc"),
    // return the original string immediately.
    val apiLevel = this.toIntOrNull() ?: return this

    // Use a 'when' expression to map the API level to the version name.
    return when (apiLevel) {
        // --- Pre-Marshmallow versions ---
        1 -> "1.0"
        2 -> "1.1"
        3 -> "1.5"
        4 -> "1.6"
        5 -> "2.0"
        6 -> "2.0.1"
        7 -> "2.1"
        8 -> "2.2"
        9 -> "2.3"
        10 -> "2.3.3"
        11 -> "3.0"
        12 -> "3.1"
        13 -> "3.2"
        14 -> "4.0"
        15 -> "4.0.3"
        16 -> "4.1"
        17 -> "4.2"
        18 -> "4.3"
        19 -> "4.4"
        20 -> "4.4W"
        21 -> "5.0"
        22 -> "5.1"
        // --- Versions from minSDK ---
        23 -> "6"     // Android 6.0 Marshmallow
        24 -> "7"     // Android 7.0 Nougat
        25 -> "7.1"   // Android 7.1 Nougat
        26 -> "8"     // Android 8.0 Oreo
        27 -> "8.1"   // Android 8.1 Oreo
        28 -> "9"     // Android 9 Pie
        29 -> "10"    // Android 10
        30 -> "11"    // Android 11
        31 -> "12"    // Android 12
        32 -> "12.1"  // Android 12L (or 12.1)
        33 -> "13"    // Android 13 Tiramisu
        34 -> "14"    // Android 14 Upside Down Cake
        35 -> "15"    // Android 15 Vanilla Ice Cream
        36 -> "16"    // Android 16 (Future version)
        // Add more future versions here as they are announced
        // 37 -> "17"

        // If the API level doesn't match any of the above cases,
        // return the original string.
        else -> this
    }
}
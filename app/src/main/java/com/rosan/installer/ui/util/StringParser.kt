package com.rosan.installer.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.data.app.model.enums.DataType
import com.rosan.installer.data.app.model.enums.MmzSelectionMode
import com.rosan.installer.ui.theme.m3color.RawColor

@Composable
fun DataType.getSupportTitle() =
    when (this) {
        DataType.MIXED_MODULE_APK -> stringResource(R.string.installer_select_from_mixed_module_apk)
        DataType.MULTI_APK_ZIP -> stringResource(R.string.installer_select_from_zip)
        DataType.MULTI_APK -> stringResource(R.string.installer_select_multi_apk)
        else -> stringResource(R.string.installer_select_install)
    }

@Composable
fun DataType.getSupportSubtitle(selectionMode: MmzSelectionMode) =
    when (this) {
        DataType.MIXED_MODULE_APK -> stringResource(R.string.installer_mixed_module_apk_description)
        DataType.MULTI_APK_ZIP -> stringResource(R.string.installer_multi_apk_zip_description)
        DataType.MULTI_APK -> stringResource(R.string.installer_multi_apk_description)
        DataType.MIXED_MODULE_ZIP -> if (selectionMode == MmzSelectionMode.INITIAL_CHOICE)
            stringResource(R.string.installer_mixed_module_zip_description)
        else stringResource(R.string.installer_multi_apk_zip_description)

        else -> null
    }

@Composable
fun RawColor.getDisplayName() = when (key) {
    "default" -> stringResource(R.string.color_default)
    "pink" -> stringResource(R.string.color_pink)
    "red" -> stringResource(R.string.color_red)
    "orange" -> stringResource(R.string.color_orange)
    "amber" -> stringResource(R.string.color_amber)
    "yellow" -> stringResource(R.string.color_yellow)
    "lime" -> stringResource(R.string.color_lime)
    "green" -> stringResource(R.string.color_green)
    "cyan" -> stringResource(R.string.color_cyan)
    "teal" -> stringResource(R.string.color_teal)
    "light_blue" -> stringResource(R.string.color_light_blue)
    "blue" -> stringResource(R.string.color_blue)
    "indigo" -> stringResource(R.string.color_indigo)
    "purple" -> stringResource(R.string.color_purple)
    "deep_purple" -> stringResource(R.string.color_deep_purple)
    "blue_grey" -> stringResource(R.string.color_blue_grey)
    "brown" -> stringResource(R.string.color_brown)
    "grey" -> stringResource(R.string.color_grey)
    else -> key
}

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
        36 -> "16"    // Android 16 Baklava
        37 -> "17"    // Android 17
        // Add more future versions here as they are announced

        // If the API level doesn't match any of the above cases,
        // return the original string.
        else -> this
    }
}

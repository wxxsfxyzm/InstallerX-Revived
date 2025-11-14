package com.rosan.installer.ui.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.model.entity.MmzSelectionMode
import com.rosan.installer.ui.theme.m3color.RawColor

@Composable
fun DataType.getSupportTitle() =
    when (this) {
        DataType.MIXED_MODULE_APK -> R.string.installer_select_from_mixed_module_apk
        DataType.MULTI_APK_ZIP -> R.string.installer_select_from_zip
        DataType.MULTI_APK -> R.string.installer_select_multi_apk
        else -> R.string.installer_select_install
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

fun RawColor.getDisplayName(context: Context): String = when (key) {
    "default" -> context.getString(R.string.color_default)
    "pink" -> context.getString(R.string.color_pink)
    "red" -> context.getString(R.string.color_red)
    "orange" -> context.getString(R.string.color_orange)
    "amber" -> context.getString(R.string.color_amber)
    "yellow" -> context.getString(R.string.color_yellow)
    "lime" -> context.getString(R.string.color_lime)
    "green" -> context.getString(R.string.color_green)
    "cyan" -> context.getString(R.string.color_cyan)
    "teal" -> context.getString(R.string.color_teal)
    "light_blue" -> context.getString(R.string.color_light_blue)
    "blue" -> context.getString(R.string.color_blue)
    "indigo" -> context.getString(R.string.color_indigo)
    "purple" -> context.getString(R.string.color_purple)
    "deep_purple" -> context.getString(R.string.color_deep_purple)
    "blue_grey" -> context.getString(R.string.color_blue_grey)
    "brown" -> context.getString(R.string.color_brown)
    "grey" -> context.getString(R.string.color_grey)
    else -> key
}
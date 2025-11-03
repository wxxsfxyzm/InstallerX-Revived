package com.rosan.installer.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.model.entity.MmzSelectionMode

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
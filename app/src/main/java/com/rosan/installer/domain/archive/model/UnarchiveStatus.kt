// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.archive.model

import androidx.annotation.StringRes
import com.rosan.installer.R

sealed class UnarchiveStatus(
    val primaryAction: UnarchiveErrorAction,
    @param:StringRes val titleResId: Int,
    @param:StringRes val messageResId: Int,
    @param:StringRes val actionLabelResId: Int
) {
    data object UserActionNeeded : UnarchiveStatus(
        primaryAction = UnarchiveErrorAction.CONTINUE,
        titleResId = R.string.unarchive_error_action_required_title,
        messageResId = R.string.unarchive_error_action_required_message,
        actionLabelResId = R.string.unarchive_action_continue
    )

    data object InsufficientStorage : UnarchiveStatus(
        primaryAction = UnarchiveErrorAction.CLEAR_STORAGE,
        titleResId = R.string.unarchive_error_storage_title,
        messageResId = R.string.unarchive_error_storage_message,
        actionLabelResId = R.string.unarchive_action_clear_storage
    )

    data object NoConnectivity : UnarchiveStatus(
        primaryAction = UnarchiveErrorAction.CLOSE,
        titleResId = R.string.unarchive_error_offline_title,
        messageResId = R.string.unarchive_error_offline_message,
        actionLabelResId = R.string.close
    )

    data object InstallerDisabled : UnarchiveStatus(
        primaryAction = UnarchiveErrorAction.OPEN_INSTALLER_SETTINGS,
        titleResId = R.string.unarchive_error_installer_disabled_title,
        messageResId = R.string.unarchive_error_installer_disabled_message,
        actionLabelResId = R.string.unarchive_action_app_settings
    )

    data object InstallerUninstalled : UnarchiveStatus(
        primaryAction = UnarchiveErrorAction.CLOSE,
        titleResId = R.string.unarchive_error_installer_uninstalled_title,
        messageResId = R.string.unarchive_error_installer_uninstalled_message,
        actionLabelResId = R.string.close
    )

    data object GenericError : UnarchiveStatus(
        primaryAction = UnarchiveErrorAction.CLOSE,
        titleResId = R.string.unarchive_error_generic_title,
        messageResId = R.string.unarchive_error_generic_message,
        actionLabelResId = R.string.close
    )

    companion object {
        const val OK = 0
        const val USER_ACTION_NEEDED = 1
        const val INSUFFICIENT_STORAGE = 2
        const val NO_CONNECTIVITY = 3
        const val GENERIC_ERROR = 100
        const val INSTALLER_DISABLED = 4
        const val INSTALLER_UNINSTALLED = 5

        fun fromPlatformStatus(status: Int): UnarchiveStatus = when (status) {
            USER_ACTION_NEEDED -> UserActionNeeded
            INSUFFICIENT_STORAGE -> InsufficientStorage
            NO_CONNECTIVITY -> NoConnectivity
            INSTALLER_DISABLED -> InstallerDisabled
            INSTALLER_UNINSTALLED -> InstallerUninstalled
            GENERIC_ERROR -> GenericError
            else -> GenericError
        }
    }
}

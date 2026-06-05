// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred

import androidx.annotation.StringRes
import com.rosan.installer.domain.settings.model.backup.BackupRestorePreview
import com.rosan.installer.domain.settings.model.backup.BackupValidationIssue

sealed interface PreferredViewEvent {
    data class ShowDefaultInstallerResult(
        @param:StringRes val messageResId: Int
    ) : PreferredViewEvent

    data class ShowDefaultInstallerErrorDetail(
        @param:StringRes val titleResId: Int,
        val exception: Throwable,
        val retryAction: PreferredViewAction
    ) : PreferredViewEvent

    data class LaunchBackupExport(
        val fileName: String,
        val content: String
    ) : PreferredViewEvent

    data class ShowBackupMessage(
        @param:StringRes val messageResId: Int
    ) : PreferredViewEvent

    data class ShowBackupError(
        @param:StringRes val titleResId: Int,
        val exception: Throwable
    ) : PreferredViewEvent

    data class ShowBackupRestorePreview(
        val preview: BackupRestorePreview
    ) : PreferredViewEvent

    data class ShowBackupValidationError(
        val issues: List<BackupValidationIssue>
    ) : PreferredViewEvent
}

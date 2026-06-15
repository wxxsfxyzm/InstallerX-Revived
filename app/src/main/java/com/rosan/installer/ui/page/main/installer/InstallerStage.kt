// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer

import android.graphics.Bitmap
import com.rosan.installer.domain.archive.model.UnarchiveStatus
import com.rosan.installer.domain.session.model.ConfirmationRequestType
import com.rosan.installer.domain.session.model.InstallResult

sealed class InstallerStage {
    data object Ready : InstallerStage()

    data object Resolving : InstallerStage()
    data object ResolveFailed : InstallerStage()

    // The new state for caching files, now with progress.
    data class Preparing(val progress: Float) : InstallerStage()

    data object Analysing : InstallerStage()
    data object AnalyseFailed : InstallerStage()

    data object InstallChoice : InstallerStage()
    data object InstallPrepare : InstallerStage()
    data object InstallExtendedMenu : InstallerStage()
    data object InstallExtendedSubMenu : InstallerStage()
    data class Installing(val progress: Float, val current: Int, val total: Int, val appLabel: String?) : InstallerStage()
    data class InstallingModule(val output: List<String>, val isFinished: Boolean = false) : InstallerStage()
    data object InstallWaitingUnknownSource : InstallerStage()
    data object InstallSuccess : InstallerStage()
    data object InstallFailed : InstallerStage()
    data object InstallRetryDowngradeUsingUninstall : InstallerStage()
    data class InstallCompleted(val results: List<InstallResult>) : InstallerStage()
    data class InstallConfirm(
        val appLabel: CharSequence,
        val appIcon: Bitmap?,
        val packageName: String,
        val sessionId: Int,
        val isSelfSession: Boolean,
        val isOwnershipConflict: Boolean,
        val sourceAppLabel: CharSequence?,
        val requestType: ConfirmationRequestType
    ) : InstallerStage()

    data object UninstallReady : InstallerStage()
    data object UninstallResolveFailed : InstallerStage()
    data object Uninstalling : InstallerStage()
    data object UninstallSuccess : InstallerStage()
    data object UninstallFailed : InstallerStage()

    data class UnarchiveReady(
        val packageName: String,
        val appLabel: CharSequence,
        val installerLabel: CharSequence
    ) : InstallerStage()

    data object Unarchiving : InstallerStage()

    data class UnarchiveError(
        val status: UnarchiveStatus,
        val requiredBytes: Long,
        val installerLabel: CharSequence?
    ) : InstallerStage()

    data object UnarchiveFailed : InstallerStage()
}

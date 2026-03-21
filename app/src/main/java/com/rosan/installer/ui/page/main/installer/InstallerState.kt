// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.rosan.installer.domain.session.model.UninstallInfo
import com.rosan.installer.domain.settings.model.NamedPackage

/**
 * Represents the entire UI state for the Installer screen.
 * Follows Unidirectional Data Flow (UDF) principles.
 */
data class InstallerState(
    // The core state machine step
    val stage: InstallerStage = InstallerStage.Ready,

    // UI specific toggles and settings
    val viewSettings: InstallerViewSettings = InstallerViewSettings(),
    val showMiuixSheetRightActionSettings: Boolean = false,
    val showMiuixPermissionList: Boolean = false,
    val navigatedFromPrepareToChoice: Boolean = false,

    // Visual data
    val currentPackageName: String? = null,
    val displayIcons: Map<String, ImageBitmap?> = emptyMap(),
    val seedColor: Color? = null,

    // Configuration data
    val installFlags: Int = 0,
    val defaultInstallerFromSettings: String? = null,
    val managedInstallerPackages: List<NamedPackage> = emptyList(),
    val selectedInstaller: String? = null,

    // User configuration
    val availableUsers: Map<Int, String> = emptyMap(),
    val selectedUserId: Int = 0,

    // Uninstallation specific data
    val uiUninstallInfo: UninstallInfo? = null,
    val uninstallFlags: Int = 0
) {
    /**
     * Determines if the dialog can be dismissed by tapping the scrim.
     * Dismissal is disallowed during ongoing operations like installing.
     */
    val isDismissible: Boolean
        get() = when (stage) {
            is InstallerStage.Analysing,
            is InstallerStage.Resolving,
            is InstallerStage.InstallExtendedMenu,
            is InstallerStage.InstallChoice,
            is InstallerStage.Uninstalling -> false

            is InstallerStage.InstallingModule -> stage.isFinished
            is InstallerStage.InstallPrepare -> !(showMiuixSheetRightActionSettings || showMiuixPermissionList)
            is InstallerStage.Preparing,
            is InstallerStage.Installing -> !viewSettings.disableNotificationOnDismiss

            else -> true
        }
}

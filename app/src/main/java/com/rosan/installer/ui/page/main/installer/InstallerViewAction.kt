// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer

import androidx.annotation.StringRes
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.session.model.SelectInstallEntity
import com.rosan.installer.domain.session.repository.InstallerSessionRepository

sealed class InstallerViewAction {
    data class CollectSession(val session: InstallerSessionRepository) : InstallerViewAction()
    data object Close : InstallerViewAction()
    data object Analyse : InstallerViewAction()
    data object InstallChoice : InstallerViewAction()
    data object InstallExtendedMenu : InstallerViewAction()
    data object InstallExtendedSubMenu : InstallerViewAction()

    /**
     * Install multiple module/apk
     *
     * **This ViewAction will forward to ActionHandler to actually process**
     * @see com.rosan.installer.data.session.repository.InstallerSessionRepositoryImpl.Action.InstallMultiple
     */
    data object InstallMultiple : InstallerViewAction()
    data object InstallPrepare : InstallerViewAction()

    /**
     * Install single module/apk
     *
     * **This ViewAction will forward to ActionHandler to actually process**
     *
     * @param triggerAuth request or not request user biometric auth
     * @see com.rosan.installer.data.session.repository.InstallerSessionRepositoryImpl.Action.Install
     */
    data class Install(val triggerAuth: Boolean) : InstallerViewAction()
    data object Background : InstallerViewAction()
    data object Cancel : InstallerViewAction()
    data class Reboot(val reason: String) : InstallerViewAction()

    data object Uninstall : InstallerViewAction()

    data object ShowMiuixSheetRightActionSettings : InstallerViewAction()
    data object HideMiuixSheetRightActionSettings : InstallerViewAction()
    data object ShowMiuixPermissionList : InstallerViewAction()
    data object HideMiuixPermissionList : InstallerViewAction()

    data class SetTempShowOPPOSpecial(val show: Boolean) : InstallerViewAction()

    /**
     * Toggles the selection state of the current app.
     */
    data class ToggleSelection(val packageName: String, val entity: SelectInstallEntity, val isMultiSelect: Boolean) :
        InstallerViewAction()

    /**
     * Sets the installer package name.
     * @param installer The installer package name.
     */
    data class SetInstaller(val installer: String?) : InstallerViewAction()

    /**
     * Sets the target user ID.
     * @param userId The target user ID.
     */
    data class SetTargetUser(val userId: Int) : InstallerViewAction()

    /**
     * Approves or denies the installation session.
     * @param sessionId The ID of the session to approve/deny.
     * @param granted True to approve, false to deny.
     */
    data class ApproveSession(val sessionId: Int, val granted: Boolean) : InstallerViewAction()

    /**
     * Toggles a specific flag for the uninstallation process.
     * @param flag The flag to toggle, e.g., DELETE_KEEP_DATA.
     * @param enable true to add the flag, false to remove it.
     */
    data class ToggleUninstallFlag(val flag: Int, val enable: Boolean) : InstallerViewAction()

    /**
     * Triggers the uninstallation process with the option to keep data.
     * @param keepData true to keep data, false to delete it.
     * @param conflictingPackage The package name of the conflicting app, if any.
     */
    data class UninstallAndRetryInstall(val keepData: Boolean, val conflictingPackage: String? = null) : InstallerViewAction()

    /**
     * Share the selected app file.
     * @param appEntity The app entity containing the file to share.
     */
    data class ShareApp(val appEntity: AppEntity) : InstallerViewAction()

    /**
     * Triggers a toast message using a String.
     * @param message The message to display.
     */
    data class ShowToast(val message: String) : InstallerViewAction()

    /**
     * Triggers a toast message using a String Resource ID.
     * @param messageResId The resource ID of the message to display.
     */
    data class ShowToastRes(@param:StringRes val messageResId: Int) : InstallerViewAction()
}

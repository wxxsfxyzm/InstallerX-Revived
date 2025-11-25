package com.rosan.installer.ui.page.main.installer.dialog

import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.installer.repo.InstallerRepo

sealed class InstallerViewAction {
    data class CollectRepo(val repo: InstallerRepo) : InstallerViewAction()
    data object Close : InstallerViewAction()
    data object Analyse : InstallerViewAction()
    data object InstallChoice : InstallerViewAction()
    data object InstallExtendedMenu : InstallerViewAction()
    data object InstallExtendedSubMenu : InstallerViewAction()
    data object InstallMultiple : InstallerViewAction()
    data object InstallPrepare : InstallerViewAction()
    data object Install : InstallerViewAction()
    data object Background : InstallerViewAction()

    /** Triggers the uninstallation process. */
    data object Uninstall : InstallerViewAction()

    data object ShowMiuixSheetRightActionSettings : InstallerViewAction()
    data object HideMiuixSheetRightActionSettings : InstallerViewAction()
    data object ShowMiuixPermissionList : InstallerViewAction()
    data object HideMiuixPermissionList : InstallerViewAction()

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
     */
    data class UninstallAndRetryInstall(val keepData: Boolean) : InstallerViewAction()
}
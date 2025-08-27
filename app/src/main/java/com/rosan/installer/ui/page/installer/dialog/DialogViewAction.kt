package com.rosan.installer.ui.page.installer.dialog

import com.rosan.installer.data.installer.repo.InstallerRepo

sealed class DialogViewAction {
    data class CollectRepo(val repo: InstallerRepo) : DialogViewAction()
    data object Close : DialogViewAction()
    data object Analyse : DialogViewAction()
    data object InstallChoice : DialogViewAction()
    data object InstallExtendedMenu : DialogViewAction()
    data object InstallExtendedSubMenu : DialogViewAction()
    data object InstallMultiple : DialogViewAction()
    data object InstallPrepare : DialogViewAction()
    data object Install : DialogViewAction()
    data object Background : DialogViewAction()

    /** Triggers the uninstallation process. */
    data object Uninstall : DialogViewAction()

    data class SetInstaller(val installer: String?) : DialogViewAction()

    /**
     * Toggles a specific flag for the uninstallation process.
     * @param flag The flag to toggle, e.g., DELETE_KEEP_DATA.
     * @param enable true to add the flag, false to remove it.
     */
    data class ToggleUninstallFlag(val flag: Int, val enable: Boolean) : DialogViewAction()
    data class UninstallAndRetryInstall(val keepData: Boolean) : DialogViewAction()
}
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

    data class UninstallAndRetryInstall(val keepData: Boolean) : DialogViewAction()
}
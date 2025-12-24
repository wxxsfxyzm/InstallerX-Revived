package com.rosan.installer.ui.page.miuix.settings

sealed class MiuixSettingsScreen(val route: String) {
    data object MiuixMain : MiuixSettingsScreen("main")
    data object MiuixEditConfig : MiuixSettingsScreen("config/edit?id={id}")
    data object MiuixApplyConfig : MiuixSettingsScreen("config/apply?id={id}")
    data object MiuixAbout : MiuixSettingsScreen("about")
    data object MiuixTheme : MiuixSettingsScreen("theme")
    data object MiuixInstallerGlobal : MiuixSettingsScreen("installerGlobal")
    data object MiuixUninstallerGlobal : MiuixSettingsScreen("uninstallerGlobal")
    data object MiuixLab : MiuixSettingsScreen("lab")


    sealed class Builder(val route: String) {
        data object MiuixMain : MiuixSettingsScreen("main")
        class MiuixEditConfig(id: Long? = null) : MiuixSettingsScreen(
            "config/edit?id=${id ?: -1}"
        )

        class MiuixApplyConfig(id: Long) : MiuixSettingsScreen(
            "config/apply?id=$id"
        )
    }
}

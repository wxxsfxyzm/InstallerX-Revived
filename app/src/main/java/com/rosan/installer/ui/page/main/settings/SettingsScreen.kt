package com.rosan.installer.ui.page.main.settings

sealed class SettingsScreen(val route: String) {
    data object Main : SettingsScreen("main")
    data object EditConfig : SettingsScreen("config/edit?id={id}")
    data object ApplyConfig : SettingsScreen("config/apply?id={id}")
    data object About : SettingsScreen("about")
    data object OpenSourceLicense : SettingsScreen("ossLicense")
    data object Theme : SettingsScreen("theme")
    data object InstallerGlobal : SettingsScreen("installerGlobal")
    data object UninstallerGlobal : SettingsScreen("uninstallerGlobal")
    data object Lab : SettingsScreen("lab")

    sealed class Builder(val route: String) {
        data object Main : SettingsScreen("main")
        class EditConfig(id: Long? = null) : SettingsScreen(
            "config/edit?id=${id ?: -1}"
        )

        class ApplyConfig(id: Long) : SettingsScreen(
            "config/apply?id=$id"
        )
    }
}

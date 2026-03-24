package com.rosan.installer.ui.page.main.settings.preferred

import com.rosan.installer.domain.settings.model.Authorizer

data class PreferredViewState(
    val useBlur: Boolean = true,
    val authorizer: Authorizer = Authorizer.Shizuku,
    val customizeAuthorizer: String = "",
    val adbVerifyEnabled: Boolean = true,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val autoLockInstaller: Boolean = false,
    val hasUpdate: Boolean = false,
    val remoteVersion: String = ""
) {
    val authorizerCustomize = authorizer == Authorizer.Customize
}

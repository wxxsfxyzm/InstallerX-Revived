package com.rosan.installer.ui.page.main.settings.preferred

sealed class PreferredViewEvent {
    data class ShowDefaultInstallerResult(val message: String) : PreferredViewEvent()
    data class ShowDefaultInstallerErrorDetail(
        val title: String,
        val exception: Throwable,
        val retryAction: PreferredViewAction
    ) :
        PreferredViewEvent()

    data object ShowUpdateLoading : PreferredViewEvent()
    data object HideUpdateLoading : PreferredViewEvent()
    data class ShowInAppUpdateErrorDetail(
        val title: String,
        val exception: Throwable
    ) : PreferredViewEvent()
}
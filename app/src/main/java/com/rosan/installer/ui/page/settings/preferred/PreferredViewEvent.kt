package com.rosan.installer.ui.page.settings.preferred

sealed class PreferredViewEvent {
    data class ShowSnackbar(val message: String) : PreferredViewEvent()
    data class ShowErrorDialog(val title: String, val exception: Throwable, val retryAction: PreferredViewAction) :
        PreferredViewEvent()
}
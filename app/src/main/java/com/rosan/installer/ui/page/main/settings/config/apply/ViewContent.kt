package com.rosan.installer.ui.page.main.settings.config.apply

data class ViewContent<T>(
    val data: T,
    val progress: Progress
) {
    sealed class Progress {
        object Loading : Progress()
        object Loaded : Progress()
    }
}
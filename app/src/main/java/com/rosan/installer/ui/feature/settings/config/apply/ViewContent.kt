package com.rosan.installer.ui.feature.settings.config.apply

data class ViewContent<T>(val data: T, val progress: Progress) {
    sealed interface Progress {
        object Loading : Progress
        object Loaded : Progress
    }
}

package com.rosan.installer.ui.page.main.settings.config.all

import com.rosan.installer.domain.settings.model.config.DeletedConfigSnapshot

sealed interface AllViewEvent {
    data class DeletedConfig(val snapshot: DeletedConfigSnapshot) : AllViewEvent

    // Event to trigger navigation to the edit page
    data class NavigateToEditConfig(val id: Long) : AllViewEvent

    // Event to trigger navigation to the apply page
    data class NavigateToApplyConfig(val id: Long) : AllViewEvent
}

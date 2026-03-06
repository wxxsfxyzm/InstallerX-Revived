package com.rosan.installer.ui.page.main.settings.config.all

import com.rosan.installer.domain.settings.model.ConfigModel

sealed class AllViewEvent {
    data class DeletedConfig(val configModel: ConfigModel) : AllViewEvent()
}

package com.rosan.installer.ui.page.main.settings.config.all

import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.model.config.DeletedConfigSnapshot
import com.rosan.installer.domain.settings.util.ConfigOrder

sealed interface AllViewAction {
    data object LoadData : AllViewAction
    data object UserReadScopeTips : AllViewAction
    data class ChangeDataConfigOrder(val configOrder: ConfigOrder) : AllViewAction
    data class DeleteDataConfig(val configModel: ConfigModel) : AllViewAction
    data class RestoreDataConfig(val snapshot: DeletedConfigSnapshot) : AllViewAction
    data class EditDataConfig(val configModel: ConfigModel) : AllViewAction
    data class ApplyConfig(val configModel: ConfigModel) : AllViewAction
}

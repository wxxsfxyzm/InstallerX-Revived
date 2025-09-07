package com.rosan.installer.ui.page.main.settings.config.all

import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.util.ConfigOrder

sealed class AllViewAction {
    object Init : AllViewAction()
    object LoadData : AllViewAction()
    data class ChangeDataConfigOrder(val configOrder: ConfigOrder) : AllViewAction()
    data class DeleteDataConfig(val configEntity: ConfigEntity) : AllViewAction()
    data class RestoreDataConfig(val configEntity: ConfigEntity) : AllViewAction()
    data class EditDataConfig(val configEntity: ConfigEntity) : AllViewAction()
    data class MiuixEditDataConfig(val configEntity: ConfigEntity) : AllViewAction()
    data class ApplyConfig(val configEntity: ConfigEntity) : AllViewAction()
}
package com.rosan.installer.ui.page.main.settings.config.all

import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.util.ConfigOrder
import com.rosan.installer.data.settings.util.OrderType

data class AllViewState(
    val data: Data = Data(),
    val userReadScopeTips: Boolean = false,
) {
    data class Data(
        val configs: List<ConfigEntity> = emptyList(),
        val configOrder: ConfigOrder = ConfigOrder.Id(OrderType.Ascending),
        val progress: Progress = Progress.Loading
    ) {
        sealed class Progress {
            object Loading : Progress()
            object Loaded : Progress()
        }
    }
}
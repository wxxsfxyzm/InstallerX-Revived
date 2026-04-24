// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.config.all

import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.util.ConfigOrder
import com.rosan.installer.domain.settings.util.OrderType

data class AllViewState(
    val data: Data = Data(),
    val userReadScopeTips: Boolean = false,
) {
    data class Data(
        val configs: List<ConfigModel> = emptyList(),
        val configOrder: ConfigOrder = ConfigOrder.Id(OrderType.Ascending),
        val progress: Progress = Progress.Loading
    ) {
        sealed class Progress {
            data object Loading : Progress()
            data object Loaded : Progress()
        }
    }
}

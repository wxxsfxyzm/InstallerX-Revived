// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.config.apply

import androidx.compose.ui.graphics.ImageBitmap
import com.rosan.installer.domain.settings.model.AppModel

data class ApplyViewState(
    // App data state
    val apps: ViewContent<List<ApplyViewApp>> = ViewContent(
        data = emptyList(), progress = ViewContent.Progress.Loading
    ),
    val appEntities: ViewContent<List<AppModel>> = ViewContent(
        data = emptyList(), progress = ViewContent.Progress.Loading
    ),

    val checkedApps: List<ApplyViewApp> = emptyList(),

    val displayIcons: Map<String, ImageBitmap?> = emptyMap(),

    val orderType: OrderType = OrderType.Label,
    val orderInReverse: Boolean = false,
    val selectedFirst: Boolean = true,
    val showSystemApp: Boolean = false,
    val showPackageName: Boolean = true,
    val search: String = "",

    // UI State
    val useBlur: Boolean = true,
) {
    enum class OrderType {
        Label, PackageName, FirstInstallTime
    }
}

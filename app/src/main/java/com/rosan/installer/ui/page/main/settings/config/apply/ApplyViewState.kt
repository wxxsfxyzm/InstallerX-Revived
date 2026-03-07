// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.config.apply

import com.rosan.installer.domain.settings.model.AppModel
import com.rosan.installer.ui.common.ViewContent

data class ApplyViewState(
    val apps: ViewContent<List<ApplyViewApp>> = ViewContent(
        data = emptyList(), progress = ViewContent.Progress.Loading
    ),
    val appEntities: ViewContent<List<AppModel>> = ViewContent(
        data = emptyList(), progress = ViewContent.Progress.Loading
    ),
    val orderType: OrderType = OrderType.Label,
    val orderInReverse: Boolean = false,
    val selectedFirst: Boolean = true,
    val showSystemApp: Boolean = false,
    val showPackageName: Boolean = true,
    val search: String = ""
) {
    enum class OrderType {
        Label, PackageName, FirstInstallTime
    }

    // Use get() to prevent blocking the thread immediately on every state.copy() call
    val checkedApps: List<ApplyViewApp>
        get() {
            val rawApps = apps.data
            if (rawApps.isEmpty()) return emptyList()

            // 1. Core performance optimization: Pre-build an O(1) hash set to replace the expensive List.find()
            val selectedPackages = appEntities.data.mapNotNull { it.packageName }.toSet()

            // 2. Memory optimization: Use ignoreCase = true to filter, eliminating memory allocation from String.lowercase()
            val filtered = rawApps.filter { app ->
                val matchSystem = showSystemApp || !app.isSystemApp
                val matchSearch = search.isEmpty() ||
                        app.packageName.contains(search, ignoreCase = true) ||
                        (app.label?.contains(search, ignoreCase = true) == true)
                matchSystem && matchSearch
            }

            // 3. Build sorting conditions
            val comparators = mutableListOf<(ApplyViewApp) -> Comparable<*>?>()

            // Priority 1: Selected apps first
            if (selectedFirst) {
                comparators.add { app ->
                    // Return 0 to put it at the front, 1 to put it at the back
                    if (selectedPackages.contains(app.packageName)) 0 else 1
                }
            }

            // Helper function to get the corresponding property
            val getProperty: (ApplyViewApp, OrderType) -> Comparable<*>? = { app, type ->
                when (type) {
                    OrderType.Label -> app.label ?: ""
                    OrderType.PackageName -> app.packageName
                    OrderType.FirstInstallTime -> app.firstInstallTime
                }
            }

            // Priority 2: Currently selected main sort order
            comparators.add { app -> getProperty(app, orderType) }

            // Priority 3: Other properties as fallback sorting to prevent random order when main sort properties are identical
            OrderType.entries.filter { it != orderType }.forEach { fallbackType ->
                comparators.add { app -> getProperty(app, fallbackType) }
            }

            // 4. Execute final sorting
            var finalComparator = compareBy(*comparators.toTypedArray())
            if (orderInReverse) {
                finalComparator = finalComparator.reversed()
            }

            return filtered.sortedWith(finalComparator)
        }
}

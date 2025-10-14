package com.rosan.installer.ui.page.main.settings.config.apply

import com.rosan.installer.data.settings.model.room.entity.AppEntity
import com.rosan.installer.ui.common.ViewContent

data class ApplyViewState(
    val apps: ViewContent<List<ApplyViewApp>> = ViewContent(
        data = emptyList(), progress = ViewContent.Progress.Loading
    ),
    val appEntities: ViewContent<List<AppEntity>> = ViewContent(
        data = emptyList(), progress = ViewContent.Progress.Loading
    ),
    val userReadScopeTips: Boolean = false,
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

    private fun contains(v1: String, v2: String): Boolean = v1.lowercase().contains(v2.lowercase())

    val checkedApps: List<ApplyViewApp> = apps.data.run {
        if (search.isEmpty()) this
        else filter { contains(it.packageName, search) || contains(it.label ?: "", search) }
    }.run {
        if (showSystemApp) this
        else filter { !it.isSystemApp }
    }.run {
        data class OrderData(
            val type: OrderType,
            val comparator: (ApplyViewApp) -> Comparable<*>?
        )

        val selectors = listOf(
            OrderData(OrderType.Label) { it.label },
            OrderData(OrderType.PackageName) { it.packageName },
            OrderData(OrderType.FirstInstallTime) { it.firstInstallTime }
        ).sortedBy {
            // selected order type first
            if (it.type == orderType) Int.MIN_VALUE
            else it.type.ordinal
        }.map {
            it.comparator
        }.run {
            // selected app first
            if (!selectedFirst) this
            else listOf<(ApplyViewApp) -> Comparable<*>?> { app ->
                appEntities.data.find { it.packageName == app.packageName } == null
            } + this
        }.toTypedArray()
        if (!orderInReverse) sortedWith { a, b -> compareValuesBy(a, b, selectors = selectors) }
        else sortedWith { a, b -> compareValuesBy(b, a, selectors = selectors) }
    }
}
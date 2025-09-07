package com.rosan.installer.ui.page.main.settings.config.apply

sealed class ApplyViewAction {
    object Init : ApplyViewAction()
    object LoadApps : ApplyViewAction()
    object LoadAppEntities : ApplyViewAction()
    data class ApplyPackageName(
        val packageName: String?,
        val applied: Boolean
    ) : ApplyViewAction()

    data class Order(val type: ApplyViewState.OrderType) : ApplyViewAction()
    data class OrderInReverse(val enabled: Boolean) : ApplyViewAction()
    data class SelectedFirst(val enabled: Boolean) : ApplyViewAction()
    data class ShowSystemApp(val enabled: Boolean) : ApplyViewAction()
    data class ShowPackageName(val enabled: Boolean) : ApplyViewAction()

    data class Search(val text: String) : ApplyViewAction()
}
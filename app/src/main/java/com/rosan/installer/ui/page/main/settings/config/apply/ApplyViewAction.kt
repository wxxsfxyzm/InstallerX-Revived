package com.rosan.installer.ui.page.main.settings.config.apply

sealed class ApplyViewAction {
    data object Init : ApplyViewAction()
    data object LoadApps : ApplyViewAction()
    data object LoadAppEntities : ApplyViewAction()
    data class ApplyPackageName(
        val packageName: String?,
        val applied: Boolean
    ) : ApplyViewAction()

    data object UserReadScopeTips : ApplyViewAction()
    data class Order(val type: ApplyViewState.OrderType) : ApplyViewAction()
    data class OrderInReverse(val enabled: Boolean) : ApplyViewAction()
    data class SelectedFirst(val enabled: Boolean) : ApplyViewAction()
    data class ShowSystemApp(val enabled: Boolean) : ApplyViewAction()
    data class ShowPackageName(val enabled: Boolean) : ApplyViewAction()

    data class Search(val text: String) : ApplyViewAction()
}
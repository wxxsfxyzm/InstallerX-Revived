package com.rosan.installer.data.settings.util

sealed class ConfigOrder(val orderType: OrderType) {
    class Id(orderType: OrderType) : ConfigOrder(orderType)
    class Name(orderType: OrderType) : ConfigOrder(orderType)
    class CreatedAt(orderType: OrderType) : ConfigOrder(orderType)
    class ModifiedAt(orderType: OrderType) : ConfigOrder(orderType)
}

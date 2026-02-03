package com.rosan.installer.data.settings.util

sealed class AppOrder(val orderType: OrderType) {
    class Id(orderType: OrderType) : AppOrder(orderType)
    class PackageName(orderType: OrderType) : AppOrder(orderType)
    class ConfigId(orderType: OrderType) : AppOrder(orderType)
    class CreateAt(orderType: OrderType) : AppOrder(orderType)
    class ModifiedAt(orderType: OrderType) : AppOrder(orderType)
}
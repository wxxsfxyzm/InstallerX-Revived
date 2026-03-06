// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.domain.settings.util

sealed class AppOrder(val orderType: OrderType) {
    class Id(orderType: OrderType) : AppOrder(orderType)
    class PackageName(orderType: OrderType) : AppOrder(orderType)
    class ConfigId(orderType: OrderType) : AppOrder(orderType)
    class CreateAt(orderType: OrderType) : AppOrder(orderType)
    class ModifiedAt(orderType: OrderType) : AppOrder(orderType)
}
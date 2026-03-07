// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.domain.settings.util

sealed class ConfigOrder(val orderType: OrderType) {
    class Id(orderType: OrderType) : ConfigOrder(orderType)
    class Name(orderType: OrderType) : ConfigOrder(orderType)
    class CreatedAt(orderType: OrderType) : ConfigOrder(orderType)
    class ModifiedAt(orderType: OrderType) : ConfigOrder(orderType)
}
// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.data.device.provider.AndroidPermissionChecker
import com.rosan.installer.data.device.provider.DeviceCapabilityProviderImpl
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.device.provider.PermissionChecker
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val deviceModule = module {
    singleOf(::AndroidPermissionChecker) { bind<PermissionChecker>() }
    singleOf(::DeviceCapabilityProviderImpl) { bind<DeviceCapabilityProvider>() }
}

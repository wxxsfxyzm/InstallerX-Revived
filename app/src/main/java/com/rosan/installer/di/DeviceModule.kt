// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.data.device.provider.DeviceCapabilityProviderImpl
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val deviceModule = module {
    single<DeviceCapabilityProvider> { DeviceCapabilityProviderImpl(androidContext(), get()) }
}

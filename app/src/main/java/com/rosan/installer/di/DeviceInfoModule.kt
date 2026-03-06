// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.build.model.impl.DeviceCapabilityChecker
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val deviceCapabilityCheckerModule = module { single { DeviceCapabilityChecker(androidContext()) } }
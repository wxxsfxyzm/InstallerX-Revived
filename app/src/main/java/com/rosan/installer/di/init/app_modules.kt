// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.di.init

import com.rosan.installer.di.coreModule
import com.rosan.installer.di.deviceModule
import com.rosan.installer.di.engineModule
import com.rosan.installer.di.installerModule
import com.rosan.installer.di.logModule
import com.rosan.installer.di.networkModule
import com.rosan.installer.di.privilegedModule
import com.rosan.installer.di.serializationModule
import com.rosan.installer.di.settingsModule
import com.rosan.installer.di.updateModule
import com.rosan.installer.di.viewModelModule

val appModules = listOf(
    viewModelModule,
    serializationModule,
    installerModule,
    coreModule,
    settingsModule,
    engineModule,
    networkModule,
    updateModule,
    deviceModule,
    logModule,
    privilegedModule
)

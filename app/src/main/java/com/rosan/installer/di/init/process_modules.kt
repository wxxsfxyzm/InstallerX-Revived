// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.di.init

import com.rosan.installer.di.coreModule
import com.rosan.installer.di.serializationModule

val processModules = listOf(
    serializationModule,
    coreModule
)



// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.framework.privileged.core.context.hook

import android.os.IBinder
import android.provider.Settings
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.core.reflection.getStaticValue
import com.rosan.installer.core.reflection.getValue
import java.lang.reflect.Field

data class SettingsReflectionInfo(
    val provider: Any,
    val remoteField: Field,
    val originalBinder: IBinder
)

fun ReflectionProvider.resolveSettingsBinder(): SettingsReflectionInfo? {
    val holder = this.getStaticValue<Any>("sProviderHolder", Settings.Global::class.java) ?: return null
    val provider = this.getValue<Any>(holder, "mContentProvider") ?: return null

    val remoteField = this.getDeclaredField("mRemote", provider.javaClass) ?: return null
    val originalBinder = remoteField.get(provider) as? IBinder ?: return null

    return SettingsReflectionInfo(provider, remoteField, originalBinder)
}

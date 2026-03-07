package com.rosan.installer.domain.settings.model

import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider

fun ConfigModel.isPrivileged(deviceCapabilityProvider: DeviceCapabilityProvider): Boolean {
    return when (authorizer) {
        Authorizer.Root, Authorizer.Shizuku -> true
        Authorizer.None -> deviceCapabilityProvider.isSystemApp
        else -> false
    }
}

val ConfigModel.isCustomizeAuthorizer: Boolean
    get() = authorizer == Authorizer.Customize
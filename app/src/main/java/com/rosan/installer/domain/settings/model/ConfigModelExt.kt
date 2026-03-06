package com.rosan.installer.domain.settings.model

import com.rosan.installer.util.OSUtils

val ConfigModel.isPrivileged: Boolean
    get() = when (authorizer) {
        Authorizer.Root, Authorizer.Shizuku -> true
        Authorizer.None -> OSUtils.isSystemApp
        else -> false
    }

val ConfigModel.isCustomizeAuthorizer: Boolean
    get() = authorizer == Authorizer.Customize
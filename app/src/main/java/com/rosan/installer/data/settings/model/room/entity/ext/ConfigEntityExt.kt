package com.rosan.installer.data.settings.model.room.entity.ext

import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity.Authorizer
import com.rosan.installer.util.OSUtils

val ConfigEntity.isPrivileged: Boolean
    get() = when (authorizer) {
        Authorizer.Root, Authorizer.Shizuku -> true
        Authorizer.None -> OSUtils.isSystemApp
        else -> false
    }

val ConfigEntity.isCustomizeAuthorizer: Boolean
    get() = authorizer == Authorizer.Customize
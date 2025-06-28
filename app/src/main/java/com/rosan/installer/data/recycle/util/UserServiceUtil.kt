package com.rosan.installer.data.recycle.util

import com.rosan.installer.IPrivilegedService
import com.rosan.installer.data.recycle.model.entity.DefaultPrivilegedService
import com.rosan.installer.data.recycle.model.impl.DhizukuUserServiceRecycler
import com.rosan.installer.data.recycle.model.impl.ProcessUserServiceRecyclers
import com.rosan.installer.data.recycle.model.impl.ShizukuUserServiceRecycler
import com.rosan.installer.data.recycle.repo.recyclable.UserService
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import timber.log.Timber

private object DefaultUserService : UserService {
    override val privileged: IPrivilegedService = DefaultPrivilegedService()

    override fun close() {
    }
}

fun useUserService(
    config: ConfigEntity,
    special: (() -> String?)? = null,
    action: (UserService) -> Unit
) {
    // special为null，或special.invoke()时，遵循config
    val recycler = if (special?.invoke() == null) when (config.authorizer) {
        ConfigEntity.Authorizer.Root -> ProcessUserServiceRecyclers.get("su").make()
        ConfigEntity.Authorizer.Shizuku -> ShizukuUserServiceRecycler.make()
        ConfigEntity.Authorizer.Dhizuku -> DhizukuUserServiceRecycler.make()
        ConfigEntity.Authorizer.Customize -> ProcessUserServiceRecyclers.get(config.customizeAuthorizer)
            .make()
        // 其余情况，不使用授权器
        else -> null
    } else {
        // special回调null时，不使用授权器
        ProcessUserServiceRecyclers.get(special.invoke()!!).make()
    }
    if (recycler != null) {
        Timber.tag("useUserService").e("use ${config.authorizer} Privileged Service: $recycler")
        recycler.use { action.invoke(it.entity) }
    } else {
        Timber.tag("useUserService").e("Use Default User Service")
        action.invoke(DefaultUserService)
    }
}

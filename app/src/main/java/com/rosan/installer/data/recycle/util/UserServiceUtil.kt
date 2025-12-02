package com.rosan.installer.data.recycle.util

import com.rosan.installer.IPrivilegedService
import com.rosan.installer.data.recycle.model.entity.DefaultPrivilegedService
import com.rosan.installer.data.recycle.model.exception.ShizukuNotWorkException
import com.rosan.installer.data.recycle.model.impl.recycler.DhizukuUserServiceRecycler
import com.rosan.installer.data.recycle.model.impl.recycler.ProcessUserServiceRecyclers
import com.rosan.installer.data.recycle.model.impl.recycler.ShizukuHookRecycler
import com.rosan.installer.data.recycle.model.impl.recycler.ShizukuUserServiceRecycler
import com.rosan.installer.data.recycle.repo.Recyclable
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
    useShizukuHookMode: Boolean = false,
    special: (() -> String?)? = null,
    action: (UserService) -> Unit
) {
    // special为null，或special.invoke()时，遵循config
    val recycler = getRecyclableInstance(config.authorizer, config.customizeAuthorizer, useShizukuHookMode, special)
    if (recycler != null) {
        Timber.tag("useUserService").e("use ${config.authorizer} Privileged Service: $recycler")
        recycler.use { action.invoke(it.entity) }
    } else {
        Timber.tag("useUserService").e("Use Default User Service")
        action.invoke(DefaultUserService)
    }
}

/**
 * Overloaded function that accepts Authorizer type and customize string directly.
 * This is ideal for callers like ViewModels that don't have a full ConfigEntity.
 *
 * @param authorizer The type of authorizer to use.
 * @param customizeAuthorizer The command string for the customize authorizer, used only when authorizer is Customize.
 * @param useShizukuHookMode Whether to use Shizuku hook mode.
 * @param special An optional lambda to override the authorizer logic.
 * @param action The action to perform with the user service.
 */
fun useUserService(
    authorizer: ConfigEntity.Authorizer,
    customizeAuthorizer: String = "",
    useShizukuHookMode: Boolean = false,
    special: (() -> String?)? = null,
    action: (UserService) -> Unit
) {
    val recycler = getRecyclableInstance(authorizer, customizeAuthorizer, useShizukuHookMode, special)
    try {
        if (recycler != null) {
            Timber.tag("useUserService").e("use $authorizer Privileged Service: $recycler")
            recycler.use { action.invoke(it.entity) }
        } else {
            Timber.tag("useUserService").e("Use Default User Service")
            action.invoke(DefaultUserService)
        }
    } catch (e: IllegalStateException) {
        // Specifically catch the Shizuku runtime binder error for any privileged action.
        if (authorizer == ConfigEntity.Authorizer.Shizuku && e.message?.contains("binder haven't been received") == true) {
            throw ShizukuNotWorkException("Shizuku service connection lost during privileged action.", e)
        }
        // Re-throw any other exception.
        throw e
    }
}

/**
 * Core private function to select the correct recycler based on authorizer type.
 */
private fun getRecyclableInstance(
    authorizer: ConfigEntity.Authorizer,
    customizeAuthorizer: String,
    useShizukuHookMode: Boolean,
    special: (() -> String?)?
): Recyclable<out UserService>? {
    // special為null，或special.invoke()時，遵循config
    Timber.tag("PrivilegedService").d("Authorizer: $authorizer")
    return if (special?.invoke() == null) when (authorizer) {
        ConfigEntity.Authorizer.Root -> ProcessUserServiceRecyclers.get("su").make()
        ConfigEntity.Authorizer.Shizuku -> {
            if (useShizukuHookMode) {
                Timber.tag("PrivilegedService").i("Using Shizuku Hook Mode (Experimental).")
                ShizukuHookRecycler.make()
            } else {
                Timber.tag("PrivilegedService").i("Using Shizuku UserService Mode (Default).")
                ShizukuUserServiceRecycler.make()
            }
        }

        ConfigEntity.Authorizer.Dhizuku -> DhizukuUserServiceRecycler.make()
        ConfigEntity.Authorizer.Customize -> ProcessUserServiceRecyclers.get(customizeAuthorizer).make()
        // 其余情况，不使用授权器
        else -> null
    } else {
        // special回调null时，不使用授权器
        ProcessUserServiceRecyclers.get(special.invoke()!!).make()
    }
}

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

private const val TAG = "PrivilegedService"

private object DefaultUserService : UserService {
    override val privileged: IPrivilegedService = DefaultPrivilegedService()
    override fun close() {}
}

fun useUserService(
    authorizer: ConfigEntity.Authorizer,
    customizeAuthorizer: String = "",
    useShizukuHookMode: Boolean = true,
    special: (() -> String?)? = null,
    action: (UserService) -> Unit
) {
    val recycler = getRecyclableInstance(authorizer, customizeAuthorizer, useShizukuHookMode, special)
    processRecycler(authorizer, recycler, action)
}

/**
 * Helper to process the recycler execution and error handling
 */
private fun processRecycler(
    authorizer: ConfigEntity.Authorizer,
    recycler: Recyclable<out UserService>?,
    action: (UserService) -> Unit
) {
    try {
        if (recycler != null) {
            Timber.tag(TAG).e("use $authorizer Privileged Service: $recycler")
            recycler.use { action.invoke(it.entity) }
        } else {
            Timber.tag(TAG).e("Use Default User Service")
            action.invoke(DefaultUserService)
        }
    } catch (e: IllegalStateException) {
        if (authorizer == ConfigEntity.Authorizer.Shizuku && e.message?.contains("binder haven't been received") == true) {
            throw ShizukuNotWorkException("Shizuku service connection lost during privileged action.", e)
        }
        throw e
    }
}

private fun getRecyclableInstance(
    authorizer: ConfigEntity.Authorizer,
    customizeAuthorizer: String,
    useShizukuHookMode: Boolean,
    special: (() -> String?)?
): Recyclable<out UserService>? {
    Timber.tag(TAG).d("Authorizer: $authorizer")

    // Check special logic first to reduce nesting
    val specialShell = special?.invoke()
    if (specialShell != null) {
        return ProcessUserServiceRecyclers.get(specialShell).make()
    }

    return when (authorizer) {
        ConfigEntity.Authorizer.Root -> ProcessUserServiceRecyclers.get("su").make()
        ConfigEntity.Authorizer.Shizuku -> {
            if (useShizukuHookMode) {
                Timber.tag(TAG).i("Using Shizuku Hook Mode.")
                ShizukuHookRecycler.make()
            } else {
                Timber.tag(TAG).i("Using Shizuku UserService Mode (Default).")
                ShizukuUserServiceRecycler.make()
            }
        }

        ConfigEntity.Authorizer.Dhizuku -> DhizukuUserServiceRecycler.make()
        ConfigEntity.Authorizer.Customize -> ProcessUserServiceRecyclers.get(customizeAuthorizer).make()
        else -> null
    }
}
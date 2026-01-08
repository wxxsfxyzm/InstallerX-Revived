package com.rosan.installer.data.recycle.util

import com.rosan.installer.IPrivilegedService
import com.rosan.installer.data.recycle.model.entity.DefaultPrivilegedService
import com.rosan.installer.data.recycle.model.exception.ShizukuNotWorkException
import com.rosan.installer.data.recycle.model.impl.recycler.DhizukuUserServiceRecycler
import com.rosan.installer.data.recycle.model.impl.recycler.ProcessHookRecycler
import com.rosan.installer.data.recycle.model.impl.recycler.ProcessUserServiceRecyclers
import com.rosan.installer.data.recycle.model.impl.recycler.ShizukuHookRecycler
import com.rosan.installer.data.recycle.model.impl.recycler.ShizukuUserServiceRecycler
import com.rosan.installer.data.recycle.repo.Recyclable
import com.rosan.installer.data.recycle.repo.recyclable.UserService
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.util.OSUtils
import timber.log.Timber

private const val TAG = "PrivilegedService"

private object DefaultUserService : UserService {
    override val privileged: IPrivilegedService = DefaultPrivilegedService()
    override fun close() {}
}

fun useUserService(
    authorizer: ConfigEntity.Authorizer,
    customizeAuthorizer: String = "",
    useHookMode: Boolean = true,
    special: (() -> String?)? = null,
    action: (UserService) -> Unit
) {
    if (authorizer == ConfigEntity.Authorizer.None) {
        if (OSUtils.isSystemApp) {
            Timber.tag(TAG).d("Running as System App with None Authorizer. Executing direct calls.")
            action.invoke(DefaultUserService)
        } else {
            Timber.tag(TAG).w("Authorizer is None but not running as System App. Privileged action skipped.")
        }
        return
    }

    val recycler = getRecyclableInstance(authorizer, customizeAuthorizer, useHookMode, special)
    processRecycler(authorizer, recycler, action)
}

private fun processRecycler(
    authorizer: ConfigEntity.Authorizer,
    recycler: Recyclable<out UserService>?,
    action: (UserService) -> Unit
) {
    try {
        if (recycler != null) {
            Timber.tag(TAG).d("Processing $authorizer with recycler: ${recycler.entity::class.java.simpleName}")
            recycler.use { action.invoke(it.entity) }
        } else {
            Timber.tag(TAG).e("No recycler found for $authorizer. Falling back to DefaultUserService.")
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
    useHookMode: Boolean,
    special: (() -> String?)?
): Recyclable<out UserService>? {
    val specialShell = special?.invoke()

    return when (authorizer) {
        ConfigEntity.Authorizer.None -> null

        ConfigEntity.Authorizer.Root -> {
            val targetShell = specialShell ?: SHELL_ROOT

            if (useHookMode) {
                Timber.tag(TAG).d("Using ProcessHookRecycler with shell: $targetShell")
                ProcessHookRecycler(targetShell).make()
            } else {
                Timber.tag(TAG).d("Using ProcessUserServiceRecycler with shell: $targetShell")
                ProcessUserServiceRecyclers.get(targetShell).make()
            }
        }

        ConfigEntity.Authorizer.Shizuku -> {
            if (useHookMode) {
                Timber.tag(TAG).i("Using Shizuku Hook Mode.")
                ShizukuHookRecycler.make()
            } else {
                Timber.tag(TAG).i("Using Shizuku UserService Mode (Default).")
                ShizukuUserServiceRecycler.make()
            }
        }

        ConfigEntity.Authorizer.Dhizuku -> DhizukuUserServiceRecycler.make()

        ConfigEntity.Authorizer.Customize -> {
            val targetShell = customizeAuthorizer.ifBlank { SHELL_ROOT }
            ProcessUserServiceRecyclers.get(targetShell).make()
        }

        else -> specialShell?.let { ProcessUserServiceRecyclers.get(it).make() }
    }
}

// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.framework.privileged.util

import com.rosan.installer.IPrivilegedService
import com.rosan.installer.framework.privileged.runtime.DefaultPrivilegedService
import com.rosan.installer.framework.privileged.lifecycle.Recyclable
import com.rosan.installer.framework.privileged.lifecycle.RecyclerManager
import com.rosan.installer.framework.privileged.lifecycle.UserService
import com.rosan.installer.framework.privileged.recycler.ProcessUserServiceRecycler
import com.rosan.installer.framework.privileged.recycler.ShizukuUserServiceRecycler
import com.rosan.installer.di.RecyclerNames
import com.rosan.installer.domain.privileged.exception.PrivilegedException
import com.rosan.installer.domain.privileged.model.PrivilegedErrorType
import com.rosan.installer.domain.settings.model.config.Authorizer
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.lang.reflect.InvocationTargetException

private const val TAG = "PrivilegedService"

private class DefaultUserService(
    override val privileged: IPrivilegedService
) : UserService {
    override fun close() {}
}

fun useUserService(
    isSystemApp: Boolean,
    authorizer: Authorizer,
    customizeAuthorizer: String = "",
    special: (() -> String?)? = null,
    action: (UserService) -> Unit
) {
    if (authorizer == Authorizer.None) {
        if (isSystemApp) {
            Timber.tag(TAG).d("Running as System App with None Authorizer. Executing direct calls.")
            action.invoke(DefaultUserService(DefaultPrivilegedService.system()))
        } else {
            Timber.tag(TAG).w("Authorizer is None but not running as System App. Privileged action skipped.")
        }
        return
    }

    if (authorizer == Authorizer.Dhizuku) {
        Timber.tag(TAG).w("Dhizuku has no remote UserService path for this operation.")
        return
    }

    val recycler = getRecyclableInstance(authorizer, customizeAuthorizer, special)
    processRecycler(authorizer, recycler, action)
}

private fun processRecycler(
    authorizer: Authorizer,
    recycler: Recyclable<out UserService>?,
    action: (UserService) -> Unit
) {
    try {
        if (recycler != null) {
            Timber.tag(TAG).d("Processing $authorizer with recycler: ${recycler.entity::class.java.simpleName}")
            recycler.use { action.invoke(it.entity) }
        } else {
            Timber.tag(TAG).e("No recycler found for $authorizer. Falling back to DefaultUserService.")
            action.invoke(DefaultUserService(DefaultPrivilegedService.userService()))
        }
    } catch (e: Exception) {
        if (e is InvocationTargetException) {
            val target = e.targetException
            if (authorizer == Authorizer.Shizuku &&
                target is IllegalStateException &&
                target.message?.contains("binder haven't been received") == true
            ) {
                throw PrivilegedException(
                    errorType = PrivilegedErrorType.SHIZUKU_NOT_WORK,
                    message = "Shizuku service connection lost during privileged action (Reflected).",
                    cause = target
                )
            }
            throw e
        }

        if (e is IllegalStateException) {
            if (authorizer == Authorizer.Shizuku && e.message?.contains("binder haven't been received") == true) {
                throw PrivilegedException(
                    errorType = PrivilegedErrorType.SHIZUKU_NOT_WORK,
                    message = "Shizuku service connection lost during privileged action.",
                    cause = e
                )
            }
        }

        throw e
    }
}

private fun getRecyclableInstance(
    authorizer: Authorizer,
    customizeAuthorizer: String,
    special: (() -> String?)?
): Recyclable<out UserService>? {
    val specialShell = special?.invoke()

    // Retrieve the active Koin instance
    val koin = GlobalContext.get()

    return when (authorizer) {
        Authorizer.None -> null

        Authorizer.Root -> {
            val targetShell = specialShell ?: SHELL_ROOT

            Timber.tag(TAG).d("Using ProcessUserServiceRecycler with shell: $targetShell")
            koin.get<RecyclerManager<String, ProcessUserServiceRecycler>>(RecyclerNames.USER_SERVICE).get(targetShell).make()
        }

        Authorizer.Shizuku -> {
            Timber.tag(TAG).i("Using Shizuku UserService Mode.")
            koin.get<ShizukuUserServiceRecycler>().make()
        }

        Authorizer.Dhizuku -> null

        Authorizer.Customize -> {
            val targetShell = customizeAuthorizer.ifBlank { SHELL_ROOT }
            Timber.tag(TAG).d("Using ProcessUserServiceRecycler with shell: $targetShell")
            koin.get<RecyclerManager<String, ProcessUserServiceRecycler>>(RecyclerNames.USER_SERVICE).get(targetShell).make()
        }

        else -> specialShell?.let {
            Timber.tag(TAG).d("Using ProcessUserServiceRecycler with shell: $it")
            koin.get<RecyclerManager<String, ProcessUserServiceRecycler>>(RecyclerNames.USER_SERVICE).get(it).make()
        }
    }
}

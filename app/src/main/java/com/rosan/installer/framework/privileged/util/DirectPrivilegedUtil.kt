// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.util

import com.rosan.dhizuku.api.Dhizuku
import com.rosan.installer.framework.privileged.runtime.DhizukuPrivilegedService
import com.rosan.installer.framework.privileged.runtime.DefaultPrivilegedService
import com.rosan.installer.framework.privileged.runtime.PrivilegedOperations
import com.rosan.installer.framework.privileged.recycler.ProcessHookRecycler
import com.rosan.installer.framework.privileged.recycler.ShizukuHookRecycler
import com.rosan.installer.domain.settings.model.config.Authorizer
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf
import timber.log.Timber

private const val DIRECT_TAG = "DirectPrivileged"

/**
 * Runs privileged binder/API operations directly in the app process.
 *
 * This is intentionally separate from [useUserService]. Binder hook backends should not be
 * shaped as IPrivilegedService user services; the only remote path left for that helper is
 * shell execution, file deletion, and session detail extraction.
 */
fun useDirectPrivileged(
    isSystemApp: Boolean,
    authorizer: Authorizer,
    customizeAuthorizer: String = "",
    special: (() -> String?)? = null,
    action: (PrivilegedOperations) -> Unit
) {
    val koin = GlobalContext.get()
    when (authorizer) {
        Authorizer.None -> {
            if (isSystemApp) action(DefaultPrivilegedService.system())
            else Timber.tag(DIRECT_TAG).w("Authorizer.None without system app privileges; direct privileged action skipped.")
        }

        Authorizer.Root -> {
            val shell = special?.invoke() ?: SHELL_ROOT
            val handle = koin.get<ProcessHookRecycler> { parametersOf(shell) }.make()
            handle.use {
                action(DefaultPrivilegedService.binderWrapped(name = "Root", useAppCallerPackage = isSystemApp) { binder ->
                    it.entity.binderWrapper(binder)
                })
            }
        }

        Authorizer.Shizuku -> {
            koin.get<ShizukuHookRecycler>().make().use {
                action(DefaultPrivilegedService.shizukuHook())
            }
        }

        Authorizer.Dhizuku -> {
            runBlocking {
                requireDhizukuPermissionGranted {
                    action(DhizukuPrivilegedService { binder ->
                        Dhizuku.binderWrapper(binder)
                    })
                }
            }
        }

        Authorizer.Customize -> {
            val shell = customizeAuthorizer.ifBlank { SHELL_ROOT }
            val handle = koin.get<ProcessHookRecycler> { parametersOf(shell) }.make()
            handle.use {
                action(DefaultPrivilegedService.binderWrapped(name = "Customize", useAppCallerPackage = isSystemApp) { binder ->
                    it.entity.binderWrapper(binder)
                })
            }
        }

        else -> {
            special?.invoke()?.let { shell ->
                val handle = koin.get<ProcessHookRecycler> { parametersOf(shell) }.make()
                handle.use {
                    action(DefaultPrivilegedService.binderWrapped(name = "Special", useAppCallerPackage = isSystemApp) { binder ->
                        it.entity.binderWrapper(binder)
                    })
                }
            }
        }
    }
}

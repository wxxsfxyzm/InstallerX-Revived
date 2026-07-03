// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.core.execution.dispatcher

import com.rosan.dhizuku.api.Dhizuku
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.framework.privileged.core.execution.authorization.requireCustomizeAuthorizer
import com.rosan.installer.framework.privileged.core.execution.authorization.requireDhizukuPermissionGranted
import com.rosan.installer.framework.privileged.core.execution.runtime.DefaultPrivilegedService
import com.rosan.installer.framework.privileged.core.execution.runtime.DhizukuPrivilegedService
import com.rosan.installer.framework.privileged.core.execution.runtime.PrivilegedOperations
import com.rosan.installer.framework.privileged.core.infrastructure.process.AppProcessTerminal
import com.rosan.installer.framework.privileged.core.infrastructure.process.ShellCommand
import com.rosan.installer.framework.privileged.core.infrastructure.recycler.ProcessHookRecycler
import com.rosan.installer.framework.privileged.core.infrastructure.recycler.ShizukuHookRecycler
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
    special: (() -> AppProcessTerminal?)? = null,
    action: (PrivilegedOperations) -> Unit
) {
    val koin = GlobalContext.get()
    when (authorizer) {
        Authorizer.None -> {
            if (isSystemApp) action(DefaultPrivilegedService.system())
            else Timber.tag(DIRECT_TAG).w("Authorizer.None without system app privileges; direct privileged action skipped.")
        }

        Authorizer.Root -> {
            val terminal = special?.invoke() ?: AppProcessTerminal.Root
            val handle = koin.get<ProcessHookRecycler> { parametersOf(terminal) }.make()
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
            val terminal = AppProcessTerminal.Customize(
                ShellCommand.parse(requireCustomizeAuthorizer(customizeAuthorizer))
            )
            val handle = koin.get<ProcessHookRecycler> { parametersOf(terminal) }.make()
            handle.use {
                action(DefaultPrivilegedService.binderWrapped(name = "Customize", useAppCallerPackage = isSystemApp) { binder ->
                    it.entity.binderWrapper(binder)
                })
            }
        }

        else -> {
            special?.invoke()?.let { terminal ->
                val handle = koin.get<ProcessHookRecycler> { parametersOf(terminal) }.make()
                handle.use {
                    action(DefaultPrivilegedService.binderWrapped(name = "Special", useAppCallerPackage = isSystemApp) { binder ->
                        it.entity.binderWrapper(binder)
                    })
                }
            }
        }
    }
}

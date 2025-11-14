package com.rosan.installer.data.app.model.impl

import android.content.ComponentName
import android.content.Intent
import com.rosan.installer.data.app.repo.PARepo
import com.rosan.installer.data.recycle.util.useUserService
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.datastore.AppDataStore.Companion.LAB_USE_SHIZUKU_HOOK_MODE
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

object PARepoImpl : PARepo, KoinComponent {
    private val appDataStore by inject<AppDataStore>()
    private val useShizukuHookModeFlow = appDataStore.getBoolean(LAB_USE_SHIZUKU_HOOK_MODE)

    override suspend fun setDefaultInstaller(
        authorizer: ConfigEntity.Authorizer,
        component: ComponentName,
        enable: Boolean
    ) {
        val useShizukuHookMode = useShizukuHookModeFlow.first()
        // The special logic for 'su 1000' is specific to this operation.
        val specialAuth = if (authorizer == ConfigEntity.Authorizer.Root) {
            { "su 1000" }
        } else null

        useUserService(
            authorizer = authorizer,
            useShizukuHookMode = useShizukuHookMode,
            special = specialAuth
        ) { userService ->
            userService.privileged.setDefaultInstaller(component, enable)
        }
    }

    override suspend fun grantRuntimePermission(
        authorizer: ConfigEntity.Authorizer,
        packageName: String,
        permission: String
    ) {
        val specialAuth = if (authorizer == ConfigEntity.Authorizer.Root) {
            { "su 1000" }
        } else null
        useUserService(authorizer, special = specialAuth) {
            try {
                // On success, this value is returned by 'useUserService'.
                it.privileged.grantRuntimePermission(packageName, permission)
            } catch (e: Exception) {
                Timber.e(e, "Failed to grant permission '$permission' for '$packageName'")
                // Re-throw to notify the caller (UI) that the query failed.
                throw e
            }
        }
    }

    override suspend fun isPermissionGranted(
        authorizer: ConfigEntity.Authorizer,
        packageName: String,
        permission: String
    ): Boolean {
        val specialAuth = if (authorizer == ConfigEntity.Authorizer.Root) {
            { "su 1000" }
        } else null
        var isGranted = false
        useUserService(authorizer, special = specialAuth) {
            try {
                isGranted = it.privileged.isPermissionGranted(packageName, permission)
            } catch (e: Exception) {
                Timber.e(e, "Failed to check permission '$permission' for '$packageName'")
                // Default to false in case of any error.
                isGranted = false
                throw e
            }
        }
        return isGranted
    }

    override suspend fun execLine(config: ConfigEntity, command: String): String {
        var result = ""
        useUserService(config) {
            try {
                // Call the corresponding execLine method on the privileged service
                result = it.privileged.execLine(command)
            } catch (e: Exception) {
                Timber.e(e, "Failed to execute line command: '$command'")
                result = e.message ?: "Execution failed"
            }
        }
        return result
    }

    override suspend fun execArr(config: ConfigEntity, command: Array<String>): String {
        var result = ""
        useUserService(config) {
            try {
                // Call the corresponding execArr method on the privileged service
                result = it.privileged.execArr(command)
            } catch (e: Exception) {
                Timber.e(e, "Failed to execute array command: ${command.joinToString(" ")}")
                result = e.message ?: "Execution failed"
            }
        }
        return result
    }

    override suspend fun startActivityPrivileged(config: ConfigEntity, intent: Intent): Boolean {
        val useShizukuHookMode = useShizukuHookModeFlow.first()
        var success = false
        useUserService(config = config, useShizukuHookMode = useShizukuHookMode) {
            try {
                success = it.privileged.startActivityPrivileged(intent)
            } catch (e: Exception) {
                Timber.e(e, "Failed to start activity privileged: $intent")
                success = false
            }
        }
        return success
    }

    override suspend fun getUsers(authorizer: ConfigEntity.Authorizer): Map<Int, String> {
        val useShizukuHookMode = useShizukuHookModeFlow.first()
        var users: Map<Int, String> = emptyMap()
        useUserService(authorizer = authorizer, useShizukuHookMode = useShizukuHookMode) {
            try {
                // Call the new getUsers method on the privileged service
                // The result from AIDL is Map<*, *>, so we cast it safely.
                @Suppress("UNCHECKED_CAST")
                users = it.privileged.users as? Map<Int, String> ?: emptyMap()
                Timber.d("Fetched users: $users")
            } catch (e: Exception) {
                Timber.e(e, "Failed to get user list from privileged service")
                // Return an empty map on failure
                users = emptyMap()
            }
        }
        return users
    }
}
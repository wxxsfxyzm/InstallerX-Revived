package com.rosan.installer.data.recycle.model.impl

import android.content.ComponentName
import android.content.Intent
import com.rosan.installer.data.recycle.util.useUserService
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.datastore.AppDataStore.Companion.LAB_USE_SHIZUKU_HOOK_MODE
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * A centralized manager for handling all privileged actions.
 *
 * This singleton object implements [KoinComponent] to self-inject dependencies
 * like [AppDataStore], without the need for the UI layer to pass configuration
 * flags manually.
 */
object PrivilegedManager : KoinComponent {

    private val appDataStore by inject<AppDataStore>()
    private val useShizukuHookModeFlow = appDataStore.getBoolean(LAB_USE_SHIZUKU_HOOK_MODE)

    /**
     * Helper to retrieve the current Shizuku Hook Mode setting.
     */
    private suspend fun getHookMode(): Boolean {
        return useShizukuHookModeFlow.first()
    }

    /**
     * Sets the app as the default installer.
     */
    suspend fun setDefaultInstaller(
        authorizer: ConfigEntity.Authorizer,
        component: ComponentName,
        enable: Boolean
    ) {
        val useHook = getHookMode()

        // The special logic for 'su 1000' is specific to this operation when using Root.
        val specialAuth = if (authorizer == ConfigEntity.Authorizer.Root) {
            { "su 1000" }
        } else null

        useUserService(
            authorizer = authorizer,
            useShizukuHookMode = useHook,
            special = specialAuth
        ) { userService ->
            userService.privileged.setDefaultInstaller(component, enable)
        }
    }

    /**
     * Grants a runtime permission to a specific package.
     */
    suspend fun grantRuntimePermission(
        authorizer: ConfigEntity.Authorizer,
        packageName: String,
        permission: String
    ) {
        val specialAuth = if (authorizer == ConfigEntity.Authorizer.Root) {
            { "su 1000" }
        } else null

        val useHook = getHookMode()

        useUserService(
            authorizer = authorizer,
            useShizukuHookMode = useHook,
            special = specialAuth
        ) {
            try {
                it.privileged.grantRuntimePermission(packageName, permission)
            } catch (e: Exception) {
                Timber.e(e, "Failed to grant permission '$permission' for '$packageName'")
                throw e
            }
        }
    }

    /**
     * Checks if a specific permission is granted.
     */
    suspend fun isPermissionGranted(
        authorizer: ConfigEntity.Authorizer,
        packageName: String,
        permission: String
    ): Boolean {
        val specialAuth = if (authorizer == ConfigEntity.Authorizer.Root) {
            { "su 1000" }
        } else null

        val useHook = getHookMode()
        var isGranted = false

        useUserService(
            authorizer = authorizer,
            useShizukuHookMode = useHook,
            special = specialAuth
        ) {
            try {
                isGranted = it.privileged.isPermissionGranted(packageName, permission)
            } catch (e: Exception) {
                Timber.e(e, "Failed to check permission '$permission' for '$packageName'")
                isGranted = false
                throw e
            }
        }
        return isGranted
    }

    /**
     * Executes a shell command array (safer).
     */
    fun execArr(config: ConfigEntity, command: Array<String>): String {
        var result = ""
        useUserService(config) {
            try {
                result = it.privileged.execArr(command)
            } catch (e: Exception) {
                Timber.e(e, "Failed to execute array command: ${command.joinToString(" ")}")
                result = e.message ?: "Execution failed"
            }
        }
        return result
    }

    /**
     * Starts an activity using a privileged context.
     */
    suspend fun startActivityPrivileged(config: ConfigEntity, intent: Intent): Boolean {
        val useHook = getHookMode()
        var success = false

        useUserService(config = config, useShizukuHookMode = useHook) {
            try {
                success = it.privileged.startActivityPrivileged(intent)
            } catch (e: Exception) {
                Timber.e(e, "Failed to start activity privileged: $intent")
                success = false
            }
        }
        return success
    }

    /**
     * Fetches the list of users on the device.
     */
    suspend fun getUsers(authorizer: ConfigEntity.Authorizer): Map<Int, String> {
        val useHook = getHookMode()
        var users: Map<Int, String> = emptyMap()

        useUserService(authorizer = authorizer, useShizukuHookMode = useHook) {
            try {
                @Suppress("UNCHECKED_CAST")
                users = it.privileged.users as? Map<Int, String> ?: emptyMap()
                Timber.d("Fetched users: $users")
            } catch (e: Exception) {
                Timber.e(e, "Failed to get user list from privileged service")
                users = emptyMap()
            }
        }
        return users
    }
}
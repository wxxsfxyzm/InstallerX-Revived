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
    private val useShizukuHookModeFlow = appDataStore.getBoolean(LAB_USE_SHIZUKU_HOOK_MODE, true)

    /**
     * Helper to retrieve the current Shizuku Hook Mode setting.
     */
    private suspend fun getHookMode(): Boolean {
        return useShizukuHookModeFlow.first()
    }

    /**
     * Helper to generate the special auth command (e.g. "su 1000") for Root mode.
     * This ensures different methods reuse the same 'su 1000' service process.
     */
    private fun getSpecialAuth(authorizer: ConfigEntity.Authorizer): (() -> String?)? {
        return if (authorizer == ConfigEntity.Authorizer.Root) {
            { "su 1000" }
        } else null
    }

    /**
     * Sets the app as the default installer.
     */
    suspend fun setDefaultInstaller(
        authorizer: ConfigEntity.Authorizer,
        component: ComponentName,
        enable: Boolean
    ) {
        useUserService(
            authorizer = authorizer,
            useShizukuHookMode = getHookMode(),
            special = getSpecialAuth(authorizer)
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
        useUserService(
            authorizer = authorizer,
            useShizukuHookMode = getHookMode(),
            special = getSpecialAuth(authorizer)
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
        var isGranted = false
        useUserService(
            authorizer = authorizer,
            useShizukuHookMode = getHookMode(),
            special = getSpecialAuth(authorizer)
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
        if (config.authorizer == ConfigEntity.Authorizer.Root || config.authorizer == ConfigEntity.Authorizer.Customize) {
            return try {
                val shellBinary = if (config.authorizer == ConfigEntity.Authorizer.Customize) {
                    config.customizeAuthorizer.ifBlank { "su" }
                } else {
                    "su"
                }

                val escapedCommand = command.joinToString(" ") { arg ->
                    "'" + arg.replace("'", "'\\''") + "'"
                }

                Timber.d("Executing local shell command: $shellBinary -c $escapedCommand")

                val process = ProcessBuilder(shellBinary, "-c", escapedCommand)
                    .redirectErrorStream(true)
                    .start()

                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()

                if (exitCode != 0) {
                    Timber.w("Local command failed with exit code $exitCode: $output")
                    "Exit Code $exitCode: $output"
                } else {
                    output
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to execute local array command")
                e.message ?: "Local execution failed"
            }
        }

        var result = ""
        useUserService(
            authorizer = config.authorizer,
            customizeAuthorizer = config.customizeAuthorizer,
            useShizukuHookMode = false
        ) {
            try {
                result = it.privileged.execArr(command)
            } catch (e: Exception) {
                Timber.e(e, "Failed to execute array command via IPC: ${command.joinToString(" ")}")
                result = e.message ?: "Execution failed"
            }
        }
        return result
    }

    /**
     * Starts an activity using a privileged context.
     */
    suspend fun startActivityPrivileged(config: ConfigEntity, intent: Intent): Boolean {
        var success = false
        useUserService(
            authorizer = config.authorizer,
            customizeAuthorizer = config.customizeAuthorizer,
            useShizukuHookMode = getHookMode(),
            special = getSpecialAuth(config.authorizer)
        ) {
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
        var users: Map<Int, String> = emptyMap()
        useUserService(
            authorizer = authorizer,
            useShizukuHookMode = getHookMode(),
            special = getSpecialAuth(authorizer)
        ) {
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
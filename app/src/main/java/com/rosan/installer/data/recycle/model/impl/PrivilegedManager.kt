package com.rosan.installer.data.recycle.model.impl

import android.content.ComponentName
import android.content.Intent
import com.rosan.installer.data.recycle.util.SHELL_ROOT
import com.rosan.installer.data.recycle.util.getSpecialAuth
import com.rosan.installer.data.recycle.util.useUserService
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import timber.log.Timber

/**
 * A centralized manager for handling all privileged actions.
 *
 * This singleton object implements [KoinComponent] to self-inject dependencies
 * like [AppDataStore], without the need for the UI layer to pass configuration
 * flags manually.
 */
object PrivilegedManager : KoinComponent {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

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
            special = getSpecialAuth(authorizer)
        ) { userService ->
            userService.privileged.setDefaultInstaller(component, enable)
        }
    }

    /**
     * Sets the "Verify apps over ADB" setting via Binder Hooking.
     * Note: useHookMode is forced to true.
     */
    fun setAdbVerify(
        authorizer: ConfigEntity.Authorizer,
        customizeAuthorizer: String = "",
        enabled: Boolean
    ) {
        useUserService(
            authorizer = authorizer,
            customizeAuthorizer = customizeAuthorizer,
            special = getSpecialAuth(authorizer)
        ) { userService ->
            try {
                // Call the updated AIDL method
                userService.privileged.setAdbVerify(enabled)
                Timber.i("Successfully requested to set ADB verify to $enabled.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to set ADB verify")
                throw e
            }
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
    fun isPermissionGranted(
        authorizer: ConfigEntity.Authorizer,
        packageName: String,
        permission: String
    ): Boolean {
        var isGranted = false
        useUserService(
            authorizer = authorizer,
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
                    config.customizeAuthorizer.ifBlank { SHELL_ROOT }
                } else {
                    SHELL_ROOT
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
            useHookMode = false
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
            special = null
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
    fun getUsers(authorizer: ConfigEntity.Authorizer): Map<Int, String> {
        var users: Map<Int, String> = emptyMap()
        useUserService(authorizer) {
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

    data class PostInstallTaskConfig(
        val packageName: String,
        val enableDexopt: Boolean = false,
        val dexoptMode: String = "speed-profile",
        val forceDexopt: Boolean = false,
        val enableAutoDelete: Boolean = false,
        val deletePaths: Array<String> = emptyArray()
    ) {
        fun hasAnyTask(): Boolean = enableDexopt || (enableAutoDelete && deletePaths.isNotEmpty())
    }

    /**
     * Executes post-install tasks.
     * Concurrently performs Dexopt and file cleanup tasks to improve efficiency.
     */
    suspend fun executePostInstallTasks(
        authorizer: ConfigEntity.Authorizer,
        customizeAuthorizer: String = "",
        config: PostInstallTaskConfig
    ) = coroutineScope { // coroutineScope automatically waits for all inner launch blocks to complete
        if (!config.hasAnyTask()) {
            Timber.d("No post-install tasks to execute")
            return@coroutineScope
        }

        Timber.d("Executing post-install tasks: $config")

        // 1. Dexopt Task (Launch directly, no variable needed)
        launch {
            if (config.enableDexopt) {
                runCatching {
                    useUserService(
                        authorizer = authorizer,
                        customizeAuthorizer = customizeAuthorizer
                    ) { userService ->
                        val result = userService.privileged.performDexOpt(
                            config.packageName,
                            config.dexoptMode,
                            config.forceDexopt
                        )
                        Timber.i("Dexopt result: $result")
                    }
                }.onFailure { e ->
                    Timber.e(e, "Dexopt failed")
                }
            }
        }

        // 2. Delete Task (Launch directly, no variable needed)
        launch {
            if (config.enableAutoDelete && config.deletePaths.isNotEmpty()) {
                runCatching {
                    useUserService(
                        authorizer = authorizer,
                        customizeAuthorizer = customizeAuthorizer,
                        useHookMode = false, // Force Shell Mode for Delete (using remote Shell Service)
                    ) { userService ->
                        userService.privileged.delete(config.deletePaths)
                        Timber.i("Delete completed")
                    }
                }.onFailure { e ->
                    Timber.e(e, "Delete failed")
                }
            }
        }

        // Execution pauses here until all children coroutines (launch blocks) are finished
    }

    /**
     * Asynchronously executes post-install tasks.
     */
    fun executePostInstallTasksAsync(
        authorizer: ConfigEntity.Authorizer,
        customizeAuthorizer: String = "",
        config: PostInstallTaskConfig
    ) {
        if (!config.hasAnyTask()) return

        coroutineScope.launch {
            runCatching {
                executePostInstallTasks(authorizer, customizeAuthorizer, config)
            }.onFailure { e ->
                Timber.e(e, "Async post-install tasks failed")
            }
        }
    }
}
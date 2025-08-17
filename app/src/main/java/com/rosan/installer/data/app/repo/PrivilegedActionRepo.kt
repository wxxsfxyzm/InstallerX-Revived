package com.rosan.installer.data.app.repo

import android.content.ComponentName
import android.content.Intent
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

typealias PARepo = PrivilegedActionRepo

/**
 * A repository responsible for handling all actions that require elevated privileges.
 * It abstracts the underlying privilege escalation mechanism (Root, Shizuku, etc.)
 * from the rest of the application.
 */
interface PrivilegedActionRepo {

    /**
     * Sets or unsets our app as the default installer.
     * @param config The current configuration which determines the authorization method.
     * @param component The ComponentName of the installer activity.
     * @param enable True to set as default, false to clear.
     */
    suspend fun setDefaultInstaller(
        config: ConfigEntity,
        component: ComponentName,
        enable: Boolean
    )

    /**
     * Grants a runtime permission to a specific package.
     * This action requires high privileges.
     * @param authorizer The authorizer to use for the check.
     * @param packageName The package to grant the permission to.
     * @param permission The permission to grant (e.g., android.permission.READ_EXTERNAL_STORAGE).
     */
    suspend fun grantRuntimePermission(
        authorizer: ConfigEntity.Authorizer,
        packageName: String,
        permission: String
    )

    /**
     * Checks if a package has been granted a specific runtime permission.
     * @param authorizer The authorizer to use for the check.
     * @param packageName The package to check.
     * @param permission The permission to check for.
     * @return True if the permission is granted, throw on failure.
     */
    suspend fun isPermissionGranted(
        authorizer: ConfigEntity.Authorizer,
        packageName: String,
        permission: String
    ): Boolean

    /**
     * Executes a shell command as a single string.
     * The system shell will parse the string.
     * @param config The current configuration which determines the authorization method.
     * @param command The command line to execute.
     * @return The standard output of the command, or an error message on failure.
     */
    suspend fun execLine(config: ConfigEntity, command: String): String

    /**
     * Executes a command with arguments provided as an array.
     * This is generally safer as it avoids shell injection issues.
     * @param config The current configuration which determines the authorization method.
     * @param command The command and its arguments as a string array.
     * @return The standard output of the command, or an error message on failure.
     */
    suspend fun execArr(config: ConfigEntity, command: Array<String>): String

    /**
     * Starts an Activity using privileged context.
     * @param config The current configuration which determines the authorization method.
     * @param intent The Intent to start.
     * @return True if the activity was successfully started, false otherwise.
     */
    suspend fun startActivityPrivileged(config: ConfigEntity, intent: Intent): Boolean
}
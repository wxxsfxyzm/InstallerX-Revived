package com.rosan.installer;

import android.content.ComponentName;
import android.content.Intent;

/**
 * AIDL interface for privileged installer service.
 *
 * Provides methods for file operations, installer settings,
 * ADB verification management, and shell command execution
 * in a privileged process.
 */
interface IPrivilegedService {

    /**
     * Deletes files at the given paths.
     *
     * @param paths an array of absolute file paths to be deleted
     */
    void delete(in String[] paths);

    /**
     * Sets or unsets the default installer component.
     *
     * @param component the {@link ComponentName} of the installer to be set as default
     * @param enable    {@code true} to enable the default installer,
     *                  {@code false} to disable it
     */
    void setDefaultInstaller(in ComponentName component, boolean enable);

    /**
     * Starts an activity in a privileged process.
     *
     * @param intent the {@link Intent} describing the activity to start
     * @return {@code true} if the activity was started successfully,
     *         {@code false} otherwise
     */
    boolean startActivityPrivileged(in Intent intent);

    /**
     * Executes a shell command as a single-line string.
     *
     * @param command the command to execute
     * @return the standard output of the executed command
     */
    String execLine(String command);

    /**
     * Executes a shell command with arguments.
     *
     * @param command an array of strings representing the command and its arguments
     * @return the standard output of the executed command
     */
    String execArr(in String[] command);

    /**
     * Grants a runtime permission to a specified package using the privileged service.
     *
     * @param packageName the package name of the app to grant the permission to
     * @param permission the name of the permission to grant (e.g., android.permission.CAMERA)
     */
    void grantRuntimePermission(String packageName, String permission);

    /**
     * Checks if a package has been granted a specific runtime permission.
     * This bypasses standard package visibility restrictions.
     *
     * @param packageName The package name of the app to check.
     * @param permission The full name of the permission to check.
     * @return {@code true} if the permission is granted, {@code false} otherwise.
     */
    boolean isPermissionGranted(String packageName, String permission);
}

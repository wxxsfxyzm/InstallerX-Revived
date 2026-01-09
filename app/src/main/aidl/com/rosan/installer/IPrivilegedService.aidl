package com.rosan.installer;

import android.content.ComponentName;
import android.content.Intent;
import com.rosan.installer.ICommandOutputListener;

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
    * Performs dex-optimization on a given package using the specified compiler filter.
    *
    * @param packageName The package to be optimized.
    * @param compilerFilter The dex2oat compiler filter (e.g., "speed", "speed-profile").
    * @param force Whether to force recompilation even if the system thinks it's unnecessary.
    * @return {@code true} if the dex optimization was successful, {@code false} otherwise.
    */
    boolean performDexOpt(String packageName, String compilerFilter, boolean force);

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
     * Executes a shell command with arguments.
     *
     * @param command an array of strings representing the command and its arguments
     * @return the standard output of the executed command
     */
    String execArr(in String[] command);

    /**
     * Executes a command and streams its output back via a listener.
     */
    void execArrWithCallback(in String[] command, ICommandOutputListener listener);

    /**
     * Sets the "Verify apps over ADB" setting in Global Settings.
     *
     * @param enabled true to enable verification (value 1), false to disable it (value 0).
     */
    void setAdbVerify(boolean enabled);

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

    /**
     * Retrieves a list of all users on the device.
     *
     * @return A Map where the key is the integer user ID and the value is the user's name.
     */
    Map getUsers();

    /**
     * Retrieves detailed information about an installation session (app name, icon).
     * @param sessionId The ID of the session to query.
     * @return A Bundle containing "appLabel" (String) and "appIcon" (byte[]),
     * or null if the session is invalid or the query fails.
     */
    Bundle getSessionDetails(int sessionId);
}

package com.rosan.installer;

import android.content.ComponentName;
import com.rosan.installer.ICommandOutputListener;

/**
 * Remote privileged operations that need a real user service process.
 */
interface IPrivilegedService {

    /**
     * Deletes files at the given paths.
     *
     * @param paths an array of absolute file paths to be deleted
     */
    void delete(in String[] paths);

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
     * Configures this app as the default package installer.
     */
    void setDefaultInstaller(in ComponentName component, boolean enable);

    /**
     * Retrieves detailed information about an installation session (app name, icon).
     * @param sessionId The ID of the session to query.
     * @return A Bundle containing "appLabel" (String) and "appIcon" (byte[]),
     * or null if the session is invalid or the query fails.
     */
    Bundle getSessionDetails(int sessionId);
}

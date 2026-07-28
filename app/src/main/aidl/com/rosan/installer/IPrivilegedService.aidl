package com.rosan.installer;

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
     * Parses an APK that is only readable from this privileged process.
     */
    Bundle parsePackageArchive(String path);

}

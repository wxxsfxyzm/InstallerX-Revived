package com.rosan.installer;

/**
 * Callback interface for receiving real-time output from a command execution.
 */
oneway interface ICommandOutputListener {
    /**
     * Called for each line of standard output from the command.
     */
    void onOutput(String line);

    /**
     * Called for each line of standard error from the command.
     */
    void onError(String line);

    /**
     * Called when the command has completed.
     * @param exitCode The exit code of the process.
     */
    void onComplete(int exitCode);
}
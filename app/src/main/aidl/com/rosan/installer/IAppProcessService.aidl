package com.rosan.installer;

import com.rosan.installer.IPrivilegedService;

interface IAppProcessService {
    oneway void quit();

    IPrivilegedService getPrivilegedService();

    /**
     * Registers a token to monitor the client's death.
     * If the client process (Main App) dies, the server process (Shell) detects it via this token.
     */
    void registerDeathToken(IBinder token);
}

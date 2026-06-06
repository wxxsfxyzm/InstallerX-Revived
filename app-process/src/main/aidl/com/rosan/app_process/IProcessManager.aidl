package com.rosan.app_process;

import com.rosan.app_process.IRemoteProcess;
import com.rosan.app_process.ParcelableBinder;

interface IProcessManager {
    void exit(int code) = 1;

    // remote binder transact: 2

    IRemoteProcess remoteProcess(in List<String> cmdList, in Map<String, String> env, in String directory) = 3;

    ParcelableBinder serviceBinder(in ComponentName componentName) = 4;
}
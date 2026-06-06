package com.rosan.app_process;

interface IRemoteProcess {
    ParcelFileDescriptor getOutputStream();

    ParcelFileDescriptor getInputStream();

    ParcelFileDescriptor getErrorStream();

    int exitValue();

    void destroy();

    int waitFor();
}
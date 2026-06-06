package com.rosan.app_process;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import java.io.InputStream;
import java.io.OutputStream;

public class RemoteProcess extends Process {
    private final @NonNull IRemoteProcess mProcess;

    public RemoteProcess(@NonNull IRemoteProcess process) {
        mProcess = process;
    }

    @Override
    public OutputStream getOutputStream() {
        try {
            return new ParcelFileDescriptor.AutoCloseOutputStream(mProcess.getOutputStream());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getInputStream() {
        try {
            return new ParcelFileDescriptor.AutoCloseInputStream(mProcess.getInputStream());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getErrorStream() {
        try {
            return new ParcelFileDescriptor.AutoCloseInputStream(mProcess.getErrorStream());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int waitFor() {
        try {
            return mProcess.waitFor();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int exitValue() {
        try {
            return mProcess.exitValue();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        try {
            mProcess.destroy();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}

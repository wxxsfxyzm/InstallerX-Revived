package com.rosan.app_process;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;

class BinderWrapper extends Binder {
    private static final String TAG = "BinderWrapper";

    private final IProcessManager mManager;

    private final IBinder mBinder;

    BinderWrapper(@NonNull IProcessManager manager, @NonNull IBinder binder) {
        this.mManager = manager;
        this.mBinder = binder;
    }

    @Nullable
    @Override
    public String getInterfaceDescriptor() {
        try {
            return mBinder.getInterfaceDescriptor();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean pingBinder() {
        return mBinder.pingBinder();
    }

    @Override
    public boolean isBinderAlive() {
        return mBinder.isBinderAlive();
    }

    @Nullable
    @Override
    public IInterface queryLocalInterface(@NonNull String descriptor) {
        return null;
    }

    @Override
    public void dump(@NonNull FileDescriptor fd, @Nullable String[] args) {
        try {
            mBinder.dump(fd, args);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to dump wrapped binder", e);
        }
    }

    @Override
    public void dumpAsync(@NonNull FileDescriptor fd, @Nullable String[] args) {
        try {
            mBinder.dumpAsync(fd, args);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to dump wrapped binder asynchronously", e);
        }
    }

    @Override
    protected boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException {
        return AppProcess.remoteTransact(mManager, mBinder, code, data, reply, flags);
    }

    @Override
    public void linkToDeath(@NonNull DeathRecipient recipient, int flags) {
        try {
            mBinder.linkToDeath(recipient, flags);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean unlinkToDeath(@NonNull DeathRecipient recipient, int flags) {
        return mBinder.unlinkToDeath(recipient, flags);
    }
}

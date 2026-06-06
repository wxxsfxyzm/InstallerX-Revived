package com.rosan.app_process;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;

class BinderWrapper implements IBinder {
    private final IProcessManager mManager;

    private final IBinder mBinder;

    BinderWrapper(@NonNull IProcessManager manager, @NonNull IBinder binder) {
        this.mManager = manager;
        this.mBinder = binder;
    }

    @Nullable
    @Override
    public String getInterfaceDescriptor() throws RemoteException {
        return mBinder.getInterfaceDescriptor();
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
    public void dump(@NonNull FileDescriptor fd, @Nullable String[] args) throws RemoteException {
        mBinder.dump(fd, args);
    }

    @Override
    public void dumpAsync(@NonNull FileDescriptor fd, @Nullable String[] args) throws RemoteException {
        mBinder.dumpAsync(fd, args);
    }

    @Override
    public boolean transact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException {
        return AppProcess.remoteTransact(mManager, mBinder, code, data, reply, flags);
    }

    @Override
    public void linkToDeath(@NonNull DeathRecipient recipient, int flags) throws RemoteException {
        mBinder.linkToDeath(recipient, flags);
    }

    @Override
    public boolean unlinkToDeath(@NonNull DeathRecipient recipient, int flags) {
        return mBinder.unlinkToDeath(recipient, flags);
    }
}

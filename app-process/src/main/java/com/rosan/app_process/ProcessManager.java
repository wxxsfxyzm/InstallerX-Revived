package com.rosan.app_process;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.Keep;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class ProcessManager extends IProcessManager.Stub {
    @Keep
    public ProcessManager() {
        super();
    }

    @Override
    public void exit(int code) {
        System.exit(code);
    }

    private boolean targetTransact(IBinder binder, int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return AppProcess.binderWithCleanCallingIdentity(() -> binder.transact(code, data, reply, flags));
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IRemoteProcess remoteProcess(List<String> cmdList, Map<String, String> env, String directory) {
        ProcessBuilder builder = new ProcessBuilder().command(cmdList);
        if (directory != null) builder = builder.directory(new File(directory));
        if (env != null) builder.environment().putAll(env);
        File file = null;
        try {
            return new RemoteProcessImpl(builder.start());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public ParcelableBinder serviceBinder(ComponentName componentName) {
        try {
            return new ParcelableBinder(NewProcess.createBinder(componentName));
        } catch (PackageManager.NameNotFoundException | NoSuchFieldException |
                 InvocationTargetException | NoSuchMethodException | IllegalAccessException |
                 ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code != Binder.FIRST_CALL_TRANSACTION + 2)
            return super.onTransact(code, data, reply, flags);
        Parcel targetData = Parcel.obtain();
        try {
            data.enforceInterface(this.asBinder().getInterfaceDescriptor());
            IBinder binder = data.readStrongBinder();
            int targetCode = data.readInt();
            int targetFlags = data.readInt();
            targetData.appendFrom(data, data.dataPosition(), data.dataAvail());
            return targetTransact(binder, targetCode, targetData, reply, targetFlags);
        } finally {
            targetData.recycle();
        }
    }
}

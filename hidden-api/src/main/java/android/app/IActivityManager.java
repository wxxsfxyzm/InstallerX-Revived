package android.app;

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

public interface IActivityManager extends IInterface {
    /**
     * @deprecated use {@link #startActivityAsUserWithFeature} instead
     */
    @Deprecated
    int startActivityAsUser(IApplicationThread caller, String callingPackage,
                            Intent intent, String resolvedType, IBinder resultTo, String resultWho,
                            int requestCode, int flags, ProfilerInfo profilerInfo,
                            Bundle options, int userId) throws RemoteException;

    int startActivityAsUserWithFeature(IApplicationThread caller, String callingPackage,
                                       String callingFeatureId, Intent intent, String resolvedType,
                                       IBinder resultTo, String resultWho, int requestCode,
                                       int flags, ProfilerInfo profilerInfo,
                                       Bundle bOptions, int userId) throws RemoteException;

    abstract class Stub extends Binder implements IActivityManager {
        public static IActivityManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}

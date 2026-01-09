package android.content.pm;

import android.content.IntentSender;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

public interface IPackageInstaller extends IInterface {
    void uninstall(VersionedPackage versionedPackage, String callerPackageName, int flags, IntentSender statusReceiver, int userId) throws RemoteException;

    void abandonSession(int sessionId);

    abstract class Stub extends Binder implements IPackageInstaller {
        public static IPackageInstaller asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
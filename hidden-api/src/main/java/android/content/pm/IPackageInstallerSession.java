package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

public interface IPackageInstallerSession extends IInterface {
    abstract class Stub extends Binder implements IPackageInstallerSession {
        public static IPackageInstallerSession asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
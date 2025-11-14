package android.os;

import android.content.pm.UserInfo;

import java.util.List;

public interface IUserManager extends IInterface {
    List<UserInfo> getUsers(boolean excludeDying) throws RemoteException;

    List<UserInfo> getUsers(boolean excludeDying, boolean excludePreCreated, boolean excludePartial) throws RemoteException;

    abstract class Stub extends Binder implements IUserManager {
        public static IUserManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
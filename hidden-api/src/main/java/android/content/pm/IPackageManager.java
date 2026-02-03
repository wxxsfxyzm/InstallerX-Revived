package android.content.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import java.util.List;

public interface IPackageManager extends IInterface {
    boolean isPackageAvailable(String packageName, int userId) throws RemoteException;

    boolean getApplicationHiddenSettingAsUser(String packageName, int userId) throws RemoteException;

    ApplicationInfo getApplicationInfo(String packageName, int flags, int userId)
            throws RemoteException;

    PackageInfo getPackageInfo(String packageName, int flags, int userId)
            throws RemoteException;

    int getPackageUid(String packageName, int flags, int userId) throws RemoteException;

    String[] getPackagesForUid(int uid)
            throws RemoteException;

    ParceledListSlice<PackageInfo> getInstalledPackages(int flags, int userId)
            throws RemoteException;

    ParceledListSlice<ApplicationInfo> getInstalledApplications(int flags, int userId)
            throws RemoteException;

    int getUidForSharedUser(String sharedUserName)
            throws RemoteException;

    void grantRuntimePermission(String packageName, String permissionName, int userId)
            throws RemoteException;

    void revokeRuntimePermission(String packageName, String permissionName, int userId)
            throws RemoteException;

    int getPermissionFlags(String permissionName, String packageName, int userId)
            throws RemoteException;

    void updatePermissionFlags(String permissionName, String packageName, int flagMask, int flagValues, int userId)
            throws RemoteException;

    int checkPermission(String permName, String pkgName, int userId)
            throws RemoteException;

    int checkUidPermission(String permName, int uid)
            throws RemoteException;

    IPackageInstaller getPackageInstaller() throws RemoteException;

    int installExistingPackageAsUser(String packageName, int userId, int installFlags,
                                     int installReason) throws RemoteException;


    int installExistingPackageAsUser(String packageName, int userId, int installFlags,
                                     int installReason, List<String> whiteListedPermissions) throws RemoteException;

    ParceledListSlice<ResolveInfo> queryIntentActivities(Intent intent,
                                                         String resolvedType, int flags, int userId) throws RemoteException;

    void setLastChosenActivity(Intent intent, String resolvedType, int flags,
                               IntentFilter filter, int match, ComponentName activity);

    // not work when api >= Android S(12)
    void addPreferredActivity(IntentFilter filter, int match,
                              ComponentName[] set, ComponentName activity, int userId);

    void addPreferredActivity(IntentFilter filter, int match,
                              ComponentName[] set, ComponentName activity, int userId, boolean removeExisting);


    void clearPackagePreferredActivities(String packageName);

    void addPersistentPreferredActivity(IntentFilter filter, ComponentName activity, int userId);

    void clearPackagePersistentPreferredActivities(String packageName, int userId);

    void flushPackageRestrictionsAsUser(int userId);

    abstract class Stub extends Binder implements IPackageManager {

        public static IPackageManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}

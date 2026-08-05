package android.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

public interface IConnectivityManager extends IInterface {

    // Firewall chains from android.net.ConnectivityManager (@hide).
    int FIREWALL_CHAIN_NONE = 0;
    int FIREWALL_CHAIN_DOZABLE = 1;
    int FIREWALL_CHAIN_STANDBY = 2;
    int FIREWALL_CHAIN_POWERSAVE = 3;
    int FIREWALL_CHAIN_RESTRICTED = 4;
    int FIREWALL_CHAIN_LOW_POWER_STANDBY = 5;
    int FIREWALL_CHAIN_BACKGROUND = 6;
    int FIREWALL_CHAIN_OEM_DENY_1 = 7;
    int FIREWALL_CHAIN_OEM_DENY_2 = 8;
    int FIREWALL_CHAIN_OEM_DENY_3 = 9;

    // Metered chains are controlled by a separate BPF path and cannot be
    // enabled or disabled through setFirewallChainEnabled().
    int FIREWALL_CHAIN_METERED_ALLOW = 10;
    int FIREWALL_CHAIN_METERED_DENY_USER = 11;
    int FIREWALL_CHAIN_METERED_DENY_ADMIN = 12;

    void setFirewallChainEnabled(int chain, boolean enable) throws RemoteException;

    // Firewall rules from android.net.ConnectivityManager (@hide).
    int FIREWALL_RULE_DEFAULT = 0;
    int FIREWALL_RULE_ALLOW = 1;
    int FIREWALL_RULE_DENY = 2;

    void setUidFirewallRule(int chain, int uid, int rule) throws RemoteException;

    int getUidFirewallRule(int chain, int uid) throws RemoteException;

    abstract class Stub extends Binder implements IConnectivityManager {

        public static IConnectivityManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}

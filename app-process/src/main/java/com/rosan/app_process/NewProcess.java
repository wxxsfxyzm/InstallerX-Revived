package com.rosan.app_process;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.app.LoadedApk;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class NewProcess {
    private static final String TAG = "NewProcess";

    private static ActivityThread mActivityThread = null;

    @Keep
    public static void main(String[] args) throws Throwable {
        // Start the orphan watchdog immediately.
        // This ensures that if the parent process (Main App) dies during the
        // initialization phase (before Binder is ready), this process
        // detects the broken pipe (STDIN) and kills itself to prevent zombies.
        startOrphanWatchdog();

        try {
            innerMain(args);
        } catch (Throwable e) {
            e.printStackTrace();
            Log.e(TAG, "main", e);
            throw e;
        }
    }

    /**
     * Starts a daemon thread to monitor the Standard Input (STDIN).
     * In a shell-spawned process structure (App -> su -> sh -> app_process),
     * STDIN is a pipe connected to the parent. If the parent dies, the pipe is closed.
     */
    private static void startOrphanWatchdog() {
        Thread watchdog = new Thread(() -> {
            try {
                // Blocking read. This will wait indefinitely as long as the parent is alive.
                // If the parent dies, the pipe closes, and read() returns -1 (EOF).
                int result;
                while ((result = System.in.read()) != -1) {
                }
                Log.w(TAG, "Watchdog: Stdin EOF detected (" + result + "). Parent died. Exiting...");
            } catch (Exception e) {
                // Any IO exception usually means the pipe is broken.
                Log.w(TAG, "Watchdog: Stdin broken. Parent likely died.", e);
            } finally {
                // Force kill the current process to clean up.
                Process.killProcess(Process.myPid());
                System.exit(0);
            }
        });

        // Set as daemon so it doesn't prevent normal JVM shutdown (though typically we loop forever).
        watchdog.setDaemon(true);
        watchdog.setName("OrphanWatchdog");
        watchdog.start();
    }

    private static void innerMain(String[] args) throws Throwable {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("");
        }
        Options options = new Options().addOption(Option.builder().longOpt("package").hasArg().required().type(String.class).build()).addOption(Option.builder().longOpt("token").hasArg().required().type(String.class).build()).addOption(Option.builder().longOpt("component").hasArg().required().type(String.class).build());
        CommandLine cmdLine = new DefaultParser().parse(options, args);
        String packageName = cmdLine.getOptionValue("package");
        String token = cmdLine.getOptionValue("token");
        String component = cmdLine.getOptionValue("component");
        ComponentName componentName = ComponentName.unflattenFromString(component);

        Bundle bundle = new Bundle();
        IBinder binder = createBinder(componentName);
        bundle.putBinder(NewProcessReceiver.EXTRA_NEW_PROCESS, binder);
        bundle.putString(NewProcessReceiver.EXTRA_TOKEN, token);
        Intent intent = new Intent(NewProcessReceiver.ACTION_SEND_NEW_PROCESS)
                .setPackage(packageName)
                .putExtras(bundle);
        getSystemContext().sendBroadcast(intent);
        Looper.loop();
    }

    public static IBinder createBinder(ComponentName componentName) throws PackageManager.NameNotFoundException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, ClassNotFoundException {
        return createBinder(getUIDContext(), componentName);
    }

    public static IBinder createBinder(Context context, ComponentName componentName) throws PackageManager.NameNotFoundException, ClassNotFoundException {
        Context packageContext = getSystemContext().createPackageContext(componentName.getPackageName(), Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
        Class<?> clazz = packageContext.getClassLoader().loadClass(componentName.getClassName());
        Constructor<?> constructor = null;
        try {
            constructor = clazz.getDeclaredConstructor(Context.class);
        } catch (NoSuchMethodException ignored) {
        }
        Object result;
        try {
            result = constructor != null ? constructor.newInstance(context) : clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return ((IInterface) result).asBinder();
    }

    public static List<String> getPackagesForUid(Context context, int uid) {
        String[] packageNames = context.getPackageManager().getPackagesForUid(uid);
        if (packageNames == null) return Collections.emptyList();
        return Arrays.asList(packageNames);
    }

    private static @NonNull ActivityThread getActivityThread() {
        if (mActivityThread != null) return mActivityThread;
        if (Looper.getMainLooper() == null) Looper.prepareMainLooper();
        if (Looper.myLooper() == null) Looper.prepare();

        mActivityThread = ActivityThread.systemMain();
        Objects.requireNonNull(mActivityThread);
        return getActivityThread();
    }

    private static @NonNull Context getSystemContext() {
        return getActivityThread().getSystemContext();
    }

    public static Context getUIDContext() throws PackageManager.NameNotFoundException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Context context = getSystemContext();

        int uid = Process.myUid();
        List<String> packageNames = getPackagesForUid(context, uid);
        if (packageNames.isEmpty()) return context;
        if (packageNames.contains(context.getPackageName()) && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || packageNames.contains(context.getOpPackageName())))
            return context;

        return createAppContext(context, packageNames.get(0));
    }

    @SuppressLint("PrivateApi")
    private static Context createAppContext(Context context, String packageName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, PackageManager.NameNotFoundException, NoSuchFieldException {
        Context impl = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
        while (impl instanceof ContextWrapper) {
            impl = ((ContextWrapper) impl).getBaseContext();
        }

        Method method = impl.getClass().getDeclaredMethod("createAppContext", ActivityThread.class, LoadedApk.class);
        method.setAccessible(true);
        return (Context) method.invoke(null, getActivityThread(), getActivityThread().peekPackageInfo(packageName, true));
    }
}

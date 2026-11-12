package com.rosan.app_process;

import android.app.ActivityThread;
import android.content.ComponentName;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public abstract class AppProcess implements Closeable {
    private static final String TAG = "AppProcess";

    static final int TRANSACTION_REMOTE_TRANSACT = IBinder.FIRST_CALL_TRANSACTION + 2;

    protected Context mContext = null;

    protected volatile IProcessManager mManager = null;

    protected final ConcurrentMap<String, IBinder> mChildProcess = new ConcurrentHashMap<>();

    // Processes started locally (e.g. the su shell hosting the manager); they must be released on close.
    private final Queue<Process> mLocalProcesses = new ConcurrentLinkedQueue<>();

    public static ProcessParams generateProcessParams(@NonNull String classPath, @NonNull String entryClassName, @NonNull List<String> args) {
        return generateProcessParams(classPath, entryClassName, args, null);
    }

    public static ProcessParams generateProcessParams(
            @NonNull String classPath,
            @NonNull String entryClassName,
            @NonNull List<String> args,
            @Nullable String niceName
    ) {
        List<String> cmdList = new ArrayList<>();
        cmdList.add("/system/bin/app_process");
        cmdList.add("-Djava.class.path=" + classPath);
        cmdList.add("/system/bin");
        if (niceName != null) cmdList.add("--nice-name=" + niceName);
        cmdList.add(entryClassName);
        cmdList.addAll(args);
        return new ProcessParams(cmdList, null, null);
    }

    public static <T> T binderWithCleanCallingIdentity(Callable<T> action) throws Exception {
        final long callingIdentity = Binder.clearCallingIdentity();
        try {
            return action.call();
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    public static IBinder binderWrapper(IProcessManager manager, IBinder binder) {
        return new BinderWrapper(manager, binder);
    }

    public static boolean remoteTransact(IProcessManager manager, IBinder binder, int code, Parcel data, Parcel reply, int flags) {
        IBinder managerBinder = manager.asBinder();
        Parcel processData = Parcel.obtain();
        try {
            processData.writeInterfaceToken(Objects.requireNonNull(managerBinder.getInterfaceDescriptor()));
            processData.writeStrongBinder(binder);
            processData.writeInt(code);
            processData.writeInt(flags);
            processData.appendFrom(data, 0, data.dataSize());
            return managerBinder.transact(TRANSACTION_REMOTE_TRANSACT, processData, reply, 0);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            processData.recycle();
        }
    }

    /*
     * Releases a process that may run as another uid (e.g. root via su).
     * destroy() cannot signal such a process, so stdin is closed first: the shell exits on EOF,
     * and NewProcess's orphan watchdog terminates app_process when its stdin is closed.
     * */
    static void terminateProcess(@NonNull Process process) {
        closeQuietly(process::getOutputStream);
        closeQuietly(process::getInputStream);
        closeQuietly(process::getErrorStream);
        try {
            process.destroy();
        } catch (RuntimeException e) {
            Log.w(TAG, "Failed to destroy process", e);
        }
    }

    private static void closeQuietly(@NonNull Callable<? extends Closeable> supplier) {
        try {
            supplier.call().close();
        } catch (Exception e) {
            Log.w(TAG, "Failed to close process stream", e);
        }
    }

    private void terminateLocalProcesses() {
        Process process;
        while ((process = mLocalProcesses.poll()) != null) terminateProcess(process);
    }

    public @NonNull Process start(@NonNull String classPath, @NonNull String entryClassName, @NonNull List<String> args) throws IOException {
        return startProcess(generateProcessParams(classPath, entryClassName, args));
    }

    public @NonNull Process start(@NonNull String classPath, @NonNull String entryClassName, @NonNull String[] args) throws IOException {
        return start(classPath, entryClassName, Arrays.asList(args));
    }

    public <T> @NonNull Process start(@NonNull String classPath, @NonNull Class<T> entryClass, @NonNull List<String> args) throws IOException {
        return start(classPath, entryClass.getName(), args);
    }

    public <T> @NonNull Process start(@NonNull String classPath, @NonNull Class<T> entryClass, @NonNull String[] args) throws IOException {
        return start(classPath, entryClass, Arrays.asList(args));
    }

    public <T> @NonNull Process start(@NonNull String classPath, @NonNull Class<T> entryClass, @NonNull String[] args, @Nullable String niceName) throws IOException {
        return startProcess(generateProcessParams(classPath, entryClass.getName(), Arrays.asList(args), niceName));
    }

    public boolean init() {
        return init(ActivityThread.currentActivityThread().getApplication());
    }

    public synchronized boolean init(@NonNull Context context) {
        if (initialized()) return true;
        mContext = context;
        mManager = null;
        mChildProcess.clear();

        IProcessManager manager = newManager();
        if (manager == null) return false;
        mManager = manager;

        try {
            final IBinder managerBinder = manager.asBinder();
            managerBinder.linkToDeath(() -> {
                IProcessManager current = mManager;
                if (current == null || managerBinder != current.asBinder()) return;
                mManager = null;
                mChildProcess.clear();
                terminateLocalProcesses();
            }, 0);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to link process manager death recipient", e);
        }
        return initialized();
    }

    /*
     * Starts a new process and returns its Android binder for remote process management.
     * */
    protected @Nullable IProcessManager newManager() {
        IBinder binder = isolatedServiceBinder(new ComponentName(mContext.getPackageName(), ProcessManager.class.getName()));
        if (binder == null) return null;
        return IProcessManager.Stub.asInterface(binder);
    }

    public boolean initialized() {
        IProcessManager manager = mManager;
        return mContext != null && manager != null && manager.asBinder().isBinderAlive();
    }

    @Override
    public synchronized void close() {
        IProcessManager manager = mManager;
        mContext = null;
        mManager = null;
        mChildProcess.clear();
        try {
            if (manager != null && manager.asBinder().pingBinder()) manager.exit(0);
        } catch (Exception ignored) {
            // exit() kills the remote process before replying, so a dead binder is expected here.
        } finally {
            terminateLocalProcesses();
        }
    }

    /*
     * Starts a process with the supplied process parameters.
     * */
    protected @NonNull Process newProcess(@NonNull ProcessParams params) throws IOException {
        List<String> cmdList = params.getCmdList();
        Map<String, String> env = params.getEnv();
        String directory = params.getDirectory();
        ProcessBuilder builder = new ProcessBuilder().command(cmdList);
        if (directory != null) builder = builder.directory(new File(directory));
        if (env != null) builder.environment().putAll(env);
        return builder.start();
    }

    private @NonNull Process startProcess(@NonNull ProcessParams params) throws IOException {
        if (!initialized()) {
            Process process = newProcess(params);
            mLocalProcesses.add(process);
            return process;
        }
        return remoteProcess(params.getCmdList(), params.getEnv(), params.getDirectory());
    }

    private @NonNull IProcessManager requireManager() {
        IProcessManager manager = mManager;
        if (mContext == null || manager == null || !manager.asBinder().isBinderAlive())
            throw new IllegalStateException("please call init() first.");
        return manager;
    }

    public boolean remoteTransact(IBinder binder, int code, Parcel data, Parcel reply, int flags) {
        return remoteTransact(requireManager(), binder, code, data, reply, flags);
    }

    public IBinder binderWrapper(IBinder binder) {
        return binderWrapper(requireManager(), binder);
    }

    public Process remoteProcess(@NonNull List<String> cmdList, @Nullable Map<String, String> env, @Nullable String directory) {
        try {
            return new RemoteProcess(requireManager().remoteProcess(cmdList, env, directory));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public IBinder serviceBinder(ComponentName componentName) {
        try {
            return requireManager().serviceBinder(componentName).getBinder();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<>();

    private Object buildLock(String token) {
        return locks.computeIfAbsent(token, ignored -> new Object());
    }

    public IBinder isolatedServiceBinder(@NonNull ComponentName componentName, boolean useCache) {
        if (!useCache) return isolatedServiceBinderUnchecked(componentName);
        return isolatedServiceBinder(componentName);
    }

    public IBinder isolatedServiceBinder(@NonNull ComponentName componentName) {
        String token = componentName.flattenToString();
        synchronized (buildLock(token)) {
            IBinder existsBinder = mChildProcess.get(token);
            if (existsBinder != null && existsBinder.isBinderAlive()) return existsBinder;
            if (existsBinder != null) mChildProcess.remove(token, existsBinder);
            final IBinder binder = isolatedServiceBinderUnchecked(componentName);
            if (binder == null) return null;
            mChildProcess.put(token, binder);
            try {
                binder.linkToDeath(() -> {
                    IBinder curBinder = mChildProcess.get(token);
                    if (curBinder == null || curBinder != binder) return;
                    mChildProcess.remove(token, binder);
                }, 0);
            } catch (RemoteException e) {
                mChildProcess.remove(token, binder);
                return null;
            }
            return binder;
        }
    }

    private IBinder isolatedServiceBinderUnchecked(@NonNull ComponentName componentName) {
        Context context = mContext;
        if (context == null) return null;
        return NewProcessReceiver.start(context, this, componentName);
    }

    public static class Default extends AppProcess {
    }

    /*
     * Runs directly in the current process instead of spawning a new process.
     * */
    public static class None extends AppProcess {
        @Nullable
        @Override
        protected IProcessManager newManager() {
            return new ProcessManager();
        }

        @Override
        public synchronized void close() {
            mManager = null;
            mChildProcess.clear();
        }
    }

    public abstract static class Terminal extends Default {
        protected abstract @NonNull List<String> newTerminal();

        @NonNull
        @Override
        protected Process newProcess(@NonNull ProcessParams params) throws IOException {
            ProcessParams newParams = new ProcessParams(params).setCmdList(newTerminal());
            Process process = super.newProcess(newParams);
            PrintWriter printWriter = new PrintWriter(process.getOutputStream(), true);
            // exec replaces the shell, so no shell is left blocking on stdin after app_process exits.
            // A trailing "exit $?" is not reliable: app_process shares stdin and its orphan watchdog consumes it.
            StringBuilder buffer = new StringBuilder("exec");
            for (String element : params.getCmdList()) {
                buffer.append(" ").append(element);
            }
            printWriter.println(buffer);
            return process;
        }
    }

    public static class Root extends Terminal {

        @NonNull
        @Override
        protected List<String> newTerminal() {
            List<String> terminal = new ArrayList<>();
            terminal.add("su");
            return terminal;
        }
    }

    public static class RootSystem extends Terminal {
        @NonNull
        @Override
        protected List<String> newTerminal() {
            List<String> terminal = new ArrayList<>();
            terminal.add("su");
            terminal.add("1000");
            return terminal;
        }
    }

    public static class ProcessParams {
        private @NonNull List<String> mCmdList;

        private @Nullable Map<String, String> mEnv;

        private @Nullable String mDirectory;

        public ProcessParams(@NonNull List<String> cmdList, @Nullable Map<String, String> env, @Nullable String directory) {
            this.mCmdList = cmdList;
            this.mEnv = env;
            this.mDirectory = directory;
        }

        public ProcessParams(@NonNull ProcessParams params) {
            this.mCmdList = new ArrayList<>(params.getCmdList());
            Map<String, String> env = params.getEnv();
            this.mEnv = env != null ? new HashMap<>(env) : null;
            this.mDirectory = params.getDirectory();
        }

        @NonNull
        public List<String> getCmdList() {
            return mCmdList;
        }

        @Nullable
        public Map<String, String> getEnv() {
            return mEnv;
        }

        @Nullable
        public String getDirectory() {
            return mDirectory;
        }

        public ProcessParams setCmdList(@NonNull List<String> mCmdList) {
            this.mCmdList = mCmdList;
            return this;
        }

        public ProcessParams setEnv(@Nullable Map<String, String> mEnv) {
            this.mEnv = mEnv;
            return this;
        }

        public ProcessParams setDirectory(@Nullable String mDirectory) {
            this.mDirectory = mDirectory;
            return this;
        }
    }
}

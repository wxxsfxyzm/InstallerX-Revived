package com.rosan.app_process;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

abstract class NewProcessReceiver extends BroadcastReceiver {
    private static final String TAG = "NewProcessReceiver";

    private static final long START_TIMEOUT_SECONDS = 15L;

    private static boolean hasExited(Process process) {
        try {
            process.exitValue();
            return true;
        } catch (IllegalThreadStateException ignored) {
            return false;
        }
    }

    private static String processName(@NonNull Context context, @NonNull ComponentName componentName) {
        String className = componentName.getClassName();
        int index = className.lastIndexOf('.');
        String shortName = index >= 0 ? className.substring(index + 1) : className;
        return context.getPackageName() + ":app_process:" + shortName;
    }

    private static NewProcessResult waitForResult(
            @NonNull Process process,
            @NonNull LinkedBlockingQueue<NewProcessResult> queue
    ) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(START_TIMEOUT_SECONDS);
        while (true) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0L) return null;

            long waitMs = Math.min(TimeUnit.NANOSECONDS.toMillis(remaining) + 1L, 100L);
            NewProcessResult result = queue.poll(waitMs, TimeUnit.MILLISECONDS);
            if (result != null) return result;
            if (hasExited(process)) return null;
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public static IBinder start(Context context, AppProcess appProcess, ComponentName componentName) {
        String token = UUID.randomUUID().toString();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SEND_NEW_PROCESS);
        LinkedBlockingQueue<NewProcessResult> queue = new LinkedBlockingQueue<>();
        BroadcastReceiver receiver = new NewProcessReceiver() {
            @Override
            void onReceive(NewProcessResult result) {
                if (!result.getToken().equals(token)) return;
                queue.offer(result);
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        else context.registerReceiver(receiver, filter);
        Process process = null;
        long startedAt = SystemClock.elapsedRealtime();
        try {
            process = appProcess.start(context.getPackageCodePath(), NewProcess.class, new String[]{
                    String.format("--package=%s", context.getPackageName()),
                    String.format("--token=%s", token),
                    String.format("--component=%s", componentName.flattenToString())
            }, processName(context, componentName));
            NewProcessResult result = waitForResult(process, queue);
            IBinder binder = result != null ? result.getBinder() : null;
            long elapsed = SystemClock.elapsedRealtime() - startedAt;
            if (binder == null) {
                Log.w(TAG, "Failed to receive app_process binder for " + componentName.flattenToShortString() + " after " + elapsed + "ms");
                process.destroy();
            } else {
                Log.d(TAG, "Received app_process binder for " + componentName.flattenToShortString() + " in " + elapsed + "ms");
            }
            return binder;
        } catch (IOException e) {
            long elapsed = SystemClock.elapsedRealtime() - startedAt;
            Log.w(TAG, "Failed to start app_process for " + componentName.flattenToShortString() + " after " + elapsed + "ms", e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long elapsed = SystemClock.elapsedRealtime() - startedAt;
            Log.w(TAG, "Interrupted while waiting for app_process binder after " + elapsed + "ms", e);
            process.destroy();
            return null;
        } finally {
            context.unregisterReceiver(receiver);
        }
    }

    static final String ACTION_SEND_NEW_PROCESS = "com.rosan.app_process.send.new_process";

    static final String EXTRA_NEW_PROCESS = "new_process";

    static final String EXTRA_TOKEN = "token";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        if (extras == null) return;
        if (!Objects.equals(action, ACTION_SEND_NEW_PROCESS)) return;
        String token = extras.getString(EXTRA_TOKEN);
        if (token == null) return;
        IBinder binder = extras.getBinder(EXTRA_NEW_PROCESS);
        if (binder == null) return;
        onReceive(new NewProcessResult(token, binder));
    }

    abstract void onReceive(NewProcessResult result);

    static class NewProcessResult {
        private final @NonNull String mToken;

        private final @NonNull IBinder mBinder;

        NewProcessResult(@NonNull String token, @NonNull IBinder binder) {
            mToken = token;
            mBinder = binder;
        }

        @NonNull
        public String getToken() {
            return mToken;
        }

        @NonNull
        public IBinder getBinder() {
            return mBinder;
        }
    }
}

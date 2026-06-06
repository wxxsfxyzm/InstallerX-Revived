package com.rosan.app_process;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

abstract class NewProcessReceiver extends BroadcastReceiver {
    private static boolean waitFor(Process process, long timeout, TimeUnit unit) throws InterruptedException {
        long startTime = System.nanoTime();
        long rem = unit.toNanos(timeout);

        do {
            try {
                process.exitValue();
                return true;
            } catch (IllegalArgumentException ex) {
                if (rem > 0)
                    Thread.sleep(
                            Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
            }
            rem = unit.toNanos(timeout) - (System.nanoTime() - startTime);
        } while (rem > 0);
        return false;
    }

    public static IBinder start(Context context, AppProcess appProcess, ComponentName componentName) {
        String token = UUID.randomUUID().toString();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SEND_NEW_PROCESS);
        LinkedBlockingQueue<AtomicReference<NewProcessResult>> queue = new LinkedBlockingQueue<>();
        BroadcastReceiver receiver = new NewProcessReceiver() {
            @Override
            void onReceive(NewProcessResult result) {
                if (!result.getToken().equals(token)) return;
                queue.offer(new AtomicReference<>(result));
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else context.registerReceiver(receiver, filter);
        ExecutorService executorService = Executors.newCachedThreadPool();
        Future<AtomicReference<NewProcessResult>> future = executorService.submit(queue::take);
        try {
            Process process = appProcess.start(context.getPackageCodePath(), NewProcess.class, new String[]{
                    String.format("--package=%s", context.getPackageName()),
                    String.format("--token=%s", token),
                    String.format("--component=%s", componentName.flattenToString())
            });
            executorService.execute(() -> {
                try {
                    waitFor(process, 15, TimeUnit.SECONDS);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                queue.offer(new AtomicReference<>());
            });
            NewProcessResult result = future.get().get();
            IBinder binder = result != null ? result.getBinder() : null;
            if (binder == null) process.destroy();
            return binder;
        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        } finally {
            context.unregisterReceiver(receiver);
            executorService.shutdown();
        }
    }

    public static String ACTION_SEND_NEW_PROCESS = "com.rosan.app_process.send.new_process";

    public static String EXTRA_NEW_PROCESS = "new_process";

    public static String EXTRA_TOKEN = "token";

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

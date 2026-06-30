package com.rosan.app_process;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RemoteProcessImpl extends IRemoteProcess.Stub {
    private static final String TAG = "RemoteProcessImpl";

    private final @NonNull Process mProcess;

    private ParcelFileDescriptor mInputStream;

    private ParcelFileDescriptor mOutputStream;

    private ParcelFileDescriptor mErrorStream;

    public RemoteProcessImpl(@NonNull Process process) {
        mProcess = process;
    }

    @Override
    public ParcelFileDescriptor getOutputStream() {
        if (mOutputStream != null) return mOutputStream;
        try {
            mOutputStream = parcelable(mProcess.getOutputStream());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return mOutputStream;
    }

    @Override
    public ParcelFileDescriptor getInputStream() throws RemoteException {
        if (mInputStream != null) return mInputStream;
        try {
            mInputStream = parcelable(mProcess.getInputStream());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return mInputStream;
    }

    @Override
    public ParcelFileDescriptor getErrorStream() {
        if (mErrorStream != null) return mErrorStream;
        try {
            mErrorStream = parcelable(mProcess.getErrorStream());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return mErrorStream;
    }

    @Override
    public int exitValue() {
        return mProcess.exitValue();
    }

    @Override
    public void destroy() {
        mProcess.destroy();
    }

    @Override
    public int waitFor() {
        try {
            return mProcess.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public static ParcelFileDescriptor parcelable(InputStream inputStream) throws IOException {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        transferThread(inputStream, new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]));
        return pipe[0];
    }

    public static ParcelFileDescriptor parcelable(OutputStream outputStream) throws IOException {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        transferThread(new ParcelFileDescriptor.AutoCloseInputStream(pipe[0]), outputStream);
        return pipe[1];
    }

    public static void transferThread(InputStream inputStream, OutputStream outputStream) {
        Thread thread = new Thread(() -> {
            byte[] bytes = new byte[8192];
            int len;
            try {
                while ((len = inputStream.read(bytes)) != -1) {
                    if (len > 0) {
                        outputStream.write(bytes, 0, len);
                        outputStream.flush();
                    }
                }
            } catch (IOException e) {
                Log.w(TAG, "Process stream transfer failed", e);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "Failed to close process input stream", e);
                }
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "Failed to close process output stream", e);
                }
            }
        }, "RemoteProcessStreamTransfer");
        thread.setDaemon(true);
        thread.start();
    }
}

package android.content.res;

import android.content.res.loader.AssetsProvider;

import java.io.FileDescriptor;
import java.io.IOException;

public class ApkAssets {
    public static ApkAssets loadFromPath(String path) throws IOException {
        return null;
    }

    public static ApkAssets loadFromPath(String path, int flags) throws IOException {
        return null;
    }

    public static ApkAssets loadFromPath(String path, int flags,
                                         AssetsProvider assets) throws IOException {
        return null;
    }

    public static ApkAssets loadFromFd(FileDescriptor fd,
                                       String friendlyName,
                                       int flags,
                                       AssetsProvider assets) throws IOException {
        return null;
    }

    public static ApkAssets loadFromFd(FileDescriptor fd,
                                       String friendlyName,
                                       long offset,
                                       long length,
                                       int flags,
                                       AssetsProvider assets)
            throws IOException {
        return null;
    }

    public static ApkAssets loadFromFd(FileDescriptor fd,
                                       String friendlyName, boolean system, boolean forceSharedLibrary) {
        return null;
    }

    public static ApkAssets loadOverlayFromPath(String idmapPath,
                                                int flags) throws IOException {
        return null;
    }
}

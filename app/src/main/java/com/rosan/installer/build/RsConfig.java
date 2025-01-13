package com.rosan.installer.build;

import android.os.Build;
import android.text.TextUtils;

import com.rosan.installer.BuildConfig;

public final class RsConfig {
    public static final Level LEVEL = getLevel();

    private static Level getLevel() {
        Level level = Level.UNSTABLE;
        switch (BuildConfig.BUILD_LEVEL) {
            case 1:
                level = Level.PREVIEW;
                break;
            case 2:
                level = Level.STABLE;
                break;
        }
        return level;
    }

    public static final String versionName = BuildConfig.VERSION_NAME;

    public static final int versionCode = BuildConfig.VERSION_CODE;

    private static String getSystemVersion() {
        if (Build.VERSION.PREVIEW_SDK_INT != 0)
            return String.format("%1$s Preview (API %2$s)",
                    Build.VERSION.CODENAME,
                    Build.VERSION.SDK_INT);
        else return String.format("%1$s (API %2$s)",
                Build.VERSION.RELEASE,
                Build.VERSION.SDK_INT);
    }

    public static final String systemVersion = getSystemVersion();

    private static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER.toUpperCase();
        String brand = Build.BRAND.toUpperCase();
        if (!TextUtils.equals(brand, manufacturer)) manufacturer += " " + brand;
        manufacturer += " " + Build.MODEL;
        return manufacturer;
    }

    public static final String deviceName = getDeviceName();

    private static String getSystemStruct() {
        String struct = System.getProperty("os.arch");
        if (struct == null) struct = "unknown";
        String[] abis = Build.SUPPORTED_ABIS;
        if (abis.length == 0)
            struct += " (Not Supported Native ABI)";
        else {
            StringBuilder supportABIs = new StringBuilder();
            for (int i = 0; i < abis.length; i++) {
                supportABIs.append(abis[i]);
                if (i + 1 < abis.length) supportABIs.append(", ");
            }
            struct += " (" + supportABIs + ")";
        }
        return struct;
    }

    public static final String systemStruct = getSystemStruct();
}

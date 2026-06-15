package android.content.pm;

public final class PackageInstallerHidden {
    public static final String ACTION_CONFIRM_INSTALL =
            "android.content.pm.action.CONFIRM_INSTALL";
    public static final String ACTION_CONFIRM_PERMISSIONS =
            "android.content.pm.action.CONFIRM_PERMISSIONS";
    public static final String ACTION_CONFIRM_PRE_APPROVAL =
            "android.content.pm.action.CONFIRM_PRE_APPROVAL";
    public static final String ACTION_UNARCHIVE_DIALOG =
            "com.android.intent.action.UNARCHIVE_DIALOG";
    public static final String ACTION_UNARCHIVE_ERROR_DIALOG =
            "com.android.intent.action.UNARCHIVE_ERROR_DIALOG";

    public static final String EXTRA_UNARCHIVE_INTENT_SENDER =
            "android.content.pm.extra.UNARCHIVE_INTENT_SENDER";
    public static final String EXTRA_UNARCHIVE_STATUS =
            "android.content.pm.extra.UNARCHIVE_STATUS";
    public static final String EXTRA_UNARCHIVE_REQUIRED_BYTES =
            "com.android.content.pm.extra.UNARCHIVE_EXTRA_REQUIRED_BYTES";
    public static final String EXTRA_UNARCHIVE_INSTALLER_PACKAGE_NAME =
            "com.android.content.pm.extra.UNARCHIVE_INSTALLER_PACKAGE_NAME";
    public static final String EXTRA_UNARCHIVE_INSTALLER_TITLE =
            "com.android.content.pm.extra.UNARCHIVE_INSTALLER_TITLE";
    public static final String EXTRA_LEGACY_STATUS =
            "android.content.pm.extra.LEGACY_STATUS";
    public static final String EXTRA_DELETE_FLAGS =
            "android.content.pm.extra.DELETE_FLAGS";

    public static final long MATCH_ARCHIVED_PACKAGES = 0x00004000L;

    private PackageInstallerHidden() {
        throw new UnsupportedOperationException();
    }
}

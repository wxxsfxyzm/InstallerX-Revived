package android.content;

public final class IntentHidden {
    public static final String EXTRA_INSTALL_RESULT =
            "android.intent.extra.INSTALL_RESULT";

    public static final String EXTRA_ORIGINATING_UID =
            "android.intent.extra.ORIGINATING_UID";

    private IntentHidden() {
        throw new UnsupportedOperationException();
    }
}

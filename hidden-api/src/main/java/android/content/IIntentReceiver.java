package android.content;

import android.os.Bundle;
import android.os.IInterface;

public interface IIntentReceiver extends IInterface {
    void performReceive(Intent intent, int resultCode, String data,
                        Bundle extras, boolean ordered, boolean sticky, int sendingUser);
}

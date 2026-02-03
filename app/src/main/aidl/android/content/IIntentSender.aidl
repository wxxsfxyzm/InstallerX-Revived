package android.content;

import android.content.IIntentReceiver;
import android.content.Intent;
import android.os.Bundle;

oneway interface IIntentSender {
    void send(int code, in Intent intent, String resolvedType, in IBinder whitelistToken,
            IIntentReceiver finishedReceiver, String requiredPermission, in Bundle options);
}
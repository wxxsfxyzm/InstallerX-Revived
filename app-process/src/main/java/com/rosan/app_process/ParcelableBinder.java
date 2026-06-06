package com.rosan.app_process;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class ParcelableBinder implements Parcelable {
    public static final Creator<ParcelableBinder> CREATOR = new Creator<ParcelableBinder>() {
        @Override
        public ParcelableBinder createFromParcel(Parcel in) {
            return new ParcelableBinder(in);
        }

        @Override
        public ParcelableBinder[] newArray(int size) {
            return new ParcelableBinder[size];
        }
    };

    private final IBinder binder;

    public ParcelableBinder(IBinder binder) {
        this.binder = binder;
    }

    public ParcelableBinder(Parcel parcel) {
        this.binder = parcel.readStrongBinder();
    }

    public IBinder getBinder() {
        return binder;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStrongBinder(binder);
    }
}

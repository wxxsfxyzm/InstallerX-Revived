/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: C:\\Users\\wzyli\\AppData\\Local\\Android\\Sdk\\build-tools\\36.0.0\\aidl.exe -pC:\\Users\\wzyli\\AppData\\Local\\Android\\Sdk\\platforms\\android-36\\framework.aidl -oC:\\Users\\wzyli\\StudioProjects\\InstallerX-Revived\\app\\build\\generated\\aidl_source_output_dir\\onlineUnstableDebug\\out -IC:\\Users\\wzyli\\StudioProjects\\InstallerX-Revived\\app\\src\\main\\aidl -IC:\\Users\\wzyli\\StudioProjects\\InstallerX-Revived\\app\\src\\Unstable\\aidl -IC:\\Users\\wzyli\\StudioProjects\\InstallerX-Revived\\app\\src\\online\\aidl -IC:\\Users\\wzyli\\StudioProjects\\InstallerX-Revived\\app\\src\\onlineUnstable\\aidl -IC:\\Users\\wzyli\\StudioProjects\\InstallerX-Revived\\app\\src\\debug\\aidl -IC:\\Users\\wzyli\\StudioProjects\\InstallerX-Revived\\app\\src\\onlineUnstableDebug\\aidl -IC:\\Users\\wzyli\\.gradle\\caches\\9.3.0\\transforms\\95635dc21a473f178fe76a014958a7c7\\workspace\\transformed\\core-1.17.0\\aidl -IC:\\Users\\wzyli\\.gradle\\caches\\9.3.0\\transforms\\086cfa218e07e615e997d260a7cc3b91\\workspace\\transformed\\versionedparcelable-1.1.1\\aidl -IC:\\Users\\wzyli\\.gradle\\caches\\9.3.0\\transforms\\e1031db55caf761cb800e4232c49451a\\workspace\\transformed\\AndroidAppProcess-v1.4.1-revived\\aidl -dC:\\Users\\wzyli\\AppData\\Local\\Temp\\aidl16520019278230846774.d C:\\Users\\wzyli\\StudioProjects\\InstallerX-Revived\\app\\src\\main\\aidl\\android\\content\\IIntentReceiver.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.content;
public interface IIntentReceiver extends android.os.IInterface
{
  /** Default implementation for IIntentReceiver. */
  public static class Default implements android.content.IIntentReceiver
  {
    @Override public void performReceive(android.content.Intent intent, int resultCode, java.lang.String data, android.os.Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.content.IIntentReceiver
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.content.IIntentReceiver interface,
     * generating a proxy if needed.
     */
    public static android.content.IIntentReceiver asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.content.IIntentReceiver))) {
        return ((android.content.IIntentReceiver)iin);
      }
      return new android.content.IIntentReceiver.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
      }
      switch (code)
      {
        case TRANSACTION_performReceive:
        {
          android.content.Intent _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.Intent.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          java.lang.String _arg2;
          _arg2 = data.readString();
          android.os.Bundle _arg3;
          _arg3 = _Parcel.readTypedObject(data, android.os.Bundle.CREATOR);
          boolean _arg4;
          _arg4 = (0!=data.readInt());
          boolean _arg5;
          _arg5 = (0!=data.readInt());
          int _arg6;
          _arg6 = data.readInt();
          this.performReceive(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.content.IIntentReceiver
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public void performReceive(android.content.Intent intent, int resultCode, java.lang.String data, android.os.Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, intent, 0);
          _data.writeInt(resultCode);
          _data.writeString(data);
          _Parcel.writeTypedObject(_data, extras, 0);
          _data.writeInt(((ordered)?(1):(0)));
          _data.writeInt(((sticky)?(1):(0)));
          _data.writeInt(sendingUser);
          boolean _status = mRemote.transact(Stub.TRANSACTION_performReceive, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    /** @hide */
    public static final java.lang.String DESCRIPTOR = "android.content.IIntentReceiver";
    static final int TRANSACTION_performReceive = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
  }
  public void performReceive(android.content.Intent intent, int resultCode, java.lang.String data, android.os.Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws android.os.RemoteException;
  /** @hide */
  static class _Parcel {
    static private <T> T readTypedObject(
        android.os.Parcel parcel,
        android.os.Parcelable.Creator<T> c) {
      if (parcel.readInt() != 0) {
          return c.createFromParcel(parcel);
      } else {
          return null;
      }
    }
    static private <T extends android.os.Parcelable> void writeTypedObject(
        android.os.Parcel parcel, T value, int parcelableFlags) {
      if (value != null) {
        parcel.writeInt(1);
        value.writeToParcel(parcel, parcelableFlags);
      } else {
        parcel.writeInt(0);
      }
    }
  }
}

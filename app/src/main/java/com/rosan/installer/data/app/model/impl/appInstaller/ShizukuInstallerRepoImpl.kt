package com.rosan.installer.data.app.model.impl.appInstaller

import android.os.IBinder
import com.rosan.installer.data.recycle.util.requireShizukuPermissionGranted
import rikka.shizuku.ShizukuBinderWrapper

object ShizukuInstallerRepoImpl : IBinderInstallerRepoImpl() {
    override suspend fun iBinderWrapper(iBinder: IBinder): IBinder =
        requireShizukuPermissionGranted {
            ShizukuBinderWrapper(iBinder)
        }
}
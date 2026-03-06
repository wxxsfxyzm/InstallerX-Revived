package com.rosan.installer.data.engine.executor.appInstaller

import android.os.IBinder
import com.rosan.installer.data.privileged.util.requireShizukuPermissionGranted
import rikka.shizuku.ShizukuBinderWrapper

object ShizukuInstallerRepoImpl : IBinderInstallerRepoImpl() {
    override suspend fun iBinderWrapper(iBinder: IBinder): IBinder =
        requireShizukuPermissionGranted {
            ShizukuBinderWrapper(iBinder)
        }
}
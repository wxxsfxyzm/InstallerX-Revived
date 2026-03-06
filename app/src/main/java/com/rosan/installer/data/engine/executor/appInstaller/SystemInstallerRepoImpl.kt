package com.rosan.installer.data.engine.executor.appInstaller

import android.os.IBinder

object SystemInstallerRepoImpl : IBinderInstallerRepoImpl() {
    override suspend fun iBinderWrapper(iBinder: IBinder): IBinder = iBinder
}
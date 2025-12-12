package com.rosan.installer.data.app.model.impl.appInstaller

import android.os.IBinder

object SystemInstallerRepoImpl : IBinderInstallerRepoImpl() {
    override suspend fun iBinderWrapper(iBinder: IBinder): IBinder = iBinder
}
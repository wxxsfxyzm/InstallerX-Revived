package com.rosan.installer.data.installer.model.impl.installer

import com.rosan.installer.data.installer.repo.InstallerRepo
import kotlinx.coroutines.CoroutineScope

abstract class Handler(val scope: CoroutineScope, open val installer: InstallerRepo) {
    abstract suspend fun onStart()

    abstract suspend fun onFinish()
}
package com.rosan.installer.data.recycle.model.impl.recycler

import com.rosan.installer.IPrivilegedService
import com.rosan.installer.data.recycle.model.entity.DefaultPrivilegedService
import com.rosan.installer.data.recycle.repo.Recycler
import com.rosan.installer.data.recycle.repo.recyclable.UserService
import com.rosan.installer.data.recycle.util.requireShizukuPermissionGranted
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import timber.log.Timber

/**
 * A Recycler that provides a UserService operating in "Shizuku Hook Mode".
 * It does NOT modify the global process state. Instead, it provides a PrivilegedService
 * that internally fetches hooked system services on-demand from the ShizukuHook factory.
 */
object ShizukuHookRecycler : Recycler<ShizukuHookRecycler.HookedUserService>(), KoinComponent {

    /**
     * A lightweight UserService that is aware of the "hook mode".
     * It acts as a KoinComponent to inject dependencies needed by DefaultPrivilegedService.
     */
    class HookedUserService : UserService, KoinComponent {
        override val privileged: IPrivilegedService by lazy {
            DefaultPrivilegedService(isHookMode = true)
        }

        override fun close() {
            Timber.tag("ShizukuHookRecycler").d("close() called, no action needed in hook mode.")
        }
    }

    override fun onMake(): HookedUserService = runBlocking {
        requireShizukuPermissionGranted {
            HookedUserService()
        }
    }
}
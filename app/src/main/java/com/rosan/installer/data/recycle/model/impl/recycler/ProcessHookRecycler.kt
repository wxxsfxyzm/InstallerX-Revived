package com.rosan.installer.data.recycle.model.impl.recycler

import android.content.Context
import android.os.IBinder
import com.rosan.app_process.AppProcess
import com.rosan.installer.IPrivilegedService
import com.rosan.installer.data.recycle.model.entity.DefaultPrivilegedService
import com.rosan.installer.data.recycle.repo.Recyclable
import com.rosan.installer.data.recycle.repo.Recycler
import com.rosan.installer.data.recycle.repo.recyclable.UserService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * A Recycler that provides a UserService operating in "Process Hook Mode" (Root Hook).
 *
 * It creates a local [DefaultPrivilegedService] but proxies underlying Binder calls
 * through a remote [AppProcess] (Root/Shell).
 */
class ProcessHookRecycler(private val shell: String) :
    Recycler<ProcessHookRecycler.HookedUserService>(), KoinComponent {

    private val context by inject<Context>()

    class HookedUserService(
        private val appProcessHandle: Recyclable<AppProcess>
    ) : UserService {

        // Provide access to the binder wrapper logic for external users (like InstallerRepo)
        fun binderWrapper(binder: IBinder): IBinder {
            return appProcessHandle.entity.binderWrapper(binder)
        }

        // Inject the binder wrapper logic into DefaultPrivilegedService for standard service calls
        override val privileged: IPrivilegedService by lazy {
            DefaultPrivilegedService { binder ->
                binderWrapper(binder)
            }
        }

        override fun close() {
            // Recycle the underlying AppProcess (shell) when this service is closed
            appProcessHandle.recycle()
        }
    }

    override fun onMake(): HookedUserService {
        // Obtain a raw AppProcess shell from the existing recyclers
        val appProcessHandle = AppProcessRecyclers.get(shell).make()

        // Critical Fix: Ensure the reused AppProcess is initialized.
        // If the process was previously closed/recycled, its context/manager might be null.
        // init() checks state internally and is safe to call multiple times.
        if (!appProcessHandle.entity.init(context)) {
            // If init fails, we might want to recycle it and try fresh or throw,
            // but usually init() will re-create the manager.
            // If it fails returning false, it usually means binder connection failed.
            throw IllegalStateException("Failed to initialize AppProcess for Hook Mode.")
        }

        return HookedUserService(appProcessHandle)
    }
}
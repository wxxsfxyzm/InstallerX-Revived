package com.rosan.installer.data.recycle.model.impl.recycler

import com.rosan.app_process.AppProcess
import com.rosan.installer.IPrivilegedService
import com.rosan.installer.data.recycle.model.entity.DefaultPrivilegedService
import com.rosan.installer.data.recycle.repo.Recyclable
import com.rosan.installer.data.recycle.repo.Recycler
import com.rosan.installer.data.recycle.repo.recyclable.UserService
import org.koin.core.component.KoinComponent

/**
 * A Recycler that provides a UserService operating in "Process Hook Mode" (Root Hook).
 *
 * Unlike [ProcessUserServiceRecycler], which runs the logic inside the remote root process,
 * this recycler runs [DefaultPrivilegedService] in the local app process but proxies
 * system service calls (PackageManager, etc.) through the root process via [AppProcess.binderWrapper].
 *
 * This mimics the architecture of [ShizukuHookRecycler].
 */
class ProcessHookRecycler(private val shell: String) :
    Recycler<ProcessHookRecycler.HookedUserService>(), KoinComponent {

    class HookedUserService(
        private val appProcessHandle: Recyclable<AppProcess>
    ) : UserService {

        // Inject the binder wrapper logic into DefaultPrivilegedService
        override val privileged: IPrivilegedService by lazy {
            DefaultPrivilegedService { binder ->
                // This is the core magic: wrap the local binder using the root process proxy
                appProcessHandle.entity.binderWrapper(binder)
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
        return HookedUserService(appProcessHandle)
    }
}
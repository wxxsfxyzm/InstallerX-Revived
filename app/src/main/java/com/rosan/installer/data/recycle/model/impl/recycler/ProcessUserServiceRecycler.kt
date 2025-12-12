package com.rosan.installer.data.recycle.model.impl.recycler

import android.content.ComponentName
import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.os.RemoteException
import android.system.Os
import androidx.annotation.Keep
import com.rosan.app_process.AppProcess
import com.rosan.installer.IAppProcessService
import com.rosan.installer.IPrivilegedService
import com.rosan.installer.data.recycle.model.entity.DefaultPrivilegedService
import com.rosan.installer.data.recycle.repo.Recycler
import com.rosan.installer.data.recycle.repo.recyclable.UserService
import com.rosan.installer.di.init.processModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import timber.log.Timber
import kotlin.system.exitProcess

class ProcessUserServiceRecycler(private val shell: String) :
    Recycler<ProcessUserServiceRecycler.UserServiceProxy>(), KoinComponent {

    class UserServiceProxy(val service: IAppProcessService) : UserService {
        override val privileged: IPrivilegedService = service.privilegedService

        override fun close() = service.quit()
    }

    class AppProcessService @Keep constructor(context: Context) : IAppProcessService.Stub() {
        init {
            if (GlobalContext.getOrNull() == null) {
                startKoin {
                    modules(processModules)
                    androidContext(context)
                }
            }
        }

        private val privileged = DefaultPrivilegedService()

        /**
         * Quit logic: Kill the JVM and the Parent Shell.
         */
        override fun quit() {
            try {
                // Get the Parent Process ID (The Shell/SU process)
                val ppid = Os.getppid()

                Timber.tag("AppProcessService").i("Quitting... Killing parent shell process (PID: $ppid)")

                // Kill the parent shell first.
                // Since we are running as Root (via su), we have permission to do this.
                // This ensures the "smaller PID" process dies.
                android.os.Process.killProcess(ppid)
            } catch (e: Exception) {
                Timber.tag("AppProcessService").e(e, "Failed to kill parent process")
            } finally {
                // Kill self (The Java JVM process)
                exitProcess(0)
            }
        }

        override fun getPrivilegedService(): IPrivilegedService = privileged

        override fun registerDeathToken(token: IBinder?) {
            try {
                token?.linkToDeath({
                    Timber.tag("AppProcessService").w("Client died unexpectedly. Committing suicide.")
                    quit()
                }, 0)
            } catch (e: RemoteException) {
                Timber.tag("AppProcessService").e(e, "Client already dead during registration.")
                quit()
            }
        }
    }

    private val context by inject<Context>()

    private lateinit var appProcessRecycler: Recycler<AppProcess>

    override fun onMake(): UserServiceProxy {
        appProcessRecycler = AppProcessRecyclers.get(shell)
        val binder = appProcessRecycler.make().entity.isolatedServiceBinder(
            ComponentName(
                context, AppProcessService::class.java
            )
        )
        binder.linkToDeath({
            if (entity?.service?.asBinder() == binder) recycleForcibly()
        }, 0)
        val serviceInterface = IAppProcessService.Stub.asInterface(binder)

        try {
            serviceInterface.registerDeathToken(Binder())
        } catch (e: RemoteException) {
            recycleForcibly()
            throw e
        }

        return UserServiceProxy(serviceInterface)
    }

    override fun onRecycle() {
        super.onRecycle()
        if (::appProcessRecycler.isInitialized) {
            appProcessRecycler.recycle()
        }
    }
}
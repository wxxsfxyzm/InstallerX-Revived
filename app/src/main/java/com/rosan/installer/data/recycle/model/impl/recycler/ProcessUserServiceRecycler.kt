package com.rosan.installer.data.recycle.model.impl.recycler

import android.content.ComponentName
import android.content.Context
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
import org.koin.core.context.startKoin
import kotlin.system.exitProcess

class ProcessUserServiceRecycler(private val shell: String) :
    Recycler<ProcessUserServiceRecycler.UserServiceProxy>(), KoinComponent {
    class UserServiceProxy(val service: IAppProcessService) : UserService {
        override val privileged: IPrivilegedService = service.privilegedService

        override fun close() = service.quit()
    }

    class AppProcessService @Keep constructor(context: Context) : IAppProcessService.Stub() {
        init {
            startKoin {
                modules(processModules)
                androidContext(context)
            }
        }

        private val privileged = DefaultPrivilegedService()

        override fun quit() {
            exitProcess(0)
        }

        override fun getPrivilegedService(): IPrivilegedService = privileged
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
        return UserServiceProxy(IAppProcessService.Stub.asInterface(binder))
    }

    override fun onRecycle() {
        super.onRecycle()
        appProcessRecycler.recycle()
    }
}
package com.rosan.installer.data.recycle.model.impl.recycler

import android.content.ComponentName
import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.os.RemoteException
import androidx.annotation.Keep
import com.rosan.app_process.AppProcess
import com.rosan.installer.IAppProcessService
import com.rosan.installer.IPrivilegedService
import com.rosan.installer.data.recycle.model.entity.DefaultPrivilegedService
import com.rosan.installer.data.recycle.repo.Recyclable
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

    class UserServiceProxy(
        val service: IAppProcessService,
        private val appProcessHandle: Recyclable<AppProcess>,
        private val binder: IBinder,            // 新增
        private val deathRecipient: IBinder.DeathRecipient // 新增
    ) : UserService {
        override val privileged: IPrivilegedService = service.privilegedService

        override fun close() {
            // 关键修复：先解绑监听，防止 quit() 触发 recycleForcibly()
            try {
                binder.unlinkToDeath(deathRecipient, 0)
            } catch (e: Exception) {
                // 忽略解绑失败
            }

            // 现在可以放心地让它去死了
            runCatching { service.quit() }
            appProcessHandle.recycle()
        }
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

        override fun quit() {
            try {
                val ppid = android.system.Os.getppid()
                Timber.i("Quitting... Killing parent shell (PID: $ppid)")
                android.os.Process.killProcess(ppid)
            } catch (e: Exception) {
                Timber.e(e, "Failed to kill parent process")
            } finally {
                exitProcess(0)
            }
        }

        override fun getPrivilegedService(): IPrivilegedService = privileged

        override fun registerDeathToken(token: IBinder?) {
            try {
                token?.linkToDeath({
                    Timber.w("Client died. Quitting...")
                    quit()
                }, 0)
            } catch (e: RemoteException) {
                Timber.e(e, "Client already dead")
                quit()
            }
        }
    }

    private val context by inject<Context>()

    override fun onMake(): UserServiceProxy {
        val appProcessRecycler = AppProcessRecyclers.get(shell)
        val appProcessHandle = appProcessRecycler.make()

        val maxRetries = 5
        val initialDelay = 100L // 100ms
        var currentBinder: IBinder? = null

        var attempt = 0
        while (attempt < maxRetries) {
            try {
                currentBinder = appProcessHandle.entity.isolatedServiceBinder(
                    ComponentName(context, AppProcessService::class.java)
                )

                if (currentBinder != null) {
                    if (currentBinder.isBinderAlive) {
                        break
                    } else {
                        Timber.w("Attempt ${attempt + 1}: Binder retrieved but dead.")
                    }
                } else {
                    Timber.w("Attempt ${attempt + 1}: isolatedServiceBinder returned null.")
                }
            } catch (e: Exception) {
                Timber.w(e, "Attempt ${attempt + 1}: Exception during bind.")
            }

            attempt++
            if (attempt < maxRetries) {
                Thread.sleep(initialDelay * (1 shl (attempt - 1)))
            }
        }

        val binder = currentBinder

        if (binder == null) {
            appProcessHandle.recycle()
            throw IllegalStateException("Failed to bind AppProcessService after $maxRetries attempts. Child process may have crashed or timed out.")
        }

        val deathRecipient = IBinder.DeathRecipient {
            Timber.w("Remote service died, forcing recycle")
            recycleForcibly()
        }

        try {
            binder.linkToDeath(deathRecipient, 0)
        } catch (e: RemoteException) {
            appProcessHandle.recycle()
            throw e
        }

        val serviceInterface = IAppProcessService.Stub.asInterface(binder)

        try {
            serviceInterface.registerDeathToken(Binder())
        } catch (e: RemoteException) {
            appProcessHandle.recycle()
            throw e
        }

        return UserServiceProxy(serviceInterface, appProcessHandle, binder, deathRecipient)
    }

    // 不再需要 onRecycle，因为 UserServiceProxy.close() 会处理
}
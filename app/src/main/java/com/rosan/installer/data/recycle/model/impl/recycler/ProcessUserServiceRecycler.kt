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
        // 注意：make() 可能会启动新进程，这本身就需要时间
        val appProcessHandle = appProcessRecycler.make()

        // 定义重试策略
        val maxRetries = 5
        val initialDelay = 100L // 100ms
        var currentBinder: IBinder? = null

        // 使用 runBlocking 并不是最佳实践，但在 onMake 这种同步上下文中是必要的妥协
        // 更好的方式是将 onMake 改为 suspend (但这需要修改 Recycler 基类)
        // 这里我们用简单的 while 循环配合 Thread.sleep 模拟重试，避免引入协程死锁风险
        var attempt = 0
        while (attempt < maxRetries) {
            try {
                currentBinder = appProcessHandle.entity.isolatedServiceBinder(
                    ComponentName(context, AppProcessService::class.java)
                )

                if (currentBinder != null) {
                    if (currentBinder.isBinderAlive) {
                        // 成功获取且 Binder 存活
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
                // 简单的指数退避：100ms, 200ms, 400ms, 800ms...
                Thread.sleep(initialDelay * (1 shl (attempt - 1)))
            }
        }

        val binder = currentBinder

        // 最终检查
        if (binder == null) {
            appProcessHandle.recycle()
            // 抛出具体异常，方便排查是超时还是彻底失败
            throw IllegalStateException("Failed to bind AppProcessService after $maxRetries attempts. Child process may have crashed or timed out.")
        }

        // 定义 recipient 变量
        val deathRecipient = IBinder.DeathRecipient {
            Timber.w("Remote service died, forcing recycle")
            recycleForcibly()
        }

        // 监听服务端死亡
        try {
            // 使用变量注册
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
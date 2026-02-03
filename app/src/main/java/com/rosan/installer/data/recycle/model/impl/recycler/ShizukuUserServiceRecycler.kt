package com.rosan.installer.data.recycle.model.impl.recycler

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Process
import android.os.UserHandle
import androidx.annotation.Keep
import com.rosan.installer.IPrivilegedService
import com.rosan.installer.IShizukuUserService
import com.rosan.installer.data.recycle.model.entity.DefaultPrivilegedService
import com.rosan.installer.data.recycle.repo.Recycler
import com.rosan.installer.data.recycle.repo.recyclable.UserService
import com.rosan.installer.data.recycle.util.requireShizukuPermissionGranted
import com.rosan.installer.di.init.processModules
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import rikka.shizuku.Shizuku
import timber.log.Timber
import kotlin.system.exitProcess

object ShizukuUserServiceRecycler : Recycler<ShizukuUserServiceRecycler.UserServiceProxy>(),
    KoinComponent {
    class UserServiceProxy(val service: IShizukuUserService) : UserService {
        override val privileged: IPrivilegedService = service.privilegedService

        override fun close() = service.destroy()
    }

    class ShizukuUserService @Keep constructor(context: Context) : IShizukuUserService.Stub() {
        init {
            startKoin {
                modules(processModules)
                androidContext(createSystemContext(context))
            }
        }

        private val privileged = DefaultPrivilegedService(isHookMode = false)

        override fun destroy() {
            exitProcess(0)
        }

        override fun getPrivilegedService(): IPrivilegedService = privileged
    }

    /**
     * Creates a system-level [Context] for operations that require elevated privileges.
     *
     * This method attempts to obtain the system context via reflection on the
     * `ActivityThread` class, then creates a package context as the `com.android.shell` user.
     * If any part of this process fails, it falls back to the provided [fallbackContext].
     *
     * Logic is from the ShellInterface.kt file in the InstallWithOptions project.
     * Under MIT License.
     *
     * @param fallbackContext The fallback context to return if system context creation fails.
     * @return A [Context] instance for `com.android.shell`, or [fallbackContext] if an error occurs.
     *
     * @see <a href="https://github.com/zacharee/InstallWithOptions/blob/main/app/src/main/java/dev/zwander/installwithoptions/util/ShellInterface.kt">ShellInterface</a>
     * @see <a href="https://github.com/zacharee/InstallWithOptions/blob/main/LICENSE">MIT License</a>
     */
    private fun createSystemContext(fallbackContext: Context): Context {
        try {
            // Get system context
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val systemMain = activityThreadClass.getMethod("systemMain").invoke(null)
            val systemContext = activityThreadClass.getMethod("getSystemContext").invoke(systemMain) as Context

            val userId = try {
                UserHandle::class.java.getMethod("myUserId").invoke(null) as Int
            } catch (e: Exception) {
                Timber.tag("ShizukuUserService").e(e, "Failed to get userId")
                Process.myUid() / 100000
            }

            val userHandle = Class.forName("android.os.UserHandle")
            val userHandleConstructor = userHandle.getConstructor(Int::class.java)
            val userHandleInstance = userHandleConstructor.newInstance(userId)

            val context = systemContext.javaClass.getMethod(
                "createPackageContextAsUser",
                String::class.java,
                Int::class.java,
                userHandle
            ).invoke(
                systemContext,
                "com.android.shell",
                Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY,
                userHandleInstance
            ) as Context

            val finalContext = context.createPackageContext("com.android.shell", 0)
            Timber.tag("ShizukuUserService").d("Created system context: ${finalContext.packageName}, UID: ${Process.myUid()}")
            return finalContext
        } catch (e: Exception) {
            Timber.tag("ShizukuUserService").e(e, "Failed to create system context: ${e.message}")
            Timber.tag("ShizukuUserService").d("Falling back to app context: ${fallbackContext.packageName}")
            return fallbackContext
        }
    }

    private val context by inject<Context>()

    override fun onMake(): UserServiceProxy = runBlocking {
        requireShizukuPermissionGranted {
            onInnerMake()
        }
    }

    private suspend fun onInnerMake(): UserServiceProxy = callbackFlow {
        Shizuku.bindUserService(
            Shizuku.UserServiceArgs(
                ComponentName(
                    context, ShizukuUserService::class.java
                )
            ).processNameSuffix("shizuku_privileged"), object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    trySend(UserServiceProxy(IShizukuUserService.Stub.asInterface(service)))
                    service?.linkToDeath({
                        if (entity?.service == service) recycleForcibly()
                    }, 0)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    close()
                }
            })
        awaitClose { }
    }.first()
}
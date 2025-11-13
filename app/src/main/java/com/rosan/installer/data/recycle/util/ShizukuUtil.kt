package com.rosan.installer.data.recycle.util

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.IActivityManager
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.os.IUserManager
import com.rosan.installer.BuildConfig
import com.rosan.installer.data.recycle.model.exception.ShizukuNotWorkException
import com.rosan.installer.data.reflect.repo.ReflectRepo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import rikka.sui.Sui
import timber.log.Timber

suspend fun <T> requireShizukuPermissionGranted(action: suspend () -> T): T {
    callbackFlow {
        Sui.init(BuildConfig.APPLICATION_ID)
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            send(Unit)
            awaitClose()
        } else {
            val requestCode = (Int.MIN_VALUE..Int.MAX_VALUE).random()
            val listener =
                Shizuku.OnRequestPermissionResultListener { _requestCode, grantResult ->
                    if (_requestCode != requestCode) return@OnRequestPermissionResultListener
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
                        trySend(Unit)
                    else close(Exception("sui/shizuku permission denied"))
                }
            Shizuku.addRequestPermissionResultListener(listener)
            Shizuku.requestPermission(requestCode)
            awaitClose { Shizuku.removeRequestPermissionResultListener(listener) }
        }
    }.catch {
        throw ShizukuNotWorkException(it)
    }.first()
    return action()
}

@SuppressLint("PrivateApi", "DiscouragedPrivateApi")
object ShizukuHook : KoinComponent {
    private val reflect by inject<ReflectRepo>()

    val hookedPackageManager: IPackageManager by lazy {
        Timber.tag("ShizukuHook").d("Creating on-demand hooked IPackageManager...")
        val originalBinder = SystemServiceHelper.getSystemService("package")
        val originalPM = IPackageManager.Stub.asInterface(originalBinder)

        val wrapper = ShizukuBinderWrapper(originalPM.asBinder())
        IPackageManager.Stub.asInterface(wrapper).also {
            Timber.tag("ShizukuHook").i("On-demand hooked IPackageManager created.")
        }
    }

    val hookedActivityManager: IActivityManager by lazy {
        Timber.tag("ShizukuHook").d("Creating on-demand hooked IActivityManager...")
        val amSingleton = reflect.getStaticObjectField(ActivityManager::class.java, "IActivityManagerSingleton")
        val singletonClass = Class.forName("android.util.Singleton")
        val mInstanceField = reflect.getDeclaredField(singletonClass, "mInstance")
        mInstanceField?.isAccessible = true
        val originalAM = mInstanceField?.get(amSingleton) as IActivityManager

        val wrapper = ShizukuBinderWrapper(originalAM.asBinder())
        IActivityManager.Stub.asInterface(wrapper).also {
            Timber.tag("ShizukuHook").i("On-demand hooked IActivityManager created.")
        }
    }

    val hookedUserManager: IUserManager by lazy {
        Timber.tag("ShizukuHook").d("Creating on-demand hooked IUserManager...")
        val originalBinder = SystemServiceHelper.getSystemService("user")
        val originalUM = IUserManager.Stub.asInterface(originalBinder)
        val wrapper = ShizukuBinderWrapper(originalUM.asBinder())
        IUserManager.Stub.asInterface(wrapper).also {
            Timber.tag("ShizukuHook").i("On-demand hooked IUserManager created.")
        }
    }
}
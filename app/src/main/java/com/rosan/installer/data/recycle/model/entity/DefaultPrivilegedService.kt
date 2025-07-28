package com.rosan.installer.data.recycle.model.entity

import android.annotation.SuppressLint
import android.app.IActivityManager
import android.app.IApplicationThread
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.content.pm.ParceledListSlice
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.os.ServiceManager
import android.util.Log
import androidx.core.net.toUri
import com.rosan.installer.data.recycle.util.InstallIntentFilter
import com.rosan.installer.data.recycle.util.deletePaths
import com.rosan.installer.data.reflect.repo.ReflectRepo
import org.koin.core.component.inject
import java.io.IOException
import java.lang.reflect.Method
import android.os.Process as AndroidProcess


class DefaultPrivilegedService : BasePrivilegedService() {
    private val reflect by inject<ReflectRepo>()

    @SuppressLint("LogNotTimber")
    override fun delete(paths: Array<out String>) = deletePaths(paths)

    override fun setDefaultInstaller(component: ComponentName, enable: Boolean) {
        val uid = AndroidProcess.myUid()
        val userId = uid / 100000
        val iPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))

        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .setDataAndType(
                "content://storage/emulated/0/test.apk".toUri(),
                "application/vnd.android.package-archive"
            )
        val list = queryIntentActivities(
            iPackageManager,
            intent,
            "application/vnd.android.package-archive",
            PackageManager.MATCH_DEFAULT_ONLY,
            userId
        )
        var bestMatch = 0
        val names = list.map {
            val iPackageName = it.activityInfo.packageName
            val iClassName = it.activityInfo.name

            if (it.match > bestMatch) bestMatch = it.match

            // clear preferred
            iPackageManager.clearPackagePreferredActivities(iPackageName)
            if (uid == 1000) iPackageManager.clearPackagePersistentPreferredActivities(
                iPackageName,
                userId
            )

            ComponentName(iPackageName, iClassName)
        }.toTypedArray()

        if (!enable) return

        iPackageManager.setLastChosenActivity(
            intent,
            intent.type,
            PackageManager.MATCH_DEFAULT_ONLY,
            InstallIntentFilter,
            bestMatch,
            component
        )
        addPreferredActivity(
            iPackageManager,
            InstallIntentFilter,
            bestMatch,
            names,
            component,
            userId,
            true
        )
        if (uid == 1000) addPersistentPreferredActivity(
            iPackageManager,
            InstallIntentFilter,
            component,
            userId
        )
    }

    @Throws(RemoteException::class)
    override fun execLine(command: String): String {
        return try {
            // 执行 shell 命令
            val process = Runtime.getRuntime().exec(command)
            // 读取执行结果
            readResult(process)
        } catch (e: IOException) {
            // 将 IOException 包装成 RemoteException 抛出
            throw RemoteException(e.message)
        } catch (e: InterruptedException) {
            // 恢复线程的中断状态
            Thread.currentThread().interrupt()
            // 将 InterruptedException 包装成 RemoteException 抛出
            throw RemoteException(e.message)
        }
    }

    @Throws(RemoteException::class)
    override fun execArr(command: Array<String>): String {
        return try {
            // 执行 shell 命令
            val process = Runtime.getRuntime().exec(command)
            // 读取执行结果
            readResult(process)
        } catch (e: IOException) {
            // 将 IOException 包装成 RemoteException 抛出
            throw RemoteException(e.message)
        } catch (e: InterruptedException) {
            // 恢复线程的中断状态
            Thread.currentThread().interrupt()
            // 将 InterruptedException 包装成 RemoteException 抛出
            throw RemoteException(e.message)
        }
    }

    // Cache the reflected method for performance.
    // A privileged process might live for a long time, so caching is beneficial.
    private var startActivityAsUserMethod: Method? = null
    private var requiresIApplicationThread: Boolean = false

    @SuppressLint("LogNotTimber")
    override fun startActivityPrivileged(intent: Intent): Boolean {
        // We are executing inside the privileged process (Root).
        // All API calls here inherit that privilege.
        try {
            // 1. Find the correct method reflectively, but only once.
            if (startActivityAsUserMethod == null) {
                findAndCacheStartActivityMethod()
            }

            // If after searching, the method is still null, we cannot proceed.
            val methodToCall = startActivityAsUserMethod
                ?: throw NoSuchMethodException("startActivityAsUser method could not be found via reflection.")

            // 2. Get the ActivityManagerService instance.
            val amBinder = ServiceManager.getService(Context.ACTIVITY_SERVICE)
                ?: run {
                    Log.e("PrivilegedService", "Failed to get ActivityManagerService binder.")
                    return false
                }
            val am = IActivityManager.Stub.asInterface(amBinder)

            // 3. Prepare common parameters.
            val userId = AndroidProcess.myUid() / 100000
            val callerPackage = "com.android.shell"
            val resolvedType = intent.resolveType(context.contentResolver)

            // 4. Invoke the method with the correct parameters based on our cached info.
            val result: Int = if (requiresIApplicationThread) {
                // Modern signature (Android O and newer)
                methodToCall.invoke(
                    am,
                    null as IApplicationThread?, // The first argument is IApplicationThread
                    callerPackage,
                    intent,
                    resolvedType,
                    null as IBinder?,
                    null as String?,
                    0,
                    0,
                    null as Bundle?, // ProfilerInfo is usually null, we use Bundle for wider compatibility
                    null as Bundle?,
                    userId
                ) as Int
            } else {
                // Older signature (pre-Android O)
                methodToCall.invoke(
                    am,
                    callerPackage,
                    intent,
                    resolvedType,
                    null as IBinder?,
                    null as String?,
                    0,
                    0,
                    null as Bundle?,
                    null as Bundle?,
                    userId
                ) as Int
            }

            Log.d("PrivilegedService", "IActivityManager.startActivityAsUser returned a result code: $result")
            return result >= 0

        } catch (e: Exception) {
            Log.e("PrivilegedService", "startActivityPrivileged via API call failed", e)
            return false
        }
    }

    /**
     * Finds the correct startActivityAsUser method using the ReflectRepo
     * and caches it for future use. This avoids repeated reflection lookups.
     */
    @SuppressLint("LogNotTimber")
    private fun findAndCacheStartActivityMethod() {
        Log.d("PrivilegedService", "Searching for startActivityAsUser method for the first time...")
        // In Android P and above, ProfilerInfo was deprecated and replaced by Bundle.
        // So we use Bundle.class for better forward compatibility.
        val bundleClass = Bundle::class.java

        // Strategy: Try the newest (Android O+) signature first.
        var method = reflect.getDeclaredMethod(
            IActivityManager::class.java,
            "startActivityAsUser",
            IApplicationThread::class.java,
            String::class.java,
            Intent::class.java,
            String::class.java,
            IBinder::class.java,
            String::class.java,
            Int::class.java,
            Int::class.java,
            bundleClass, // Represents ProfilerInfo
            Bundle::class.java, // Represents options
            Int::class.java     // userId
        )

        if (method != null) {
            Log.d("PrivilegedService", "Found modern (11-arg) startActivityAsUser method.")
            requiresIApplicationThread = true
        } else {
            // If the modern one fails, fall back to the older signature (without IApplicationThread).
            Log.d("PrivilegedService", "Modern method not found, trying older (10-arg) version.")
            method = reflect.getDeclaredMethod(
                IActivityManager::class.java,
                "startActivityAsUser",
                String::class.java,
                Intent::class.java,
                String::class.java,
                IBinder::class.java,
                String::class.java,
                Int::class.java,
                Int::class.java,
                bundleClass, // Represents ProfilerInfo
                Bundle::class.java, // Represents options
                Int::class.java     // userId
            )
            if (method != null) {
                Log.d("PrivilegedService", "Found older (10-arg) startActivityAsUser method.")
                requiresIApplicationThread = false
            }
        }

        method?.isAccessible = true
        startActivityAsUserMethod = method
    }

    private fun addPreferredActivity(
        iPackageManager: IPackageManager,
        filter: IntentFilter,
        match: Int,
        names: Array<ComponentName>,
        name: ComponentName,
        userId: Int,
        removeExisting: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            reflect.getDeclaredMethod(
                IPackageManager::class.java,
                "addPreferredActivity",
                IntentFilter::class.java,
                Int::class.java,
                Array<ComponentName>::class.java,
                ComponentName::class.java,
                Int::class.java,
                Boolean::class.java,
            )?.invoke(
                iPackageManager,
                filter,
                match,
                names,
                name,
                userId,
                removeExisting
            )
        } else {
            reflect.getDeclaredMethod(
                IPackageManager::class.java,
                "addPreferredActivity",
                IntentFilter::class.java,
                Int::class.java,
                Array<ComponentName>::class.java,
                ComponentName::class.java,
                Int::class.java
            )?.invoke(
                iPackageManager,
                filter,
                match,
                names,
                name,
                userId
            )
        }
    }

    private fun addPersistentPreferredActivity(
        iPackageManager: IPackageManager,
        filter: IntentFilter,
        name: ComponentName,
        userId: Int,
    ) {
        reflect.getDeclaredMethod(
            IPackageManager::class.java,
            "addPersistentPreferredActivity",
            IntentFilter::class.java,
            ComponentName::class.java,
            Int::class.java,
        )?.invoke(
            iPackageManager,
            filter,
            name,
            userId,
        )
    }

    private fun queryIntentActivities(
        iPackageManager: IPackageManager,
        intent: Intent,
        resolvedType: String,
        flags: Int,
        userId: Int
    ): List<ResolveInfo> {
        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            reflect.getDeclaredMethod(
                IPackageManager::class.java,
                "queryIntentActivities",
                Intent::class.java,
                String::class.java,
                Long::class.java,
                Int::class.java
            )?.invoke(iPackageManager, intent, resolvedType, flags.toLong(), userId)
        } else {
            reflect.getDeclaredMethod(
                IPackageManager::class.java,
                "queryIntentActivities",
                Intent::class.java,
                String::class.java,
                Int::class.java,
                Int::class.java
            )?.invoke(iPackageManager, intent, resolvedType, flags, userId)
        } as ParceledListSlice<ResolveInfo>).list
    }

    /**
     * 读取执行结果，利用 Kotlin 的扩展函数简化代码。
     * 如果有异常会向上抛出。
     */
    @Throws(IOException::class, InterruptedException::class)
    private fun readResult(process: Process): String {
        // 分别读取标准输出和标准错误
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val error = process.errorStream.bufferedReader().use { it.readText() }

        // 等待命令执行完成，并获取退出码
        val exitCode = process.waitFor()

        // 检查退出码。0 通常代表成功，任何非零值都代表失败。
        if (exitCode != 0) {
            // 如果失败了，就构造一个详细的错误信息并抛出 IOException
            // 这样 execArr 方法里的 catch 块就能捕捉到它，并转换成 RemoteException
            throw IOException("命令执行失败，退出码: $exitCode, 错误信息: '$error'")
        }

        // 如果成功，返回标准输出的内容
        return output
    }
}
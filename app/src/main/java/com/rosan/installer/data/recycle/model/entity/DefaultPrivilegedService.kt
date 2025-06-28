package com.rosan.installer.data.recycle.model.entity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.content.pm.ParceledListSlice
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.RemoteException
import android.os.ServiceManager
import android.util.Log
import androidx.core.net.toUri
import com.rosan.installer.data.recycle.util.InstallIntentFilter
import com.rosan.installer.data.recycle.util.deletePaths
import com.rosan.installer.data.recycle.util.pathUnify
import com.rosan.installer.data.reflect.repo.ReflectRepo
import org.koin.core.component.inject
import java.io.IOException
import android.os.Process as AndroidProcess


class DefaultPrivilegedService : BasePrivilegedService() {
    private val reflect by inject<ReflectRepo>()

    // Privileged Process is not Main Process, so we use Log.d instead of Timber
    @SuppressLint("LogNotTimber")
    override fun delete(paths: Array<out String>) {
        for (path in paths) {
            // 部分系统路径可能以 "/mnt/user/0" 开头，shell无权访问，这里进行处理
            val target = pathUnify(path)
            Log.d("DELETE_PATH", "准备通过 rm -f 命令删除文件: $target")
            // println("通过 rm -f 命令删除文件: $path")
            try {
                // 严格执行 "rm -f"，只删除文件，不进行递归操作
                val result = execArr(arrayOf("rm", "-f", target))
                Log.d("DELETE_PATH", "执行结果: $result")
                // 判断执行结果是否为空字符串
                if (result.isEmpty()) {
                    // 为空，代表命令成功执行且无输出，这是预期的成功情况
                    Log.d("DELETE_PATH", "成功删除 $target (命令无输出)")
                } else {
                    // 如果有输出，可能是非致命的警告或某些特殊情况，记录下来以供排查
                    Log.w("DELETE_PATH", "删除 $target 时命令有输出，请关注: $result")
                }
            } catch (e: IOException) {
                // 如果执行过程中发生 IOException，记录错误信息，并尝试回退到 deletePaths 方法
                Log.e("DELETE_PATH", "删除 $path 失败: ${e.message}，回退方法")
                deletePaths(paths)
            }
        }
    }

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
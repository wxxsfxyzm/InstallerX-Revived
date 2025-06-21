package com.rosan.installer.data.recycle.model.entity

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.RemoteException
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.installer.data.recycle.util.InstallIntentFilter
import com.rosan.installer.data.recycle.util.delete
import java.io.IOException

class DhizukuPrivilegedService : BasePrivilegedService() {
    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    override fun delete(paths: Array<out String>) = paths.delete()

    override fun setDefaultInstaller(component: ComponentName, enable: Boolean) {
        devicePolicyManager.clearPackagePersistentPreferredActivities(
            // TODO
            // DhizukuVariables.PARAM_COMPONENT
            Dhizuku.getOwnerComponent(),
            component.packageName
        )
        if (!enable) return
        devicePolicyManager.addPersistentPreferredActivity(
            // TODO
            // DhizukuVariables.PARAM_COMPONENT,
            Dhizuku.getOwnerComponent(),
            InstallIntentFilter, component
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

    /**
     * 读取执行结果，利用 Kotlin 的扩展函数简化代码。
     * 如果有异常会向上抛出。
     */
    @Throws(IOException::class, InterruptedException::class)
    private fun readResult(process: Process): String {
        // 使用 'use' 块可以自动关闭流，更安全、简洁
        val output = process.inputStream.bufferedReader().use { reader ->
            reader.readText()
        }
        process.waitFor()
        return output
    }
}
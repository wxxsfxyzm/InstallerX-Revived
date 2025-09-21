package com.rosan.installer.data.recycle.model.entity

import android.annotation.SuppressLint
import android.app.IActivityManager
import android.app.IApplicationThread
import android.app.ProfilerInfo
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
import rikka.shizuku.SystemServiceHelper
import java.io.IOException
import android.os.Process as AndroidProcess

@SuppressLint("LogNotTimber")
class DefaultPrivilegedService : BasePrivilegedService() {
    private val reflect by inject<ReflectRepo>()

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

    /**
     * Implements the grantRuntimePermission from our IPrivilegedService.aidl.
     * This method runs inside the privileged process.
     */
    override fun grantRuntimePermission(packageName: String, permission: String) {
        try {
            val iPackageManager = IPackageManager.Stub.asInterface(
                ServiceManager.getService("package")
            ) ?: throw RemoteException("Failed to get 'package' service.")

            val userId = AndroidProcess.myUid() / 100000

            Log.d("PrivilegedService", "Granting $permission for $packageName (UID: $userId)")

            iPackageManager.grantRuntimePermission(packageName, permission, userId)

            Log.i("PrivilegedService", "Successfully granted $permission to $packageName")

        } catch (e: Exception) {
            // 捕获所有可能的异常 (包括 SecurityException)
            Log.e("PrivilegedService", "ERROR granting permission", e)
            // 将异常包装成 RemoteException 抛回给客户端
            throw RemoteException("Failed to grant permission via system API: ${e.message}")
        }
    }

    /**
     * Checks if a package has a given permission.
     * This method runs inside the privileged process, which is not subject to
     * package visibility restrictions that a normal app would face.
     *
     * @param packageName The package to check.
     * @param permission The permission to check for.
     * @return True if the permission is granted, false otherwise.
     * @throws RemoteException if any underlying error occurs.
     */
    override fun isPermissionGranted(packageName: String, permission: String): Boolean {
        Log.d("PrivilegedService", "Checking permission '$permission' for package '$packageName'")
        try {
            // Because this code runs in a privileged context with access to a full Context,
            // we can directly use the standard PackageManager API.
            val result = context.packageManager.checkPermission(permission, packageName)
            // The API returns PERMISSION_GRANTED (0) on success.
            return result == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            // Catch potential exceptions, e.g., if the package name is invalid,
            // though checkPermission typically returns PERMISSION_DENIED for that.
            Log.e("PrivilegedService", "Failed to check permission '$permission' for '$packageName'", e)
            // It's safer to return false on any error.
            return false
        }
    }

    override fun startActivityPrivileged(intent: Intent): Boolean {
        try {
            val amBinder = ServiceManager.getService(Context.ACTIVITY_SERVICE)
            val am = IActivityManager.Stub.asInterface(amBinder)

            val userId = AndroidProcess.myUid() / 100000

            // By setting the calling package to "com.android.shell",
            // we leverage the permissions granted to the shell user,
            // bypassing many standard security checks.
            val callerPackage = "com.android.shell"
            val resolvedType = intent.resolveType(context.contentResolver)

            // Call the new, non-deprecated API 'startActivityAsUserWithFeature'.
            // The 'callingFeatureId' is passed as null, which mimics the behavior
            // of the old 'startActivityAsUser' method.
            val result = am.startActivityAsUserWithFeature(
                null as IApplicationThread?,
                callerPackage,
                null as String?, // This is the new 'callingFeatureId' parameter.
                intent,
                resolvedType,
                null as IBinder?,
                null as String?,
                0,
                0,
                null as ProfilerInfo?,
                null as Bundle?,
                userId
            )

            // A result code >= 0 indicates success.
            // See ActivityManager.START_SUCCESS, START_DELIVERED_TO_TOP, etc.
            return result >= 0
        } catch (e: SecurityException) {
            // Log security exceptions specifically, as they indicate a permission issue.
            Log.e("PrivilegedService", "startActivityPrivileged failed due to SecurityException", e)
            return false
        } catch (e: Exception) {
            // Catch other potential exceptions, such as RemoteException.
            Log.e("PrivilegedService", "startActivityPrivileged failed with an exception", e)
            return false
        }
    }

    /**
     * Implements the getUsers method from our AIDL interface.
     * This method runs inside the privileged process and uses reflection
     * to call hidden system APIs for fetching a complete user list.
     */
    @SuppressLint("PrivateApi")
    override fun getUsers(): Map<Int, String> {
        val userMap = mutableMapOf<Int, String>()
        try {
            Log.d("PrivilegedService", "Using context: ${context.packageName}, UID: ${AndroidProcess.myUid()}")

            val userManagerBinder = SystemServiceHelper.getSystemService(Context.USER_SERVICE)
            val userManagerClass = Class.forName("android.os.IUserManager\$Stub")
            val asInterfaceMethod = reflect.getMethod(userManagerClass, "asInterface", IBinder::class.java)
                ?: throw NoSuchMethodException("asInterface method not found")
            val userManagerInstance = asInterfaceMethod.invoke(null, userManagerBinder)

            val getUsersMethod =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT_FULL <= Build.VERSION_CODES_FULL.BAKLAVA) {
                    reflect.getMethod(
                        userManagerInstance::class.java,
                        "getUsers",
                        Boolean::class.java,
                        Boolean::class.java,
                        Boolean::class.java
                    )
                } else {
                    reflect.getMethod(
                        userManagerInstance::class.java,
                        "getUsers",
                        Boolean::class.java
                    )
                } ?: throw NoSuchMethodException("getUsers method not found")

            val usersList =
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT_FULL <= Build.VERSION_CODES_FULL.BAKLAVA) {
                    getUsersMethod.invoke(userManagerInstance, false, false, false)
                } else {
                    getUsersMethod.invoke(userManagerInstance, false)
                }) as? List<*>

            if (usersList == null) {
                Log.e("PrivilegedService", "Failed to get user list, method returned null.")
                return userMap
            }

            val userInfoClass = Class.forName("android.content.pm.UserInfo")
            val idField = reflect.getField(userInfoClass, "id")
                ?: throw NoSuchFieldException("Field 'id' not found in UserInfo")
            val nameField = reflect.getField(userInfoClass, "name")
                ?: throw NoSuchFieldException("Field 'name' not found in UserInfo")

            for (userObject in usersList) {
                if (userObject != null) {
                    idField.isAccessible = true
                    nameField.isAccessible = true
                    val userId = idField.getInt(userObject)
                    val userName = nameField.get(userObject) as? String ?: "Unknown User"
                    userMap[userId] = userName
                }
            }
            Log.d("PrivilegedService", "Fetched users: $userMap")
        } catch (e: SecurityException) {
            Log.e("PrivilegedService", "Permission denied for getUsers, falling back to current user", e)
            val userId = AndroidProcess.myUid() / 100000
            userMap[userId] = "Current User"
        } catch (e: Exception) {
            Log.e("PrivilegedService", "Error getting users using ReflectRepo", e)
        }
        return userMap
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
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
import android.content.pm.UserInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.IUserManager
import android.os.RemoteException
import android.os.ServiceManager
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.rosan.installer.ICommandOutputListener
import com.rosan.installer.data.recycle.util.InstallIntentFilter
import com.rosan.installer.data.recycle.util.ShizukuHook
import com.rosan.installer.data.recycle.util.deletePaths
import com.rosan.installer.data.reflect.repo.ReflectRepo
import org.koin.core.component.inject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import android.os.Process as AndroidProcess

@SuppressLint("LogNotTimber")
class DefaultPrivilegedService(
    // Add binderWrapper parameter to support Process Hook Mode
    private val binderWrapper: ((IBinder) -> IBinder)? = null
) : BasePrivilegedService() {
    companion object {
        private const val TAG = "PrivilegedService"
    }

    private val reflect by inject<ReflectRepo>()

    private val isHookMode by lazy {
        val processName: String? = try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentProcessNameMethod = reflect.getDeclaredMethod(activityThreadClass, "currentProcessName")
            currentProcessNameMethod?.isAccessible = true

            currentProcessNameMethod?.invoke(null) as? String
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current process name reflection setup: ${e.message}")
            null
        }

        Log.d(TAG, "Detected process name: '$processName'")

        if (processName == null) {
            Log.d(TAG, "Process name is null, assuming UserService Mode.")
            false // isHookMode is false (UserService Mode)
        } else {
            val isShizukuProcess = processName.endsWith(":shizuku_privileged")
            Log.d(
                TAG,
                "Process name is '$processName', isShizukuProcess: $isShizukuProcess. Assuming Hook Mode: ${!isShizukuProcess}"
            )
            !isShizukuProcess // isHookMode is true for main process
        }
    }

    private val iPackageManager: IPackageManager by lazy {
        // [Mod] Priority: Custom Wrapper (Root Hook) > Shizuku Hook > Direct/Service
        if (binderWrapper != null) {
            Log.d(TAG, "Getting IPackageManager in Process Hook Mode.")
            val original = ServiceManager.getService("package")
            IPackageManager.Stub.asInterface(binderWrapper.invoke(original))
        } else if (isHookMode) {
            Log.d(TAG, "Getting IPackageManager in Hook Mode (Directly).")
            ShizukuHook.hookedPackageManager
        } else {
            Log.d(TAG, "Getting IPackageManager in UserService Mode.")
            IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
        }
    }

    private val iActivityManager: IActivityManager by lazy {
        if (binderWrapper != null) {
            Log.d(TAG, "Getting IActivityManager in Process Hook Mode.")
            val original = ServiceManager.getService(Context.ACTIVITY_SERVICE)
            IActivityManager.Stub.asInterface(binderWrapper.invoke(original))
        } else if (isHookMode) {
            ShizukuHook.hookedActivityManager
        } else {
            IActivityManager.Stub.asInterface(ServiceManager.getService(Context.ACTIVITY_SERVICE))
        }
    }

    private val iUserManager: IUserManager by lazy {
        if (binderWrapper != null) {
            Log.d(TAG, "Getting IUserManager in Process Hook Mode.")
            val original = ServiceManager.getService(Context.USER_SERVICE)
            IUserManager.Stub.asInterface(binderWrapper.invoke(original))
        } else if (isHookMode) {
            Log.d("", "Getting IUserManager in Hook Mode (From ShizukuHook Factory).")
            ShizukuHook.hookedUserManager
        } else {
            Log.d(TAG, "Getting IUserManager in UserService Mode.")
            IUserManager.Stub.asInterface(ServiceManager.getService(Context.USER_SERVICE))
        }
    }

    override fun delete(paths: Array<out String>) = deletePaths(paths)

    override fun performDexOpt(
        packageName: String,
        compilerFilter: String,
        force: Boolean
    ): Boolean {
        Timber.tag(TAG).d("performDexOpt: $packageName, filter=$compilerFilter, force=$force")

        return try {
            val iPackageManager = IPackageManager.Stub.asInterface(
                ServiceManager.getService("package")
            )

            val method = iPackageManager::class.java.getDeclaredMethod(
                "performDexOptMode",
                String::class.java,
                Boolean::class.javaPrimitiveType,
                String::class.java,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                String::class.java
            ).apply { isAccessible = true }

            val result = method.invoke(
                iPackageManager,
                packageName,
                false,           // checkProfiles
                compilerFilter,
                force,
                true,            // bootComplete
                null             // splitName
            ) as Boolean

            Timber.tag(TAG).i("performDexOpt result for $packageName: $result")
            result
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "performDexOpt failed for $packageName")
            false
        }
    }

    override fun setDefaultInstaller(component: ComponentName, enable: Boolean) {
        Log.d(TAG, "Hook Mode: $isHookMode")
        val uid = AndroidProcess.myUid()
        val userId = uid / 100000

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

    @Throws(RemoteException::class)
    override fun execArrWithCallback(command: Array<String>, listener: ICommandOutputListener?) {
        if (listener == null) {
            // If no listener is provided, we can't stream output.
            // You could either throw an exception or just execute without feedback.
            Log.w(TAG, "execArrWithCallback called with a null listener.")
            return
        }

        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(command)

            // Thread to read standard output
            val stdoutThread = Thread {
                try {
                    process.inputStream.bufferedReader(StandardCharsets.UTF_8).forEachLine {
                        listener.onOutput(it)
                    }
                } catch (e: Exception) {
                    if (e is IOException || e is RemoteException) {
                        Log.e(TAG, "Error reading stdout or sending callback", e)
                    }
                }
            }

            // Thread to read standard error
            val stderrThread = Thread {
                try {
                    process.errorStream.bufferedReader(StandardCharsets.UTF_8).forEachLine {
                        listener.onError(it)
                    }
                } catch (e: Exception) {
                    if (e is IOException || e is RemoteException) {
                        Log.e(TAG, "Error reading stderr or sending callback", e)
                    }
                }
            }

            stdoutThread.start()
            stderrThread.start()

            // Wait for the process to complete
            val exitCode = process.waitFor()
            process.destroy()

            // Wait for reader threads to finish to ensure all output is captured
            stdoutThread.join()
            stderrThread.join()

            // Notify client that the process is complete
            listener.onComplete(exitCode)

        } catch (e: Exception) {
            // If process creation itself fails
            val errorMessage = "Failed to execute command: ${e.message}"
            Log.e(TAG, errorMessage, e)
            try {
                listener.onError(errorMessage)
                listener.onComplete(-1) // Send a failure exit code
            } catch (re: RemoteException) {
                // The client might be dead, just log it.
                Log.e(TAG, "Failed to send execution error to client.", re)
            }
        } finally {
            process?.destroy()
        }
    }

    override fun grantRuntimePermission(packageName: String, permission: String) {
        try {
            val userId = AndroidProcess.myUid() / 100000

            Log.d(TAG, "Granting $permission for $packageName (UID: $userId)")

            iPackageManager.grantRuntimePermission(packageName, permission, userId)

            Log.i(TAG, "Successfully granted $permission to $packageName")

        } catch (e: Exception) {
            // 捕获所有可能的异常 (包括 SecurityException)
            Log.e(TAG, "ERROR granting permission", e)
            // 将异常包装成 RemoteException 抛回给客户端
            throw RemoteException("Failed to grant permission via system API: ${e.message}")
        }
    }

    override fun isPermissionGranted(packageName: String, permission: String): Boolean {
        Log.d(TAG, "Checking permission '$permission' for package '$packageName'")
        try {
            // Because this code runs in a privileged context with access to a full Context,
            // we can directly use the standard PackageManager API.
            val result = context.packageManager.checkPermission(permission, packageName)
            // The API returns PERMISSION_GRANTED (0) on success.
            return result == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            // Catch potential exceptions, e.g., if the package name is invalid,
            // though checkPermission typically returns PERMISSION_DENIED for that.
            Log.e(TAG, "Failed to check permission '$permission' for '$packageName'", e)
            // It's safer to return false on any error.
            return false
        }
    }

    override fun startActivityPrivileged(intent: Intent): Boolean {
        try {
            val am = iActivityManager

            val userId = AndroidProcess.myUid() / 100000
            val callerPackage = "com.android.shell"
            val resolvedType = intent.resolveType(context.contentResolver)

            val result = am.startActivityAsUser(
                null as IApplicationThread?,
                callerPackage,
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
            Log.e(TAG, "startActivityPrivileged failed due to SecurityException", e)
            return false
        } catch (e: Exception) {
            // Catch other potential exceptions, such as RemoteException.
            Log.e(TAG, "startActivityPrivileged failed with an exception", e)
            return false
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun getSessionDetails(sessionId: Int): Bundle? {
        Log.d(TAG, "getSessionDetails: sessionId=$sessionId")
        try {
            val packageInstaller = context.packageManager.packageInstaller
            val sessionInfo = packageInstaller.getSessionInfo(sessionId) ?: run {
                Log.w(TAG, "getSessionDetails: SessionInfo is null for id $sessionId")
                return null
            }

            val originatingUid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                sessionInfo.originatingUid
            } else {
                -1 // Return -1 if SDK too low (Process.INVALID_UID)
            }
            Log.d(TAG, "Got originatingUid: $originatingUid")

            var resolvedLabel: CharSequence? = null
            var resolvedIcon: Bitmap? = null
            var path: String? = null

            // Get apk path via reflection
            try {
                val resolvedField: Field? = reflect.getDeclaredField(
                    sessionInfo::class.java,
                    "resolvedBaseCodePath"
                )
                if (resolvedField != null) {
                    resolvedField.isAccessible = true
                    path = resolvedField.get(sessionInfo) as? String
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reflect resolvedBaseCodePath", e)
            }

            // Load appInfo from path
            if (!path.isNullOrEmpty()) {
                Log.d(TAG, "Loading info from APK path: $path")
                try {
                    val pm = context.packageManager
                    val pkgInfo = pm.getPackageArchiveInfo(
                        path,
                        PackageManager.GET_PERMISSIONS
                    )
                    val appInfo = pkgInfo?.applicationInfo
                    if (appInfo != null) {
                        appInfo.publicSourceDir = path

                        // Load Label
                        try {
                            resolvedLabel = appInfo.loadLabel(pm)
                            Log.d(TAG, "Label loaded successfully: $resolvedLabel")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to load label", e)
                        }

                        try {
                            // Load Resources for APK
                            // appInfo.publicSourceDir must be set
                            val apkResources = pm.getResourcesForApplication(appInfo)
                            val iconId = appInfo.icon

                            if (iconId == 0) {
                                Log.w(TAG, "appInfo.icon ID is 0, using default icon.")
                                // Fallback to default icon
                                val defaultIcon = pm.defaultActivityIcon
                                resolvedIcon = (defaultIcon as? BitmapDrawable)?.bitmap
                                    ?: defaultIcon.toBitmap(
                                        defaultIcon.intrinsicWidth.coerceAtLeast(1),
                                        defaultIcon.intrinsicHeight.coerceAtLeast(1)
                                    )
                            } else {
                                Log.d(TAG, "Loading icon ID $iconId from APK resources.")
                                val drawableIcon = apkResources.getDrawable(iconId, null) // Use null for theme

                                val width = drawableIcon.intrinsicWidth
                                val height = drawableIcon.intrinsicHeight
                                Log.d(
                                    TAG,
                                    "Drawable loaded from resources. Class: ${drawableIcon.javaClass.name}, WxH: ${width}x${height}"
                                )

                                if (width <= 0 || height <= 0) {
                                    Log.w(TAG, "Drawable has invalid dimensions, cannot convert.")
                                } else {
                                    // Render Drawable to Bitmap manually
                                    // Handles VectorDrawable, AdaptiveIconDrawable etc.
                                    val bitmap = createBitmap(width, height)
                                    val canvas = Canvas(bitmap)
                                    drawableIcon.setBounds(0, 0, canvas.width, canvas.height)
                                    drawableIcon.draw(canvas)
                                    resolvedIcon = bitmap
                                    Log.d(TAG, "Manually rendered drawable to bitmap.")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to load icon using getResourcesForApplication", e)
                        }

                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load info from APK path", e)
                }
            }

            // Fallback to sessionInfo (mostly null)
            Log.d(TAG, "Icon Decision: resolvedIcon is null: ${resolvedIcon == null}")
            Log.d(TAG, "Icon Decision: sessionInfo.appIcon is null: ${sessionInfo.appIcon == null}")

            val finalLabel = resolvedLabel ?: sessionInfo.appLabel ?: "N/A"
            val finalIcon = resolvedIcon ?: sessionInfo.appIcon
            Log.d(TAG, "Icon Decision: finalIcon is null: ${finalIcon == null}")

            // Package into Bundle
            val bundle = Bundle()
            bundle.putCharSequence("appLabel", finalLabel)
            if (finalIcon != null) {
                val stream = ByteArrayOutputStream()
                finalIcon.compress(Bitmap.CompressFormat.PNG, 100, stream)
                bundle.putByteArray("appIcon", stream.toByteArray())
            }
            bundle.putInt("originatingUid", originatingUid)
            return bundle
        } catch (e: Exception) {
            Log.e(TAG, "getSessionDetails failed", e)
            return null
        }
    }

    override fun approveSession(sessionId: Int, granted: Boolean) {
        try {
            val packageInstaller = context.packageManager.packageInstaller

            val method: Method? = reflect.getMethod(
                packageInstaller::class.java,
                "setPermissionsResult",
                Int::class.java,
                Boolean::class.java
            )

            if (method != null) {
                method.invoke(packageInstaller, sessionId, granted)
                Log.d(TAG, "Invoked setPermissionsResult($sessionId, $granted)")
            } else {
                throw NoSuchMethodException("setPermissionsResult not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "approveSession failed", e)
            if (!granted) {
                try {
                    context.packageManager.packageInstaller.abandonSession(sessionId)
                } catch (e2: Exception) {
                    Log.e(TAG, "Fallback abandonSession failed", e2)
                }
            }
        }
    }

    @SuppressLint("PrivateApi")
    override fun getUsers(): Map<Int, String> {
        val userMap = mutableMapOf<Int, String>()
        try {
            val userManagerInstance = this.iUserManager

            val usersList: List<UserInfo>? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT_FULL <= Build.VERSION_CODES_FULL.BAKLAVA) {
                    userManagerInstance.getUsers(false, false, false)
                } else {
                    userManagerInstance.getUsers(false)
                }

            if (usersList == null) {
                Log.e(TAG, "Failed to get user list, method returned null.")
                return userMap
            }

            for (userObject in usersList) {
                userMap[userObject.id] = userObject.name ?: "Unknown User"
            }

            Log.d(TAG, "Fetched users: $userMap")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for getUsers, falling back to current user", e)
            val userId = AndroidProcess.myUid() / 100000
            userMap[userId] = "Current User"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting users", e)
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

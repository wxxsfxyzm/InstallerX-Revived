package com.rosan.installer.data.recycle.model.entity

import android.annotation.SuppressLint
import android.app.IActivityManager
import android.app.IApplicationThread
import android.app.ProfilerInfo
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.content.pm.ParceledListSlice
import android.content.pm.ResolveInfo
import android.content.pm.UserInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.IUserManager
import android.os.RemoteException
import android.os.ServiceManager
import android.provider.Settings
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.rosan.installer.ICommandOutputListener
import com.rosan.installer.data.recycle.util.InstallIntentFilter
import com.rosan.installer.data.recycle.util.ShizukuContext
import com.rosan.installer.data.recycle.util.ShizukuHook
import com.rosan.installer.data.recycle.util.SystemContext
import com.rosan.installer.data.recycle.util.deletePaths
import com.rosan.installer.data.recycle.util.resolveSettingsBinder
import com.rosan.installer.data.reflect.repo.ReflectRepo
import com.rosan.installer.util.OSUtils
import org.koin.core.component.inject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.reflect.Field
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
        if (binderWrapper != null) return@lazy true

        if (OSUtils.isSystemApp) {
            // In this case, it's a direct call in the local process, no Shizuku Hook needed
            Log.d(TAG, "Running as System App (Direct Mode). isHookMode = false")
            return@lazy false
        }

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
        if (binderWrapper != null) {
            Log.d(TAG, "Getting IPackageManager in Process Hook Mode.")
            val original = ServiceManager.getService("package")
            IPackageManager.Stub.asInterface(binderWrapper.invoke(original))
        } else if (isHookMode) {
            Log.d(TAG, "Getting IPackageManager in Hook Mode (Directly).")
            ShizukuHook.hookedPackageManager
        } else {
            if (OSUtils.isSystemApp) Log.d(TAG, "Getting IPackageManager in System Mode.")
            else Log.d(TAG, "Getting IPackageManager in UserService Mode.")
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
            if (OSUtils.isSystemApp) Log.d(TAG, "Getting IActivityManager in System Mode.")
            else Log.d(TAG, "Getting IActivityManager in UserService Mode.")
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
            if (OSUtils.isSystemApp) Log.d(TAG, "Getting IUserManager in System Mode.")
            else Log.d(TAG, "Getting IUserManager in UserService Mode.")
            IUserManager.Stub.asInterface(ServiceManager.getService(Context.USER_SERVICE))
        }
    }

    private val settingsBinder: IBinder? by lazy {
        val original = resolveSettingsBinder(reflect)?.originalBinder

        if (binderWrapper != null) {
            Log.d(TAG, "Getting Settings Binder in Process Hook Mode.")
            if (original != null) binderWrapper.invoke(original) else null
        } else if (isHookMode) {
            Log.d(TAG, "Getting Settings Binder in Hook Mode (via ShizukuHook).")
            ShizukuHook.hookedSettingsBinder
        } else {
            if (OSUtils.isSystemApp) Log.d(TAG, "Getting Settings Binder in System Mode.")
            else Log.d(TAG, "Getting Settings Binder in UserService Mode.")
            original
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
            // Execute shell command
            val process = Runtime.getRuntime().exec(command)
            // Read execution result
            readResult(process)
        } catch (e: IOException) {
            // Wrap IOException in RemoteException and throw
            throw RemoteException(e.message)
        } catch (e: InterruptedException) {
            // Restore thread's interrupted status
            Thread.currentThread().interrupt()
            // Wrap InterruptedException in RemoteException and throw
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

    override fun setAdbVerify(enabled: Boolean) {
        val key = "verifier_verify_adb_installs"
        val targetValue = if (enabled) 1 else 0

        // 1. Check current value
        val currentValue = try {
            Settings.Global.getInt(context.contentResolver, key, 1)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to read initial setting for $key")
            1
        }

        if (currentValue == targetValue) {
            Timber.tag(TAG).d("ADB verify state matches target ($enabled). No action needed.")
            return
        }

        // 2. Get the privileged binder from our unified lazy loader
        val targetBinder = settingsBinder
        if (targetBinder == null) {
            Timber.tag(TAG).w("Cannot change ADB verify: Privileged Settings binder is unavailable.")
            return
        }

        Timber.tag(TAG).d("Trying to set ADB verify to $targetValue (Current: $currentValue)")

        try {
            // 3. Prepare Reflection for Swapping
            val info = resolveSettingsBinder(reflect)
            if (info == null) {
                Timber.tag(TAG).e("Failed to resolve Settings reflection info for swapping")
                return
            }

            val provider = info.provider
            val remoteField = info.remoteField
            val originalBinder = info.originalBinder

            // 4. Swap -> Execute -> Restore
            try {
                if (originalBinder != targetBinder) {
                    remoteField.set(provider, targetBinder)
                }
                val targetResolver = if (binderWrapper != null) {
                    // [Root Mode] UID is 1000.
                    // Must spoof package name "android" to pass AppOps check.
                    Timber.tag(TAG).d("Root Mode: Using SystemContextResolver (UID 1000, Pkg: android)")
                    val systemContext = SystemContext(context)
                    object : ContentResolver(systemContext) {}
                } else {
                    // [Shizuku Mode] UID is 2000.
                    // Must spoof package name "com.android.shell".
                    Timber.tag(TAG).d("Shizuku Mode: Using ShellContextResolver (UID 2000, Pkg: com.android.shell)")
                    val shellContext = ShizukuContext(context)
                    object : ContentResolver(shellContext) {}
                }

                val result = Settings.Global.putInt(targetResolver, key, targetValue)
                Timber.tag(TAG).i("Set $key to $targetValue. Result: $result")

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to putInt for ADB verify")
            } finally {
                if (originalBinder !== targetBinder) {
                    try {
                        remoteField.set(provider, originalBinder)
                        Timber.tag(TAG).d("Restored original settings binder")
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "Failed to restore original binder!")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Critical error in setAdbVerify setup")
        }
    }

    override fun grantRuntimePermission(packageName: String, permission: String) {
        try {
            val userId = AndroidProcess.myUid() / 100000

            Log.d(TAG, "Granting $permission for $packageName (UID: $userId)")

            iPackageManager.grantRuntimePermission(packageName, permission, userId)

            Log.i(TAG, "Successfully granted $permission to $packageName")

        } catch (e: Exception) {
            Log.e(TAG, "ERROR granting permission", e)
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
            val callerPackage = if (OSUtils.isSystemApp) {
                context.packageName
            } else "com.android.shell"
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

    @SuppressLint("LogNotTimber")
    override fun getSessionDetails(sessionId: Int): Bundle? {
        Log.d(TAG, "getSessionDetails: sessionId=$sessionId")
        try {
            val packageInstaller = context.packageManager.packageInstaller
            val sessionInfo = packageInstaller.getSessionInfo(sessionId)

            if (sessionInfo == null) {
                Log.w(TAG, "getSessionDetails: sessionInfo is null for id $sessionId")
                return null
            }

            var resolvedLabel: CharSequence? = null
            var resolvedIcon: Bitmap? = null
            var path: String? = null

            // ---------------------------------------------------------
            // STRATEGY 1: Try to get the APK path via reflection
            // ---------------------------------------------------------
            try {
                // Try to get "resolvedBaseCodePath" field
                val resolvedField: Field? = reflect.getDeclaredField(
                    sessionInfo::class.java,
                    "resolvedBaseCodePath"
                )
                if (resolvedField != null) {
                    resolvedField.isAccessible = true
                    path = resolvedField.get(sessionInfo) as? String
                }

                // ---------------------------------------------------------
                // STRATEGY 2: If path is null, try "stageDir" (Android 16+ / Staged Sessions)
                // ---------------------------------------------------------
                if (path == null) {
                    val stageDirField = reflect.getDeclaredField(
                        sessionInfo::class.java,
                        "stageDir"
                    )
                    if (stageDirField != null) {
                        stageDirField.isAccessible = true
                        val stageDir = stageDirField.get(sessionInfo) as? java.io.File
                        // Find the first .apk file in the staging directory
                        if (stageDir != null && stageDir.exists() && stageDir.isDirectory) {
                            path = stageDir.listFiles { _, name -> name.endsWith(".apk") }
                                ?.firstOrNull()?.absolutePath
                            Log.d(TAG, "Found APK path via stageDir: $path")
                        }
                    }
                } else {
                    Log.d(TAG, "Reflected resolvedBaseCodePath: $path")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reflect path or stageDir", e)
            }

            // ---------------------------------------------------------
            // STRATEGY 2.5: Root/Privileged Direct File Access
            // ---------------------------------------------------------
            // When running as Root process, access /data/app/ directory directly to find vmdl{sessionId}.tmp
            // This is the standard location for Android PackageInstallerService temporary files
            if (path == null) {
                try {
                    // Standard Session staging directory structure: /data/app/vmdl{sessionId}.tmp
                    val sessionDir = java.io.File("/data/app/vmdl${sessionId}.tmp")

                    if (sessionDir.exists() && sessionDir.isDirectory) {
                        Log.d(TAG, "Direct Access: Found session dir at ${sessionDir.absolutePath}")

                        // 1. Get list of all .apk files
                        val apkFiles = sessionDir.listFiles { _, name ->
                            name.endsWith(".apk", ignoreCase = true)
                        }

                        if (!apkFiles.isNullOrEmpty()) {
                            // 2. Core Logic: Prefer 'base.apk', otherwise take the first one found
                            // Split APK installations must contain base.apk; Single APK installs usually are base.apk too
                            val targetApk = apkFiles.find { it.name == "base.apk" } ?: apkFiles.first()

                            path = targetApk.absolutePath
                            Log.d(TAG, "Direct Access: Found APK path: $path (Selected from ${apkFiles.size} files)")
                        } else {
                            Log.w(TAG, "Direct Access: Session dir exists but contains no APKs")
                        }
                    } else {
                        // Rare cases or older versions might be in /data/local/tmp (mainly ADB push)
                        // Or /data/app/vmdl{sessionId}.tmp does not exist (Session hasn't written data yet)
                        Log.d(TAG, "Direct Access: Session dir not found at standard path.")
                    }
                } catch (e: Exception) {
                    // Only happens if process lacks file read permissions (e.g. SELinux denial)
                    Log.e(TAG, "Failed to perform direct file search", e)
                }
            }

            // ---------------------------------------------------------
            // Parse APK from path (if found)
            // ---------------------------------------------------------
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
                        appInfo.sourceDir = path

                        // Load Label
                        try {
                            resolvedLabel = appInfo.loadLabel(pm)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to load label from APK", e)
                        }

                        // Load Icon
                        try {
                            val drawable = appInfo.loadIcon(pm)
                            resolvedIcon = if (drawable is BitmapDrawable) {
                                drawable.bitmap
                            } else {
                                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
                                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
                                drawable.toBitmap(width, height, Bitmap.Config.ARGB_8888)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to load icon from APK", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse APK from path", e)
                }
            }

            // ---------------------------------------------------------
            // STRATEGY 3: Fallback to Installed App Info (Crucial for Updates)
            // ---------------------------------------------------------
            // If we still don't have a label or icon, check if the app is already installed.
            // This fixes "N/A" when updating an app where the new APK path is hidden.
            if (resolvedLabel == null || resolvedIcon == null) {
                try {
                    val pm = context.packageManager
                    val appPackageName = sessionInfo.appPackageName

                    if (appPackageName != null) {
                        val installedInfo = pm.getApplicationInfo(appPackageName, 0)

                        if (resolvedLabel == null) {
                            resolvedLabel = installedInfo.loadLabel(pm)
                            Log.d(TAG, "Fallback: Loaded label from installed app")
                        }

                        if (resolvedIcon == null) {
                            val drawable = installedInfo.loadIcon(pm)
                            resolvedIcon = if (drawable is BitmapDrawable) {
                                drawable.bitmap
                            } else {
                                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
                                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
                                drawable.toBitmap(width, height, Bitmap.Config.ARGB_8888)
                            }
                            Log.d(TAG, "Fallback: Loaded icon from installed app")
                        }
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.d(TAG, "App not installed, cannot use fallback info.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load info from installed app fallback", e)
                }
            }

            // ---------------------------------------------------------
            // Final Data Preparation
            // ---------------------------------------------------------
            val finalLabel = resolvedLabel ?: sessionInfo.appLabel ?: "N/A"
            val finalIcon = resolvedIcon ?: sessionInfo.appIcon

            Log.d(TAG, "Final Data -> Label: '$finalLabel', Has Icon: ${finalIcon != null}")

            val bundle = Bundle()
            bundle.putCharSequence("appLabel", finalLabel)

            if (finalIcon != null) {
                try {
                    val stream = ByteArrayOutputStream()
                    finalIcon.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val iconBytes = stream.toByteArray()

                    if (iconBytes.size > 500 * 1024) {
                        Log.w(TAG, "WARNING: Icon size is large (${iconBytes.size} bytes).")
                    }
                    bundle.putByteArray("appIcon", iconBytes)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to compress icon", e)
                }
            }

            return bundle

        } catch (e: Exception) {
            Log.e(TAG, "getSessionDetails CRITICAL FAILURE", e)
            return null
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
        // Read standard output and standard error respectively
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val error = process.errorStream.bufferedReader().use { it.readText() }

        // Wait for command execution to complete and get the exit code
        val exitCode = process.waitFor()

        // Check exit code. 0 typically represents success, any non-zero value represents failure.
        if (exitCode != 0) {
            // If it failed, construct a detailed error message and throw IOException
            // This way the catch block in the execArr method can catch it and convert it to RemoteException
            throw IOException("Command execution failed, exit code: $exitCode, error message: '$error'")
        }

        // If successful, return the content of standard output
        return output
    }
}

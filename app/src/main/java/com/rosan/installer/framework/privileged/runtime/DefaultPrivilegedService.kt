// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.framework.privileged.runtime

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.IActivityManager
import android.app.IApplicationThread
import android.app.ProfilerInfo
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.UserInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.IConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.IUserManager
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.os.ResultReceiver
import android.provider.Settings
import android.system.Os
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.rosan.installer.ICommandOutputListener
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.core.reflection.getValue
import com.rosan.installer.core.reflection.invoke
import com.rosan.installer.core.reflection.invokeStatic
import com.rosan.installer.framework.privileged.util.deletePaths
import com.rosan.installer.framework.privileged.util.resolveSettingsBinder
import com.rosan.installer.util.pm.REASON_REMIND_OWNERSHIP
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import android.os.Process as AndroidProcess

@SuppressLint("PrivateApi")
class DefaultPrivilegedService private constructor(
    private val runtime: PrivilegedRuntime
) : BasePrivilegedService(), PrivilegedOperations {
    companion object {
        private const val TAG = "PrivilegedService"

        private const val SHELL_COMMAND_TRANSACTION = 0x5f434d44 // '_CMD'
        private const val SYSTEM_UID = 1000

        fun system() = DefaultPrivilegedService(PrivilegedRuntime.SystemApp)

        fun userService() = DefaultPrivilegedService(PrivilegedRuntime.UserService)

        fun shizukuHook() = DefaultPrivilegedService(PrivilegedRuntime.ShizukuHooked)

        fun binderWrapped(
            name: String,
            useAppCallerPackage: Boolean,
            binderWrapper: (IBinder) -> IBinder
        ) = DefaultPrivilegedService(PrivilegedRuntime.BinderWrapped(name, useAppCallerPackage, binderWrapper))
    }

    private val reflect by inject<ReflectionProvider>()

    private val iPackageManager: IPackageManager by lazy {
        runtime.packageManager()
    }

    private val iActivityManager: IActivityManager by lazy {
        runtime.activityManager()
    }

    private val iUserManager: IUserManager by lazy {
        runtime.userManager()
    }

    private val settingsBinder: IBinder? by lazy {
        runtime.settingsBinder(reflect)
    }

    private val iConnectivityManager: IConnectivityManager by lazy {
        runtime.connectivityManager()
    }

    private val appOpsManager: AppOpsManager by lazy {
        runtime.appOpsManager(context, reflect)
    }

    override fun delete(paths: Array<out String>) = deletePaths(paths.toList())

    override fun performDexOpt(
        packageName: String,
        compilerFilter: String,
        force: Boolean
    ): Boolean {
        Timber.tag(TAG).d("performDexOpt: $packageName, filter=$compilerFilter, force=$force")

        return try {
            val args = buildList {
                add("compile")
                add("-m")
                add(compilerFilter)

                if (force) {
                    add("-f")
                }

                add(packageName)
            }.toTypedArray()

            // Important: this must be the wrapped package binder.
            // Do not call ServiceManager.getService("package") again here.
            sendPackageShellCommandOneway(
                binder = iPackageManager.asBinder(),
                args = args
            )

            Timber.tag(TAG).d("Dexopt command dispatched: ${args.joinToString(" ")}")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to dispatch dexopt for $packageName")
            false
        }
    }

    override fun setDefaultInstaller(component: ComponentName, enable: Boolean) {
        val userId = AndroidProcess.myUid() / 100000
        val effectiveUid = Os.geteuid()
        val canCallSystemRestrictedPreferredApis =
            runtime.canCallSystemRestrictedPreferredApis || effectiveUid == SYSTEM_UID

        Timber.tag(TAG).d(
            "setDefaultInstaller called: component=%s, enable=%b, userId=%d, effectiveUid=%d, canCallSystemRestrictedPreferredApis=%b",
            component.flattenToShortString(),
            enable,
            userId,
            effectiveUid,
            canCallSystemRestrictedPreferredApis
        )

        // Reset state for our own package
        Timber.tag(TAG).v("Resetting preferred state for %s", component.packageName)
        clearPackageActivities(
            packageName = component.packageName,
            userId = userId,
            canCallSystemRestrictedPreferredApis = canCallSystemRestrictedPreferredApis
        )

        if (!enable) {
            Timber.tag(TAG).i("Enable flag is false. Exiting after clearing own preferred activities.")
            return
        }

        // Split IntentFilters
        val actions = arrayOf(Intent.ACTION_VIEW, @Suppress("Deprecation") Intent.ACTION_INSTALL_PACKAGE)

        for (action in actions) {
            Timber.tag(TAG).d("Processing intent action: %s", action)

            val intent = Intent(action).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                setDataAndType(
                    "content://storage/emulated/0/test.apk".toUri(),
                    "application/vnd.android.package-archive"
                )
            }

            val list = queryIntentActivities(
                iPackageManager,
                intent,
                "application/vnd.android.package-archive",
                PackageManager.MATCH_DEFAULT_ONLY,
                userId
            )

            val names = mutableListOf<ComponentName>()

            for (resolveInfo in list) {
                val infoPackageName = resolveInfo.activityInfo.packageName
                val infoClassName = resolveInfo.activityInfo.name

                // Dynamically clear preferred activities for other apps
                if (infoPackageName != component.packageName && infoPackageName != "android") {
                    // Use the extracted helper to clear competing apps
                    clearPackageActivities(
                        packageName = infoPackageName,
                        userId = userId,
                        canCallSystemRestrictedPreferredApis = canCallSystemRestrictedPreferredApis
                    )
                }

                names.add(ComponentName(infoPackageName, infoClassName))
            }

            Timber.tag(TAG).d("Found %d existing handlers to construct ComponentName array.", names.size)

            val filter = IntentFilter().apply {
                addAction(action)
                addCategory(Intent.CATEGORY_DEFAULT)
                addDataType("application/vnd.android.package-archive")
            }

            val match = IntentFilter.MATCH_CATEGORY_TYPE or IntentFilter.MATCH_ADJUSTMENT_MASK

            Timber.tag(TAG).d("Setting up preferred activities for %s", action)

            setupPackagePreferredActivities(
                iPackageManager = iPackageManager,
                filter = filter,
                match = match,
                names = names.toTypedArray(),
                component = component,
                userId = userId,
                removeExisting = true,
                canCallSystemRestrictedPreferredApis = canCallSystemRestrictedPreferredApis
            )
        }

        Timber.tag(TAG).i("Successfully configured default installer.")
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
            Timber.tag(TAG).w("execArrWithCallback called with a null listener.")
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
                        Timber.tag(TAG).e(e, "Error reading stdout or sending callback")
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
                        Timber.tag(TAG).e(e, "Error reading stderr or sending callback")
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
            Timber.tag(TAG).e(e, errorMessage)
            try {
                listener.onError(errorMessage)
                listener.onComplete(-1) // Send a failure exit code
            } catch (_: RemoteException) {
                // The client might be dead, just log it.
                Timber.tag(TAG).e(e, "Failed to send execution error to client.")
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
            val info = reflect.resolveSettingsBinder()
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
                val targetResolver = object : ContentResolver(runtime.settingsResolverContext(context)) {}

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

            Timber.tag(TAG).d("Granting $permission for $packageName (UID: $userId)")

            iPackageManager.grantRuntimePermission(packageName, permission, userId)

            Timber.tag(TAG).i("Successfully granted $permission to $packageName")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "ERROR granting permission")
            throw RemoteException("Failed to grant permission via system API: ${e.message}")
        }
    }

    override fun isPermissionGranted(packageName: String, permission: String): Boolean {
        Timber.tag(TAG).d("Checking permission '$permission' for package '$packageName'")
        try {
            // Because this code runs in a privileged context with access to a full Context,
            // we can directly use the standard PackageManager API.
            val result = context.packageManager.checkPermission(permission, packageName)
            // The API returns PERMISSION_GRANTED (0) on success.
            return result == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            // Catch potential exceptions, e.g., if the package name is invalid,
            // though checkPermission typically returns PERMISSION_DENIED for that.
            Timber.tag(TAG).e(e, "Failed to check permission '$permission' for '$packageName'")
            // It's safer to return false on any error.
            return false
        }
    }

    override fun startActivityPrivileged(intent: Intent): Boolean {
        try {
            val am = iActivityManager

            val userId = AndroidProcess.myUid() / 100000
            val callerPackage = runtime.activityCallerPackage(context)
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
            Timber.tag(TAG).e(e, "startActivityPrivileged failed due to SecurityException")
            return false
        } catch (e: Exception) {
            // Catch other potential exceptions, such as RemoteException.
            Timber.tag(TAG).e(e, "startActivityPrivileged failed with an exception")
            return false
        }
    }

    override fun sendBroadcastPrivileged(intent: Intent): Boolean {
        try {
            val am = iActivityManager

            val userId = AndroidProcess.myUid() / 100000
            val resolvedType = intent.resolveType(context.contentResolver)

            // 假装result不存在，防止一些系统上可能出问题
            am.broadcastIntent(
                currentApplicationThread,
                intent,
                resolvedType,
                null,
                0,
                null,
                null,
                null,
                -1,
                null,
                false,
                false,
                userId
            )
            return true
        } catch (e: SecurityException) {
            // Log security exceptions specifically, as they indicate a permission issue.
            Timber.tag(TAG).e(e, "sendBroadcastPrivileged failed due to SecurityException")
            return false
        } catch (e: Exception) {
            // Catch other potential exceptions, such as RemoteException.
            Timber.tag(TAG).e(e, "sendBroadcastPrivileged failed with an exception")
            return false
        }
    }

    private val currentApplicationThread: IApplicationThread?
        get() {
            return try {
                val activityThreadClass = Class.forName("android.app.ActivityThread")
                val activityThread = reflect.invokeStatic<Any>("currentActivityThread", activityThreadClass)
                activityThread?.let {
                    reflect.invoke<IApplicationThread>(it, "getApplicationThread", activityThreadClass)
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to get IApplicationThread")
                null
            }
        }

    override fun getSessionDetails(sessionId: Int): Bundle? {
        Timber.tag(TAG).d("getSessionDetails: sessionId=$sessionId")

        val packageInstaller = context.packageManager.packageInstaller
        val sessionInfo = packageInstaller.getSessionInfo(sessionId) ?: run {
            Timber.tag(TAG).w("getSessionDetails: sessionInfo is null for id $sessionId")
            return null
        }

        var resolvedLabel: CharSequence? = null
        var resolvedIcon: Bitmap? = null
        var path: String?

        // ---------------------------------------------------------
        // STRATEGY 1 & 2: Reflection for APK Path
        // ---------------------------------------------------------
        path = reflect.getValue<String>(sessionInfo, "resolvedBaseCodePath")

        if (path == null) {
            val stageDir = reflect.getValue<File>(sessionInfo, "stageDir")
            if (stageDir != null && stageDir.exists() && stageDir.isDirectory) {
                path = stageDir.listFiles { _, name -> name.endsWith(".apk") }
                    ?.firstOrNull()?.absolutePath
                Timber.tag(TAG).d("Found APK path via stageDir: $path")
            }
        }

        // ---------------------------------------------------------
        // STRATEGY 2.5: Root/Privileged Direct File Access
        // ---------------------------------------------------------
        if (path == null) {
            val sessionDir = File("/data/app/vmdl${sessionId}.tmp")
            if (sessionDir.exists() && sessionDir.isDirectory) {
                val apkFiles = sessionDir.listFiles { _, name -> name.endsWith(".apk", true) }
                if (!apkFiles.isNullOrEmpty()) {
                    val targetApk = apkFiles.find { it.name == "base.apk" } ?: apkFiles.first()
                    path = targetApk.absolutePath
                    Timber.tag(TAG).d("Direct Access: Found APK path: $path")
                }
            }
        }

        // ---------------------------------------------------------
        // Parse APK from path (if found)
        // ---------------------------------------------------------
        if (!path.isNullOrEmpty()) {
            runCatching {
                val pm = context.packageManager
                val pkgInfo = pm.getPackageArchiveInfo(path, PackageManager.GET_META_DATA)
                pkgInfo?.applicationInfo?.let { appInfo ->
                    appInfo.publicSourceDir = path
                    appInfo.sourceDir = path

                    resolvedLabel = appInfo.loadLabel(pm)

                    val drawable = appInfo.loadIcon(pm)
                    resolvedIcon = if (drawable is BitmapDrawable) {
                        drawable.bitmap
                    } else {
                        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 1
                        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 1
                        drawable.toBitmap(width, height, Bitmap.Config.ARGB_8888)
                    }
                }
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "Failed to parse APK from path: $path")
            }
        }

        val packageName = sessionInfo.appPackageName ?: ""

        // Try to get the exact target User ID for this session, fallback to current process user
        val targetUserId = runCatching {
            sessionInfo.user.hashCode() // On modern Android, userHandle.hashCode() == userId
        }.getOrDefault(AndroidProcess.myUid() / 100000)

        // ---------------------------------------------------------
        // STRATEGY 3: Fallback to Installed App Info (Crucial for Updates)
        // ---------------------------------------------------------
        if ((resolvedLabel == null || resolvedIcon == null) && packageName.isNotEmpty()) {
            runCatching {
                // [Optimized] Use our hidden iPackageManager to respect the correct userId
                val appInfo = iPackageManager.getApplicationInfo(packageName, 0, targetUserId)
                val pm = context.packageManager

                if (resolvedLabel == null) resolvedLabel = appInfo.loadLabel(pm)
                if (resolvedIcon == null) {
                    val drawable = appInfo.loadIcon(pm)
                    resolvedIcon = if (drawable is BitmapDrawable) {
                        drawable.bitmap
                    } else {
                        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 1
                        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 1
                        drawable.toBitmap(width, height, Bitmap.Config.ARGB_8888)
                    }
                }
                Timber.tag(TAG).d("Fallback: Loaded missing info from installed app")
            }
        }

        var isUpdate = false
        var isOwnershipConflict = false
        val isPreApprovalRequested = runCatching {
            reflect.invoke<Boolean>(
                sessionInfo,
                "isPreApprovalRequested",
                sessionInfo::class.java,
                emptyArray()
            ) == true
        }.getOrDefault(false)
        var sourceAppLabel: CharSequence? = null

        if (packageName.isNotEmpty()) {
            // 1. Check if this is an update using iPackageManager and specific userId
            isUpdate = runCatching {
                iPackageManager.getPackageInfo(packageName, 0, targetUserId) != null
            }.getOrDefault(false)

            // 2. Check for Android 14+ Update Ownership Conflict
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                runCatching {
                    val pendingReason = reflect.invoke<Int>(
                        sessionInfo, "getPendingUserActionReason",
                        sessionInfo::class.java, emptyArray()
                    ) ?: 0
                    isOwnershipConflict = (pendingReason == REASON_REMIND_OWNERSHIP)
                }.onFailure {
                    // Fallback inference
                    runCatching {
                        val ownerPkg = context.packageManager.getInstallSourceInfo(packageName).updateOwnerPackageName
                        if (!ownerPkg.isNullOrEmpty() && ownerPkg != context.packageName) {
                            isOwnershipConflict = true
                        }
                    }
                }
            }

            // 3. Determine the appropriate source app label
            runCatching {
                val pm = context.packageManager
                val targetPkgToResolve = if (isOwnershipConflict) {
                    pm.getInstallSourceInfo(packageName).updateOwnerPackageName
                } else {
                    sessionInfo.installerPackageName
                }

                if (!targetPkgToResolve.isNullOrEmpty()) {
                    val appInfo = pm.getApplicationInfo(targetPkgToResolve, 0) // No need for flags if 0
                    sourceAppLabel = pm.getApplicationLabel(appInfo)
                    Timber.tag(TAG).d("Source app label resolved: $sourceAppLabel (Conflict: $isOwnershipConflict)")
                }
            }
        }

        // ---------------------------------------------------------
        // Final Data Preparation
        // ---------------------------------------------------------
        val finalLabel = resolvedLabel ?: sessionInfo.appLabel ?: "N/A"
        Timber.tag(TAG).d("Final Data -> Label: '$finalLabel', Has Icon: ${resolvedIcon != null}")

        return Bundle().apply {
            putCharSequence("appLabel", finalLabel)
            putString("packageName", packageName)
            putString("installerPackageName", sessionInfo.installerPackageName)
            putBoolean("isUpdate", isUpdate)
            putBoolean("isOwnershipConflict", isOwnershipConflict)
            putBoolean("isPreApprovalRequested", isPreApprovalRequested)

            sourceAppLabel?.let { putCharSequence("sourceAppLabel", it) }

            // Pass Bitmap directly.
            // Binder will automatically transfer pixel data via Ashmem (Shared Memory),
            // completely avoiding the 1MB Binder Transaction Limit. No compression needed!
            resolvedIcon?.let { putParcelable("appIcon", it) }
        }
    }

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
                Timber.tag(TAG).e("Failed to get user list, method returned null.")
                return userMap
            }

            for (userObject in usersList) {
                userMap[userObject.id] = userObject.name ?: "Unknown User"
            }

            Timber.tag(TAG).d("Fetched users: $userMap")
        } catch (e: SecurityException) {
            Timber.tag(TAG).e(e, "Permission denied for getUsers, falling back to current user")
            val userId = AndroidProcess.myUid() / 100000
            userMap[userId] = "Current User"
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting users")
        }
        return userMap
    }

    override fun setPackageNetworkingEnabled(uid: Int, enabled: Boolean) {
        try {
            val cm = this.iConnectivityManager

            // The integer 3 actually means FIREWALL_CHAIN_POWERSAVE (Whitelist mode).
            // We must use 9, which represents FIREWALL_CHAIN_OEM_DENY_3 (Blacklist mode).
            val chain = 9

            // FIREWALL_RULE_DEFAULT = 0, FIREWALL_RULE_ALLOW = 1, FIREWALL_RULE_DENY = 2
            // For a DENY chain, use DENY (2) to block, and DEFAULT (0) to remove the block.
            val rule = if (enabled) 0 else 2

            if (!enabled) {
                // Block network: Ensure the chain is enabled, then apply DENY rule to the UID
                cm.setFirewallChainEnabled(chain, true)
                cm.setUidFirewallRule(chain, uid, rule)
                Timber.tag(TAG).i("Network BLOCKED for UID: $uid via OEM_DENY_3")
            } else {
                // Restore network: Reset the UID rule to DEFAULT to remove the restriction
                cm.setUidFirewallRule(chain, uid, rule)
                // WARNING: Do NOT disable the entire chain here, otherwise other apps blocked
                // in this chain will also regain network access unexpectedly.
                Timber.tag(TAG).i("Network RESTORED for UID: $uid via OEM_DENY_3")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to set package networking via AIDL Stub")
            throw RemoteException("AIDL Stub invocation failed: ${e.message}")
        }
    }

    override fun prepareUnknownSourceAppOp(uid: Int, packageName: String): Int {
        val op = AppOpsManager.permissionToOp(Manifest.permission.REQUEST_INSTALL_PACKAGES)
            ?: run {
                Timber.tag(TAG).w("REQUEST_INSTALL_PACKAGES has no AppOps mapping for $packageName/$uid")
                return AppOpsManager.MODE_ERRORED
            }

        Timber.tag(TAG).d("noteOp for request-install AppOps: package=$packageName, uid=$uid, op=$op")

        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            appOpsManager.noteOpNoThrow(
                op,
                uid,
                packageName,
                null,
                "Started package installation activity"
            )
        } else {
            @Suppress("Deprecation")
            appOpsManager.noteOpNoThrow(op, uid, packageName)
        }

        Timber.tag(TAG).d("request-install AppOps noteOp result for $packageName/$uid: mode=$mode")
        if (mode == AppOpsManager.MODE_DEFAULT) {
            Timber.tag(TAG).d("Setting request-install AppOps default mode to errored for $packageName/$uid")
            reflect.getMethod(
                "setMode",
                appOpsManager.javaClass,
                String::class.java,
                Int::class.javaPrimitiveType!!,
                String::class.java,
                Int::class.javaPrimitiveType!!
            )?.invoke(appOpsManager, op, uid, packageName, AppOpsManager.MODE_ERRORED)
        }

        return mode
    }

    private fun sendPackageShellCommandOneway(
        binder: IBinder,
        args: Array<String>
    ) {
        val data = Parcel.obtain()

        val stdin = ParcelFileDescriptor.open(
            File("/dev/null"),
            ParcelFileDescriptor.MODE_READ_ONLY
        )

        val stdout = ParcelFileDescriptor.open(
            File("/dev/null"),
            ParcelFileDescriptor.MODE_WRITE_ONLY
        )

        val stderr = ParcelFileDescriptor.open(
            File("/dev/null"),
            ParcelFileDescriptor.MODE_WRITE_ONLY
        )

        try {
            data.writeFileDescriptor(stdin.fileDescriptor)
            data.writeFileDescriptor(stdout.fileDescriptor)
            data.writeFileDescriptor(stderr.fileDescriptor)
            data.writeStringArray(args)

            writeNullShellCallback(data)

            // Server still expects a ResultReceiver object, but we do not wait for it.
            ResultReceiver(null).writeToParcel(data, 0)

            binder.transact(
                SHELL_COMMAND_TRANSACTION,
                data,
                null,
                FLAG_ONEWAY
            )
        } finally {
            data.recycle()
            runCatching { stdin.close() }
            runCatching { stdout.close() }
            runCatching { stderr.close() }
        }
    }

    @SuppressLint("PrivateApi")
    private fun writeNullShellCallback(parcel: Parcel) {
        val shellCallbackClass = Class.forName("android.os.ShellCallback")
        val method = shellCallbackClass.getDeclaredMethod(
            "writeToParcel",
            shellCallbackClass,
            Parcel::class.java
        )

        method.invoke(null, null, parcel)
    }

    /**
     * Clears both preferred and persistent preferred activities for a specific package.
     * Includes error handling to prevent crashes on restricted environments.
     */
    private fun clearPackageActivities(
        packageName: String,
        userId: Int,
        canCallSystemRestrictedPreferredApis: Boolean
    ) {
        // 1. Clear standard preferred activities (Always try)
        try {
            Timber.tag(TAG).d("Clearing standard preferred activities for $packageName")
            iPackageManager.clearPackagePreferredActivities(packageName)
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to clear preferred activities for $packageName")
        }

        // 2. Clear persistent preferred activities (Only if the runtime can call system-restricted APIs)
        if (canCallSystemRestrictedPreferredApis) {
            try {
                Timber.tag(TAG).d("Clearing persistent preferred activities for $packageName")
                packageManagerFromContext().clearPackagePersistentPreferredActivities(packageName, userId)
                Timber.tag(TAG).d("Successfully cleared persistent preferred activities for $packageName")
            } catch (e: SecurityException) {
                // Specific log for the "only be run by the system" issue
                Timber.tag(TAG).e(e, "SecurityException: System restricted clearing persistent preferred for $packageName")
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to clear persistent preferred activities for $packageName")
            }
        }
    }

    /**
     * Sets both standard and persistent preferred activities for the target component.
     * Includes fallback mechanisms and error boundaries for restricted environments.
     */
    private fun setupPackagePreferredActivities(
        iPackageManager: IPackageManager,
        filter: IntentFilter,
        match: Int,
        names: Array<ComponentName>,
        component: ComponentName,
        userId: Int,
        removeExisting: Boolean,
        canCallSystemRestrictedPreferredApis: Boolean
    ) {
        // 1. Add standard preferred activity
        try {
            Timber.tag(TAG).d("Adding preferred activity for %s", component.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                iPackageManager.addPreferredActivity(filter, match, names, component, userId, removeExisting)
            } else {
                iPackageManager.addPreferredActivity(filter, match, names, component, userId)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to add preferred activity for %s", component.packageName)
        }

        // 2. Add persistent preferred activity (Only if the runtime can call system-restricted APIs)
        if (canCallSystemRestrictedPreferredApis) {
            try {
                Timber.tag(TAG).d("Adding persistent preferred activity for %s", component.packageName)
                packageManagerFromContext().addPersistentPreferredActivity(filter, component, userId)
                Timber.tag(TAG).d("Successfully added persistent preferred activity for %s", component.packageName)
            } catch (e: SecurityException) {
                Timber.e(e, "SecurityException: System restricted adding persistent preferred for %s", component.packageName)
            } catch (e: Exception) {
                Timber.e(e, "Failed to add persistent preferred activity for %s", component.packageName)
            }
        }
    }

    private fun packageManagerFromContext(): IPackageManager =
        reflect.getValue<IPackageManager>(context.packageManager, "mPM")
            ?: iPackageManager

    private fun queryIntentActivities(
        iPackageManager: IPackageManager,
        intent: Intent,
        resolvedType: String?,
        flags: Int,
        userId: Int
    ): List<ResolveInfo> = try {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            iPackageManager.queryIntentActivities(intent, resolvedType, flags.toLong(), userId)
        } else {
            iPackageManager.queryIntentActivities(intent, resolvedType, flags, userId)
        }
        result?.list ?: emptyList()
    } catch (e: Exception) {
        Timber.e(e, "Failed to query intent activities")
        emptyList()
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

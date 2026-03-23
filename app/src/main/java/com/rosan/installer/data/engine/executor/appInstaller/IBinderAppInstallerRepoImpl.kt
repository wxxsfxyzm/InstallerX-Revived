// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appInstaller

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.Session
import android.content.pm.PackageManager
import android.content.pm.VersionedPackage
import android.os.Build
import android.os.IBinder
import android.os.IInterface
import android.os.ServiceManager
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.installer.BuildConfig
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.core.reflection.getValue
import com.rosan.installer.data.engine.executor.PackageInstallerUtil.abiOverride
import com.rosan.installer.data.engine.executor.PackageInstallerUtil.installFlags
import com.rosan.installer.data.engine.executor.PackageManagerUtil
import com.rosan.installer.data.privileged.util.requireDhizukuPermissionGranted
import com.rosan.installer.domain.device.model.Architecture
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.exception.InstallException
import com.rosan.installer.domain.engine.model.DataType
import com.rosan.installer.domain.engine.model.InstallEntity
import com.rosan.installer.domain.engine.model.InstallErrorType
import com.rosan.installer.domain.engine.model.InstallOption
import com.rosan.installer.domain.engine.model.sourcePath
import com.rosan.installer.domain.engine.repository.AppInstallerRepository
import com.rosan.installer.domain.privileged.model.PostInstallTaskInfo
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.model.PackageSource
import com.rosan.installer.util.addFlag
import com.rosan.installer.util.pm.isFreshInstallCandidate
import com.rosan.installer.util.pm.isPackageArchivedCompat
import com.rosan.installer.util.removeFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import android.os.Process as AndroidProcess

abstract class IBinderAppInstallerRepoImpl(
    protected val context: Context,
    protected val reflect: ReflectionProvider,
    protected val capabilityProvider: DeviceCapabilityProvider,
    protected val postInstallTaskProvider: PostInstallTaskProvider
) : AppInstallerRepository {
    private val taskScope = CoroutineScope(Dispatchers.IO)

    protected abstract suspend fun iBinderWrapper(iBinder: IBinder): IBinder

    private suspend fun getPackageInstaller(config: ConfigModel): PackageInstaller {
        val iPackageManager =
            IPackageManager.Stub.asInterface(iBinderWrapper(ServiceManager.getService("package")))
        val iPackageInstaller =
            IPackageInstaller.Stub.asInterface(iBinderWrapper(iPackageManager.packageInstaller.asBinder()))

        // Resolve the target user ID based on config
        val finalUserId = if (config.enableCustomizeUser) config.targetUserId else AndroidProcess.myUid() / 100000

        val installerPackageName = when (config.authorizer) {
            Authorizer.Dhizuku -> getDhizukuComponentName()
            Authorizer.None -> if (capabilityProvider.isSystemApp) context.packageName else BuildConfig.APPLICATION_ID
            else -> config.installer ?: BuildConfig.APPLICATION_ID
        }

        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            reflect.getDeclaredConstructor(
                PackageInstaller::class.java,
                IPackageInstaller::class.java,
                String::class.java,
                String::class.java,
                Int::class.java,
            )!!.newInstance(iPackageInstaller, installerPackageName, null, finalUserId)
        else reflect.getDeclaredConstructor(
            PackageInstaller::class.java,
            IPackageInstaller::class.java,
            String::class.java,
            Int::class.java,
        )!!.newInstance(
            iPackageInstaller,
            installerPackageName,
            finalUserId
        )) as PackageInstaller
    }

    private suspend fun getDhizukuComponentName(): String =
        requireDhizukuPermissionGranted {
            Dhizuku.getOwnerPackageName()
        }

    private suspend fun setSessionIBinder(session: Session) {
        val iPackageInstallerSession = reflect.getValue<IInterface>(session, "mSession") ?: return
        val iBinder = iPackageInstallerSession.asBinder()
        reflect.setFieldValue(
            session,
            "mSession",
            session::class.java,
            IPackageInstallerSession.Stub.asInterface(iBinderWrapper(iBinder))
        )
    }

    override suspend fun approveSession(
        config: ConfigModel,
        sessionId: Int,
        granted: Boolean
    ) {
        val iPackageManager =
            IPackageManager.Stub.asInterface(iBinderWrapper(ServiceManager.getService("package")))

        val iPackageInstaller =
            IPackageInstaller.Stub.asInterface(iBinderWrapper(iPackageManager.packageInstaller.asBinder()))

        try {
            Timber.d("Approving session $sessionId (granted: $granted) via Binder wrapper")

            reflect.invokeMethod(
                iPackageInstaller,
                "setPermissionsResult",
                IPackageInstaller::class.java,
                arrayOf(Int::class.java, Boolean::class.java),
                sessionId,
                granted
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to approve session via Binder")

            if (!granted) {
                try {
                    iPackageInstaller.abandonSession(sessionId)
                    Timber.d("Fallback: Session $sessionId abandoned.")
                } catch (_: Exception) {
                }
            }
            throw e
        }
    }

    override suspend fun doInstallWork(
        config: ConfigModel,
        entities: List<InstallEntity>,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    ) {
        val result = runCatching {
            entities.groupBy { it.packageName }.forEach { (packageName, entities) ->
                doInnerWork(config, entities, packageName, blacklist, sharedUserIdBlacklist, sharedUserIdExemption)
            }
        }
        doFinishWork(config, entities, result)
        result.onFailure {
            throw it
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun doUninstallWork(
        config: ConfigModel,
        packageName: String
    ) {
        // Get the underlying IPackageManager and IPackageInstaller interfaces.
        val iPackageManager =
            IPackageManager.Stub.asInterface(iBinderWrapper(ServiceManager.getService("package")))
        val iPackageInstaller =
            IPackageInstaller.Stub.asInterface(iBinderWrapper(iPackageManager.packageInstaller.asBinder()))

        // Prepare parameters for the direct AIDL call.
        val receiver = LocalIntentReceiver(reflect)
        val flags = config.uninstallFlags
        val versionedPackage = VersionedPackage(packageName, PackageManager.VERSION_CODE_HIGHEST)
        val callerPackageName = when (config.authorizer) {
            Authorizer.Dhizuku -> getDhizukuComponentName()
            else -> context.packageName
        }

        // Always use the current user for uninstallation, ignoring config.enableCustomizeUser
        // This prevents accidental uninstallation from other profiles if the config was reused from an install session.
        val currentUserId = AndroidProcess.myUid() / 100000

        Timber.d("Directly calling IPackageInstaller.uninstall with flags: $flags")

        // Directly call the method from the stub interface.
        iPackageInstaller.uninstall(
            versionedPackage,
            callerPackageName,
            flags,
            receiver.getIntentSender(),
            currentUserId
        )

        // The result verification logic remains the same.
        PackageManagerUtil.uninstallResultVerify(context, receiver)
    }

    private suspend fun doInnerWork(
        config: ConfigModel,
        entities: List<InstallEntity>,
        packageName: String,
        managedBlacklistPackages: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    ) {
        if (!config.bypassBlacklistInstallSetByUser) {
            // Blacklisted package names
            if (managedBlacklistPackages.contains(packageName)) {
                Timber.w("Installation blocked for $packageName because it is in the blacklist.")
                throw InstallException(
                    InstallErrorType.BLACKLISTED_PACKAGE,
                    "Installation blocked for $packageName because it is in the blacklist."
                )
            }

            // Blacklisted SharedUserID
            // Simply take the first entity's sharedUserId because they are all the same.
            val sharedUid = entities.firstOrNull()?.sharedUserId

            if (sharedUid != null) {
                if (sharedUserIdBlacklist.contains(sharedUid) && !sharedUserIdExemption.contains(packageName)) {
                    Timber.w("Installation blocked for $packageName because its sharedUserId '$sharedUid' is blacklisted.")
                    throw InstallException(
                        InstallErrorType.BLACKLISTED_PACKAGE,
                        "Installation blocked for $packageName because its sharedUserId '$sharedUid' is blacklisted."
                    )
                }
            }
        }

        if (entities.isEmpty()) return
        val packageInstaller = getPackageInstaller(config)
        var session: Session? = null
        try {
            session = createSession(config, entities, packageInstaller, packageName)
            installIts(entities, session)
            commit(session)
        } catch (e: Exception) {
            session?.abandon()
            throw e
        } finally {
            session?.runCatching { close() }
        }
    }

    private suspend fun createSession(
        config: ConfigModel,
        entities: List<InstallEntity>,
        packageInstaller: PackageInstaller,
        packageName: String
    ): Session {
        val pm = context.packageManager
        val containerType = entities.first().sourceType
        val params = if (containerType == DataType.MULTI_APK_ZIP || containerType == DataType.MULTI_APK)
            PackageInstaller.SessionParams(
                // Always use full mode when apk is definite
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
        else
            PackageInstaller.SessionParams(
                when (entities.count { it.name == "base.apk" }) {
                    1 -> PackageInstaller.SessionParams.MODE_FULL_INSTALL
                    0 -> PackageInstaller.SessionParams.MODE_INHERIT_EXISTING
                    else -> throw Exception("can't install multiple package name in single session")
                }
            )
        config.callingFromUid?.let { params.setOriginatingUid(it) }
        params.setAppPackageName(packageName)
        // Customize Install Reason
        if (config.enableCustomizeInstallReason) {
            Timber.d("Setting installReason to ${config.installReason.name} (${config.installReason.value})")
            params.setInstallReason(config.installReason.value)
        } else
            params.setInstallReason(PackageManager.INSTALL_REASON_UNKNOWN)
        // --- Customize PackageSource ---
        // Only available on Android 13+, Dhizuku need test
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && config.authorizer != Authorizer.Dhizuku) {
            Timber.d("Setting packageSource to ${config.packageSource.name} (${config.packageSource.value})")
            if (config.enableCustomizePackageSource)
                params.setPackageSource(config.packageSource.value)
            else
                params.setPackageSource(PackageSource.UNSPECIFIED.value)
        }
        // --- PackageSource End ---

        // --- InstallFlags Start ---
        Timber.tag("InstallFlags").d("Initial install flags: ${params.installFlags}")
        Timber.tag("InstallFlags").d("Install flags from config: ${config.installFlags}")
        // Start with the base flags from params and config
        var newFlags = params.installFlags.addFlag(config.installFlags)
        // Force-enable the 'ReplaceExisting' flag as a baseline
        newFlags = newFlags.addFlag(InstallOption.ReplaceExisting.value)
        Timber.tag("InstallFlags").d("After adding baseline flags: $newFlags")
        // Conditionally add the 'UnArchive' flag
        if (context.packageManager.isPackageArchivedCompat(packageName)) {
            Timber.tag("InstallFlags").d("Package $packageName is archived, adding unarchive option.")
            newFlags = newFlags.addFlag(InstallOption.UnArchive.value)
        } else {
            Timber.tag("InstallFlags").d("Package $packageName is not archived.")
        }
        params.installFlags = newFlags
        Timber.tag("InstallFlags").d("Install flags after customization: ${params.installFlags}")
        // --- InstallFlags End ---

        // --- Disable not supported stuff ---
        val shouldGrantAll =
            config.allowAllRequestedPermissions &&
                    config.authorizer != Authorizer.Dhizuku &&
                    config.authorizer != Authorizer.None &&
                    pm.isFreshInstallCandidate(packageName)

        if (!shouldGrantAll) {
            params.installFlags = params.installFlags.removeFlag(InstallOption.GrantAllRequestedPermissions.value)
        }
        // --- Disable End ---

        // Android System will ignore INSTALL_ALLOW_DOWNGRADE for None ROOT/SYSTEM on Android 15+, no need to disable it here

        // --- Set abiOverride ---
        // Get the architecture of the base APK.
        // With the updated ApkParser logic, this accurately reflects the actual native libraries in the APK.
        val baseApkArch = entities.firstOrNull { it.name == "base.apk" }?.arch
        Timber.d("Current Arch to install: $baseApkArch")

        // Only set abiOverride if the APK actually contains native libraries.
        // Pure Java/Kotlin apps (Architecture.NONE) should be left to the system to decide.
        if ((containerType == DataType.APK || containerType == DataType.MULTI_APK || containerType == DataType.MULTI_APK_ZIP) &&
            baseApkArch != null &&
            baseApkArch != Architecture.NONE
        ) {
            val abiToOverride = if (baseApkArch != Architecture.UNKNOWN) {
                // Trust the parser result.
                // Even for mismatched architectures (e.g., x86 on ARM), passing the actual arch string (e.g., "x86")
                // allows the system to attempt binary translation (like Houdini) if available.
                baseApkArch.arch
            } else {
                // Fallback for extremely rare cases where arch cannot be identified but native libs exist.
                "armeabi-v7a"
            }

            Timber.d("Setting abiOverride to $abiToOverride")
            params.abiOverride = abiToOverride
        }
        // --- abiOverride End ---

        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)
        setSessionIBinder(session)
        return session
    }

    private fun installIts(entities: List<InstallEntity>, session: Session) {
        for (entity in entities) installIt(entity, session)
    }

    private fun installIt(entity: InstallEntity, session: Session) {
        Timber.d("Installing entity: ${entity.name}, data path: ${entity.data}, top source: ${entity.data.getSourceTop()}")
        val inputStream = entity.data.getInputStreamWhileNotEmpty()
            ?: throw Exception("can't open input stream for this data: '${entity.data}'")
        val sizeBytes = entity.data.getSize()

        if (sizeBytes <= 0) {
            throw Exception("Invalid data size: $sizeBytes. Content-Length is required for stream installation.")
        }
        session.openWrite(
            entity.name,
            0,
            sizeBytes // Use the explicit size
        ).use { outputStream ->
            inputStream.copyTo(outputStream)
            session.fsync(outputStream)
        }
        inputStream.close()
    }

    @SuppressLint("RequestInstallPackagesPolicy")
    private suspend fun commit(session: Session) {
        val receiver = LocalIntentReceiver(reflect)
        session.commit(receiver.getIntentSender())
        PackageManagerUtil.installResultVerify(context, receiver)
    }

    open suspend fun doFinishWork(
        config: ConfigModel,
        entities: List<InstallEntity>,
        result: Result<Unit>
    ) {
        Timber.tag("doFinishWork").d("isSuccess: ${result.isSuccess}")
        if (result.isSuccess) {
            val packageName = entities.firstOrNull()?.packageName ?: return

            val isDeleteCapable = entities.firstOrNull()?.sourceType !in listOf(
                DataType.MULTI_APK_ZIP,
                DataType.MIXED_MODULE_APK,
                DataType.MIXED_MODULE_ZIP
            )

            val shouldDelete = config.autoDelete && (isDeleteCapable || config.autoDeleteZip)

            val pathsToDelete = if (shouldDelete) entities.sourcePath().toList() else emptyList()

            taskScope.launch {
                runCatching {
                    postInstallTaskProvider.executeTasks(
                        authorizer = config.authorizer,
                        customizeAuthorizer = config.customizeAuthorizer,
                        info = PostInstallTaskInfo(
                            packageName = packageName,
                            enableDexopt = config.enableManualDexopt,
                            dexoptMode = config.dexoptMode.value,
                            forceDexopt = config.forceDexopt,
                            enableAutoDelete = shouldDelete,
                            deletePaths = pathsToDelete
                        )
                    )
                }.onFailure { e ->
                    Timber.e(e, "Async post-install tasks failed")
                }
            }
        }
    }
}

// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appinstaller

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.Session
import android.content.pm.PackageManager
import android.content.pm.PackageManagerHidden
import android.content.pm.VersionedPackage
import android.content.res.AssetFileDescriptor
import android.os.Build
import android.os.IBinder
import android.os.IInterface
import android.os.ServiceManager
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.installer.BuildConfig
import com.rosan.installer.core.bitmask.addFlag
import com.rosan.installer.core.bitmask.removeFlag
import com.rosan.installer.core.device.model.Architecture
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.core.reflection.getValue
import com.rosan.installer.data.engine.executor.PackageInstallerUtil.abiOverride
import com.rosan.installer.data.engine.executor.PackageInstallerUtil.installFlags
import com.rosan.installer.data.engine.executor.PackageManagerUtil
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.exception.InstallException
import com.rosan.installer.domain.engine.model.error.InstallErrorType
import com.rosan.installer.domain.engine.model.install.InstallEntity
import com.rosan.installer.domain.engine.model.install.InstallMetadata
import com.rosan.installer.domain.engine.model.install.InstallOption
import com.rosan.installer.domain.engine.model.install.shouldAutoDeleteSource
import com.rosan.installer.domain.engine.model.install.sourcePath
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.engine.repository.AppInstallerRepository
import com.rosan.installer.domain.privileged.model.PostInstallTaskInfo
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.model.config.InstallerMode
import com.rosan.installer.framework.privileged.core.execution.authorization.requireDhizukuPermissionGranted
import com.rosan.installer.util.pm.isFreshInstallCandidate
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
    private companion object {
        const val INSTALL_FLAGS_TAG = "InstallFlags"
    }

    private val taskScope = CoroutineScope(Dispatchers.IO)

    protected abstract suspend fun iBinderWrapper(iBinder: IBinder): IBinder

    private suspend fun resolvePackageInstallerBinder(): IPackageInstaller {
        val packageManager =
            IPackageManager.Stub.asInterface(iBinderWrapper(ServiceManager.getService("package")))
        return IPackageInstaller.Stub.asInterface(iBinderWrapper(packageManager.packageInstaller.asBinder()))
    }

    override suspend fun resolveInstallerPackageName(config: ConfigModel): String =
        when (config.authorizer) {
            Authorizer.Dhizuku -> getDhizukuComponentName()
            Authorizer.None if (!capabilityProvider.isSystemApp) -> BuildConfig.APPLICATION_ID
            else -> config.resolveConfiguredInstallerPackageName()
        }

    private suspend fun resolvePackageInstallerCallerPackageName(config: ConfigModel): String =
        when (config.authorizer) {
            Authorizer.Dhizuku -> getDhizukuComponentName()
            Authorizer.None -> if (capabilityProvider.isSystemApp) context.packageName else BuildConfig.APPLICATION_ID
            else -> config.resolveConfiguredInstallerPackageName()
        }

    private fun ConfigModel.resolveConfiguredInstallerPackageName(): String =
        when (installerMode) {
            InstallerMode.Self -> BuildConfig.APPLICATION_ID
            InstallerMode.Initiator -> initiatorPackageName ?: BuildConfig.APPLICATION_ID
            InstallerMode.Custom -> installer ?: BuildConfig.APPLICATION_ID
        }

    private suspend fun getPackageInstaller(config: ConfigModel): PackageInstaller {
        val packageInstaller = resolvePackageInstallerBinder()

        // Resolve the target user ID based on config
        val finalUserId = if (config.enableCustomizeUser) config.targetUserId else AndroidProcess.myUid() / 100000

        val callerPackageName = resolvePackageInstallerCallerPackageName(config)

        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            reflect.getDeclaredConstructor(
                PackageInstaller::class.java,
                IPackageInstaller::class.java,
                String::class.java,
                String::class.java,
                Int::class.java,
            )!!.newInstance(packageInstaller, callerPackageName, null, finalUserId)
        else reflect.getDeclaredConstructor(
            PackageInstaller::class.java,
            IPackageInstaller::class.java,
            String::class.java,
            Int::class.java,
        )!!.newInstance(
            packageInstaller,
            callerPackageName,
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
        val packageInstaller = resolvePackageInstallerBinder()

        try {
            Timber.d("Approving session $sessionId (granted: $granted) via Binder wrapper")

            packageInstaller.setPermissionsResult(sessionId, granted)
        } catch (e: Exception) {
            Timber.e(e, "Failed to approve session via Binder")

            if (!granted) {
                try {
                    packageInstaller.abandonSession(sessionId)
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
        metadata: InstallMetadata,
        respectPlatformInstallPolicy: Boolean,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    ) {
        val result = runCatching {
            entities.groupBy { it.packageName }.forEach { (packageName, entities) ->
                doInnerWork(
                    config,
                    entities,
                    metadata,
                    respectPlatformInstallPolicy,
                    packageName,
                    blacklist,
                    sharedUserIdBlacklist,
                    sharedUserIdExemption
                )
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
        val packageInstaller = resolvePackageInstallerBinder()

        // Prepare parameters for the direct AIDL call.
        val receiver = LocalIntentReceiver(reflect)
        val flags = config.uninstallFlags
        val versionedPackage = VersionedPackage(packageName, PackageManager.VERSION_CODE_HIGHEST)
        val callerPackageName = when (config.authorizer) {
            Authorizer.Dhizuku -> getDhizukuComponentName()
            else -> context.packageName
        }

        // Uninstall targets the current user by default. Profile target-user settings are for
        // installs and can otherwise make launcher-visible apps appear unchanged after uninstall.
        val currentUserId = AndroidProcess.myUid() / 100000

        Timber.d("Directly calling IPackageInstaller.uninstall with flags: $flags, userId: $currentUserId")

        // Directly call the method from the stub interface.
        packageInstaller.uninstall(
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
        metadata: InstallMetadata,
        respectPlatformInstallPolicy: Boolean,
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
            session = createSession(
                config,
                entities,
                metadata,
                respectPlatformInstallPolicy,
                packageInstaller,
                packageName
            )
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
        metadata: InstallMetadata,
        respectPlatformInstallPolicy: Boolean,
        packageInstaller: PackageInstaller,
        packageName: String
    ): Session {
        val pm = context.packageManager
        val sourceType = entities.first().sourceType
        val params = if (sourceType == DataType.MULTI_APK_ZIP || sourceType == DataType.MULTI_APK)
            PackageInstaller.SessionParams(
                // Always use full mode when apk is definite
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
        else
            PackageInstaller.SessionParams(
                when (entities.count { it.name == "base.apk" }) {
                    1 -> PackageInstaller.SessionParams.MODE_FULL_INSTALL
                    0 -> PackageInstaller.SessionParams.MODE_INHERIT_EXISTING
                    else -> throw IllegalArgumentException("Multiple base APK entries in a single install session")
                }
            )
        config.callingFromUid?.let { params.setOriginatingUid(it) }
        params.setAppPackageName(packageName)
        params.applySessionContext(
            config = config,
            metadata = metadata,
            entities = entities,
            installerPackageName = resolveInstallerPackageName(config),
            respectPlatformInstallPolicy = respectPlatformInstallPolicy
        )

        // --- InstallFlags Start ---
        Timber.tag(INSTALL_FLAGS_TAG).d("Initial install flags: ${params.installFlags}")
        Timber.tag(INSTALL_FLAGS_TAG).d("Install flags from config: ${config.installFlags}")
        // Start with the base flags from params and config
        var newFlags = params.installFlags.addFlag(config.installFlags)
        // Force-enable the 'ReplaceExisting' flag as a baseline
        newFlags = newFlags.addFlag(PackageManagerHidden.INSTALL_REPLACE_EXISTING)
        Timber.tag(INSTALL_FLAGS_TAG).d("After adding baseline flags: $newFlags")
        params.installFlags = newFlags
        Timber.tag(INSTALL_FLAGS_TAG).d("Install flags after customization: ${params.installFlags}")
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
        if ((sourceType == DataType.APK || sourceType == DataType.MULTI_APK || sourceType == DataType.MULTI_APK_ZIP) &&
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
            ?: throw IllegalStateException("Unable to open install entity input stream: ${entity.data}")
        val sizeBytes = entity.data.getSize()

        if (sizeBytes == 0L || sizeBytes < AssetFileDescriptor.UNKNOWN_LENGTH) {
            throw IllegalStateException("Invalid data size: $sizeBytes.")
        }
        session.openWrite(
            entity.name,
            0,
            sizeBytes
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

            val shouldDelete = config.shouldAutoDeleteSource(entities.firstOrNull()?.sourceType)

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

package com.rosan.installer.data.app.model.impl.appInstaller

import android.annotation.SuppressLint
import android.content.Context
import android.content.IIntentReceiver
import android.content.IIntentSender
import android.content.Intent
import android.content.IntentSender
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.Session
import android.content.pm.PackageManager
import android.content.pm.VersionedPackage
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.IInterface
import android.os.ServiceManager
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.installer.BuildConfig
import com.rosan.installer.build.model.entity.Architecture
import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.app.model.enums.DataType
import com.rosan.installer.data.app.model.exception.InstallFailedBlacklistedPackageException
import com.rosan.installer.data.app.repo.InstallerRepo
import com.rosan.installer.data.app.util.InstallOption
import com.rosan.installer.data.app.util.PackageInstallerUtil.abiOverride
import com.rosan.installer.data.app.util.PackageInstallerUtil.installFlags
import com.rosan.installer.data.app.util.PackageManagerUtil
import com.rosan.installer.data.app.util.sourcePath
import com.rosan.installer.data.common.util.isPackageArchivedCompat
import com.rosan.installer.data.recycle.model.impl.PrivilegedManager
import com.rosan.installer.data.recycle.util.requireDhizukuPermissionGranted
import com.rosan.installer.data.recycle.util.useUserService
import com.rosan.installer.data.reflect.repo.ReflectRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.util.OSUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import timber.log.Timber
import java.lang.reflect.Field
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

abstract class IBinderInstallerRepoImpl : InstallerRepo, KoinComponent {
    private val context by inject<Context>()
    private val reflect = get<ReflectRepo>()

    protected abstract suspend fun iBinderWrapper(iBinder: IBinder): IBinder

    private fun getField(targetClass: Class<*>, name: String, expectedType: Class<*>): Field? {
        // Try to find the field directly by name
        val directField = reflect.getDeclaredField(targetClass, name)?.apply {
            isAccessible = true
        }
        // If found and the type matches, return it
        if (directField != null && directField.type == expectedType) {
            return directField
        }
        // Otherwise, search for the first field that matches the expected type
        val matchedField = reflect.getDeclaredFields(targetClass)
            .firstOrNull { it.type == expectedType }
            ?.apply { isAccessible = true }
        // Return the found field or null if none matched
        return matchedField
    }

    private suspend fun getPackageInstaller(
        config: ConfigEntity, entities: List<InstallEntity>, extra: InstallExtraInfoEntity
    ): PackageInstaller {
        val iPackageManager =
            IPackageManager.Stub.asInterface(iBinderWrapper(ServiceManager.getService("package")))
        val iPackageInstaller =
            IPackageInstaller.Stub.asInterface(iBinderWrapper(iPackageManager.packageInstaller.asBinder()))

        val installerPackageName = when (config.authorizer) {
            ConfigEntity.Authorizer.Dhizuku -> getDhizukuComponentName()
            ConfigEntity.Authorizer.None -> if (OSUtils.isSystemApp) context.packageName else BuildConfig.APPLICATION_ID
            else -> config.installer ?: BuildConfig.APPLICATION_ID
        }

        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            reflect.getDeclaredConstructor(
                PackageInstaller::class.java,
                IPackageInstaller::class.java,
                String::class.java,
                String::class.java,
                Int::class.java,
            )!!.also {
                it.isAccessible = true
            }.newInstance(iPackageInstaller, installerPackageName, null, extra.userId)
        } else
            reflect.getDeclaredConstructor(
                PackageInstaller::class.java,
                IPackageInstaller::class.java,
                String::class.java,
                Int::class.java,
            )!!.also {
                it.isAccessible = true
            }.newInstance(
                iPackageInstaller,
                installerPackageName,
                extra.userId
            )) as PackageInstaller
    }

    private suspend fun getDhizukuComponentName(): String =
        requireDhizukuPermissionGranted {
            Dhizuku.getOwnerPackageName()
        }

    private suspend fun setSessionIBinder(session: Session) {
        val field = getField(session::class.java, "mSession", IPackageInstallerSession::class.java)
            ?: return
        val iBinder = (field.get(session) as IInterface).asBinder()
        field.set(
            session, IPackageInstallerSession.Stub.asInterface(iBinderWrapper(iBinder))
        )
    }

    override suspend fun approveSession(
        config: ConfigEntity,
        sessionId: Int,
        granted: Boolean
    ) {
        val iPackageManager =
            IPackageManager.Stub.asInterface(iBinderWrapper(ServiceManager.getService("package")))

        val iPackageInstaller =
            IPackageInstaller.Stub.asInterface(iBinderWrapper(iPackageManager.packageInstaller.asBinder()))

        try {
            Timber.d("Approving session $sessionId (granted: $granted) via Binder wrapper")

            reflect.getDeclaredMethod(
                IPackageInstaller::class.java,
                "setPermissionsResult",
                Int::class.java,
                Boolean::class.java
            )!!.invoke(iPackageInstaller, sessionId, granted)

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
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    ) {
        val result = kotlin.runCatching {
            entities.groupBy { it.packageName }.forEach { (packageName, entities) ->
                doInnerWork(config, entities, extra, packageName, blacklist, sharedUserIdBlacklist, sharedUserIdExemption)
            }
        }
        doFinishWork(config, entities, extra, result)
        result.onFailure {
            throw it
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun doUninstallWork(
        config: ConfigEntity,
        packageName: String,
        extra: InstallExtraInfoEntity,
    ) {
        // Get the underlying IPackageManager and IPackageInstaller interfaces.
        val iPackageManager =
            IPackageManager.Stub.asInterface(iBinderWrapper(ServiceManager.getService("package")))
        val iPackageInstaller =
            IPackageInstaller.Stub.asInterface(iBinderWrapper(iPackageManager.packageInstaller.asBinder()))

        // Prepare parameters for the direct AIDL call.
        val receiver = LocalIntentReceiver()
        val flags = config.uninstallFlags
        val versionedPackage = VersionedPackage(packageName, PackageManager.VERSION_CODE_HIGHEST)
        val callerPackageName = when (config.authorizer) {
            ConfigEntity.Authorizer.Dhizuku -> getDhizukuComponentName()
            else -> context.packageName
        }

        Timber.d("Directly calling IPackageInstaller.uninstall with flags: $flags")

        // Directly call the method from the stub interface.
        iPackageInstaller.uninstall(
            versionedPackage,
            callerPackageName,
            flags,
            receiver.getIntentSender(),
            extra.userId
        )

        // The result verification logic remains the same.
        PackageManagerUtil.uninstallResultVerify(context, receiver)
    }

    private suspend fun doInnerWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        packageName: String,
        managedBlacklistPackages: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    ) {
        if (!config.bypassBlacklistInstallSetByUser) {
            // Blacklisted package names
            if (managedBlacklistPackages.contains(packageName)) {
                Timber.w("Installation blocked for $packageName because it is in the blacklist.")
                throw InstallFailedBlacklistedPackageException("Installation blocked for $packageName because it is in the blacklist.")
            }

            // Blacklisted SharedUserID
            // Simply take the first entity's sharedUserId because they are all the same.
            val sharedUid = entities.firstOrNull()?.sharedUserId

            if (sharedUid != null) {
                if (sharedUserIdBlacklist.contains(sharedUid) && !sharedUserIdExemption.contains(packageName)) {
                    Timber.w("Installation blocked for $packageName because its sharedUserId '$sharedUid' is blacklisted.")
                    throw InstallFailedBlacklistedPackageException("Installation blocked for $packageName because its sharedUserId '$sharedUid' is blacklisted.")
                }
            }
        }

        if (entities.isEmpty()) return
        val packageInstaller = getPackageInstaller(config, entities, extra)
        var session: Session? = null
        try {
            session = createSession(config, entities, extra, packageInstaller, packageName)
            installIts(config, entities, extra, session)
            commit(config, entities, extra, session)
        } catch (e: Exception) {
            // 如果在提交之前或期间发生任何错误，应该放弃会话
            session?.abandon()
            // 将原始异常重新抛出，以便上层调用者可以处理（存疑）
            throw e
        } finally {
            // 无论成功与否，都应关闭会话以释放本地资源（存疑）
            session?.runCatching { close() }
        }
    }

    private suspend fun createSession(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        packageInstaller: PackageInstaller,
        packageName: String
    ): Session {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && config.authorizer != ConfigEntity.Authorizer.Dhizuku) {
            Timber.d("Setting packageSource to ${config.packageSource.name} (${config.packageSource.value})")
            if (config.enableCustomizePackageSource)
                params.setPackageSource(config.packageSource.value)
            else
                params.setPackageSource(ConfigEntity.PackageSource.UNSPECIFIED.value)
        }
        // --- PackageSource End ---

        // --- InstallFlags Start ---
        Timber.tag("InstallFlags").d("Initial install flags: ${params.installFlags}")
        Timber.tag("InstallFlags").d("Install flags from config: ${config.installFlags}")
        // Start with the base flags from params and config
        var newFlags = params.installFlags or config.installFlags
        // Force-enable the 'ReplaceExisting' flag as a baseline
        newFlags = newFlags or InstallOption.ReplaceExisting.value
        Timber.tag("InstallFlags").d("After adding baseline flags: $newFlags")
        // Conditionally add the 'UnArchive' flag
        if (context.packageManager.isPackageArchivedCompat(packageName)) {
            Timber.tag("InstallFlags").d("Package $packageName is archived, adding unarchive option.")
            newFlags = newFlags or InstallOption.UnArchive.value
        } else {
            Timber.tag("InstallFlags").d("Package $packageName is not archived.")
        }
        params.installFlags = newFlags
        Timber.tag("InstallFlags").d("Install flags after customization: ${params.installFlags}")
        // --- InstallFlags End ---

        // --- Disable Dhizuku not supported stuff ---
        if (config.authorizer == ConfigEntity.Authorizer.Dhizuku)
        // Dhizuku does not support GrantAllRequestedPermissions
            params.installFlags = params.installFlags and InstallOption.GrantAllRequestedPermissions.value.inv()
        // --- Dhizuku End ---

        // Android System will ignore INSTALL_ALLOW_DOWNGRADE for None ROOT/SYSTEM on Android 15+, no need to disable it here

        // --- Set abiOverride ---
        val baseApkArch = entities.firstOrNull { it.name == "base.apk" }?.arch
        Timber.d("Current Arch to install: $baseApkArch")

        if ((containerType == DataType.APK || containerType == DataType.MULTI_APK || containerType == DataType.MULTI_APK_ZIP) &&
            baseApkArch != null && baseApkArch != Architecture.NONE
        ) {
            val abiToOverride = if (baseApkArch != Architecture.UNKNOWN) {
                baseApkArch.arch
            } else {
                "armeabi-v7a" // Retain original fallback for UNKNOWN
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

    private fun installIts(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        session: Session
    ) {
        for (entity in entities) installIt(config, entity, extra, session)
    }

    private fun installIt(
        config: ConfigEntity, entity: InstallEntity, extra: InstallExtraInfoEntity, session: Session
    ) {
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
    private fun commit(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        session: Session
    ) {
        val receiver = LocalIntentReceiver()
        session.commit(receiver.getIntentSender())
        PackageManagerUtil.installResultVerify(context, receiver)
    }

    open suspend fun doFinishWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extraInfo: InstallExtraInfoEntity,
        result: Result<Unit>
    ) {
        Timber.tag("doFinishWork").d("isSuccess: ${result.isSuccess}")
        if (result.isSuccess) {
            Timber.tag("doFinishWork").d("isSuccess: ${result.isSuccess}")
            if (!result.isSuccess) return

            val packageName = entities.firstOrNull()?.packageName ?: return

            val isDeleteCapable = entities.firstOrNull()?.sourceType !in listOf(
                DataType.MULTI_APK_ZIP,
                DataType.MIXED_MODULE_APK,
                DataType.MIXED_MODULE_ZIP
            )

            val shouldDelete = config.autoDelete && isDeleteCapable

            PrivilegedManager.executePostInstallTasksAsync(
                authorizer = config.authorizer,
                customizeAuthorizer = config.customizeAuthorizer,
                config = PrivilegedManager.PostInstallTaskConfig(
                    packageName = packageName,
                    enableDexopt = config.enableManualDexopt,
                    dexoptMode = config.dexoptMode.value,
                    forceDexopt = config.forceDexopt,
                    enableAutoDelete = shouldDelete,
                    deletePaths = if (shouldDelete) entities.sourcePath() else emptyArray()
                )
            )
        }
    }

    protected open suspend fun onDeleteWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity
    ) {
        fun special() = null
        val authorizer = config.authorizer

        useUserService(
            authorizer = authorizer,
            special = if (authorizer == ConfigEntity.Authorizer.None
                || authorizer == ConfigEntity.Authorizer.Dhizuku
            ) ::special else null,
            useHookMode = false
        ) {
            it.privileged.delete(entities.sourcePath())
        }
    }

    class LocalIntentReceiver : KoinComponent {
        private val reflect = get<ReflectRepo>()

        private val queue = LinkedBlockingQueue<Intent>(1)

        private val localSender = object : IIntentSender.Stub() {
            // this api only work for upper Android O (8.0)
            // see this url:
            // Android N (7.1): http://aospxref.com/android-7.1.2_r39/xref/frameworks/base/core/java/android/content/IIntentSender.aidl
            // Android O (8.0): http://aospxref.com/android-8.0.0_r36/xref/frameworks/base/core/java/android/content/IIntentSender.aidl
            override fun send(
                code: Int,
                intent: Intent?,
                resolvedType: String?,
                whitelistToken: IBinder?,
                finishedReceiver: IIntentReceiver?,
                requiredPermission: String?,
                options: Bundle?
            ) {
                queue.offer(intent, 5, TimeUnit.SECONDS)
            }
        }

        fun getIntentSender(): IntentSender {
            return reflect.getDeclaredConstructor(
                IntentSender::class.java, IIntentSender::class.java
            )!!.also {
                it.isAccessible = true
            }.newInstance(localSender) as IntentSender
        }

        fun getResult(): Intent {
            return try {
                val result = queue.take()
                queue.remove(result)
                result
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }
    }
}
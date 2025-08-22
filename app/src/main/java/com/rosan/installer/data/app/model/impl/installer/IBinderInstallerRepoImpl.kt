package com.rosan.installer.data.app.model.impl.installer

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
import com.rosan.installer.build.Architecture
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.app.model.exception.InstallFailedBlacklistedPackageException
import com.rosan.installer.data.app.repo.InstallerRepo
import com.rosan.installer.data.app.util.InstallOption
import com.rosan.installer.data.app.util.PackageInstallerUtil.Companion.abiOverride
import com.rosan.installer.data.app.util.PackageInstallerUtil.Companion.installFlags
import com.rosan.installer.data.app.util.PackageManagerUtil
import com.rosan.installer.data.app.util.sourcePath
import com.rosan.installer.data.common.util.isPackageArchivedCompat
import com.rosan.installer.data.recycle.util.requireDhizukuPermissionGranted
import com.rosan.installer.data.recycle.util.useUserService
import com.rosan.installer.data.reflect.repo.ReflectRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    protected abstract suspend fun iBinderWrapper(iBinder: IBinder): IBinder

    private fun getField(any: Class<*>, name: String, clazz: Class<*>): Field? {
        var field = reflect.getDeclaredField(any, name)
        field?.isAccessible = true
        if (field?.type != clazz) {
            val fields = reflect.getDeclaredFields(any)
            for (item in fields) {
                if (item.type != clazz) continue
                field = item
                break
            }
        }
        field?.isAccessible = true
        return field
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
            ConfigEntity.Authorizer.None -> BuildConfig.APPLICATION_ID
            else -> config.installer
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

    private suspend fun getDhizukuComponentName(): String? =
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

    override suspend fun doInstallWork(
        config: ConfigEntity, entities: List<InstallEntity>, extra: InstallExtraInfoEntity, blacklist: List<String>
    ) {
        val result = kotlin.runCatching {
            entities.groupBy { it.packageName }.forEach { (packageName, entities) ->
                doInnerWork(config, entities, extra, packageName, blacklist)
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
        val callerPackageName = context.packageName

        Timber.d("Directly calling IPackageInstaller.uninstall with flags: $flags")

        // Directly call the method from the stub interface.
        iPackageInstaller.uninstall(
            versionedPackage,
            callerPackageName,
            flags,
            receiver.getIntentSender(),
            extra.userId
        )

        // Step 4: The result verification logic remains the same.
        PackageManagerUtil.uninstallResultVerify(context, receiver)
    }

    private suspend fun doInnerWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        packageName: String,
        managedBlacklistPackages: List<String>
    ) {
        if (managedBlacklistPackages.contains(packageName)) {
            Timber.w("Installation blocked for $packageName because it is in the blacklist.")
            throw InstallFailedBlacklistedPackageException("Installation blocked for $packageName because it is in the blacklist.")
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
            // runCatching 是 "quietly" close 的标准 Kotlin 写法
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
        val containerType = entities.first().containerType
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
        params.setAppPackageName(packageName)
        params.installFlags = config.installFlags or
                if (context.packageManager.isPackageArchivedCompat(packageName)) {
                    Timber.d("Package $packageName is archived, using unarchive option.")
                    // It's an unarchive operation. Set the specific flag.
                    InstallOption.UnArchive.value or InstallOption.ReplaceExisting.value
                } else {
                    // It's a new install or a regular update. Use replace existing.
                    Timber.d("Package $packageName is not archived, using replace existing option.")
                    InstallOption.ReplaceExisting.value
                }

        Timber.d(
            "Current Arch to install: ${entities.firstOrNull { it.name == "base.apk" }?.arch}, isUnknownArch: ${
                entities.firstOrNull { it.name == "base.apk" }?.arch == Architecture.UNKNOWN
            }"
        )

        if ((containerType == DataType.APK || containerType == DataType.MULTI_APK || containerType == DataType.MULTI_APK_ZIP) &&
            entities.firstOrNull { it.name == "base.apk" }?.arch == Architecture.UNKNOWN
        )
            params.abiOverride = "armeabi-v7a"

        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)
        setSessionIBinder(session)
        return session
    }

    private suspend fun installIts(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        session: Session
    ) {
        for (entity in entities) installIt(config, entity, extra, session)
    }

    private suspend fun installIt(
        config: ConfigEntity, entity: InstallEntity, extra: InstallExtraInfoEntity, session: Session
    ) {
        val inputStream = entity.data.getInputStreamWhileNotEmpty()
            ?: throw Exception("can't open input stream for this data: '${entity.data}'")
        session.openWrite(
            entity.name, 0,
            withContext(Dispatchers.IO) {
                inputStream.available()
            }.toUInt().toLong()
        ).use {
            inputStream.copyTo(it)
            session.fsync(it)
        }
    }

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
            coroutineScope.launch {
                runCatching { onExtraWork() }.exceptionOrNull()
                    ?.printStackTrace()
            }
        }
        // Never Delete Multi-APK-ZIP files automatically
        // Enable autoDelete only when the containerType is not MULTI_APK_ZIP_ZIP
        if (result.isSuccess && config.autoDelete && entities.first().containerType != DataType.MULTI_APK_ZIP) {
            Timber.tag("doFinishWork").d("autoDelete is enabled, do delete work")
            coroutineScope.launch {
                runCatching { onDeleteWork(config, entities, extraInfo) }.exceptionOrNull()
                    ?.printStackTrace()
            }
        }
    }

    protected open suspend fun onExtraWork() {
        // TODO Override this method to perform any extra work after installation
        Timber.tag("onExtraWork").d("No extra work defined.")
    }

    protected open suspend fun onDeleteWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity
    ) {
        fun special() = null
        val authorizer = config.authorizer
        useUserService(
            config, if (authorizer == ConfigEntity.Authorizer.None
                || authorizer == ConfigEntity.Authorizer.Dhizuku
            ) ::special
            else null
        ) {
            Timber.tag("onDeleteWork").d("onDeleteWork: ${entities.sourcePath()}")
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

            fun send(
                code: Int,
                intent: Intent?,
                resolvedType: String?,
                finishedReceiver: IIntentReceiver?,
                requiredPermission: String?,
                options: Bundle?
            ) {
                send(
                    code, intent, resolvedType, null, finishedReceiver, requiredPermission, options
                )
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
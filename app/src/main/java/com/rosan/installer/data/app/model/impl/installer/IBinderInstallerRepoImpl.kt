package com.rosan.installer.data.app.model.impl.installer

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
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import android.os.ServiceManager
import com.rosan.dhizuku.shared.DhizukuVariables
import com.rosan.installer.BuildConfig
import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraEntity
import com.rosan.installer.data.app.repo.InstallerRepo
import com.rosan.installer.data.app.util.InstallFlag
import com.rosan.installer.data.app.util.PackageInstallerUtil.Companion.installFlags
import com.rosan.installer.data.app.util.PackageManagerUtil
import com.rosan.installer.data.app.util.sourcePath
import com.rosan.installer.data.recycle.util.useUserService
import com.rosan.installer.data.reflect.repo.ReflectRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.internal.closeQuietly
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import java.lang.reflect.Field
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

abstract class IBinderInstallerRepoImpl : InstallerRepo, KoinComponent {
    private val context by inject<Context>()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val reflect = get<ReflectRepo>()

    protected abstract suspend fun iBinderWrapper(iBinder: IBinder): IBinder

    private suspend fun getFiled(any: Class<*>, name: String, clazz: Class<*>): Field? {
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
        config: ConfigEntity, entities: List<InstallEntity>, extra: InstallExtraEntity
    ): PackageInstaller {
        val iPackageManager =
            IPackageManager.Stub.asInterface(iBinderWrapper(ServiceManager.getService("package")))
        val iPackageInstaller =
            IPackageInstaller.Stub.asInterface(iBinderWrapper(iPackageManager.packageInstaller.asBinder()))

        val installerPackageName = when (config.authorizer) {
            ConfigEntity.Authorizer.Dhizuku -> DhizukuVariables.OFFICIAL_PACKAGE_NAME
            ConfigEntity.Authorizer.None -> BuildConfig.APPLICATION_ID
            else -> config.installer
        }

        return (
                reflect.getDeclaredConstructor(
                    PackageInstaller::class.java,
                    IPackageInstaller::class.java,
                    String::class.java,
                    String::class.java,
                    Int::class.java,
                )!!.also {
                    it.isAccessible = true
                }.newInstance(iPackageInstaller, installerPackageName, null, extra.userId)
                ) as PackageInstaller
    }

    private suspend fun setSessionIBinder(session: Session) {
        val field = getFiled(session::class.java, "mSession", IPackageInstallerSession::class.java)
            ?: return
        val iBinder = (field.get(session) as IInterface).asBinder()
        field.set(
            session, IPackageInstallerSession.Stub.asInterface(iBinderWrapper(iBinder))
        )
    }

    override suspend fun doWork(
        config: ConfigEntity, entities: List<InstallEntity>, extra: InstallExtraEntity
    ) {
        val result = kotlin.runCatching {
            entities.groupBy { it.packageName }.forEach { (packageName, entities) ->
                doInnerWork(config, entities, extra, packageName)
            }
        }
        doFinishWork(config, entities, extra, result)
        result.onFailure {
            throw it
        }
    }

    private suspend fun doInnerWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraEntity,
        packageName: String
    ) {
        if (entities.isEmpty()) return
        val packageInstaller = getPackageInstaller(config, entities, extra)
        var session: Session? = null
        try {
            session = createSession(config, entities, extra, packageInstaller, packageName)
            installIts(config, entities, extra, session)
            commit(config, entities, extra, session)
        } finally {
            session?.run {
                abandon()
                closeQuietly()
            }
        }
    }

    private suspend fun createSession(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraEntity,
        packageInstaller: PackageInstaller,
        packageName: String
    ): Session {
        val params = PackageInstaller.SessionParams(
            when (entities.count { it.name == "base.apk" }) {
                1 -> PackageInstaller.SessionParams.MODE_FULL_INSTALL
                0 -> PackageInstaller.SessionParams.MODE_INHERIT_EXISTING
                else -> throw Exception("can't install multiple package name in single session")
            }
        )
        params.setAppPackageName(packageName)
        params.installFlags = params.installFlags or InstallFlag.INSTALL_REPLACE_EXISTING.value

        if (config.allowTestOnly) params.installFlags =
            params.installFlags or InstallFlag.INSTALL_ALLOW_TEST.value

        if (config.allowDowngrade) {
            params.installFlags = params.installFlags or InstallFlag.INSTALL_REQUEST_DOWNGRADE.value
            params.installFlags = params.installFlags or InstallFlag.INSTALL_ALLOW_DOWNGRADE.value
        }

        if (config.forAllUser) params.installFlags =
            params.installFlags or InstallFlag.INSTALL_ALL_USERS.value
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)
        setSessionIBinder(session)
        return session
    }

    private suspend fun installIts(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraEntity,
        session: Session
    ) {
        for (entity in entities) installIt(config, entity, extra, session)
    }

    private suspend fun installIt(
        config: ConfigEntity, entity: InstallEntity, extra: InstallExtraEntity, session: Session
    ) {
        val inputStream = entity.data.getInputStreamWhileNotEmpty()
            ?: throw Exception("can't open input stream for this data: '${entity.data}'")
        session.openWrite(entity.name, 0, inputStream.available().toUInt().toLong()).use {
            inputStream.copyTo(it)
            session.fsync(it)
        }
    }

    private suspend fun commit(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraEntity,
        session: Session
    ) {
        val receiver = LocalIntentReceiver()
        session.commit(receiver.getIntentSender())
        PackageManagerUtil.installResultVerify(context, receiver)
    }

    open suspend fun doFinishWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraEntity,
        result: Result<Unit>
    ) {

        if (result.isSuccess && config.autoDelete) {
            coroutineScope.launch {
                kotlin.runCatching { onDeleteWork(config, entities, extra) }.exceptionOrNull()
                    ?.printStackTrace()
            }
        }
    }

    protected open suspend fun onDeleteWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraEntity
    ) {
        fun special() = null
        val authorizer = config.authorizer

        useUserService(
            config, if (authorizer == ConfigEntity.Authorizer.None
                || authorizer == ConfigEntity.Authorizer.Dhizuku
            ) ::special
            else null
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

            override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return super.onTransact(
                    code, data, reply, flags
                )
                val descriptor = "android.content.IIntentSender"
                return when (code) {
                    1 -> {
                        data.enforceInterface(descriptor)
                        send(
                            data.readInt(),
                            if (data.readInt() != 0) Intent.CREATOR.createFromParcel(data) else null,
                            data.readString(),
                            IIntentReceiver.Stub.asInterface(data.readStrongBinder()),
                            data.readString(),
                            if (data.readInt() != 0) Bundle.CREATOR.createFromParcel(data) else null
                        )
                        true
                    }

                    0x5F4E5446 -> {
                        reply?.writeString(descriptor)
                        true
                    }

                    else -> return super.onTransact(code, data, reply, flags)
                }
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
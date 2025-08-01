package com.rosan.installer.data.installer.model.impl.installer

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.Os
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.app.model.impl.AnalyserRepoImpl
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.installer.model.exception.ResolveException
import com.rosan.installer.data.installer.model.impl.InstallerRepoImpl
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.util.ConfigUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.util.UUID

class ActionHandler(scope: CoroutineScope, installer: InstallerRepo) :
    Handler(scope, installer), KoinComponent {
    override val installer: InstallerRepoImpl = super.installer as InstallerRepoImpl
    private var job: Job? = null
    private val context by inject<Context>()
    private val cacheParcelFileDescriptors = mutableListOf<ParcelFileDescriptor>()
    private val cacheDirectory = "${context.externalCacheDir?.absolutePath}/${installer.id}".apply {
        File(this).mkdirs()
    }

    override suspend fun onStart() {
        Timber.d("[id=${installer.id}] onStart: Starting to collect actions.")
        job = scope.launch {
            installer.action.collect { action ->
                Timber.d("[id=${installer.id}] Received action: ${action::class.simpleName}")
                when (action) {
                    is InstallerRepoImpl.Action.Resolve -> resolve(action.activity)
                    is InstallerRepoImpl.Action.Analyse -> analyse()
                    is InstallerRepoImpl.Action.Install -> install()
                    is InstallerRepoImpl.Action.Finish -> finish()
                }
            }
        }
    }

    override suspend fun onFinish() {
        Timber.d("[id=${installer.id}] onFinish: Cleaning up resources and cancelling job.")
        cacheParcelFileDescriptors.forEach { it.runCatching { close() } }
        cacheParcelFileDescriptors.clear()
        File(cacheDirectory).deleteRecursively()
        job?.cancel()
    }

    private suspend fun resolve(activity: Activity) {
        Timber.d("[id=${installer.id}] resolve: Starting new task.")

        // --- Reset all state fields here at the beginning ---
        installer.error = Throwable()
        installer.config = ConfigEntity.default
        installer.data = emptyList()
        installer.entities = emptyList()
        installer.progress.emit(ProgressEntity.Ready) // Also reset progress

        Timber.d("[id=${installer.id}] resolve: State has been reset. Emitting ProgressEntity.Resolving.")
        installer.progress.emit(ProgressEntity.Resolving)


        installer.config = try {
            resolveConfig(activity)
        } catch (e: Exception) {
            Timber.e(e, "[id=${installer.id}] resolve: Failed to resolve config.")
            installer.error = e
            installer.progress.emit(ProgressEntity.ResolvedFailed)
            return
        }
        Timber.d("[id=${installer.id}] resolve: Config resolved. installMode=${installer.config.installMode}")

        if (installer.config.installMode == ConfigEntity.InstallMode.Ignore) {
            Timber.d("[id=${installer.id}] resolve: InstallMode is Ignore. Finishing task.")
            installer.progress.emit(ProgressEntity.Finish)
            return
        }

        installer.data = try {
            resolveData(activity)
        } catch (e: Exception) {
            Timber.e(e, "[id=${installer.id}] resolve: Failed to resolve data.")
            installer.error = e
            installer.progress.emit(ProgressEntity.ResolvedFailed)
            return
        }
        Timber
            .d("[id=${installer.id}] resolve: Data resolved successfully (${installer.data.size} items). Emitting ProgressEntity.ResolveSuccess.")
        installer.progress.emit(ProgressEntity.ResolveSuccess)
    }

    private suspend fun analyse() {
        Timber.d("[id=${installer.id}] analyse: Starting. Emitting ProgressEntity.Analysing.")
        installer.progress.emit(ProgressEntity.Analysing)
        installer.entities = runCatching {
            analyseEntities(installer.data)
        }.getOrElse {
            Timber.e(it, "[id=${installer.id}] analyse: Failed.")
            installer.error = it
            installer.progress.emit(ProgressEntity.AnalysedFailed)
            return
        }.sortedWith(compareBy({ it.packageName }, {
            when (it) {
                is AppEntity.BaseEntity -> it.name
                is AppEntity.SplitEntity -> it.name
                is AppEntity.DexMetadataEntity -> it.name
                is AppEntity.CollectionEntity -> it.name
            }
        })).map { SelectInstallEntity(app = it, selected = true) }

        val isNotificationInstall = installer.config.installMode == ConfigEntity.InstallMode.Notification ||
                installer.config.installMode == ConfigEntity.InstallMode.AutoNotification

        val isMultiApkZip = installer.entities.firstOrNull()?.app?.containerType == DataType.MULTI_APK_ZIP
        val isMultiApk = installer.entities.firstOrNull()?.app?.containerType == DataType.MULTI_APK
        Timber.d("[id=${installer.id}] analyse: Analyse completed. isNotificationInstall=$isNotificationInstall, isMultiApkZip=$isMultiApkZip")
        if (isNotificationInstall && isMultiApkZip) {
            Timber.w("[id=${installer.id}] analyse: Multi-APK ZIP not supported in notification mode. Emitting AnalysedUnsupported.")
            installer.progress.emit(
                ProgressEntity.AnalysedUnsupported(context.getString(R.string.installer_current_install_mode_not_supported))
            )
        } else if (isNotificationInstall && isMultiApk) {
            Timber.w("[id=${installer.id}] analyse: Multi-APK not supported in notification mode. Emitting AnalysedUnsupported.")
            installer.progress.emit(
                ProgressEntity.AnalysedUnsupported(context.getString(R.string.installer_current_install_mode_not_supported))
            )
        } else {
            Timber.d("[id=${installer.id}] analyse: Emitting ProgressEntity.AnalysedSuccess.")
            installer.progress.emit(ProgressEntity.AnalysedSuccess)
        }
    }

    private suspend fun install() {
        Timber.d("[id=${installer.id}] install: Starting. Emitting ProgressEntity.Installing.")
        installer.progress.emit(ProgressEntity.Installing)
        runCatching {
            installEntities(installer.config, installer.entities.filter { it.selected }.map {
                InstallEntity(
                    name = it.app.name,
                    packageName = it.app.packageName,
                    data = when (val app = it.app) {
                        is AppEntity.BaseEntity -> app.data
                        is AppEntity.SplitEntity -> app.data
                        is AppEntity.DexMetadataEntity -> app.data
                        is AppEntity.CollectionEntity -> app.data
                    },
                    containerType = it.app.containerType!!
                )
            }, InstallExtraInfoEntity(Os.getuid() / 100000, cacheDirectory))
        }.getOrElse {
            Timber.e(it, "[id=${installer.id}] install: Failed.")
            installer.error = it
            installer.progress.emit(ProgressEntity.InstallFailed)
            return
        }
        Timber.d("[id=${installer.id}] install: Succeeded. Emitting ProgressEntity.InstallSuccess.")
        installer.progress.emit(ProgressEntity.InstallSuccess)
    }

    private suspend fun finish() {
        Timber.d("[id=${installer.id}] finish: Emitting ProgressEntity.Finish.")
        installer.progress.emit(ProgressEntity.Finish)
    }

    private suspend fun resolveConfig(activity: Activity): ConfigEntity {
        val packageName = activity.callingPackage
            ?: (activity.referrer?.host)
        var config = ConfigUtil.getByPackageName(packageName)
        if (config.installer == null) config = config.copy(
            installer = packageName
        )
        return config
    }

    private fun resolveData(activity: Activity): List<DataEntity> {
        val uris = resolveDataUris(activity)
        val data = mutableListOf<DataEntity>()
        uris.forEach {
            data.addAll(resolveDataUri(activity, it))
        }
        return data
    }

    private fun resolveDataUris(activity: Activity): List<Uri> {
        val intent = activity.intent ?: throw ResolveException(
            action = null, uris = emptyList()
        )
        val intentAction = intent.action ?: throw ResolveException(
            action = null, uris = emptyList()
        )

        val uris = when (intentAction) {
            Intent.ACTION_SEND -> {
                val uri =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        intent.getParcelableExtra(
                            Intent.EXTRA_STREAM, Uri::class.java
                        )
                    else intent.getParcelableExtra(Intent.EXTRA_STREAM)
                if (uri == null) emptyList() else listOf(uri)
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    intent.getParcelableArrayListExtra(
                        Intent.EXTRA_STREAM, Uri::class.java
                    )
                else intent.getParcelableArrayListExtra(
                    Intent.EXTRA_STREAM
                )) ?: emptyList()
            }

            else -> {
                val uri = intent.data
                if (uri == null) emptyList()
                else listOf(uri)
            }
        }

        if (uris.isEmpty()) throw ResolveException(
            action = intentAction, uris = uris
        )
        return uris
    }

    private fun resolveDataUri(activity: Activity, uri: Uri): List<DataEntity> {
        Timber.d("Source URI: $uri")
        if (uri.scheme == ContentResolver.SCHEME_FILE) return resolveDataFileUri(activity, uri)
        return resolveDataContentFile(activity, uri)
    }

    private fun resolveDataFileUri(activity: Activity, uri: Uri): List<DataEntity> {
        Timber.d("uri:$uri")
        val path = uri.path ?: throw Exception("can't get uri path: $uri")
        val data = DataEntity.FileEntity(path)
        data.source = DataEntity.FileEntity(path)
        return listOf(data)
    }

    private fun resolveDataContentFile(
        activity: Activity,
        uri: Uri,
        retry: Int = 3
    ): List<DataEntity> {
        // wait for PermissionRecords ok.
        // if not, maybe show Uri Read Permission Denied
        if (activity.checkCallingOrSelfUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED &&
            retry > 0
        ) {
            Thread.sleep(50)
            return resolveDataContentFile(activity, uri, retry - 1)
        }
        val assetFileDescriptor = context.contentResolver?.openAssetFileDescriptor(uri, "r")
            ?: throw Exception("can't open file descriptor: $uri")
        val parcelFileDescriptor = assetFileDescriptor.parcelFileDescriptor
        val pid = Os.getpid()
        val descriptor = parcelFileDescriptor.fd
        val path = "/proc/$pid/fd/$descriptor"

        // only full file, can't handle a sub-section of a file
        if (assetFileDescriptor.declaredLength < 0) {

            // file descriptor can't be pipe or socket
            val source = Os.readlink(path)
            if (source.startsWith('/')) {
                cacheParcelFileDescriptors.add(parcelFileDescriptor)
                val file = File(path)
                val data = if (file.exists() && file.canRead() && runCatching {
                        file.inputStream().use { }
                        return@runCatching true
                    }.getOrDefault(false)) DataEntity.FileEntity(path)
                else DataEntity.FileDescriptorEntity(pid, descriptor)
                data.source = DataEntity.FileEntity(source)
                return listOf(data)
            }
        }

        // cache it
        val tempFile = File.createTempFile(UUID.randomUUID().toString(), null, File(cacheDirectory))
        tempFile.outputStream().use { output ->
            assetFileDescriptor.use {
                it.createInputStream().copyTo(output)
            }
        }
        return listOf(DataEntity.FileEntity(tempFile.absolutePath))
    }

    private suspend fun analyseEntities(data: List<DataEntity>): List<AppEntity> =
        AnalyserRepoImpl.doWork(installer.config, data, AnalyseExtraEntity(cacheDirectory))

    private suspend fun installEntities(
        config: ConfigEntity, entities: List<InstallEntity>, extra: InstallExtraInfoEntity
    ) = com.rosan.installer.data.app.model.impl.InstallerRepoImpl.doWork(config, entities, extra)
}
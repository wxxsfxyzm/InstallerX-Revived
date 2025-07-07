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
import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
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
import okhttp3.internal.closeQuietly
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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
        job = scope.launch {
            installer.action.collect {
                // 异步处理请求
                launch {
                    when (it) {
                        is InstallerRepoImpl.Action.Resolve -> resolve(it.activity)
                        is InstallerRepoImpl.Action.Analyse -> analyse()
                        is InstallerRepoImpl.Action.Install -> install()
                        is InstallerRepoImpl.Action.Finish -> finish()
                    }
                }
            }
        }
    }

    override suspend fun onFinish() {
        cacheParcelFileDescriptors.forEach {
            it.closeQuietly()
        }
        cacheParcelFileDescriptors.clear()
        File(cacheDirectory).deleteRecursively()
        job?.cancel()
    }

    private suspend fun resolve(activity: Activity) {
        // resolve 函数的核心作用是解析文件/URI并填充 installer.data。
        // 如果 data 字段已经有内容，说明解析已经成功完成过一次，
        // 这是一个多余的调用，应当直接忽略，以防止后续流程出错。
        if (installer.data.isNotEmpty()) {
            return
        }

        installer.progress.emit(ProgressEntity.Resolving)

        // 直接开始执行核心的解析逻辑
        installer.config = try {
            resolveConfig(activity)
        } catch (e: Exception) {
            installer.error = e
            installer.progress.emit(ProgressEntity.ResolvedFailed)
            return
        }

        if (installer.config.installMode == ConfigEntity.InstallMode.Ignore) {
            installer.progress.emit(ProgressEntity.Finish)
            return
        }
        installer.data = try {
            resolveData(activity)
        } catch (e: Exception) {
            installer.error = e
            installer.progress.emit(ProgressEntity.ResolvedFailed)
            return
        }
        installer.progress.emit(ProgressEntity.ResolveSuccess)
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

    /*    private suspend fun requestNotificationPermission(activity: Activity) {
            // Determine the required permissions based on the Android version
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyList() // No runtime permission needed for Android versions below 13
            }

            // If the permission list is empty, no runtime request is needed.
            // This is the case for Android versions below 13.
            if (permissions.isEmpty()) {
                return // Successfully return as no permission is required
            }
            // If permissions are required (Android 13+), proceed with the request.
            callbackFlow {
                if (XXPermissions.isGranted(activity, permissions)) {
                    trySend(true) // Permission already granted
                    close()
                } else {
                    XXPermissions.with(activity)
                        .permission(permissions)
                        .request { _, allGranted ->
                            trySend(allGranted) // Send the result of the request
                            close()
                        }
                }
                awaitClose { }
            }.first()
        }*/

    private suspend fun resolveData(activity: Activity): List<DataEntity> {
        // requestStoragePermissions(activity)
        val uris = resolveDataUris(activity)
        val data = mutableListOf<DataEntity>()
        uris.forEach {
            data.addAll(resolveDataUri(activity, it))
        }
        return data
    }

    /*    private suspend fun requestStoragePermissions(activity: Activity) {
            callbackFlow<Any?> {
                val permissions = listOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                if (XXPermissions.isGranted(activity, permissions)) {
                    send(null)
                } else {
                    XXPermissions.with(activity).permission(permissions).request { _, all ->
                        if (all) trySend(null)
                        else close()
                    }
                }
                awaitClose { }
            }.first()
        }*/

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
        if (uri.scheme == ContentResolver.SCHEME_FILE) return resolveDataFileUri(activity, uri)
        return resolveDataContentFile(activity, uri)
    }

    private fun resolveDataFileUri(activity: Activity, uri: Uri): List<DataEntity> {
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
        val assetFileDescriptor = activity.contentResolver?.openAssetFileDescriptor(uri, "r")
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
                val data = if (file.exists() && file.canRead() && kotlin.runCatching {
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

    private suspend fun analyse() {
        installer.progress.emit(ProgressEntity.Analysing)
        installer.entities = kotlin.runCatching {
            analyseEntities(installer.data)
        }.getOrElse {
            installer.error = it
            installer.progress.emit(ProgressEntity.AnalysedFailed)
            return
        }.sortedWith(compareBy({
            it.packageName
        }, {

            when (it) {
                is AppEntity.BaseEntity -> it.name
                is AppEntity.SplitEntity -> it.name
                is AppEntity.DexMetadataEntity -> it.name
                is AppEntity.CollectionEntity -> it.name
            }
        })).map {
            SelectInstallEntity(
                app = it, selected = true
            )
        }
        installer.progress.emit(ProgressEntity.AnalysedSuccess)
    }

    private suspend fun analyseEntities(data: List<DataEntity>): List<AppEntity> =
        AnalyserRepoImpl.doWork(installer.config, data, AnalyseExtraEntity(cacheDirectory))

    private suspend fun install() {
        installer.progress.emit(ProgressEntity.Installing)
        kotlin.runCatching {
            installEntities(installer.config, installer.entities.filter { it.selected }.map {
                InstallEntity(
                    name = it.app.name,
                    packageName = it.app.packageName,
                    data = when (val app = it.app) {
                        is AppEntity.BaseEntity -> app.data
                        is AppEntity.SplitEntity -> app.data
                        is AppEntity.DexMetadataEntity -> app.data
                        is AppEntity.CollectionEntity -> app.data
                    }
                )
            }, InstallExtraInfoEntity(Os.getuid() / 100000, cacheDirectory))
        }.getOrElse {
            installer.error = it
            installer.progress.emit(ProgressEntity.InstallFailed)
            return
        }
        installer.progress.emit(ProgressEntity.InstallSuccess)
    }

    private suspend fun installEntities(
        config: ConfigEntity, entities: List<InstallEntity>, extra: InstallExtraInfoEntity
    ) = com.rosan.installer.data.app.model.impl.InstallerRepoImpl.doWork(config, entities, extra)

    private suspend fun finish() = installer.progress.emit(ProgressEntity.Finish)
}
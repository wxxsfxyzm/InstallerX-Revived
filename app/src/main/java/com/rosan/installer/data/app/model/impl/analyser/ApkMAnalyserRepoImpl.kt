package com.rosan.installer.data.app.model.impl.analyser

import android.graphics.drawable.Drawable
import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.repo.AnalyserRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object ApkMAnalyserRepoImpl : AnalyserRepo, KoinComponent {
    private val json = get<Json>()

    override suspend fun doWork(
        config: ConfigEntity,
        data: List<DataEntity>,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val apps = mutableListOf<AppEntity>()
        data.forEach { apps.addAll(doWork(config, it, extra)) }
        return apps
    }

    private fun doWork(
        config: ConfigEntity,
        data: DataEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        return when (data) {
            is DataEntity.FileEntity -> doFileWork(config, data, extra)
            is DataEntity.FileDescriptorEntity -> doFileDescriptorWork(config, data, extra)
            is DataEntity.ZipFileEntity -> doZipFileWork(config, data, extra)
            is DataEntity.ZipInputStreamEntity -> doZipInputStreamWork(config, data, extra)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun doFileWork(
        config: ConfigEntity,
        data: DataEntity.FileEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val apps = mutableListOf<AppEntity>()
        ZipFile(data.path).use { zip ->
            val manifest =
                json.decodeFromStream<Manifest>(zip.getInputStream(zip.getEntry("info.json")))
            val icon = zip.getEntry("icon.png")?.let {
                Drawable.createFromStream(zip.getInputStream(it), it.name)
            }
            zip.entries().iterator().forEach {
                if (it.isDirectory) return@forEach
                apps.addAll(
                    doSingleWork(
                        config,
                        manifest,
                        icon,
                        it.name,
                        DataEntity.ZipFileEntity(it.name, DataEntity.FileEntity(data.path)),
                        extra
                    )
                )
            }
        }
        return apps
    }

    private fun doFileDescriptorWork(
        config: ConfigEntity,
        data: DataEntity.FileDescriptorEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> = doZipInputStreamWork(config, data, ZipInputStream(data.getInputStream()), extra)

    private fun doZipFileWork(
        config: ConfigEntity,
        data: DataEntity.ZipFileEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> =
        doZipInputStreamWork(config, data, ZipInputStream(data.getInputStream()), extra)

    private fun doZipInputStreamWork(
        config: ConfigEntity,
        data: DataEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> =
        doZipInputStreamWork(config, data, ZipInputStream(data.getInputStream()), extra)

    @OptIn(ExperimentalSerializationApi::class)
    private fun doZipInputStreamWork(
        config: ConfigEntity,
        data: DataEntity,
        zip: ZipInputStream,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val apps = mutableListOf<AppEntity>()
        val names = mutableListOf<String>()
        var manifestOrNull: Manifest? = null
        var icon: Drawable? = null
        zip.use { inputStream ->
            while (true) {
                val entry = inputStream.nextEntry ?: break
                if (entry.isDirectory) continue
                val file = File(entry.name)
                when {
                    file.extension == "apk" -> names.add(entry.name)
                    file.extension == "dm" -> names.add(entry.name)
                    entry.name == "info.json" -> manifestOrNull =
                        json.decodeFromStream(inputStream)

                    entry.name == "icon.png" -> icon =
                        Drawable.createFromStream(inputStream, entry.name)
                }
            }
        }
        if (names.isEmpty()) return apps
        if (manifestOrNull == null) return apps
        val manifest = manifestOrNull
        names.forEach {
            apps.addAll(
                doSingleWork(
                    config,
                    manifest,
                    icon,
                    it,
                    DataEntity.ZipInputStreamEntity(it, data),
                    extra
                )
            )
        }
        return apps
    }

    private fun doSingleWork(
        config: ConfigEntity,
        manifest: Manifest,
        icon: Drawable?,
        name: String,
        data: DataEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        var dmName: String? = null
        var splitName: String? = null
        when (File(name).extension) {
            "apk" -> {
                val name = File(name).nameWithoutExtension
                splitName = if (name == "base") null
                else name
            }

            "dm" -> dmName = File(name).nameWithoutExtension
            else -> return listOf()
        }
        // APKM格式仅能获取到 MIN_API
        val minSdk = manifest.minApi!!
        val targetSdk = null
        val app = if (splitName?.isNotEmpty() == true) AppEntity.SplitEntity(
            packageName = manifest.packageName,
            data = data,
            splitName = splitName,
            targetSdk = targetSdk,
            minSdk = minSdk,
            containerType = extra.dataType
        ) else if (dmName?.isNotEmpty() == true) AppEntity.DexMetadataEntity(
            packageName = manifest.packageName,
            data = data,
            dmName = dmName,
            targetSdk = targetSdk,
            minSdk = minSdk,
            containerType = extra.dataType
        ) else AppEntity.BaseEntity(
            packageName = manifest.packageName,
            data = data,
            versionCode = manifest.versionCode,
            versionName = manifest.versionName,
            label = manifest.label,
            icon = icon,
            targetSdk = targetSdk,
            minSdk = minSdk,
            containerType = extra.dataType
        )
        return listOf(app)
    }

    @Serializable
    private data class Manifest(
        @SerialName("pname")
        val packageName: String,
        @SerialName("versioncode")
        private val versionCodeStr: String,
        @SerialName("release_version")
        val releaseVersion: String?,
        @SerialName("app_name")
        val appName: String?,
        @SerialName("apk_title")
        val apkTitle: String?,
        @SerialName("release_title")
        val releaseTitle: String?,
        @SerialName("min_api")
        val minApi: String? = null,
    ) {
        val versionCode: Long = versionCodeStr.toLong()
        val versionName: String = releaseVersion ?: ""
        val label: String? = appName ?: apkTitle ?: releaseTitle
    }
}
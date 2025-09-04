package com.rosan.installer.data.app.model.impl.analyser

import android.graphics.drawable.Drawable
import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.repo.FileAnalyserRepo
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

object ApkMAnalyserRepoImpl : FileAnalyserRepo, KoinComponent {
    private val json = get<Json>()

    override suspend fun doWork(
        config: ConfigEntity,
        data: List<DataEntity>,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val apps = mutableListOf<AppEntity>()
        // The upstream handler now ensures all data entities are FileEntity.
        data.forEach { entity ->
            val fileEntity = entity as? DataEntity.FileEntity
                ?: throw IllegalArgumentException("ApkMAnalyserRepoImpl expected a FileEntity.")
            apps.addAll(doFileWork(config, fileEntity, extra))
        }
        return apps
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun doFileWork(
        config: ConfigEntity,
        data: DataEntity.FileEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val apps = mutableListOf<AppEntity>()
        // Use the efficient ZipFile API.
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
                        // Create a ZipFileEntity to represent the entry within the file.
                        DataEntity.ZipFileEntity(it.name, data),
                        extra
                    )
                )
            }
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
        // APKM does not provide targetSdk information.
        val minSdk = manifest.minApi!!
        val targetSdk = null
        val app = if (splitName?.isNotEmpty() == true) AppEntity.SplitEntity(
            packageName = manifest.packageName,
            data = data,
            splitName = splitName,
            targetSdk = targetSdk,
            minSdk = minSdk,
            arch = null,
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
            sharedUserId = null,
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
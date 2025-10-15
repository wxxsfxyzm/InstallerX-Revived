package com.rosan.installer.data.app.model.impl.analyser

import android.graphics.drawable.Drawable
import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.repo.FileAnalyserRepo
import com.rosan.installer.data.app.util.FlexibleXapkVersionCodeSerializer
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

object XApkAnalyserRepoImpl : FileAnalyserRepo, KoinComponent {
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
                ?: throw IllegalArgumentException("XApkAnalyserRepoImpl expected a FileEntity.")
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
                json.decodeFromStream<Manifest>(zip.getInputStream(zip.getEntry("manifest.json")))
            val icon = zip.getEntry("icon.png")?.let {
                Drawable.createFromStream(zip.getInputStream(it), it.name)
            }
            manifest.splits.forEach {
                apps.addAll(
                    doSingleWork(
                        config,
                        manifest,
                        icon,
                        it,
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
        split: Manifest.Split,
        data: DataEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        var dmName: String? = null
        var splitName: String? = null
        when (File(split.name).extension) {
            "apk" -> {
                splitName = if (split.splitName == "base") null
                else split.splitName
            }

            "dm" -> dmName = File(split.name).nameWithoutExtension
            else -> return listOf()
        }
        val app = if (splitName?.isNotEmpty() == true) AppEntity.SplitEntity(
            packageName = manifest.packageName,
            data = data,
            splitName = splitName,
            targetSdk = manifest.targetSdk,
            minSdk = manifest.minSdk,
            arch = null,
            containerType = extra.dataType
        ) else if (dmName?.isNotEmpty() == true) AppEntity.DexMetadataEntity(
            packageName = manifest.packageName,
            data = data,
            dmName = dmName,
            targetSdk = manifest.targetSdk,
            minSdk = manifest.minSdk,
            containerType = extra.dataType
        ) else AppEntity.BaseEntity(
            packageName = manifest.packageName,
            sharedUserId = null,
            data = data,
            versionCode = manifest.versionCode,
            versionName = manifest.versionName,
            label = manifest.label,
            icon = icon,
            targetSdk = manifest.targetSdk,
            minSdk = manifest.minSdk,
            containerType = extra.dataType
        )
        return listOf(app)
    }

    @Serializable
    private data class Manifest(
        @SerialName("package_name") val packageName: String,
        @SerialName("version_code") @Serializable(with = FlexibleXapkVersionCodeSerializer::class) val versionCodeStr: String,
        @SerialName("version_name") val versionNameStr: String?,
        @SerialName("name") val label: String?,
        @SerialName("split_apks") val splits: List<Split>,
        @SerialName("min_sdk_version") val minSdk: String? = null,
        @SerialName("target_sdk_version") val targetSdk: String? = null,
    ) {
        val versionCode: Long = versionCodeStr.toLong()
        val versionName: String = versionNameStr ?: ""

        @Serializable
        data class Split(
            @SerialName("file") val name: String, @SerialName("id") val splitName: String
        )
    }
}
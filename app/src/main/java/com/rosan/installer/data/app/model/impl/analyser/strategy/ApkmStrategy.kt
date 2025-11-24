package com.rosan.installer.data.app.model.impl.analyser.strategy

import android.graphics.drawable.Drawable
import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.repo.AnalysisStrategy
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.zip.ZipFile

object ApkmStrategy : AnalysisStrategy, KoinComponent {
    private val json by inject<Json>()

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun analyze(
        config: ConfigEntity,
        data: DataEntity,
        zipFile: ZipFile?,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        requireNotNull(zipFile)
        require(data is DataEntity.FileEntity)

        // 1. Parse Info
        val infoEntry = zipFile.getEntry("info.json") ?: return emptyList()
        val manifest = zipFile.getInputStream(infoEntry).use {
            json.decodeFromStream<Manifest>(it)
        }

        // 2. Load Icon
        val icon = zipFile.getEntry("icon.png")?.let {
            Drawable.createFromStream(zipFile.getInputStream(it), it.name)
        }

        // 3. Iterate all entries (ApkM usually just lists files flatly)
        return zipFile.entries().asSequence()
            .filter { !it.isDirectory }
            .flatMap { entry ->
                val entryName = entry.name
                val entryData = DataEntity.ZipFileEntity(entryName, data)
                val file = File(entryName)

                when (file.extension) {
                    "apk" -> {
                        val nameWithoutExt = file.nameWithoutExtension
                        val splitName = if (nameWithoutExt == "base") null else nameWithoutExt

                        val entity = if (!splitName.isNullOrEmpty()) {
                            AppEntity.SplitEntity(
                                packageName = manifest.packageName,
                                data = entryData,
                                splitName = splitName,
                                targetSdk = null, // APKM doesn't provide targetSdk in json
                                minSdk = manifest.minApi,
                                arch = null,
                                sourceType = extra.dataType
                            )
                        } else {
                            AppEntity.BaseEntity(
                                packageName = manifest.packageName,
                                sharedUserId = null,
                                data = entryData,
                                versionCode = manifest.versionCode,
                                versionName = manifest.versionName,
                                label = manifest.label,
                                icon = icon,
                                targetSdk = null,
                                minSdk = manifest.minApi,
                                sourceType = extra.dataType
                            )
                        }
                        sequenceOf(entity)
                    }

                    "dm" -> {
                        val dmName = file.nameWithoutExtension
                        if (dmName.isNotEmpty()) {
                            sequenceOf(
                                AppEntity.DexMetadataEntity(
                                    packageName = manifest.packageName,
                                    data = entryData,
                                    dmName = dmName,
                                    targetSdk = null,
                                    minSdk = manifest.minApi,
                                    sourceType = extra.dataType
                                )
                            )
                        } else emptySequence()
                    }

                    else -> emptySequence()
                }
            }.toList()
    }

    @Serializable
    private data class Manifest(
        @SerialName("pname") val packageName: String,
        @SerialName("versioncode") private val versionCodeStr: String,
        @SerialName("release_version") val releaseVersion: String?,
        @SerialName("app_name") val appName: String?,
        @SerialName("apk_title") val apkTitle: String?,
        @SerialName("release_title") val releaseTitle: String?,
        @SerialName("min_api") val minApi: String? = null,
    ) {
        val versionCode: Long = versionCodeStr.toLong()
        val versionName: String = releaseVersion ?: ""
        val label: String? = appName ?: apkTitle ?: releaseTitle
    }
}
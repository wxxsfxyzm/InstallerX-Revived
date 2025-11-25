package com.rosan.installer.data.app.model.impl.analyser.strategy

import android.graphics.drawable.Drawable
import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.repo.AnalysisStrategy
import com.rosan.installer.data.app.util.FlexibleXapkVersionCodeSerializer
import com.rosan.installer.data.app.util.parseSplitMetadata
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

object XApkStrategy : AnalysisStrategy, KoinComponent {
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

        // 1. Parse Manifest
        val manifestEntry = zipFile.getEntry("manifest.json") ?: return emptyList()
        val manifest = zipFile.getInputStream(manifestEntry).use {
            json.decodeFromStream<Manifest>(it)
        }

        // 2. Load Icon (if available)
        val icon = zipFile.getEntry("icon.png")?.let {
            Drawable.createFromStream(zipFile.getInputStream(it), it.name)
        }

        // 3. Map Splits to Entities
        return manifest.splits.flatMap { split ->
            val entryName = split.name
            // Construct the virtual data entity for the entry
            val entryData = DataEntity.ZipFileEntity(entryName, data)

            val file = File(entryName)
            when (file.extension) {
                "apk" -> {
                    val splitName = if (split.splitName == "base") null else split.splitName

                    val entity = if (!splitName.isNullOrEmpty()) {
                        val metadata = splitName.parseSplitMetadata()

                        AppEntity.SplitEntity(
                            packageName = manifest.packageName,
                            data = entryData,
                            splitName = splitName,
                            targetSdk = manifest.targetSdk,
                            minSdk = manifest.minSdk,
                            arch = null,
                            sourceType = extra.dataType,
                            type = metadata.type,
                            filterType = metadata.filterType,
                            configValue = metadata.configValue
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
                            targetSdk = manifest.targetSdk,
                            minSdk = manifest.minSdk,
                            sourceType = extra.dataType
                        )
                    }
                    listOf(entity)
                }

                "dm" -> {
                    val dmName = file.nameWithoutExtension
                    if (dmName.isNotEmpty()) {
                        listOf(
                            AppEntity.DexMetadataEntity(
                                packageName = manifest.packageName,
                                data = entryData,
                                dmName = dmName,
                                targetSdk = manifest.targetSdk,
                                minSdk = manifest.minSdk,
                                sourceType = extra.dataType
                            )
                        )
                    } else emptyList()
                }

                else -> emptyList()
            }
        }
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
            @SerialName("file") val name: String,
            @SerialName("id") val splitName: String
        )
    }
}
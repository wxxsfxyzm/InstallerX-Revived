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

object XApkAnalyserRepoImpl : AnalyserRepo, KoinComponent {
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

    /* private fun doWork(config: ConfigEntity, data: DataEntity, extra: AnalyseExtraEntity): List<AppEntity> {
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
                         DataEntity.ZipFileEntity(it.name, data),
                         extra
                     )
                 )
             }
         }
         return apps
     }

     private fun doFileDescriptorWork(
         config: ConfigEntity, data: DataEntity.FileDescriptorEntity, extra: AnalyseExtraEntity
     ): List<AppEntity> = doZipInputStreamWork(config, data, ZipInputStream(data.getInputStream()), extra)

     private fun doZipFileWork(
         config: ConfigEntity, data: DataEntity.ZipFileEntity, extra: AnalyseExtraEntity
     ): List<AppEntity> = doZipInputStreamWork(config, data, ZipInputStream(data.getInputStream()), extra)

     private fun doZipInputStreamWork(
         config: ConfigEntity, data: DataEntity.ZipInputStreamEntity, extra: AnalyseExtraEntity
     ): List<AppEntity> = doZipInputStreamWork(config, data, ZipInputStream(data.getInputStream()), extra)

     @OptIn(ExperimentalSerializationApi::class)
     private fun doZipInputStreamWork(
         config: ConfigEntity,
         data: DataEntity,
         zip: ZipInputStream,
         extra: AnalyseExtraEntity
     ): List<AppEntity> {
         val apps = mutableListOf<AppEntity>()
         var manifestOrNull: Manifest? = null
         var icon: Drawable? = null
         zip.use { inputStream ->
             while (true) {
                 val entry = inputStream.nextEntry ?: break
                 if (entry.isDirectory) continue
                 val file = File(entry.name)
                 when (entry.name) {
                     "manifest.json" -> manifestOrNull =
                         json.decodeFromStream(inputStream)

                     "icon.png" -> icon =
                         Drawable.createFromStream(inputStream, entry.name)
                 }
             }
         }
         if (manifestOrNull == null) return apps
         val manifest = manifestOrNull
         manifest.splits.forEach {
             apps.addAll(
                 doSingleWork(
                     config,
                     manifest,
                     icon,
                     it,
                     DataEntity.ZipInputStreamEntity(it.name, data),
                     extra
                 )
             )
         }
         return apps
     }*/

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
        @SerialName("version_code") val versionCodeStr: String,
        @SerialName("version_name") val versionNameStr: String?,
        @SerialName("name") val label: String?,
        @SerialName("split_apks") val splits: List<Split>,
        @SerialName("min_sdk_version") val minSdk: String?,
        @SerialName("target_sdk_version") val targetSdk: String?,
    ) {
        val versionCode: Long = versionCodeStr.toLong()
        val versionName: String = versionNameStr ?: ""

        @Serializable
        data class Split(
            @SerialName("file") val name: String, @SerialName("id") val splitName: String
        )
    }
}
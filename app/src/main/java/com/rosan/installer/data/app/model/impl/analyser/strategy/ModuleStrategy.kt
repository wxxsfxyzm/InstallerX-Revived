package com.rosan.installer.data.app.model.impl.analyser.strategy

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.enums.DataType
import com.rosan.installer.data.app.repo.AnalysisStrategy
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.util.Properties
import java.util.zip.ZipFile

object ModuleStrategy : AnalysisStrategy {
    override suspend fun analyze(
        config: ConfigEntity,
        data: DataEntity,
        zipFile: ZipFile?,
        extra: AnalyseExtraEntity
    ): List<AppEntity> = coroutineScope {
        require(data is DataEntity.FileEntity)

        // For MIXED_MODULE_APK, zipFile might be null (if not passed by the caller), ensure zipFile exists here.
        val ensureZip = zipFile ?: ZipFile(data.path)

        val useZipBlock = suspend {
            // 1. Analyze Module Props (Always)
            val moduleDeferred = async {
                parseModuleProp(data, ensureZip, extra)
            }

            // 2. Analyze Application Content based on Type
            val appDeferred = async {
                when (extra.dataType) {
                    DataType.MIXED_MODULE_APK -> {
                        // It's an APK that is also a Module.
                        // We use SingleApkStrategy logic (via ApkParser)
                        SingleApkStrategy.analyze(config, data, ensureZip, extra)
                    }

                    DataType.MIXED_MODULE_ZIP -> {
                        // It's a Zip containing APKs + Module prop
                        MultiApkZipStrategy.analyze(config, data, ensureZip, extra)
                    }

                    else -> emptyList() // Pure MODULE_ZIP
                }
            }

            moduleDeferred.await() + appDeferred.await()
        }

        // If we created the ZipFile locally, we must close it.
        // If it was passed in, UnifiedContainerAnalyser manages it.
        if (zipFile == null) {
            ensureZip.use { useZipBlock() }
        } else {
            useZipBlock()
        }
    }

    private fun parseModuleProp(
        data: DataEntity,
        zipFile: ZipFile,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        try {
            Timber.d("Module: Attempting to find module.prop")

            // Debug: Uncomment the line below to print all entries and check for case-sensitivity issues
            // zipFile.entries().asSequence().forEach { Timber.d("DebugModule: ZipEntry found: ${it.name}") }

            val modulePropEntry = zipFile.getEntry("module.prop")
                ?: zipFile.getEntry("common/module.prop")

            if (modulePropEntry == null) {
                Timber.w("Module: module.prop or common/module.prop not found in Zip")
                return emptyList()
            }

            zipFile.getInputStream(modulePropEntry).buffered().use { inputStream ->
                // --- BOM Handling ---
                inputStream.mark(3)
                val bom = ByteArray(3)
                val bytesRead = inputStream.read(bom, 0, 3)

                if (bytesRead < 3 || bom[0] != 0xEF.toByte() || bom[1] != 0xBB.toByte() || bom[2] != 0xBF.toByte()) {
                    inputStream.reset()
                }
                // --- End BOM Handling ---

                val properties = Properties().apply {
                    load(inputStream.reader(Charsets.UTF_8))
                }

                val id = properties.getProperty("id", "")
                val name = properties.getProperty("name", "")

                Timber.d("Module: Parse result id='$id', name='$name'")

                if (id.isBlank() || name.isBlank()) {
                    Timber.w("Module: Incomplete module.prop in file $data (id or name is blank)")
                    return emptyList()
                }

                return listOf(
                    AppEntity.ModuleEntity(
                        id = id,
                        name = name,
                        version = properties.getProperty("version", ""),
                        versionCode = properties.getProperty("versionCode", "-1").toLongOrNull() ?: -1L,
                        author = properties.getProperty("author", ""),
                        description = properties.getProperty("description", ""),
                        data = data,
                        sourceType = extra.dataType
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Module: Exception occurred while parsing module.prop in $data")
            return emptyList()
        }
    }
}
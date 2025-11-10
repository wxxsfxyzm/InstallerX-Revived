package com.rosan.installer.data.app.model.impl.analyser

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.repo.FileAnalyserRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import timber.log.Timber
import java.util.Properties
import java.util.zip.ZipFile

/**
 * Analyzes ZIP archives that are Magisk or KernelSU modules.
 * It reads and parses the `module.prop` file to extract module information.
 */
object ModuleZipAnalyserRepoImpl : FileAnalyserRepo {
    override suspend fun doWork(
        config: ConfigEntity,
        data: List<DataEntity>,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        return data.flatMap { analyseSingleModule(it, extra) }
    }

    private fun analyseSingleModule(
        dataEntity: DataEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val fileEntity = dataEntity as? DataEntity.FileEntity
            ?: throw IllegalArgumentException("ModuleZipAnalyser expected a FileEntity.")

        try {
            ZipFile(fileEntity.path).use { zipFile ->
                val modulePropEntry = zipFile.getEntry("module.prop")
                    ?: zipFile.getEntry("common/module.prop")
                    ?: return emptyList()

                zipFile.getInputStream(modulePropEntry).buffered().use { inputStream ->
                    // --- ROBUST BOM HANDLING ---
                    // Mark the beginning of the stream
                    inputStream.mark(3)
                    val bom = ByteArray(3)
                    val bytesRead = inputStream.read(bom, 0, 3)

                    // Check if the first 3 bytes match the UTF-8 BOM (0xEF, 0xBB, 0xBF)
                    if (bytesRead < 3 || bom[0] != 0xEF.toByte() || bom[1] != 0xBB.toByte() || bom[2] != 0xBF.toByte()) {
                        // If it's not a BOM, reset the stream to the beginning
                        inputStream.reset()
                    }
                    // If it IS a BOM, we've now consumed it and the stream is positioned right after it.
                    // --- END OF BOM HANDLING ---

                    val properties = Properties().apply {
                        // Now, load from the potentially BOM-skipped stream.
                        // Using a reader with specified charset is still good practice.
                        load(inputStream.reader(Charsets.UTF_8))
                    }

                    val id = properties.getProperty("id", "")
                    val name = properties.getProperty("name", "")
                    val version = properties.getProperty("version", "")
                    val versionCode = properties.getProperty("versionCode", "-1").toLongOrNull() ?: -1L
                    val author = properties.getProperty("author", "")
                    val description = properties.getProperty("description", "")

                    if (id.isBlank() || name.isBlank()) {
                        Timber.w("Module file ${fileEntity.path} has incomplete module.prop (missing id or name). Parsed properties: $properties") // Added more logging
                        return emptyList()
                    }

                    val moduleEntity = AppEntity.ModuleEntity(
                        id = id,
                        name = name,
                        version = version,
                        versionCode = versionCode,
                        author = author,
                        description = description,
                        data = dataEntity,
                        containerType = extra.dataType
                    )
                    Timber.d("Found module: ${moduleEntity.name}, version: ${moduleEntity.version}, description: ${moduleEntity.description} ")
                    return listOf(moduleEntity)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to analyse module file: ${fileEntity.path}")
            return emptyList()
        }
    }
}
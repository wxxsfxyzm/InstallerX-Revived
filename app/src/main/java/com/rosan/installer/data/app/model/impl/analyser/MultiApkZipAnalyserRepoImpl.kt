package com.rosan.installer.data.app.model.impl.analyser

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.repo.FileAnalyserRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipFile

/**
 * Analyzes ZIP archives containing multiple, independent APK files.
 * It iterates through the archive and analyzes each .apk file individually.
 */
object MultiApkZipAnalyserRepoImpl : FileAnalyserRepo {

    override suspend fun doWork(
        config: ConfigEntity,
        data: List<DataEntity>,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        // Use flatMap to merge the analysis results from multiple ZIP files into a single list.
        return data.flatMap { analyseSingleZip(config, it, extra) }
    }

    /**
     * Analyzes a single ZIP file entity.
     * Now uses the efficient ZipFile API.
     */
    private suspend fun analyseSingleZip(
        config: ConfigEntity,
        zipDataEntity: DataEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val fileEntity = zipDataEntity as? DataEntity.FileEntity
            ?: throw IllegalArgumentException("MultiApkZipAnalyser expected a FileEntity.")

        val results = mutableListOf<AppEntity>()

        ZipFile(fileEntity.path).use { zipFile ->
            for (entry in zipFile.entries()) {
                if (entry.isDirectory || !entry.name.endsWith(".apk", ignoreCase = true)) {
                    continue
                }

                // Core logic: only process files ending with .apk (case-insensitive).
                Timber.tag("MultiApkZipAnalyser")
                    .d("Found APK entry: ${entry.name} in $zipDataEntity")

                // Create a temporary DataEntity for the APK entry within the ZIP.
                val entryDataEntity = DataEntity.ZipFileEntity(entry.name, fileEntity)

                // Get the input stream for this specific entry.
                zipFile.getInputStream(entry).use { inputStream ->
                    // Analyze the APK stream. The helper function will cache it.
                    val analysedApks = analyseApkStream(config, entryDataEntity, inputStream, extra)
                    results.addAll(analysedApks)
                }
            }
        }
        return results
    }

    /**
     * Extracts a single APK stream to a temporary file and calls ApkAnalyserRepoImpl.
     * This internal caching logic is still necessary for this analyser.
     *
     * @param config The configuration entity for the analysis.
     * @param data The source entity (e.g., ZipFileEntity)
     * @param inputStream The stream of the APK inside the ZIP.
     * @param extra Additional analysis parameters, including cache directory and data type.
     * @return A list of AppEntity objects representing the analyzed APKs.
     */
    private suspend fun analyseApkStream(
        config: ConfigEntity,
        data: DataEntity,
        inputStream: InputStream,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        // Create a temporary file to store the extracted APK.
        val tempFile =
            File.createTempFile(UUID.randomUUID().toString(), ".apk", File(extra.cacheDirectory))

        // Copy the APK content from the ZIP stream to the temporary file.
        val result = runCatching {
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            // Create a DataEntity pointing to this temporary file.
            val tempData = DataEntity.FileEntity(tempFile.absolutePath).apply {
                source = data // Maintain the chain of data sources.
            }

            // Reuse the existing ApkAnalyserRepoImpl to analyze the standalone APK file.
            val originalEntities = ApkAnalyserRepoImpl.doWork(config, listOf(tempData), extra)

            // Enhance the data: add containerType and a more descriptive name/label.
            val displayNameFromZip = if (data is DataEntity.ZipFileEntity) {
                File(data.name).nameWithoutExtension
            } else {
                null
            }

            originalEntities.map { entity ->
                if (entity is AppEntity.BaseEntity) {
                    val entryName = (data as? DataEntity.ZipFileEntity)?.name ?: entity.name
                    entity.copy(
                        // --- FIX: Use File(entryName).name to get just the filename. ---
                        // PackageInstaller will throw IllegalArgumentException if the name contains '/'.
                        name = File(entryName).name,
                        label = entity.label ?: displayNameFromZip, // Use the filename from the ZIP as a fallback label
                        containerType = extra.dataType // Add the container type (MULTI_APK_ZIP)
                    )
                } else {
                    entity
                }
            }
        }

        // Handle the result.
        result.onSuccess { entities ->
            Timber.tag("MultiApkZipAnalyser").d("Successfully analysed $data")
            // IMPORTANT: Do NOT delete the tempFile on success.
            // The returned AppEntity points to it, and it's needed for the installation step.
            // ActionHandler's onFinish() will clean it up later.
            return entities
        }

        result.onFailure { error ->
            Timber.tag("MultiApkZipAnalyser").e(error, "Failed to analyse $data")
            tempFile.delete() // Delete the invalid temp file on failure.
        }

        return emptyList()
    }
}
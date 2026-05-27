// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser.strategy

import android.graphics.drawable.Drawable
import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.settings.model.config.ConfigModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.util.Properties
import java.util.zip.ZipFile

class ModuleStrategy(
    private val singleApkStrategy: SingleApkStrategy,
    private val multiApkZipStrategy: MultiApkZipStrategy
) : AnalysisStrategy {
    override suspend fun analyze(
        config: ConfigModel,
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
                        singleApkStrategy.analyze(config, data, ensureZip, extra)
                    }

                    DataType.MIXED_MODULE_ZIP -> {
                        // It's a Zip containing APKs + Module prop
                        multiApkZipStrategy.analyze(config, data, ensureZip, extra)
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

                // Attempt to parse and load the icon from zip, fallback to webuiIcon if actionIcon is missing or invalid
                var iconDrawable: Drawable? = null
                val iconKeys = listOf("actionIcon", "webuiIcon")

                for (key in iconKeys) {
                    // Remove leading slash and whitespaces just in case the developer incorrectly format the path
                    val iconPath = properties.getProperty(key)?.trim()?.removePrefix("/")

                    if (!iconPath.isNullOrEmpty()) {
                        val iconEntry = zipFile.getEntry(iconPath)
                        if (iconEntry != null) {
                            try {
                                zipFile.getInputStream(iconEntry).use { iconStream ->
                                    iconDrawable = Drawable.createFromStream(iconStream, iconPath)
                                }
                                // Break the loop if the icon is successfully loaded
                                if (iconDrawable != null) {
                                    break
                                }
                            } catch (e: Exception) {
                                Timber.w(e, "Module: Failed to decode $key from entry: $iconPath")
                            }
                        } else {
                            Timber.d("Module: $key entry not found in zip: $iconPath")
                        }
                    }
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
                        icon = iconDrawable,
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
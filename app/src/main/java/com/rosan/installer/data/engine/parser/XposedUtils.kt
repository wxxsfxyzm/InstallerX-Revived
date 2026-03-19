// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.model.XposedModuleInfo
import java.util.Properties
import java.util.zip.ZipFile

object XposedUtils {
    // Defines the modern Xposed configuration file path in the APK archive
    private const val MODULE_PROP_PATH = "META-INF/xposed/module.prop"

    // Keys for modern module.prop file
    private const val PROP_MIN_API = "minApiVersion"
    private const val PROP_LEGACY_MIN_API = "xposedMinVersion"
    private const val PROP_TARGET_API = "targetApiVersion"
    private const val PROP_DESCRIPTION = "description"

    // Keys for legacy AndroidManifest.xml meta-data
    private const val META_MIN_VERSION = "xposedminversion"
    private const val META_DESCRIPTION = "xposeddescription"

    /**
     * Extracts Xposed module information from an APK file.
     *
     * @param zipFile The opened ZipFile instance of the APK.
     * @param metaDataMap A map containing meta-data key-value pairs parsed from AndroidManifest.xml.
     * @param manifestDescription The application description explicitly parsed from AndroidManifest.xml, if available.
     * @return XposedModuleInfo containing minApi, targetApi, and description.
     */
    fun extract(
        zipFile: ZipFile,
        metaDataMap: Map<String, String>,
        manifestDescription: String? = null
    ): XposedModuleInfo {
        val moduleProp = loadModuleProp(zipFile)

        val minApi = resolveMinApi(moduleProp, metaDataMap)
        val targetApi = moduleProp?.getProperty(PROP_TARGET_API)
        val description = resolveDescription(moduleProp, metaDataMap, manifestDescription)

        return XposedModuleInfo(
            minApi = minApi,
            targetApi = targetApi,
            description = description
        )
    }

    // Safely loads the properties from the module.prop file if it exists
    private fun loadModuleProp(zipFile: ZipFile): Properties? {
        val entry = zipFile.getEntry(MODULE_PROP_PATH) ?: return null
        return try {
            Properties().apply {
                zipFile.getInputStream(entry).use { inputStream ->
                    load(inputStream)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    // Resolves the minimum API version by checking modern and legacy keys
    private fun resolveMinApi(moduleProp: Properties?, metaDataMap: Map<String, String>): String? {
        val minVersions = mutableListOf<Int>()

        moduleProp?.getProperty(PROP_MIN_API)?.toIntOrNull()?.let { minVersions.add(it) }
        moduleProp?.getProperty(PROP_LEGACY_MIN_API)?.toIntOrNull()?.let { minVersions.add(it) }
        metaDataMap[META_MIN_VERSION]?.toIntOrNull()?.let { minVersions.add(it) }

        return minVersions.minOrNull()?.toString()
    }

    // Resolves the module description with a specific fallback priority
    private fun resolveDescription(
        moduleProp: Properties?,
        metaDataMap: Map<String, String>,
        manifestDescription: String?
    ): String? {
        if (!manifestDescription.isNullOrBlank() && manifestDescription != "null") {
            return manifestDescription
        }

        val propDesc = moduleProp?.getProperty(PROP_DESCRIPTION)
        if (!propDesc.isNullOrBlank() && propDesc != "null") {
            return propDesc
        }

        val metaDesc = metaDataMap[META_DESCRIPTION]
        if (!metaDesc.isNullOrBlank() && metaDesc != "null") {
            return metaDesc
        }

        return null
    }
}

// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ApkAssets
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.core.reflection.invoke
import com.rosan.installer.core.resParser.parser.AxmlTreeParser
import com.rosan.installer.core.resParser.parser.AxmlTreeParserImpl
import com.rosan.installer.domain.device.model.Architecture
import com.rosan.installer.domain.device.model.Manufacturer
import com.rosan.installer.domain.engine.exception.AnalyseFailedAllFilesUnsupportedException
import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.DataEntity
import com.rosan.installer.domain.engine.model.XposedModuleInfo
import com.rosan.installer.domain.settings.model.ConfigModel
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class ApkParser(
    private val context: Context,
    private val reflect: ReflectionProvider
) {
    @SuppressLint("DiscouragedPrivateApi")
    fun parseFull(
        data: DataEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val fileEntity = data as? DataEntity.FileEntity
            ?: throw IllegalArgumentException("ApkParser expects a FileEntity, got: ${data::class.simpleName}")

        val path = fileEntity.path
        Timber.d("ApkParser: Processing file path: $path")

        val bestArch = analyseAndSelectBestArchitecture(path, DeviceConfig.supportedArchitectures)
        Timber.d("ApkParser: Selected Arch for $path is $bestArch")

        return useResources { resources ->
            try {
                val apkResources = loadApkResources(resources, path)
                Timber.d("ApkParser: Resources loaded successfully for $path")

                val entity = loadAppEntity(
                    apkResources,
                    apkResources.newTheme(),
                    path,
                    data,
                    extra,
                    bestArch ?: Architecture.UNKNOWN
                )
                Timber.d("ApkParser: Entity parsed successfully -> Pkg: ${entity.packageName}")
                listOf(entity)
            } catch (e: Exception) {
                Timber.e(e, "ApkParser: Failed to parse $path")
                emptyList()
            }
        }
    }

    fun parseZipEntryFull(
        config: ConfigModel,
        zipFile: ZipFile,
        entry: ZipEntry,
        parentData: DataEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val tempFile = File.createTempFile("anl_${UUID.randomUUID()}", ".apk", File(extra.cacheDirectory))

        return try {
            zipFile.getInputStream(entry).use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }

            val tempData = DataEntity.FileEntity(tempFile.absolutePath).apply {
                source = DataEntity.ZipFileEntity(entry.name, parentData as DataEntity.FileEntity)
            }

            val results = parseFull(tempData, extra)

            if (results.isEmpty()) tempFile.delete()
            results
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse zip entry: ${entry.name}")
            tempFile.delete()
            emptyList()
        }
    }

    private fun <R> useResources(block: (resources: Resources) -> R): R {
        val resources = createResources()
        return resources.assets.use {
            block.invoke(resources)
        }
    }

    @Suppress("DEPRECATION")
    private fun createResources(): Resources {
        val resources = Resources.getSystem()
        val constructor = reflect.getDeclaredConstructor(AssetManager::class.java)
            ?: return resources
        val assetManager = constructor.newInstance() as AssetManager
        return Resources(assetManager, resources.displayMetrics, resources.configuration)
    }

    private fun loadApkResources(systemResources: Resources, path: String): Resources {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                setAssetPath(systemResources.assets, arrayOf(ApkAssets.loadFromPath(path)))
                return systemResources
            } catch (e: IOException) {
                Timber.e(e, "Failed to load APK assets from path: $path")
                throw AnalyseFailedAllFilesUnsupportedException("Failed to load APK assets.")
            }
        } else {
            val constructor = reflect.getDeclaredConstructor(AssetManager::class.java)
                ?: throw AnalyseFailedAllFilesUnsupportedException("Failed to find AssetManager constructor via reflection")

            val assets = constructor.newInstance() as AssetManager

            val cookie = reflect.invoke<Int>(
                obj = assets,
                name = "addAssetPath",
                parameterTypes = arrayOf(String::class.java),
                args = arrayOf(path)
            ) ?: throw AnalyseFailedAllFilesUnsupportedException("Failed to find or invoke addAssetPath via reflection")

            if (cookie == 0) {
                throw AnalyseFailedAllFilesUnsupportedException("addAssetPath returned 0 for: $path")
            }
            @Suppress("DEPRECATION") return Resources(assets, systemResources.displayMetrics, systemResources.configuration)
        }
    }

    private fun setAssetPath(assetManager: AssetManager, assets: Array<ApkAssets>) {
        val setApkAssetsMtd = reflect.getDeclaredMethod(
            "setApkAssets",
            AssetManager::class.java,
            Array<ApkAssets>::class.java,
            Boolean::class.java
        ) ?: throw AnalyseFailedAllFilesUnsupportedException("Failed to find setApkAssets method")

        setApkAssetsMtd.isAccessible = true
        setApkAssetsMtd.invoke(assetManager, assets, true)
    }

    private fun loadAppEntity(
        resources: Resources,
        theme: Resources.Theme?,
        path: String,
        data: DataEntity,
        extra: AnalyseExtraEntity,
        arch: Architecture
    ): AppEntity {
        var packageName: String? = null
        var sharedUserId: String? = null
        var splitName: String? = null
        var versionCode: Long = -1
        var versionName = ""
        var minOsdkVersion: String? = null
        var label: String? = null
        var icon: Drawable? = null
        var roundIcon: Drawable? = null
        var minSdk: String? = null
        var targetSdk: String? = null
        val permissions = mutableListOf<String>()
        val signatureHash = (data as? DataEntity.FileEntity)?.path?.let {
            SignatureUtils.getApkSignatureHash(context, it)
        }

        // Variables for Xposed extraction
        val metaDataMap = mutableMapOf<String, String>()
        var appDescription: String? = null
        var isPotentialXposed = false

        AxmlTreeParserImpl(resources.assets.openXmlResourceParser("AndroidManifest.xml"))
            .register("/manifest") {
                packageName = getAttributeValue(null, "package")
                sharedUserId = getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "sharedUserId")
                splitName = getAttributeValue(null, "split")
                val versionCodeMajor = getAttributeIntValue(AxmlTreeParser.ANDROID_NAMESPACE, "versionCodeMajor", 0).toLong()
                val versionCodeMinor = getAttributeIntValue(AxmlTreeParser.ANDROID_NAMESPACE, "versionCode", 0).toLong()
                versionCode = versionCodeMajor shl 32 or (versionCodeMinor and 0xffffffffL)

                versionName = resolveString(
                    resources,
                    getAttributeResourceValue(AxmlTreeParser.ANDROID_NAMESPACE, "versionName", ResourcesCompat.ID_NULL),
                    getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "versionName")
                ) ?: versionName
            }
            .register("/manifest/uses-sdk") {
                minSdk = getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "minSdkVersion")
                targetSdk = getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "targetSdkVersion")
            }
            .register("/manifest/application") {
                label = resolveString(
                    resources,
                    getAttributeResourceValue(AxmlTreeParser.ANDROID_NAMESPACE, "label", ResourcesCompat.ID_NULL),
                    getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "label")
                )
                icon = resolveDrawable(
                    resources,
                    theme,
                    getAttributeResourceValue(AxmlTreeParser.ANDROID_NAMESPACE, "icon", ResourcesCompat.ID_NULL)
                )
                roundIcon = resolveDrawable(
                    resources,
                    theme,
                    getAttributeResourceValue(AxmlTreeParser.ANDROID_NAMESPACE, "roundIcon", ResourcesCompat.ID_NULL)
                )

                // Extract description for Xposed fallback
                appDescription = resolveString(
                    resources,
                    getAttributeResourceValue(AxmlTreeParser.ANDROID_NAMESPACE, "description", ResourcesCompat.ID_NULL),
                    getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "description")
                )
            }
            .register("/manifest/application/meta-data") {
                if (DeviceConfig.currentManufacturer == Manufacturer.OPPO || DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS) {
                    if ("minOsdkVersion" == getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "name")) {
                        minOsdkVersion = resolveString(
                            resources,
                            getAttributeResourceValue(AxmlTreeParser.ANDROID_NAMESPACE, "value", ResourcesCompat.ID_NULL),
                            getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "value")
                        )
                    }
                }

                val metaDataName = getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "name")
                val metaDataValue = resolveString(
                    resources,
                    getAttributeResourceValue(AxmlTreeParser.ANDROID_NAMESPACE, "value", ResourcesCompat.ID_NULL),
                    getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "value")
                )

                if (metaDataName != null && metaDataValue != null) {
                    metaDataMap[metaDataName] = metaDataValue
                }

                // Check legacy xposed indicators
                if ("xposedmodule" == metaDataName ||
                    "xposedminversion" == metaDataName ||
                    "xposeddescription" == metaDataName
                ) {
                    isPotentialXposed = true
                }
            }
            .register("/manifest/uses-permission") {
                getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "name")?.let { if (it.isNotBlank()) permissions.add(it) }
            }
            .map { }

        // Final Xposed verification and data extraction
        var xposedInfo: XposedModuleInfo? = null
        ZipFile(path).use { zip ->
            // Check modern Xposed indicators if legacy ones were not found
            if (!isPotentialXposed && (zip.getEntry("META-INF/xposed/module.prop") != null || zip.getEntry("assets/xposed_init") != null)) {
                isPotentialXposed = true
            }

            if (isPotentialXposed) {
                xposedInfo = XposedUtils.extract(zip, metaDataMap, appDescription)
            }
        }

        Timber.d("ApkParser: Manifest parsed. Package: $packageName, Split: $splitName, IsXposed: ${xposedInfo != null}")
        if (packageName.isNullOrEmpty()) throw Exception("Invalid APK: missing package name")

        return if (splitName.isNullOrEmpty()) AppEntity.BaseEntity(
            packageName = packageName,
            sharedUserId = sharedUserId,
            data = data,
            versionCode = versionCode,
            versionName = versionName,
            label = label,
            icon = roundIcon ?: icon,
            targetSdk = targetSdk,
            minSdk = minSdk,
            minOsdkVersion = minOsdkVersion,
            xposedInfo = xposedInfo, // Using the extracted info object
            arch = arch,
            permissions = permissions,
            sourceType = extra.dataType,
            signatureHash = signatureHash
        ) else {
            val metadata = splitName.parseSplitMetadata()
            AppEntity.SplitEntity(
                packageName = packageName,
                data = data,
                splitName = splitName,
                targetSdk = targetSdk,
                minSdk = minSdk,
                arch = null,
                sourceType = extra.dataType,
                type = metadata.type,
                filterType = metadata.filterType,
                configValue = metadata.configValue
            )
        }
    }

    private fun resolveString(res: Resources, resId: Int, rawValue: String?): String? {
        if (resId == ResourcesCompat.ID_NULL) return rawValue
        return try {
            res.getString(resId)
        } catch (_: Exception) {
            rawValue
        }
    }

    private fun resolveDrawable(res: Resources, theme: Resources.Theme?, resId: Int): Drawable? {
        if (resId == ResourcesCompat.ID_NULL) return null
        return try {
            ResourcesCompat.getDrawable(res, resId, theme)
        } catch (_: Exception) {
            null
        }
    }

    private fun analyseAndSelectBestArchitecture(path: String, deviceSupportedArchs: List<Architecture>): Architecture? {
        val apkArchs = mutableSetOf<Architecture>()
        runCatching {
            ZipFile(path).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val name = entries.nextElement().name
                    if (name.startsWith("lib/") && name.count { it == '/' } >= 2) {
                        Architecture.fromArchString(name.split('/')[1])
                            .takeIf { it != Architecture.UNKNOWN }
                            ?.let { apkArchs.add(it) }
                    }
                }
            }
        }

        if (apkArchs.isEmpty()) return Architecture.NONE

        // If the APK contains an architecture explicitly supported by the device, return it.
        for (deviceArch in deviceSupportedArchs) {
            if (apkArchs.contains(deviceArch)) return deviceArch
        }

        // Force selection for binary translation scenarios (e.g., running 32-bit libs on arm64-only devices).
        if (DeviceConfig.isArm) {
            // Prefer ARMv7a (Architecture.ARM) if available.
            if (apkArchs.contains(Architecture.ARM)) return Architecture.ARM

            // Fallback to ARMEABI if ARMv7a is missing but ARMEABI exists.
            if (apkArchs.contains(Architecture.ARMEABI)) return Architecture.ARMEABI
        }

        if (DeviceConfig.isX86 && apkArchs.contains(Architecture.X86)) return Architecture.X86

        return apkArchs.firstOrNull { it == Architecture.ARM64 }
            ?: apkArchs.firstOrNull { it == Architecture.ARM }
            ?: apkArchs.firstOrNull { it == Architecture.X86_64 }
            ?: apkArchs.firstOrNull { it == Architecture.X86 }
            ?: apkArchs.firstOrNull()
    }
}

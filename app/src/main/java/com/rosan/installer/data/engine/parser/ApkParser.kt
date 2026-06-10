// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ApkAssets
import android.content.res.AssetManager
import android.content.res.`AssetManager$Builder`
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import com.rosan.installer.core.device.model.Architecture
import com.rosan.installer.core.device.model.Manufacturer
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.core.reflection.invoke
import com.rosan.installer.core.resParser.parser.AxmlTreeParser
import com.rosan.installer.domain.engine.exception.AnalyseException
import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.error.AnalyseErrorType
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.XposedModuleInfo
import com.rosan.installer.domain.engine.model.source.DataEntity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
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

                val entity = if (apkResources.closeResourcesAfterUse) apkResources.resources.assets.use {
                    loadAppEntity(
                        apkResources.resources,
                        apkResources.resources.newTheme(),
                        apkResources.openManifestParser,
                        path,
                        data,
                        extra,
                        bestArch ?: Architecture.UNKNOWN
                    )
                } else {
                    loadAppEntity(
                        apkResources.resources,
                        apkResources.resources.newTheme(),
                        apkResources.openManifestParser,
                        path,
                        data,
                        extra,
                        bestArch ?: Architecture.UNKNOWN
                    )
                }
                Timber.d("ApkParser: Entity parsed successfully -> Pkg: ${entity.packageName}")
                listOf(entity)
            } catch (e: Exception) {
                Timber.e(e, "ApkParser: Failed to parse $path")
                emptyList()
            }
        }
    }

    fun parseZipEntryFull(
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

    private data class ApkResourceContext(
        val resources: Resources,
        val openManifestParser: () -> XmlResourceParser,
        val closeResourcesAfterUse: Boolean
    )

    private fun loadApkResources(systemResources: Resources, path: String): ApkResourceContext {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val apkAssets = ApkAssets.loadFromPath(path)
                val assetManager = `AssetManager$Builder`()
                    .addApkAssets(apkAssets)
                    .build()
                val apkResources = Resources(assetManager, systemResources.displayMetrics, systemResources.configuration)
                return ApkResourceContext(
                    resources = apkResources,
                    openManifestParser = { apkAssets.openXml("AndroidManifest.xml") },
                    closeResourcesAfterUse = true
                )
            } catch (e: IOException) {
                Timber.e(e, "Failed to load APK assets from path: $path")
                throw AnalyseException(
                    errorType = AnalyseErrorType.ALL_FILES_UNSUPPORTED,
                    message = "Failed to load APK assets.",
                    cause = e
                )
            }
        } else {
            val constructor = reflect.getDeclaredConstructor(AssetManager::class.java)
                ?: throw AnalyseException(
                    errorType = AnalyseErrorType.ALL_FILES_UNSUPPORTED,
                    message = "Failed to find AssetManager constructor via reflection"
                )

            val assets = constructor.newInstance() as AssetManager

            val cookie = reflect.invoke<Int>(
                obj = assets,
                name = "addAssetPath",
                parameterTypes = arrayOf(String::class.java),
                args = arrayOf(path)
            ) ?: throw AnalyseException(
                errorType = AnalyseErrorType.ALL_FILES_UNSUPPORTED,
                message = "Failed to find or invoke addAssetPath via reflection"
            )

            if (cookie == 0) {
                throw AnalyseException(
                    errorType = AnalyseErrorType.ALL_FILES_UNSUPPORTED,
                    message = "addAssetPath returned 0 for: $path"
                )
            }
            @Suppress("DEPRECATION")
            val apkResources = Resources(assets, systemResources.displayMetrics, systemResources.configuration)
            return ApkResourceContext(
                resources = apkResources,
                openManifestParser = { apkResources.assets.openXmlResourceParser("AndroidManifest.xml") },
                closeResourcesAfterUse = true
            )
        }
    }

    private fun setAssetPath(assetManager: AssetManager, assets: Array<ApkAssets>) {
        val setApkAssetsMtd = reflect.getDeclaredMethod(
            "setApkAssets",
            AssetManager::class.java,
            Array<ApkAssets>::class.java,
            Boolean::class.java
        ) ?: throw AnalyseException(
            errorType = AnalyseErrorType.ALL_FILES_UNSUPPORTED,
            message = "Failed to find setApkAssets method"
        )

        setApkAssetsMtd.isAccessible = true
        setApkAssetsMtd.invoke(assetManager, assets, true)
    }

    private fun loadAppEntity(
        resources: Resources,
        theme: Resources.Theme?,
        openManifestParser: () -> XmlResourceParser,
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

        try {
            openManifestParser().use { manifestParser ->
                var insideApplication = false
                var applicationDepth = -1
                var eventType = manifestParser.eventType

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> when (manifestParser.name) {
                            "manifest" -> {
                                packageName = manifestParser.getAttributeValue(null, "package")
                                sharedUserId = manifestParser.getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "sharedUserId")
                                splitName = manifestParser.getAttributeValue(null, "split")
                                val versionCodeMajor =
                                    manifestParser.getAttributeIntValue(AxmlTreeParser.ANDROID_NAMESPACE, "versionCodeMajor", 0).toLong()
                                val versionCodeMinor =
                                    manifestParser.getAttributeIntValue(AxmlTreeParser.ANDROID_NAMESPACE, "versionCode", 0).toLong()
                                versionCode = versionCodeMajor shl 32 or (versionCodeMinor and 0xffffffffL)

                                versionName = resolveString(
                                    resources,
                                    manifestParser.getAttributeResourceValue(
                                        AxmlTreeParser.ANDROID_NAMESPACE,
                                        "versionName",
                                        ResourcesCompat.ID_NULL
                                    ),
                                    manifestParser.getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "versionName")
                                ) ?: versionName
                            }

                            "uses-sdk" -> {
                                minSdk = manifestParser.getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "minSdkVersion")
                                targetSdk = manifestParser.getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "targetSdkVersion")
                            }

                            "application" -> {
                                insideApplication = true
                                applicationDepth = manifestParser.depth

                                label = resolveString(
                                    resources,
                                    manifestParser.getAttributeResourceValue(AxmlTreeParser.ANDROID_NAMESPACE, "label", ResourcesCompat.ID_NULL),
                                    manifestParser.getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "label")
                                )
                                icon = resolveDrawable(
                                    resources,
                                    theme,
                                    manifestParser.getAttributeResourceValue(AxmlTreeParser.ANDROID_NAMESPACE, "icon", ResourcesCompat.ID_NULL),
                                    "icon"
                                )
                                roundIcon = resolveDrawable(
                                    resources,
                                    theme,
                                    manifestParser.getAttributeResourceValue(
                                        AxmlTreeParser.ANDROID_NAMESPACE,
                                        "roundIcon",
                                        ResourcesCompat.ID_NULL
                                    ),
                                    "roundIcon"
                                )

                                // Extract description for Xposed fallback
                                appDescription = resolveString(
                                    resources,
                                    manifestParser.getAttributeResourceValue(
                                        AxmlTreeParser.ANDROID_NAMESPACE,
                                        "description",
                                        ResourcesCompat.ID_NULL
                                    ),
                                    manifestParser.getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "description")
                                )
                            }

                            "meta-data" -> {
                                if (insideApplication) {
                                    if (DeviceConfig.currentManufacturer == Manufacturer.OPPO || DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS) {
                                        if ("minOsdkVersion" == manifestParser.getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "name")) {
                                            minOsdkVersion = resolveString(
                                                resources,
                                                manifestParser.getAttributeResourceValue(
                                                    AxmlTreeParser.ANDROID_NAMESPACE,
                                                    "value",
                                                    ResourcesCompat.ID_NULL
                                                ),
                                                manifestParser.getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "value")
                                            )
                                        }
                                    }

                                    val metaDataName = manifestParser.getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "name")
                                    val metaDataValue = resolveString(
                                        resources,
                                        manifestParser.getAttributeResourceValue(
                                            AxmlTreeParser.ANDROID_NAMESPACE,
                                            "value",
                                            ResourcesCompat.ID_NULL
                                        ),
                                        manifestParser.getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "value")
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
                            }

                            "uses-permission", "uses-permission-sdk-m" -> {
                                manifestParser.getAttributeValue(AxmlTreeParser.ANDROID_NAMESPACE, "name")
                                    ?.let { if (it.isNotBlank()) permissions.add(it) }
                            }
                        }

                        XmlPullParser.END_TAG -> {
                            if (insideApplication && manifestParser.depth == applicationDepth && manifestParser.name == "application") {
                                insideApplication = false
                                applicationDepth = -1
                            }
                        }
                    }
                    eventType = manifestParser.next()
                }
            }
        } catch (e: Exception) {
            if (!e.isRecoverableManifestParseException() || packageName.isNullOrEmpty()) throw e
            Timber.w(e, "ApkParser: Manifest parsing stopped early for $path. Optional manifest data may be incomplete.")
        }

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
            icon = icon ?: roundIcon,
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

    private fun resolveDrawable(res: Resources, theme: Resources.Theme?, resId: Int, attrName: String): Drawable? {
        if (resId == ResourcesCompat.ID_NULL) return null
        return try {
            ResourcesCompat.getDrawable(res, resId, theme)
        } catch (e: Exception) {
            Timber.w(e, "ApkParser: Failed to resolve application $attrName resource: 0x${resId.toString(16)}")
            null
        }
    }

    private fun Exception.isRecoverableManifestParseException(): Boolean {
        return this is XmlPullParserException || this is IOException
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

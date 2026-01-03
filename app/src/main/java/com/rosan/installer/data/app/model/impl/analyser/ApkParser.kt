package com.rosan.installer.data.app.model.impl.analyser

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ApkAssets
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Architecture
import com.rosan.installer.build.model.entity.Manufacturer
import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.exception.AnalyseFailedAllFilesUnsupportedException
import com.rosan.installer.data.app.util.SignatureUtils
import com.rosan.installer.data.app.util.parseSplitMetadata
import com.rosan.installer.data.reflect.repo.ReflectRepo
import com.rosan.installer.data.res.model.impl.AxmlTreeRepoImpl
import com.rosan.installer.data.res.repo.AxmlTreeRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object ApkParser : KoinComponent {
    private val reflect by inject<ReflectRepo>()
    private val context by inject<Context>()

    @SuppressLint("DiscouragedPrivateApi")
    fun parseFull(
        config: ConfigEntity,
        data: DataEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val fileEntity = data as? DataEntity.FileEntity
            ?: throw IllegalArgumentException("ApkParser expects a FileEntity, got: ${data::class.simpleName}")

        val path = fileEntity.path
        Timber.d("ApkParser: Processing file path: $path")

        val bestArch = analyseAndSelectBestArchitecture(path, RsConfig.supportedArchitectures)
        Timber.d("ApkParser: Selected Arch for $path is $bestArch")

        return useResources { resources ->
            try {
                val apkResources = loadApkResources(resources, path)
                Timber.d("ApkParser: Resources loaded successfully for $path")

                val entity = loadAppEntity(
                    apkResources,
                    apkResources.newTheme(),
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
        config: ConfigEntity,
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

            val results = parseFull(config, tempData, extra)

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

            val addAssetPath = reflect.getDeclaredMethod(AssetManager::class.java, "addAssetPath", String::class.java)
                ?: throw AnalyseFailedAllFilesUnsupportedException("Failed to find addAssetPath method via reflection")

            addAssetPath.isAccessible = true

            val cookie = addAssetPath.invoke(assets, path) as Int
            if (cookie == 0) {
                throw AnalyseFailedAllFilesUnsupportedException("addAssetPath returned 0 for: $path")
            }
            return Resources(assets, systemResources.displayMetrics, systemResources.configuration)
        }
    }

    private fun setAssetPath(assetManager: AssetManager, assets: Array<ApkAssets>) {
        val setApkAssetsMtd = reflect.getDeclaredMethod(
            AssetManager::class.java,
            "setApkAssets",
            Array<ApkAssets>::class.java,
            Boolean::class.java
        ) ?: throw AnalyseFailedAllFilesUnsupportedException("Failed to find setApkAssets method")

        setApkAssetsMtd.isAccessible = true
        setApkAssetsMtd.invoke(assetManager, assets, true)
    }

    private fun loadAppEntity(
        resources: Resources, theme: Resources.Theme?, data: DataEntity, extra: AnalyseExtraEntity, arch: Architecture
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

        AxmlTreeRepoImpl(resources.assets.openXmlResourceParser("AndroidManifest.xml"))
            .register("/manifest") {
                packageName = getAttributeValue(null, "package")
                sharedUserId = getAttributeValue(AxmlTreeRepo.ANDROID_NAMESPACE, "sharedUserId")
                splitName = getAttributeValue(null, "split")
                val versionCodeMajor = getAttributeIntValue(AxmlTreeRepo.ANDROID_NAMESPACE, "versionCodeMajor", 0).toLong()
                val versionCodeMinor = getAttributeIntValue(AxmlTreeRepo.ANDROID_NAMESPACE, "versionCode", 0).toLong()
                versionCode = versionCodeMajor shl 32 or (versionCodeMinor and 0xffffffffL)

                versionName = when (val resId = getAttributeResourceValue(AxmlTreeRepo.ANDROID_NAMESPACE, "versionName", -1)) {
                    -1 -> getAttributeValue(AxmlTreeRepo.ANDROID_NAMESPACE, "versionName") ?: versionName
                    0 -> versionName
                    else -> try {
                        resources.getString(resId)
                    } catch (e: Exception) {
                        getAttributeValue(AxmlTreeRepo.ANDROID_NAMESPACE, "versionName") ?: versionName
                    }
                }
            }
            .register("/manifest/uses-sdk") {
                minSdk = getAttributeValue(AxmlTreeRepo.ANDROID_NAMESPACE, "minSdkVersion")
                targetSdk = getAttributeValue(AxmlTreeRepo.ANDROID_NAMESPACE, "targetSdkVersion")
            }
            .register("/manifest/application") {
                label = resolveString(
                    resources,
                    getAttributeResourceValue(AxmlTreeRepo.ANDROID_NAMESPACE, "label", -1),
                    getAttributeValue(AxmlTreeRepo.ANDROID_NAMESPACE, "label")
                )
                icon = resolveDrawable(resources, theme, getAttributeResourceValue(AxmlTreeRepo.ANDROID_NAMESPACE, "icon", -1))
                roundIcon =
                    resolveDrawable(resources, theme, getAttributeResourceValue(AxmlTreeRepo.ANDROID_NAMESPACE, "roundIcon", -1))
            }
            .register("/manifest/application/meta-data") {
                if (RsConfig.currentManufacturer == Manufacturer.OPPO || RsConfig.currentManufacturer == Manufacturer.ONEPLUS) {
                    if ("minOsdkVersion" == getAttributeValue(AxmlTreeRepo.ANDROID_NAMESPACE, "name")) {
                        minOsdkVersion = resolveString(
                            resources,
                            getAttributeResourceValue(AxmlTreeRepo.ANDROID_NAMESPACE, "value", -1),
                            getAttributeValue(AxmlTreeRepo.ANDROID_NAMESPACE, "value")
                        )
                    }
                }
            }
            .register("/manifest/uses-permission") {
                getAttributeValue(AxmlTreeRepo.ANDROID_NAMESPACE, "name")?.let { if (it.isNotBlank()) permissions.add(it) }
            }
            .map { }
        Timber.d("ApkParser: Manifest parsed. Package: $packageName, Split: $splitName")
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
        if (resId > 0) {
            try {
                return res.getString(resId)
            } catch (_: Exception) {
            }
        }
        return rawValue
    }

    private fun resolveDrawable(res: Resources, theme: Resources.Theme?, resId: Int): Drawable? {
        if (resId > 0) {
            try {
                return ResourcesCompat.getDrawable(res, resId, theme)
            } catch (_: Exception) {
            }
        }
        return null
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
        if (RsConfig.isArm) {
            // Prefer ARMv7a (Architecture.ARM) if available.
            if (apkArchs.contains(Architecture.ARM)) return Architecture.ARM

            // Fallback to ARMEABI if ARMv7a is missing but ARMEABI exists.
            if (apkArchs.contains(Architecture.ARMEABI)) return Architecture.ARMEABI
        }

        if (RsConfig.isX86 && apkArchs.contains(Architecture.X86)) return Architecture.X86

        return null
    }
}
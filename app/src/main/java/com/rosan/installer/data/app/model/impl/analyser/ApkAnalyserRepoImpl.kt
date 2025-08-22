package com.rosan.installer.data.app.model.impl.analyser

import android.content.res.ApkAssets
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import com.rosan.installer.build.Architecture
import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.exception.AnalyseFailedAllFilesUnsupportedException
import com.rosan.installer.data.app.repo.AnalyserRepo
import com.rosan.installer.data.reflect.repo.ReflectRepo
import com.rosan.installer.data.res.model.impl.AxmlTreeRepoImpl
import com.rosan.installer.data.res.repo.AxmlTreeRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import timber.log.Timber
import java.io.IOException
import java.util.zip.ZipFile

object ApkAnalyserRepoImpl : AnalyserRepo, KoinComponent {
    private val reflect = get<ReflectRepo>()

    override suspend fun doWork(
        config: ConfigEntity,
        data: List<DataEntity>,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val apps = mutableListOf<AppEntity>()
        // The upstream handler now ensures all data entities are FileEntity.
        data.forEach { entity ->
            val fileEntity = entity as? DataEntity.FileEntity
                ?: throw IllegalArgumentException("ApkAnalyserRepoImpl expected a FileEntity.")
            apps.addAll(doFileWork(config, fileEntity, extra))
        }
        return apps
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

    /**
     * Analyzes a single APK file and returns the AppEntity representation.
     * This method is used for both FileEntity and FileDescriptorEntity.
     *
     * @param config The configuration entity containing settings for the analysis.
     * @param data The data entity representing the APK file.
     * @param extra Additional analysis parameters.
     * @return A list of AppEntity representing the analyzed APK.
     */
    private fun doFileWork(
        config: ConfigEntity,
        data: DataEntity.FileEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        Timber.d("doFileWork: ${data.path}, extra: $extra")
        val path = data.path
        val bestArch = analyseAndSelectBestArchitecture(path, RsConfig.supportedArchitectures)
        return useResources { resources ->
            try {
                setAssetPath(resources.assets, arrayOf(ApkAssets.loadFromPath(path)))
            } catch (e: IOException) {
                Timber.e(e, "Failed to load APK assets from path: $path")
                throw AnalyseFailedAllFilesUnsupportedException("Failed to load APK assets. Maybe the file is corrupted or not supported?")
            }
            listOf(loadAppEntity(resources, resources.newTheme(), data, extra, bestArch ?: Architecture.UNKNOWN))
        }
    }

    private fun setAssetPath(assetManager: AssetManager, assets: Array<ApkAssets>) {
        val setApkAssetsMtd = reflect.getDeclaredMethod(
            AssetManager::class.java,
            "setApkAssets",
            Array<ApkAssets>::class.java,
            Boolean::class.java
        )!!
        setApkAssetsMtd.isAccessible = true
        setApkAssetsMtd.invoke(assetManager, assets, true)
    }

    private fun loadAppEntity(
        resources: Resources, theme: Resources.Theme?, data: DataEntity, extra: AnalyseExtraEntity, arch: Architecture
    ): AppEntity {
        var packageName: String? = null
        var splitName: String? = null
        var versionCode: Long = -1
        var versionName = ""
        var label: String? = null
        var icon: Drawable? = null
        var roundIcon: Drawable? = null
        var minSdk: String? = null
        var targetSdk: String? = null
        val permissions = mutableListOf<String>()
        AxmlTreeRepoImpl(resources.assets.openXmlResourceParser("AndroidManifest.xml"))
            .register("/manifest") {
                packageName = getAttributeValue(null, "package")
                splitName = getAttributeValue(null, "split")
                val versionCodeMajor = getAttributeIntValue(
                    AxmlTreeRepo.ANDROID_NAMESPACE, "versionCodeMajor", 0
                ).toLong()
                val versionCodeMinor = getAttributeIntValue(
                    AxmlTreeRepo.ANDROID_NAMESPACE, "versionCode", 0
                ).toLong()
                versionCode = versionCodeMajor shl 32 or (versionCodeMinor and 0xffffffffL)
                versionName =
                    getAttributeValue(AxmlTreeRepo.ANDROID_NAMESPACE, "versionName") ?: versionName
            }
            .register("/manifest/uses-sdk") {
                minSdk = getAttributeValue(AxmlTreeRepo.ANDROID_NAMESPACE, "minSdkVersion")
                targetSdk = getAttributeValue(AxmlTreeRepo.ANDROID_NAMESPACE, "targetSdkVersion")
            }
            .register("/manifest/application") {
                label = when (val resId =
                    getAttributeResourceValue(AxmlTreeRepo.ANDROID_NAMESPACE, "label", -1)) {
                    -1 -> getAttributeValue(AxmlTreeRepo.ANDROID_NAMESPACE, "label") ?: label
                    0 -> null
                    else -> {
                        try {
                            resources.getString(resId)
                        } catch (e: Resources.NotFoundException) {
                            null
                        }
                    }
                }

                icon = when (val resId =
                    getAttributeResourceValue(AxmlTreeRepo.ANDROID_NAMESPACE, "icon", -1)) {
                    -1 -> null
                    0 -> null
                    else -> {
                        try {
                            ResourcesCompat.getDrawable(resources, resId, theme)
                        } catch (e: Resources.NotFoundException) {
                            null
                        }
                    }
                }

                roundIcon = when (val resId = getAttributeResourceValue(
                    AxmlTreeRepo.ANDROID_NAMESPACE, "roundIcon", -1
                )) {
                    -1 -> null
                    0 -> null
                    else -> {
                        try {
                            ResourcesCompat.getDrawable(resources, resId, theme)
                        } catch (e: Resources.NotFoundException) {
                            null
                        }
                    }
                }
            }
            .register("/manifest/uses-permission") {
                val permissionName = getAttributeValue(AxmlTreeRepo.ANDROID_NAMESPACE, "name")
                if (!permissionName.isNullOrBlank()) {
                    permissions.add(permissionName)
                }
            }
            .map { }
        if (packageName.isNullOrEmpty()) throw Exception("can't get the package from this package")
        return if (splitName.isNullOrEmpty()) AppEntity.BaseEntity(
            packageName = packageName,
            data = data,
            versionCode = versionCode,
            versionName = versionName,
            label = label,
            icon = roundIcon ?: icon,
            targetSdk = targetSdk,
            minSdk = minSdk,
            arch = arch,
            permissions = permissions,
            containerType = extra.dataType
        ) else AppEntity.SplitEntity(
            packageName = packageName,
            data = data,
            splitName = splitName,
            targetSdk = targetSdk,
            minSdk = minSdk,
            arch = null,
            containerType = extra.dataType
        )
    }

    /**
     * Analyzes the APK's supported architectures and selects the best one for the current device.
     * It extracts all ABIs from the APK's 'lib' directory, compares them against the
     * device's supported ABIs (from `ConfigEntity`), and returns the highest-priority match.
     *
     * @param path The file path to the APK.
     * @param deviceSupportedArchs A prioritized list of architectures supported by the device.
     * @return The best matching Architecture for the device, or null if no compatible architecture is found.
     */
    private fun analyseAndSelectBestArchitecture(
        path: String,
        deviceSupportedArchs: List<Architecture>
    ): Architecture? {
        Timber.d("deviceSupportedArchs: ${deviceSupportedArchs.joinToString(", ")}")
        // Step 1: Extract all supported architectures from the APK file.
        val apkArchs = mutableSetOf<Architecture>()
        runCatching {
            ZipFile(path).use { zipFile ->
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val entryName = entry.name
                    // Check for native library directories.
                    if (entryName.startsWith("lib/") && entryName.count { it == '/' } >= 2) {
                        val archString = entryName.split('/')[1]
                        val architecture = Architecture.fromArchString(archString)
                        if (architecture != Architecture.UNKNOWN) {
                            Timber.d("Found architecture: $architecture in APK: $path")
                            apkArchs.add(architecture)
                        }
                    }
                }
            }
        }

        if (apkArchs.isEmpty()) {
            return Architecture.NONE // The APK is architecture-independent or contains no native libs.
        }

        // Step 2: Find the best match.
        // Iterate through the device's prioritized list of ABIs.
        for (deviceArch in deviceSupportedArchs) {
            // Return the first one that is also supported by the APK.
            if (apkArchs.contains(deviceArch)) {
                return deviceArch
            }
        }

        // Return null if no match was found.
        return null
    }
}
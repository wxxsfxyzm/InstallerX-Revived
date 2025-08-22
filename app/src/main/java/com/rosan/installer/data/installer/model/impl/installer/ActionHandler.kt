package com.rosan.installer.data.installer.model.impl.installer

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.Os
import com.rosan.installer.R
import com.rosan.installer.build.Architecture
import com.rosan.installer.build.Density
import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.app.model.exception.AnalyseFailedAllFilesUnsupportedException
import com.rosan.installer.data.app.model.impl.AnalyserRepoImpl
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.installer.model.exception.ResolveException
import com.rosan.installer.data.installer.model.impl.InstallerRepoImpl
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.installer.util.getRealPathFromUri
import com.rosan.installer.data.installer.util.pathUnify
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.util.ConfigUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.UUID

class ActionHandler(scope: CoroutineScope, installer: InstallerRepo) :
    Handler(scope, installer), KoinComponent {
    override val installer: InstallerRepoImpl = super.installer as InstallerRepoImpl
    private var job: Job? = null
    private val context by inject<Context>()
    private val appDataStore by inject<AppDataStore>()
    private val cacheParcelFileDescriptors = mutableListOf<ParcelFileDescriptor>()
    private val cacheDirectory = "${context.externalCacheDir?.absolutePath}/${installer.id}".apply {
        File(this).mkdirs()
    }

    override suspend fun onStart() {
        Timber.d("[id=${installer.id}] onStart: Starting to collect actions.")
        job = scope.launch {
            installer.action.collect { action ->
                Timber.d("[id=${installer.id}] Received action: ${action::class.simpleName}")
                when (action) {
                    is InstallerRepoImpl.Action.Resolve -> resolve(action.activity)
                    is InstallerRepoImpl.Action.Analyse -> analyse()
                    is InstallerRepoImpl.Action.Install -> install()
                    is InstallerRepoImpl.Action.Uninstall -> uninstall(action.packageName)
                    is InstallerRepoImpl.Action.Finish -> finish()
                }
            }
        }
    }

    override suspend fun onFinish() {
        Timber.d("[id=${installer.id}] onFinish: Cleaning up resources and cancelling job.")
        cacheParcelFileDescriptors.forEach { it.runCatching { close() } }
        cacheParcelFileDescriptors.clear()
        File(cacheDirectory).deleteRecursively()
        job?.cancel()
    }

    private suspend fun resolve(activity: Activity) {
        Timber.d("[id=${installer.id}] resolve: Starting new task.")

        // --- Reset all state fields here at the beginning ---
        installer.error = Throwable()
        installer.config = ConfigEntity.default
        installer.data = emptyList()
        installer.entities = emptyList()
        installer.progress.emit(ProgressEntity.Ready) // Also reset progress

        Timber.d("[id=${installer.id}] resolve: State has been reset. Emitting ProgressEntity.Resolving.")
        installer.progress.emit(ProgressEntity.Resolving)


        installer.config = try {
            resolveConfig(activity)
        } catch (e: Exception) {
            Timber.e(e, "[id=${installer.id}] resolve: Failed to resolve config.")
            installer.error = e
            installer.progress.emit(ProgressEntity.ResolvedFailed)
            return
        }
        Timber.d("[id=${installer.id}] resolve: Config resolved. installMode=${installer.config.installMode}")

        if (installer.config.installMode == ConfigEntity.InstallMode.Ignore) {
            Timber.d("[id=${installer.id}] resolve: InstallMode is Ignore. Finishing task.")
            installer.progress.emit(ProgressEntity.Finish)
            return
        }

        /*        installer.data = try {
                    resolveData(activity)
                } catch (e: Exception) {
                    Timber.e(e, "[id=${installer.id}] resolve: Failed to resolve data.")
                    installer.error = e
                    installer.progress.emit(ProgressEntity.ResolvedFailed)
                    return
                }*/
        // OPTIMIZATION: The caching logic is now integrated directly into the data resolution step.
        installer.data = try {
            resolveAndStabilizeData(activity)
        } catch (e: Exception) {
            Timber.e(e, "[id=${installer.id}] resolve: Failed to resolve and stabilize data.")
            installer.error = e
            installer.progress.emit(ProgressEntity.ResolvedFailed)
            return
        }

        Timber
            .d("[id=${installer.id}] resolve: Data resolved successfully (${installer.data.size} items). Emitting ProgressEntity.ResolveSuccess.")
        installer.progress.emit(ProgressEntity.ResolveSuccess)
    }

    private suspend fun analyse() {
        Timber.d("[id=${installer.id}] analyse: Starting. Emitting ProgressEntity.Analysing.")
        installer.progress.emit(ProgressEntity.Analysing)
        val rawAppEntities = runCatching {
            analyseEntities(installer.data)
        }.getOrElse {
            Timber.e(it, "[id=${installer.id}] analyse: Failed.")
            installer.error = it
            installer.progress.emit(ProgressEntity.AnalysedFailed)
            return
        }
        // --- Add a check here for empty results. ---
        if (rawAppEntities.isEmpty()) {
            Timber.w("[id=${installer.id}] analyse: Analysis resulted in an empty list. No valid apps found.")
            // You can either use a generic error or a more specific one.
            installer.error = AnalyseFailedAllFilesUnsupportedException("No valid files were found in the selection.")
            installer.progress.emit(ProgressEntity.AnalysedFailed)
            return
        }

        val containerType = rawAppEntities.firstOrNull()?.containerType
        val isMultiAppMode = containerType == DataType.MULTI_APK || containerType == DataType.MULTI_APK_ZIP
        // Process each package group separately
        val groupedByPackage = rawAppEntities.groupBy { it.packageName }
        val finalSelectEntities = mutableListOf<SelectInstallEntity>()

        if (isMultiAppMode) {
            // --- Multi App Mode (MULTI_APK / MULTI_APK_ZIP) ---
            Timber.d("[id=${installer.id}] analyse: Entering Multi-App Mode.")
            // Only consider BaseEntity for deduplication as they contain version info.
            val allBaseEntities = rawAppEntities.filterIsInstance<AppEntity.BaseEntity>()
            Timber.d("-> Found ${allBaseEntities.size} BaseEntities before deduplication.")

            // Create a composite key for uniqueness check.
            val uniqueBaseEntities =
                allBaseEntities.distinctBy { "${it.packageName}:${it.versionCode}:${it.versionName}:${it.arch?.arch}" }
            Timber.d("-> Found ${uniqueBaseEntities.size} BaseEntities after deduplication.")

            // The list to be processed is now the deduplicated list.
            val groupedByPackage = uniqueBaseEntities.groupBy { it.packageName }

            groupedByPackage.forEach { (packageName, itemsInGroup) ->

                if (itemsInGroup.isEmpty()) {
                    // If no base entities, add all items as selected (might be an unusual case)
                    finalSelectEntities.addAll(itemsInGroup.map { SelectInstallEntity(it, selected = true) })
                } else {
                    // Sort the base entities to find the best one.
                    val deviceAbis = RsConfig.supportedArchitectures.map { it.arch }
                    Timber.d("-> Device supported ABIs (priority order): ${deviceAbis.joinToString()}")

                    val sortedBases = itemsInGroup.sortedWith(
                        // Primary sort: best ABI match
                        compareBy<AppEntity.BaseEntity> {
                            val abiIndex = it.arch?.let { arch -> deviceAbis.indexOf(arch.arch) } ?: -1
                            if (abiIndex == -1) Int.MAX_VALUE else abiIndex // Unmatched ABIs go to the end
                        }
                            // Secondary sort: highest version code
                            .thenByDescending { it.versionCode }
                            // Tertiary sort: highest version name (as a fallback)
                            .thenByDescending { it.versionName }
                    )

                    val bestInGroup = sortedBases.firstOrNull()
                    Timber.d("-> For package '$packageName', best version found: ${bestInGroup?.versionName} (v${bestInGroup?.versionCode})")

                    // Mark only the best one as selected by default.
                    finalSelectEntities.addAll(itemsInGroup.map {
                        SelectInstallEntity(it, selected = (it == bestInGroup))
                    })
                }
            }
        } else {
            // --- Single App Mode (APK, APKS, APKM, etc.) ---
            Timber.d("[id=${installer.id}] analyse: Entering Single-App/Split Mode.")
            // Process each package group to create a complete list with default selections.
            groupedByPackage.forEach { (_, packageEntities) ->
                val splits = packageEntities.filterIsInstance<AppEntity.SplitEntity>()

                // Find the optimal splits for this package group. This set will be used to mark selections.
                val optimalSplits = if (splits.isNotEmpty()) findOptimalSplits(splits).toSet() else emptySet()

                // Map All AppEntity items to SelectInstallEntity, setting the 'selected' flag based on rules.
                val processedEntities = packageEntities.map { entity ->
                    val isSelected = when (entity) {
                        is AppEntity.BaseEntity,
                        is AppEntity.DexMetadataEntity -> true // Base and metadata are always selected as required.
                        is AppEntity.SplitEntity -> entity in optimalSplits // A split is selected if it's in the optimal set.
                        is AppEntity.CollectionEntity -> false // Should never reach here
                    }
                    SelectInstallEntity(app = entity, selected = isSelected)
                }
                finalSelectEntities.addAll(processedEntities)
            }
        }

        // Sort the final list for consistent ordering and assign it to the installer.
        installer.entities = finalSelectEntities.sortedWith(
            compareBy({ it.app.packageName }, { it.app.name })
        )

        val isNotificationInstall = installer.config.installMode == ConfigEntity.InstallMode.Notification ||
                installer.config.installMode == ConfigEntity.InstallMode.AutoNotification

        Timber.d("[id=${installer.id}] analyse: Analyse completed. isNotificationInstall=$isNotificationInstall, isMultiApkZip=$isMultiAppMode")
        if (isNotificationInstall && isMultiAppMode) {
            Timber.w("[id=${installer.id}] analyse: Multi-APK not supported in notification mode. Emitting AnalysedUnsupported.")
            installer.progress.emit(
                ProgressEntity.AnalysedUnsupported(context.getString(R.string.installer_current_install_mode_not_supported))
            )
        } else {
            Timber.d("[id=${installer.id}] analyse: Emitting ProgressEntity.AnalysedSuccess.")
            installer.progress.emit(ProgressEntity.AnalysedSuccess)
        }
    }

    private suspend fun install() {
        Timber.d("[id=${installer.id}] install: Starting. Emitting ProgressEntity.Installing.")
        installer.progress.emit(ProgressEntity.Installing)
        runCatching {
            Timber.d("[id=${installer.id}] install: Loading blacklist from AppDataStore.")
            val blacklist = appDataStore
                .getNamedPackageList(AppDataStore.MANAGED_BLACKLIST_PACKAGES_LIST)
                .first() // Gets the latest value from the Flow
                .map { it.packageName } // We only need the package name strings
            Timber.d("[id=${installer.id}] install: Blacklist loaded with ${blacklist.size} entries.")
            installEntities(
                installer.config,
                installer.entities.filter { it.selected }.map {
                    InstallEntity(
                        name = it.app.name,
                        packageName = it.app.packageName,
                        arch = it.app.arch,
                        data = when (val app = it.app) {
                            is AppEntity.BaseEntity -> app.data
                            is AppEntity.SplitEntity -> app.data
                            is AppEntity.DexMetadataEntity -> app.data
                            is AppEntity.CollectionEntity -> app.data
                        },
                        containerType = it.app.containerType!!
                    )
                },
                InstallExtraInfoEntity(Os.getuid() / 100000, cacheDirectory),
                blacklist
            )
        }.getOrElse {
            Timber.e(it, "[id=${installer.id}] install: Failed.")
            installer.error = it
            Timber.d("Caught exception, emitting InstallFailed state now.")
            installer.progress.emit(ProgressEntity.InstallFailed)
            return
        }
        Timber.d("[id=${installer.id}] install: Succeeded. Emitting ProgressEntity.InstallSuccess.")
        installer.progress.emit(ProgressEntity.InstallSuccess)
    }

    private suspend fun uninstall(packageName: String) {
        Timber.d("[id=${installer.id}] uninstall: Starting for $packageName. Emitting ProgressEntity.Uninstalling.")
        installer.progress.emit(ProgressEntity.Uninstalling)

        runCatching {
            // Call the dispatcher from the worker layer
            com.rosan.installer.data.app.model.impl.InstallerRepoImpl.doUninstallWork(
                installer.config,
                packageName,
                InstallExtraInfoEntity(Os.getuid() / 100000, cacheDirectory)
            )
        }.getOrElse {
            Timber.e(it, "[id=${installer.id}] uninstall: Failed for $packageName.")
            installer.error = it
            installer.progress.emit(ProgressEntity.UninstallFailed)
            return
        }

        Timber.d("[id=${installer.id}] uninstall: Succeeded for $packageName. Emitting ProgressEntity.UninstallSuccess.")
        installer.progress.emit(ProgressEntity.UninstallSuccess)
    }

    private suspend fun finish() {
        Timber.d("[id=${installer.id}] finish: Emitting ProgressEntity.Finish.")
        installer.progress.emit(ProgressEntity.Finish)
    }

    private suspend fun resolveConfig(activity: Activity): ConfigEntity {
        val packageName = activity.callingPackage
            ?: (activity.referrer?.host)
        var config = ConfigUtil.getByPackageName(packageName)
        if (config.installer == null) config = config.copy(
            installer = packageName
        )
        return config
    }

    /**
     * New function that combines resolving and stabilizing (caching) data in one step.
     *
     * @param activity The activity context to use for resolving data URIs.
     * @return A list of DataEntity objects representing the resolved and stabilized data.
     */
    private suspend fun resolveAndStabilizeData(activity: Activity): List<DataEntity> {
        val uris = resolveDataUris(activity)
        val data = mutableListOf<DataEntity>()
        for (uri in uris) {
            data.addAll(resolveDataUri(activity, uri))
        }
        return data
    }

    private fun resolveDataUris(activity: Activity): List<Uri> {
        val intent = activity.intent ?: throw ResolveException(
            action = null, uris = emptyList()
        )
        val intentAction = intent.action ?: throw ResolveException(
            action = null, uris = emptyList()
        )

        val uris = when (intentAction) {
            Intent.ACTION_SEND -> {
                val uri =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        intent.getParcelableExtra(
                            Intent.EXTRA_STREAM, Uri::class.java
                        )
                    else intent.getParcelableExtra(Intent.EXTRA_STREAM)
                if (uri == null) emptyList() else listOf(uri)
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    intent.getParcelableArrayListExtra(
                        Intent.EXTRA_STREAM, Uri::class.java
                    )
                else intent.getParcelableArrayListExtra(
                    Intent.EXTRA_STREAM
                )) ?: emptyList()
            }

            else -> {
                val uri = intent.data
                if (uri == null) emptyList()
                else listOf(uri)
            }
        }

        if (uris.isEmpty()) throw ResolveException(
            action = intentAction, uris = uris
        )
        return uris
    }

    /**
     * Resolves a data URI, which can be either a file URI or a content URI.
     *
     * @param activity The activity context to use for resolving content URIs.
     * @param uri The URI to resolve.
     * @return A list of DataEntity objects representing the resolved data.
     */
    private suspend fun resolveDataUri(activity: Activity, uri: Uri): List<DataEntity> {
        Timber.d("Source URI: $uri")
        if (uri.scheme == ContentResolver.SCHEME_FILE) return resolveDataFileUri(uri)
        return resolveDataContentFile(activity, uri)
    }

    /**
     * Resolves a file URI directly, assuming it points to a local file.
     * This is a simplified version that does not handle caching or progress.
     *
     * @param uri The file URI to resolve.
     * @return A list containing a single FileEntity with the file path.
     */
    private fun resolveDataFileUri(uri: Uri): List<DataEntity> {
        Timber.d("uri:$uri")
        val path = uri.path ?: throw Exception("can't get uri path: $uri")
        val data = DataEntity.FileEntity(path)
        data.source = DataEntity.FileEntity(path)
        return listOf(data)
    }

    /**
     * This function is now responsible for caching the file if direct access is not possible,
     * and it will emit progress updates.
     */
    private suspend fun resolveDataContentFile(
        activity: Activity,
        uri: Uri,
        retry: Int = 3
    ): List<DataEntity> {
        // HACK: wait for PermissionRecords ok.
        if (activity.checkCallingOrSelfUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED &&
            retry > 0
        ) {
            Thread.sleep(50)
            return resolveDataContentFile(activity, uri, retry - 1)
        }
        val assetFileDescriptor = context.contentResolver?.openAssetFileDescriptor(uri, "r")
            ?: throw IOException("can't open file descriptor: $uri")

        // Attempt to get a direct, readable file path first.
        val procPath = "/proc/${Os.getpid()}/fd/${assetFileDescriptor.parcelFileDescriptor.fd}"
        val file = File(procPath)
        val sourcePath = runCatching { Os.readlink(procPath).getRealPathFromUri(uri).pathUnify() }.getOrDefault("")

        // Check if the proc path is a valid, readable file. This is the zero-copy happy path.
        if (file.exists() && file.canRead()) {
            try {
                // We perform a more robust access test. RandomAccessFile will fail with an
                // IOException (like FileNotFoundException caused by EACCES) if we don't have
                // proper permissions, but it won't fail for a malformed zip content.
                RandomAccessFile(file, "r").use { } // Test open and immediately close.

                // If the test above passes, we have sufficient permission to use this path directly.
                if (sourcePath.startsWith('/')) {
                    Timber.d("Success! Got direct, usable file access through procfs: $sourcePath")
                    cacheParcelFileDescriptors.add(assetFileDescriptor.parcelFileDescriptor)
                    return listOf(DataEntity.FileEntity(procPath).apply {
                        source = DataEntity.FileEntity(sourcePath)
                    })
                }
            } catch (e: IOException) {
                // This will catch EACCES or other low-level IO errors.
                // THIS is the trigger for the stream fallback, as you requested.
                Timber.w(e, "Direct access test to /proc path failed. This will trigger the caching fallback.")
            }
        }

        // --- Fallback to Caching with Progress ---
        // If we reach here, it means we could not get a directly readable file path.
        // We MUST copy the file content to our cache. This handles all other cases,
        // including the one that was previously creating a FileDescriptorEntity.
        Timber.d("Direct file access failed. Falling back to caching with progress.")
        val tempFile = File.createTempFile(UUID.randomUUID().toString(), ".cache", File(cacheDirectory))
        val totalSize = assetFileDescriptor.declaredLength

        assetFileDescriptor.use { afd ->
            afd.createInputStream().use { input ->
                tempFile.outputStream().use { output ->
                    if (totalSize > 0) {
                        // We can show determinate progress.
                        Timber.d("Starting copy of $totalSize bytes.")
                        installer.progress.emit(ProgressEntity.Preparing(0f)) // Show progress bar at 0%
                        input.copyToWithProgress(output, totalSize) { progress ->
                            // Update progress in a coroutine to avoid blocking.
                            scope.launch { installer.progress.emit(ProgressEntity.Preparing(progress)) }
                        }
                    } else {
                        // Indeterminate progress.
                        Timber.d("Starting copy of unknown size.")
                        installer.progress.emit(ProgressEntity.Preparing(-1f)) // Use -1 for indeterminate
                        input.copyTo(output)
                    }
                }
            }
        }

        Timber.d("Caching complete. Temp file: ${tempFile.absolutePath}")
        // Return a FileEntity pointing to the newly cached file.
        return listOf(DataEntity.FileEntity(tempFile.absolutePath).apply {
            source = DataEntity.FileEntity(sourcePath) // Set original URI as source
        })
    }

    private suspend fun analyseEntities(data: List<DataEntity>): List<AppEntity> =
        AnalyserRepoImpl.doWork(installer.config, data, AnalyseExtraEntity(cacheDirectory))

    private suspend fun installEntities(
        config: ConfigEntity, entities: List<InstallEntity>, extra: InstallExtraInfoEntity, blacklist: List<String>
    ) = com.rosan.installer.data.app.model.impl.InstallerRepoImpl.doInstallWork(config, entities, extra, blacklist)

    private enum class SplitCategory { ABI, DENSITY, LANGUAGE, FEATURE }

    private data class ParsedSplit(
        val category: SplitCategory,
        val value: String, // The extracted value, e.g., "arm64-v8a", "xhdpi", "en"
        val entity: AppEntity.SplitEntity
    )

    /**
     * Parses a raw split name into a clean configuration value.
     * This logic is now robust against filenames containing dots.
     *
     * Example transformations:
     * - "split_config.arm64_v8a.apk" -> "arm64_v8a"
     * - "split_phonesky_data_loader.config.arm64_v8a.apk" -> "arm64_v8a"
     * - "split_config_zh.apk" -> "zh"
     * - "split_phonesky_data_loader.apk" -> "phonesky_data_loader"
     */
    private fun AppEntity.SplitEntity.getCleanConfigValue(): String {
        // Safely remove only the .apk suffix
        val nameWithoutApk = this.splitName.removeSuffix(".apk")

        // Remove the standard split_ prefix
        val strippedPrefix = nameWithoutApk.removePrefix("split_")

        // Extract the actual config value. This is the crucial part.
        // If ".config." exists, take the part after the last occurrence.
        // Otherwise, if it starts with "config.", remove that prefix.
        // If neither, use the whole string (for feature splits like "phonesky_data_loader").
        val configValue = if (strippedPrefix.contains(".config.")) {
            strippedPrefix.substringAfterLast(".config.")
        } else if (strippedPrefix.startsWith("config.")) {
            strippedPrefix.removePrefix("config.")
        } else {
            strippedPrefix
        }

        Timber.d("-> Parsing split '${this.splitName}': raw='${configValue}'")
        return configValue
    }

    /**
     * Finds the most suitable split APKs from a list based on the current device's configuration.
     * This version implements the final, correct selection logic for each category.
     *
     * @param splits The list of all available SplitEntity objects.
     * @return A list containing only the best-matching splits for each category.
     */
    private fun findOptimalSplits(splits: List<AppEntity.SplitEntity>): List<AppEntity.SplitEntity> {
        if (splits.isEmpty()) return emptyList()

        Timber.d("--- Begin findOptimalSplits for ${splits.size} splits ---")

        val isoLanguages = java.util.Locale.getISOLanguages().toSet()

        // Parse and categorize all splits
        val parsedSplits = splits.mapNotNull { split ->
            val rawConfigValue = split.getCleanConfigValue()
            if (rawConfigValue.isEmpty()) {
                Timber.w("Parsed empty config value from split: '${split.splitName}', skipping.")
                return@mapNotNull null
            }

            val category = when {
                Architecture.fromArchString(rawConfigValue) != Architecture.UNKNOWN -> SplitCategory.ABI
                Density.fromDensityString(rawConfigValue) != Density.UNKNOWN -> SplitCategory.DENSITY
                isoLanguages.contains(convertLegacyLanguageCode(rawConfigValue).substringBefore('-')) -> SplitCategory.LANGUAGE
                else -> SplitCategory.FEATURE
            }
            Timber.d("   > Categorized '$rawConfigValue' (from ${split.splitName}) as $category")

            val normalizedValue = rawConfigValue.replace('_', '-')
            ParsedSplit(category, normalizedValue, split)
        }

        val categorized = parsedSplits.groupBy { it.category }
        val optimalSplits = mutableListOf<AppEntity.SplitEntity>()

        // Select BEST ABI
        val abiSplitsGroups = categorized[SplitCategory.ABI]?.groupBy { it.value } ?: emptyMap()
        if (abiSplitsGroups.isNotEmpty()) {
            val deviceAbis = RsConfig.supportedArchitectures.map { it.arch }
            Timber.d("Device ABIs (priority order): $deviceAbis")
            Timber.d("Available ABI splits: ${abiSplitsGroups.keys}")

            // Find the single best ABI that is available
            val bestAbi = deviceAbis.firstOrNull { abi -> abiSplitsGroups.containsKey(abi) }

            if (bestAbi != null) {
                Timber.d("   -> Best ABI match is '$bestAbi'. Selecting all splits for this ABI.")
                // Add all splits for that best ABI ONLY
                abiSplitsGroups.getValue(bestAbi).forEach { optimalSplits.add(it.entity) }
            } else {
                Timber.w("No matching ABI splits found for this device's architectures.")
            }
        }

        // Select BEST Density
        val densitySplitsGroups = categorized[SplitCategory.DENSITY]?.groupBy { it.value } ?: emptyMap()
        if (densitySplitsGroups.isNotEmpty()) {
            val deviceDensities = RsConfig.supportedDensities.map { it.key }
            Timber.d("Device Densities (priority order): $deviceDensities")
            Timber.d("Available Density splits: ${densitySplitsGroups.keys}")

            val bestDensity = deviceDensities.firstOrNull { density -> densitySplitsGroups.containsKey(density) }

            if (bestDensity != null) {
                Timber.d("   -> Best Density match is '$bestDensity'. Selecting.")
                densitySplitsGroups.getValue(bestDensity).forEach { optimalSplits.add(it.entity) }
            }
            // Include 'nodpi' if it exists, as it's universal
            // TODO is there a nodpi split ?
            /*densitySplitsGroups["nodpi"]?.let {
                Timber.d("   -> Found 'nodpi' split(s). Selecting.")
                it.forEach { p -> optimalSplits.add(p.entity) }
            }*/
        }

        // Select BEST Language
        val langSplitsGroups =
            categorized[SplitCategory.LANGUAGE]?.groupBy { it.value.let(::convertLegacyLanguageCode) } ?: emptyMap()
        if (langSplitsGroups.isNotEmpty()) {
            val applicationLanguages = langSplitsGroups.keys
            Timber.d("Available Language splits: $applicationLanguages")
            val deviceLanguages = RsConfig.supportedLocales
            Timber.d("Device Languages (priority order): $deviceLanguages")
            var langFound = false
            // First, try for an exact match (e.g., 'zh-cn')
            for (lang in deviceLanguages) {
                val modernLang = convertLegacyLanguageCode(lang)
                if (langSplitsGroups.containsKey(modernLang)) {
                    Timber.d("   -> Found best Language match (exact): '$modernLang'. Selecting.")
                    langSplitsGroups.getValue(modernLang).forEach { optimalSplits.add(it.entity) }
                    langFound = true
                    break
                }
            }
            // If no exact match, try for a base language match (e.g., 'zh')
            if (!langFound) {
                for (lang in deviceLanguages) {
                    val baseLang = convertLegacyLanguageCode(lang).substringBefore('-')
                    if (langSplitsGroups.containsKey(baseLang)) {
                        Timber.d("   -> Found best Language match (base): '$baseLang'. Selecting.")
                        langSplitsGroups.getValue(baseLang).forEach { optimalSplits.add(it.entity) }
                        langFound = true
                        break
                    }
                }
            }
        }

        // ALWAYS include all feature splits
        categorized[SplitCategory.FEATURE]?.forEach {
            Timber.d("   -> Including feature split: '${it.value}'")
            optimalSplits.add(it.entity)
        }

        Timber.d("--- Finished findOptimalSplits. Total selected: ${optimalSplits.distinct().size} ---")
        val result = optimalSplits.distinct()
        result.forEach { Timber.d("   >> Final Selected: ${it.splitName}") }
        return result
    }

    /**
     * Custom extension function for InputStream to copy data to an OutputStream
     * while reporting progress.
     *
     * @param out The OutputStream to write to.
     * @param totalSize The total number of bytes to be copied.
     * @param onProgress A lambda that will be invoked with the current progress (0.0f to 1.0f).
     */
    private fun InputStream.copyToWithProgress(
        out: OutputStream,
        totalSize: Long,
        onProgress: (Float) -> Unit
    ) {
        var bytesCopied: Long = 0
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytes = read(buffer)
        while (bytes >= 0) {
            out.write(buffer, 0, bytes)
            bytesCopied += bytes
            onProgress(bytesCopied.toFloat() / totalSize)
            bytes = read(buffer)
        }
    }

    /**
     * Converts legacy Android language codes to their modern equivalents.
     * This logic is migrated from the original PackageUtil to ensure compatibility.
     *
     * @param code The legacy language code to convert.
     * @return The modern language code.
     * @see <a href="https://developer.android.com/reference/java/util/Locale#legacy-language-codes">java.util.Locale#legacy-language-codes</a>
     */
    private fun convertLegacyLanguageCode(code: String): String {
        return when (code) {
            "in" -> "id" // Indonesian
            "iw" -> "he" // Hebrew
            "ji" -> "yi" // Yiddish
            else -> code
        }
    }
}
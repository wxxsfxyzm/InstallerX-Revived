package com.rosan.installer.data.installer.model.impl.installer

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.system.Os
import androidx.core.net.toUri
import com.rosan.installer.R
import com.rosan.installer.build.Manufacturer
import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.app.model.entity.PackageAnalysisResult
import com.rosan.installer.data.app.model.exception.AnalyseFailedAllFilesUnsupportedException
import com.rosan.installer.data.app.model.impl.AnalyserRepoImpl
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.entity.UninstallInfo
import com.rosan.installer.data.installer.model.exception.ResolveException
import com.rosan.installer.data.installer.model.exception.ResolvedFailedNoInternetAccessException
import com.rosan.installer.data.installer.model.impl.InstallerRepoImpl
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.installer.util.getRealPathFromUri
import com.rosan.installer.data.installer.util.pathUnify
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.util.ConfigUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class ActionHandler(scope: CoroutineScope, installer: InstallerRepo) :
    Handler(scope, installer), KoinComponent {
    companion object {
        // Try a larger buffer. 1MB is a good starting point for modern devices.
        // It reduces the number of system calls for I/O operations.
        private const val COPY_BUFFER_SIZE = 1 * 1024 * 1024 // 1MB
    }

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
                    is InstallerRepoImpl.Action.ResolveInstall -> resolve(action.activity)
                    is InstallerRepoImpl.Action.Analyse -> analyse()
                    is InstallerRepoImpl.Action.Install -> install()
                    is InstallerRepoImpl.Action.ResolveUninstall -> resolveUninstall(
                        action.activity,
                        action.packageName
                    )

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
        installer.analysisResults = emptyList()
        installer.progress.emit(ProgressEntity.Ready) // Also reset progress

        Timber.d("[id=${installer.id}] resolve: State has been reset. Emitting ProgressEntity.Resolving.")
        installer.progress.emit(ProgressEntity.InstallResolving)

        installer.config = try {
            resolveConfig(activity)
        } catch (e: Exception) {
            Timber.e(e, "[id=${installer.id}] resolve: Failed to resolve config.")
            installer.error = e
            installer.progress.emit(ProgressEntity.InstallResolvedFailed)
            return
        }
        Timber.d("[id=${installer.id}] resolve: Config resolved. installMode=${installer.config.installMode}")

        // Check for notification mode immediately after resolving config.
        val isNotificationInstall = installer.config.installMode == ConfigEntity.InstallMode.Notification ||
                installer.config.installMode == ConfigEntity.InstallMode.AutoNotification

        if (isNotificationInstall) {
            Timber.d("[id=${installer.id}] Notification mode detected early. Switching to background.")
            // This will trigger the Activity to finish itself and the ForegroundInfoHandler to start showing notifications.
            installer.background(true)
        }

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
            installer.progress.emit(ProgressEntity.InstallResolvedFailed)
            return
        }

        Timber
            .d("[id=${installer.id}] resolve: Data resolved successfully (${installer.data.size} items). Emitting ProgressEntity.ResolveSuccess.")
        installer.progress.emit(ProgressEntity.InstallResolveSuccess)
    }

    private suspend fun analyse() {
        Timber.d("[id=${installer.id}] analyse: Starting. Emitting ProgressEntity.Analysing.")
        // --- Enforce a minimum duration for the "Analysing" state ---
        val startTime = System.currentTimeMillis()
        installer.progress.emit(ProgressEntity.InstallAnalysing)
        val analysisResults = runCatching {
            analyseEntities(installer.data)
        }.getOrElse {
            Timber.e(it, "[id=${installer.id}] analyse: Failed.")
            installer.error = it
            installer.progress.emit(ProgressEntity.InstallAnalysedFailed)
            return
        }
        // --- Add a check here for empty results. ---
        if (analysisResults.isEmpty()) {
            Timber.w("[id=${installer.id}] analyse: Analysis resulted in an empty list. No valid apps found.")
            installer.error = AnalyseFailedAllFilesUnsupportedException("No valid files were found in the selection.")
            installer.progress.emit(ProgressEntity.InstallAnalysedFailed)
            return
        }

        // This ensures the "Analysing" UI state is visible for at least a brief moment (e.g., 100ms),
        // providing a stable starting point for the transition animation, regardless of how fast the analysis is.
        val elapsedTime = System.currentTimeMillis() - startTime
        val minDuration = 100L // 100 milliseconds
        if (elapsedTime < minDuration) {
            delay(minDuration - elapsedTime)
        }

        // --- REFACTORED LOGIC ---
        // The complex logic for deduplication and optimal split selection has been moved to AnalyserRepoImpl.
        // This method now receives a complete, UI-ready result.
        // Its only job is to commit this result to the repository's state.

        // 1. Store the final, complete result in the repository. This is now the single source of truth.
        installer.analysisResults = analysisResults

        // 2. Perform final checks based on the result.
        // Get the containerType from the first entity in the result to check the session type.
        val containerType = analysisResults.firstOrNull()
            ?.appEntities?.firstOrNull()
            ?.app?.containerType

        val isMultiAppMode = containerType == DataType.MULTI_APK || containerType == DataType.MULTI_APK_ZIP

        val isNotificationInstall = installer.config.installMode == ConfigEntity.InstallMode.Notification ||
                installer.config.installMode == ConfigEntity.InstallMode.AutoNotification

        Timber.d("[id=${installer.id}] analyse: Analyse completed. isNotificationInstall=$isNotificationInstall, isMultiAppMode=$isMultiAppMode")

        if (isNotificationInstall && isMultiAppMode) {
            Timber.w("[id=${installer.id}] analyse: Multi-APK not supported in notification mode. Emitting AnalysedUnsupported.")
            installer.progress.emit(
                ProgressEntity.InstallAnalysedUnsupported(context.getString(R.string.installer_current_install_mode_not_supported))
            )
        } else {
            Timber.d("[id=${installer.id}] analyse: Emitting ProgressEntity.AnalysedSuccess.")
            installer.progress.emit(ProgressEntity.InstallAnalysedSuccess)
        }
    }

    private suspend fun install() {
        Timber.d("[id=${installer.id}] install: Starting. Emitting ProgressEntity.Installing.")
        installer.progress.emit(ProgressEntity.Installing)
        runCatching {
            // Load the blacklist by package name from AppDataStore.
            Timber.d("[id=${installer.id}] install: Loading package name blacklist from AppDataStore.")
            val packageBlacklist = appDataStore
                .getNamedPackageList(AppDataStore.MANAGED_BLACKLIST_PACKAGES_LIST)
                .first()
                .map { it.packageName }
            Timber.d("[id=${installer.id}] install: Package name blacklist loaded with ${packageBlacklist.size} entries.")

            // Load the blacklist by UID from AppDataStore.
            Timber.d("[id=${installer.id}] install: Loading SharedUID blacklist from AppDataStore.")
            val sharedUserIdBlacklist = appDataStore
                .getSharedUidList(AppDataStore.MANAGED_SHARED_USER_ID_BLACKLIST)
                .first()
                .map { it.uidName } // Extract the UID names to compare
            Timber.d("[id=${installer.id}] install: SharedUID blacklist loaded with ${sharedUserIdBlacklist.size} entries.")

            // Load the whitelist by UID from AppDataStore.
            Timber.d("[id=${installer.id}] install: Loading SharedUID whitelist from AppDataStore.")
            val sharedUserIdExemption = appDataStore
                .getNamedPackageList(
                    key = AppDataStore.MANAGED_SHARED_USER_ID_EXEMPTED_PACKAGES_LIST,
                    default = if (RsConfig.currentManufacturer == Manufacturer.XIAOMI) AppDataStore.defaultSharedUidExemptedPackagesForXiaoMi else emptyList()
                )
                .first()
                .map { it.packageName } // Extract the package names to compare
            Timber.d("[id=${installer.id}] install: SharedUID whitelist loaded with ${sharedUserIdExemption.size} entries.")

            // Read from the new 'analysisResults' data source.
            val entitiesToInstall = installer.analysisResults
                .flatMap { it.appEntities }  // 1. Flatten the list of packages into a single list of selectable entities.
                .filter { it.selected }      // 2. Filter for the ones the user has selected.
                .map {                       // 3. Map to the InstallEntity model
                    InstallEntity(
                        name = it.app.name,
                        packageName = it.app.packageName,
                        sharedUserId = if (it.app is AppEntity.BaseEntity) it.app.sharedUserId else null,
                        arch = it.app.arch,
                        data = when (val app = it.app) {
                            is AppEntity.BaseEntity -> app.data
                            is AppEntity.SplitEntity -> app.data
                            is AppEntity.DexMetadataEntity -> app.data
                            is AppEntity.CollectionEntity -> app.data
                        },
                        containerType = it.app.containerType!!
                    )
                }

            installEntities(
                installer.config,
                entitiesToInstall, // Pass the newly constructed list to the installer backend.
                InstallExtraInfoEntity(Os.getuid() / 100000, cacheDirectory),
                packageBlacklist,
                sharedUserIdBlacklist,
                sharedUserIdExemption
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

    /**
     * Resolves information for the package to be uninstalled and prepares for user confirmation.
     */
    private suspend fun resolveUninstall(activity: Activity, packageName: String) {
        Timber.d("[id=${installer.id}] resolveUninstall: Starting for $packageName.")

        installer.config = try {
            resolveConfig(activity)
        } catch (e: Exception) {
            Timber.e(e, "[id=${installer.id}] resolveUninstall: Failed to resolve config.")
            installer.error = e
            installer.progress.emit(ProgressEntity.UninstallResolveFailed)
            return
        }

        Timber.d("[id=${installer.id}] resolveUninstall: Config resolved. Authorizer is '${installer.config.authorizer}'")
        installer.progress.emit(ProgressEntity.UninstallResolving)

        try {
            val pm = context.packageManager
            val appInfo: ApplicationInfo = pm.getApplicationInfo(packageName, 0)

            val appLabel = pm.getApplicationLabel(appInfo).toString()
            val packageInfo = pm.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) packageInfo.longVersionCode else packageInfo.versionCode.toLong()
            val appIcon = pm.getApplicationIcon(packageName)

            val uninstallDetails = UninstallInfo(
                packageName = packageName,
                appLabel = appLabel,
                versionName = versionName,
                versionCode = versionCode,
                appIcon = appIcon
            )

            // Update the repo with the fetched information
            installer.uninstallInfo.update { uninstallDetails }

            Timber.d("[id=${installer.id}] resolveUninstall: Success. Emitting UninstallReady.")
            installer.progress.emit(ProgressEntity.UninstallReady)

        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e, "[id=${installer.id}] resolveUninstall: Failed. Package not found.")
            installer.error = e
            installer.progress.emit(ProgressEntity.UninstallResolveFailed)
        } catch (e: Exception) {
            Timber.e(e, "[id=${installer.id}] resolveUninstall: An unexpected error occurred.")
            installer.error = e
            installer.progress.emit(ProgressEntity.UninstallResolveFailed)
        }
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
                // Handle text/plain for URLs, otherwise handle as a file stream.
                if (intent.type?.startsWith("text/") == true) {
                    if (!RsConfig.isInternetAccessEnabled) {
                        Timber.d("Internet access is disabled in the app settings.")
                        throw ResolvedFailedNoInternetAccessException("No internet access to download files.")
                    }
                    val urlString = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (urlString.isNullOrBlank()) {
                        emptyList()
                    } else {
                        // Attempt to parse the received text as a URI.
                        runCatching { listOf(urlString.trim().toUri()) }.getOrDefault(emptyList())
                    }
                } else {
                    // Keep original logic for file streams.
                    val uri =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            intent.getParcelableExtra(
                                Intent.EXTRA_STREAM, Uri::class.java
                            )
                        else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    if (uri == null) emptyList() else listOf(uri)
                }
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
        // Route URI based on its scheme.
        return when (uri.scheme?.lowercase()) {
            ContentResolver.SCHEME_FILE -> resolveDataFileUri(uri)
            ContentResolver.SCHEME_CONTENT -> resolveDataContentFile(activity, uri)
            "http", "https" -> downloadAndCacheHttpUri(uri) // New case for HTTP/HTTPS links.
            else -> throw ResolveException(action = "Unsupported URI scheme: ${uri.scheme}", uris = listOf(uri))
        }
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

        // Aggressively guess file size and go to determinate progress
        val totalSize = guessContentLength(uri, assetFileDescriptor)

        withContext(Dispatchers.IO) {
            assetFileDescriptor.use { afd ->
                afd.createInputStream().use { input ->
                    tempFile.outputStream().use { output ->
                        if (totalSize > 0) {
                            installer.progress.emit(ProgressEntity.InstallPreparing(0f))
                            // Directly emit progress
                            input.copyToFastWithProgress(output, totalSize) { progress ->
                                installer.progress.tryEmit(
                                    ProgressEntity.InstallPreparing(progress)
                                )
                            }
                        } else {
                            // Unknown size: emit indeterminate progress
                            installer.progress.emit(ProgressEntity.InstallPreparing(-1f))
                            // Keep using a large buffer for all cases
                            val buf = ByteArray(COPY_BUFFER_SIZE)
                            var n = input.read(buf)
                            while (n >= 0) {
                                output.write(buf, 0, n)
                                n = input.read(buf)
                            }
                        }
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

    private suspend fun analyseEntities(data: List<DataEntity>): List<PackageAnalysisResult> =
        AnalyserRepoImpl.doWork(installer.config, data, AnalyseExtraEntity(cacheDirectory))

    private suspend fun installEntities(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    ) = com.rosan.installer.data.app.model.impl.InstallerRepoImpl.doInstallWork(
        config, entities, extra, blacklist, sharedUserIdBlacklist, sharedUserIdExemption
    )

    /**
     * Downloads a file from an HTTP/HTTPS URL, caches it locally, and reports progress.
     *
     * @param uri The HTTP/HTTPS URI to download.
     * @return A list containing a single DataEntity.FileEntity pointing to the cached file.
     * @throws IOException for network errors or if the server response is not successful.
     * @throws ResolveException if the link doesn't appear to be a direct download link for a supported file type.
     */
    private suspend fun downloadAndCacheHttpUri(uri: Uri): List<DataEntity> {
        Timber.d("[id=${installer.id}] Starting download for HTTP URI: $uri")

        // Emit initial progress state (indeterminate) as we don't know the file size yet.
        installer.progress.emit(ProgressEntity.InstallPreparing(-1f))

        val url = URL(uri.toString())
        val connection = url.openConnection() as HttpURLConnection
        // Set a reasonable timeout for connection and reading.
        connection.connectTimeout = 15000 // 15 seconds
        connection.readTimeout = 15000 // 15 seconds
        connection.requestMethod = "GET"
        connection.connect() // Executes the request

        try {
            // Check for successful HTTP response.
            if (connection.responseCode !in 200..299) {
                throw IOException("HTTP error ${connection.responseCode}: ${connection.responseMessage}")
            }

            // Validate if the URL points to a likely installer file.
            // First, check the URL path extension.
            val path = uri.path ?: ""
            val isSupportedExtension = listOf(".apk", ".xapk", ".apkm", ".apks").any { ext ->
                path.endsWith(ext, ignoreCase = true)
            }
            // As a fallback, check the Content-Type header from the server response.
            val contentType = connection.contentType
            val isSupportedMimeType = contentType != null && (
                    contentType.equals("application/vnd.android.package-archive", ignoreCase = true) ||
                            contentType.equals("application/octet-stream", ignoreCase = true) ||
                            contentType.equals("application/vnd.apkm", ignoreCase = true) ||
                            contentType.equals("application/vnd.apks", ignoreCase = true) ||
                            contentType.equals("application/xapk-package-archive", ignoreCase = true)
                    )
            if (!isSupportedExtension && !isSupportedMimeType) {
                throw ResolveException(
                    action = "Unsupported file type from URL. Path: $path, Content-Type: $contentType",
                    uris = listOf(uri)
                )
            }

            // Create a temporary file in the installer's specific cache directory.
            val tempFile = File.createTempFile(UUID.randomUUID().toString(), ".apk_cache", File(cacheDirectory))
            val totalSize = connection.contentLengthLong

            // Emit initial progress state. If size is unknown, it's indeterminate (-1f).
            // Otherwise, start at a determinate 0f.
            if (totalSize > 0) {
                Timber.d("Starting download of $totalSize bytes from URL.")
                installer.progress.emit(ProgressEntity.InstallPreparing(0f))
            } else {
                Timber.d("Starting download of unknown size from URL.")
                installer.progress.emit(ProgressEntity.InstallPreparing(-1f))
            }

            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    // Unified call to the fast copy method.
                    // It uses a large buffer for all cases and handles progress reporting
                    // only when totalSize is known.
                    input.copyToFastWithProgress(output, totalSize) { progress ->
                        // Use tryEmit for efficiency, it's non-suspending and suitable for frequent updates.
                        installer.progress.tryEmit(ProgressEntity.InstallPreparing(progress))
                    }
                }
            }

            Timber.d("URL download and caching complete. Cached file: ${tempFile.absolutePath}")

            // Return a FileEntity pointing to the newly cached file, which feeds into the analysis process.
            return listOf(DataEntity.FileEntity(tempFile.absolutePath))
        } finally {
            connection.disconnect() // Ensure the connection is always closed.
        }
    }

    /**
     * Faster copy
     *
     * @param minStepRatio
     * @param minIntervalMs
     */
    private fun InputStream.copyToFastWithProgress(
        out: OutputStream,
        totalSize: Long,
        minStepRatio: Float = 0.01f,
        minIntervalMs: Long = 200L,
        onProgress: (Float) -> Unit
    ) {
        var bytesCopied = 0L
        val buf = ByteArray(COPY_BUFFER_SIZE)
        val step = if (totalSize > 0) (totalSize * minStepRatio).toLong().coerceAtLeast(128 * 1024) else Long.MAX_VALUE
        var nextEmitAt = step
        var lastEmitTime = 0L

        var read = read(buf)
        while (read >= 0) {
            out.write(buf, 0, read)
            bytesCopied += read

            if (totalSize > 0) {
                val now = SystemClock.uptimeMillis()
                if (bytesCopied >= nextEmitAt && now - lastEmitTime >= minIntervalMs) {
                    onProgress(bytesCopied.toFloat() / totalSize)
                    lastEmitTime = now
                    nextEmitAt = (bytesCopied / step + 1) * step
                }
            }
            read = read(buf)
        }

        if (totalSize > 0) onProgress(1f)
    }

    /**
     * Guess the content length of a file.
     * 1) AssetFileDescriptor.declaredLength
     * 2) ParcelFileDescriptor.statSize
     * 3) ContentResolver.query(OpenableColumns.SIZE / DocumentsContract.Document.COLUMN_SIZE)
     * 4) Os.fstat(fd).st_size
     */
    private fun guessContentLength(uri: Uri, afd: android.content.res.AssetFileDescriptor?): Long {
        // 1) AFD 的 declaredLength
        if (afd != null && afd.declaredLength > 0) return afd.declaredLength

        // 2) PFD.statSize
        runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                if (pfd.statSize > 0) return pfd.statSize
            }
        }

        // 3) query OpenableColumns / DocumentsContract
        fun queryLong(projection: Array<String>): Long {
            context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    for (col in projection) {
                        val idx = c.getColumnIndex(col)
                        if (idx != -1) {
                            val v = c.getLong(idx)
                            if (v > 0) return v
                        }
                    }
                }
            }
            return -1L
        }

        val fromOpenable = queryLong(arrayOf(OpenableColumns.SIZE))
        if (fromOpenable > 0) return fromOpenable

        if (DocumentsContract.isDocumentUri(context, uri)) {
            val fromDoc = queryLong(arrayOf(DocumentsContract.Document.COLUMN_SIZE))
            if (fromDoc > 0) return fromDoc
        }

        // 4) fstat(fd)
        runCatching {
            afd?.parcelFileDescriptor?.fileDescriptor?.let { fd ->
                val st = Os.fstat(fd)
                if (st.st_size > 0) return st.st_size
            }
        }

        return -1L
    }

}
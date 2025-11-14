package com.rosan.installer.data.installer.model.impl.installer

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.system.Os
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.app.model.entity.PackageAnalysisResult
import com.rosan.installer.data.app.model.entity.RootImplementation
import com.rosan.installer.data.app.model.exception.AnalyseFailedAllFilesUnsupportedException
import com.rosan.installer.data.app.model.impl.AnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.ModuleInstallerRepoImpl
import com.rosan.installer.data.installer.model.entity.ConfirmationDetails
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.entity.UninstallInfo
import com.rosan.installer.data.installer.model.exception.ResolveException
import com.rosan.installer.data.installer.model.exception.ResolvedFailedNoInternetAccessException
import com.rosan.installer.data.installer.model.impl.InstallerRepoImpl
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.installer.util.getRealPathFromUri
import com.rosan.installer.data.installer.util.pathUnify
import com.rosan.installer.data.recycle.util.setInstallerDefaultPrivileged
import com.rosan.installer.data.recycle.util.useUserService
import com.rosan.installer.data.reflect.repo.ReflectRepo
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.util.ConfigUtil
import com.rosan.installer.util.isSystemInstaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
import java.lang.reflect.Field
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
    private val reflect by inject<ReflectRepo>()
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
                    is InstallerRepoImpl.Action.ResolveConfirmInstall -> resolveConfirmInstall(action.activity, action.sessionId)
                    is InstallerRepoImpl.Action.ApproveSession -> approveSession(action.sessionId, action.granted)
                    is InstallerRepoImpl.Action.Finish -> finish()
                }
            }
        }
    }

    override suspend fun onFinish() {
        Timber.d("[id=${installer.id}] onFinish: Cleaning up resources and cancelling job.")
        clearCacheDirectory()
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

        val uris = try {
            resolveDataUris(activity)
        } catch (e: Exception) {
            Timber.e(e, "[id=${installer.id}] resolve: Could not resolve any URIs from the intent.")
            installer.error = e
            installer.progress.emit(ProgressEntity.InstallResolvedFailed)
            return
        }

        Timber.d("[id=${installer.id}] resolve: URIs resolved successfully (${uris.size}).")

        var forceDialogMode = false
        if (uris.size > 1 || uris.any { it.path?.endsWith(".zip", ignoreCase = true) == true }) {
            Timber.d("[id=${installer.id}] resolve: Batch share (count=${uris.size}) or module file detected. Will force install mode to Dialog.")
            forceDialogMode = true
        }

        installer.config = try {
            resolveConfig(activity)
        } catch (e: Exception) {
            Timber.e(e, "[id=${installer.id}] resolve: Failed to resolve config.")
            installer.error = e
            installer.progress.emit(ProgressEntity.InstallResolvedFailed)
            return
        }
        Timber.d("[id=${installer.id}] resolve: Config resolved. installMode=${installer.config.installMode}")

        if (forceDialogMode) {
            Timber.d("[id=${installer.id}] resolve: Forcing install mode to Dialog (overriding config).")
            installer.config.installMode = ConfigEntity.InstallMode.Dialog
        }

        // Launch in a separate coroutine to avoid blocking the UI.
        scope.launch {
            if (appDataStore.getBoolean(AppDataStore.AUTO_LOCK_INSTALLER).first()) runCatching {
                Timber.d("[id=${installer.id}] resolve: Attempting to auto-lock default installer.")
                setInstallerDefaultPrivileged(
                    context,
                    installer.config,
                    true
                )
                Timber.d("[id=${installer.id}] resolve: Auto-lock attempt finished successfully.")
            }.onFailure {
                Timber.w(it, "[id=${installer.id}] resolve: Failed to auto-lock default installer. This is non-fatal.")
            }
        }

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

        // The caching logic is now integrated directly into the data resolution step.
        installer.data = try {
            resolveAndStabilizeData(activity, uris)
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

        Timber.d("[id=${installer.id}] analyse: Emitting ProgressEntity.AnalysedSuccess.")
        installer.progress.emit(ProgressEntity.InstallAnalysedSuccess)

    }

    private suspend fun install() {
        Timber.d("[id=${installer.id}] install: Starting.")

        // Get all entities marked for installation from the analysis results.
        val entitiesToInstall = installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }

        if (entitiesToInstall.isEmpty()) {
            Timber.w("[id=${installer.id}] install: No entities selected for installation. Finishing.")
            installer.progress.emit(ProgressEntity.InstallFailed) // Or a more specific error
            installer.error = IllegalStateException("No items were selected for installation.")
            return
        }

        val firstEntity = entitiesToInstall.first().app

        if (firstEntity is AppEntity.ModuleEntity) {
            installModule(firstEntity)
        } else {
            installApp()
        }
    }

    private suspend fun installModule(moduleEntity: AppEntity.ModuleEntity) {
        Timber.d("[id=${installer.id}] installModule: Starting module installation for ${moduleEntity.name}")
        val banner = """
             ___           _        _ _         __  __ 
            |_ _|_ __  ___| |_ __ _| | | ___ _ _\ \/ / 
             | || '_ \/ __| __/ _` | | |/ _ \ '__\  / 
             | || | | \__ \ || (_| | | |  __/ |  /  \ 
            |___|_| |_|___/\__\__,_|_|_|\___|_| /_/\_\ 

             ____            _               _ 
            |  _ \ _____   _(_)_   _____  __| | 
            | |_) / _ \ \ / / \ \ / / _ \/ _` | 
            |  _ <  __/\ V /| |\ V /  __/ (_| | 
            |_| \_\___| \_/ |_| \_/ \___|\__,_| 

            Starting installation...
        """.trimIndent()
        // Initialize the output list with the banner lines
        val outputLines = banner.lines().toMutableList()

        // Emit the initial state with the full banner
        installer.progress.emit(ProgressEntity.InstallingModule(outputLines.toList()))

        try {
            // Get the selected Root Implementation from settings.
            val rootImplementation = appDataStore.getString(AppDataStore.LAB_ROOT_IMPLEMENTATION)
                .map { RootImplementation.fromString(it) }
                .first()

            // Call the module installer backend, which returns a Flow.
            val moduleInstallerFlow = ModuleInstallerRepoImpl.doInstallWork(
                installer.config,
                moduleEntity,
                rootImplementation
            )

            // Collect the flow of output lines.
            moduleInstallerFlow.collect { line ->
                outputLines.add(line)
                // Emit the new progress state with the updated list of logs.
                installer.progress.emit(ProgressEntity.InstallingModule(outputLines.toList()))
            }

            // If the flow completes without an exception, it's a success.
            Timber.d("[id=${installer.id}] installModule: Succeeded. Emitting ProgressEntity.InstallSuccess.")
            installer.progress.emit(ProgressEntity.InstallSuccess)

        } catch (e: Exception) {
            Timber.e(e, "[id=${installer.id}] installModule: Failed.")
            installer.error = e
            installer.progress.emit(ProgressEntity.InstallFailed) // Then emit failure state
        }
    }

    private suspend fun installApp() {
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
                .getNamedPackageList(key = AppDataStore.MANAGED_SHARED_USER_ID_EXEMPTED_PACKAGES_LIST)
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
                            is AppEntity.ModuleEntity -> app.data
                        },
                        containerType = it.app.containerType!!
                    )
                }

            val targetUserId = if (installer.config.enableCustomizeUser) {
                // Use UserId from profile if enableCustomizeUser is true
                Timber.d("Custom user is enabled. Installing for user: ${installer.config.targetUserId}")
                installer.config.targetUserId
            } else {
                // Otherwise, use the current userId instead
                val currentUserId = Os.getuid() / 100000
                Timber.d("Custom user is disabled. Installing for current user: $currentUserId")
                currentUserId
            }
            val sessionContainerType = entitiesToInstall.firstOrNull()?.containerType
            val isSingleSession =
                sessionContainerType == DataType.APK || sessionContainerType == DataType.APKS || sessionContainerType == DataType.APKM || sessionContainerType == DataType.XAPK

            installEntities(
                installer.config,
                entitiesToInstall, // Pass the newly constructed list to the installer backend.
                InstallExtraInfoEntity(targetUserId, cacheDirectory),
                packageBlacklist,
                sharedUserIdBlacklist,
                sharedUserIdExemption
            )

            if (isSingleSession) {
                Timber.d("[id=${installer.id}] Single-app install succeeded. Clearing cache now.")
                clearCacheDirectory()
            } else {
                Timber.d("[id=${installer.id}] Multi-app install step succeeded. Deferring cache cleanup.")
            }
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

    private suspend fun resolveConfirmInstall(activity: Activity, sessionId: Int) {
        Timber.d("[id=${installer.id}] resolveConfirmInstall: Starting for session $sessionId.")
        installer.progress.emit(ProgressEntity.InstallResolving)

        try {
            // Assure using the correct global authorizer
            installer.config.authorizer = ConfigUtil.getGlobalAuthorizer()

            // TODO implement Notification Confirmation
            /* val isNotificationMode = installer.config.installMode == ConfigEntity.InstallMode.Notification ||
                    installer.config.installMode == ConfigEntity.InstallMode.AutoNotification

            if (isNotificationMode) {
                Timber.w("[id=${installer.id}] CONFIRM_INSTALL received in notification mode. This is unsupported. Rejecting and finishing.")
                approveSession(sessionId, false) // Auto refuse
                return
            }*/

            var finalLabel: CharSequence?
            var finalIcon: Bitmap?

            when {
                // Use reflect directly when running as system
                context.isSystemInstaller() -> {
                    Timber.d("[id=${installer.id}] Handling CONFIRM_INSTALL as system installer.")
                    val (label, icon) = getSessionDetailsLocally(sessionId)
                    finalLabel = label
                    finalIcon = icon
                }

                // Use PrivilegedService for Root, Shizuku(Root), and Customize
                installer.config.authorizer == ConfigEntity.Authorizer.Root ||
                        installer.config.authorizer == ConfigEntity.Authorizer.Shizuku ||
                        installer.config.authorizer == ConfigEntity.Authorizer.Customize -> {
                    Timber.d("[id=${installer.id}] Handling CONFIRM_INSTALL using ${installer.config.authorizer} service.")
                    var detailsBundle: Bundle? = null
                    useUserService(installer.config) { userService ->
                        detailsBundle = userService.privileged.getSessionDetails(sessionId)
                    }

                    if (detailsBundle == null) {
                        Timber.e("[id=${installer.id}] getSessionDetails() failed via ${installer.config.authorizer}.")
                        throw Exception("Failed to get session details from privileged service.")
                    }

                    finalLabel = detailsBundle.getCharSequence("appLabel")
                    val iconBytes = detailsBundle.getByteArray("appIcon")
                    finalIcon = iconBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                }

                // Directly close when authorizer is other than above
                else -> {
                    Timber.w("[id=${installer.id}] Received CONFIRM_INSTALL with unsupported authorizer (${installer.config.authorizer}). Finishing.")
                    installer.progress.emit(ProgressEntity.Finish)
                    return
                }
            }
            // Update repo state
            installer.confirmationDetails.value = ConfirmationDetails(
                sessionId,
                finalLabel ?: "N/A",
                finalIcon
            )
            installer.progress.emit(ProgressEntity.InstallAnalysedSuccess)
        } catch (e: Exception) {
            Timber.e(e, "[id=${installer.id}] resolveConfirmInstall: Failed.")
            installer.error = e
            installer.progress.emit(ProgressEntity.InstallResolvedFailed)
        }
    }

    private suspend fun approveSession(sessionId: Int, granted: Boolean) {
        Timber.d("[id=${installer.id}] approveSession: $granted for session $sessionId")
        try {
            when {
                context.isSystemInstaller() -> {
                    Timber.d("[id=${installer.id}] Approving session as system installer.")
                    approveSessionLocally(sessionId, granted)
                }

                installer.config.authorizer == ConfigEntity.Authorizer.Root ||
                        installer.config.authorizer == ConfigEntity.Authorizer.Shizuku ||
                        installer.config.authorizer == ConfigEntity.Authorizer.Customize -> {
                    Timber.d("[id=${installer.id}] Approving session using ${installer.config.authorizer} service.")
                    useUserService(installer.config) { userService ->
                        userService.privileged.approveSession(sessionId, granted)
                    }
                }

                else -> {
                    Timber.w("[id=${installer.id}] approveSession called with unsupported authorizer (${installer.config.authorizer}). Ignoring.")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "[id=${installer.id}] approveSession: Failed.")
            installer.error = e
        } finally {
            // Close whatever the result is
            installer.progress.emit(ProgressEntity.Finish)
        }
    }

    /**
     * Execute session detail retrieval locally in the app process (only when acting as the system package manager).
     */
    private fun getSessionDetailsLocally(sessionId: Int): Pair<CharSequence, Bitmap?> {
        val packageInstaller = context.packageManager.packageInstaller
        val sessionInfo = packageInstaller.getSessionInfo(sessionId)
            ?: throw Exception("Local getSessionInfo failed for id $sessionId")

        var resolvedLabel: CharSequence? = null
        var resolvedIcon: Bitmap? = null
        var path: String? = null

        try {
            val resolvedField: Field? = reflect.getDeclaredField(
                sessionInfo::class.java, "resolvedBaseCodePath"
            )
            if (resolvedField != null) {
                resolvedField.isAccessible = true
                path = resolvedField.get(sessionInfo) as? String
            }
        } catch (e: Exception) {
            Timber.e(e, "Local reflection for 'resolvedBaseCodePath' failed")
        }

        if (!path.isNullOrEmpty()) {
            try {
                val pkgInfo = context.packageManager.getPackageArchiveInfo(
                    path, PackageManager.GET_PERMISSIONS
                )
                val appInfo = pkgInfo?.applicationInfo
                if (appInfo != null) {
                    appInfo.publicSourceDir = path
                    resolvedLabel = appInfo.loadLabel(context.packageManager)
                    val drawableIcon = appInfo.loadIcon(context.packageManager)
                    if (drawableIcon != null) {
                        resolvedIcon = (drawableIcon as? BitmapDrawable)?.bitmap
                            ?: drawableIcon.toBitmap(
                                drawableIcon.intrinsicWidth.coerceAtLeast(1),
                                drawableIcon.intrinsicHeight.coerceAtLeast(1)
                            )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Local APK parsing failed")
            }
        }

        val finalLabel = resolvedLabel ?: sessionInfo.appLabel ?: "N/A"
        val finalIcon = resolvedIcon ?: sessionInfo.appIcon
        return Pair(finalLabel, finalIcon)
    }

    /**
     * Execute session approval locally in the app process (only when acting as the system package manager).
     */
    private fun approveSessionLocally(sessionId: Int, granted: Boolean) {
        try {
            val packageInstaller = context.packageManager.packageInstaller
            val method = reflect.getMethod(
                packageInstaller::class.java,
                "setPermissionsResult",
                Int::class.java,
                Boolean::class.java
            )

            if (method != null) {
                method.invoke(packageInstaller, sessionId, granted)
            } else {
                throw NoSuchMethodException("setPermissionsResult not found via ReflectRepo")
            }
        } catch (e: Exception) {
            Timber.e(e, "Local approveSession failed")
            if (!granted) {
                try {
                    context.packageManager.packageInstaller.abandonSession(sessionId)
                } catch (e2: Exception) {
                    Timber.e(e2, "Local fallback abandonSession failed")
                }
            }
        }
    }

    private suspend fun finish() {
        Timber.d("[id=${installer.id}] finish: Emitting ProgressEntity.Finish.")
        installer.progress.emit(ProgressEntity.Finish)
    }

    private val SYSTEM_URI_AUTHORITIES = setOf(
        "media", // Common MediaStore authority
        "com.android.providers.media.module", // Media provider on modern Android
        "com.android.providers.downloads", // Legacy download manager
        "com.android.externalstorage.documents" // External storage provider
        // Add any other generic authorities users might encounter
    )

    private suspend fun resolveConfig(activity: Activity): ConfigEntity {
        Timber.tag("InstallSource").d("[id=${installer.id}] resolveConfig: Starting.")

        // --- Method 1: The original attempt (fast but often null) ---
        val callingPackage = activity.callingPackage
        Timber.tag("InstallSource").d("activity.callingPackage: $callingPackage")
        if (callingPackage != null) {
            return getConfigForPackage(callingPackage)
        }

        // --- Method 2: Check the referrer (with scheme validation) ---
        val referrer = activity.referrer
        Timber.tag("InstallSource").d("activity.referrer: $referrer")
        if (referrer?.scheme == "android-app" && referrer.host != null) {
            val referrerPackage = referrer.host
            Timber.tag("InstallSource").d("Valid app referrer found: $referrerPackage")
            return getConfigForPackage(referrerPackage)
        } else if (referrer != null) {
            Timber.tag("InstallSource").w("Ignoring referrer with non-app scheme: ${referrer.scheme}")
        }

        // --- Method 3: The most robust fallback - Inspect the Intent's data URI authority ---
        val intent = activity.intent
        val authority = intent.data?.authority ?: intent.clipData?.getItemAt(0)?.uri?.authority
        Timber.tag("InstallSource").d("URI authority: $authority")

        if (authority != null) {
            // FINAL CHECK: If the authority is a generic system provider, treat it as unknown.
            // Otherwise, we can trust it as the source app's provider.
            if (authority in SYSTEM_URI_AUTHORITIES) {
                Timber.tag("InstallSource").w("Authority '$authority' is a generic system provider. Using default config.")
                return getConfigForPackage(null) // Use default/global config
            } else {
                // This is a specific app's FileProvider, extract the package name.
                val authorityPackage = extractPackageFromAuthority(authority)
                Timber.tag("InstallSource").d("Package from app-specific authority: $authorityPackage")
                return getConfigForPackage(authorityPackage)
            }
        }

        // --- Fallback: If all methods fail, return a default config ---
        Timber.tag("InstallSource").w("Could not determine calling package. Using default config.")
        return getConfigForPackage(null) // Or your preferred default behavior
    }

    /**
     * A helper function to create the config.
     * Passing null will result in the global default config.
     */
    private suspend fun getConfigForPackage(packageName: String?): ConfigEntity {
        // getByPackageName should be designed to return the default config if packageName is null.
        val config = ConfigUtil.getByPackageName(packageName)
        Timber.tag("InstallSource").d("Resolved config for '${packageName ?: "default"}': $config")
        return config
    }

    /**
     * Extracts the base package name from a FileProvider authority.
     */
    private fun extractPackageFromAuthority(authority: String): String {
        return authority.removeSuffix(".FileProvider")
            .removeSuffix(".provider")
    }


    /**
     * New function that combines resolving and stabilizing (caching) data in one step.
     *
     * @param activity The activity context to use for resolving data URIs.
     * @return A list of DataEntity objects representing the resolved and stabilized data.
     */
    private suspend fun resolveAndStabilizeData(activity: Activity, uris: List<Uri>): List<DataEntity> {
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
        // Wait for PermissionRecords ok.
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
            val isSupportedExtension = listOf(".apk", ".xapk", ".apkm", ".apks", ".zip").any { ext ->
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
    private fun guessContentLength(uri: Uri, afd: AssetFileDescriptor?): Long {
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

    /**
     * Clears all cached file descriptors and deletes the temporary cache directory.
     * This method is idempotent and safe to call multiple times.
     */
    private fun clearCacheDirectory() {
        Timber.d("[id=${installer.id}] clearCacheDirectory: Clearing cache...")

        // Close and clear cached file descriptors
        cacheParcelFileDescriptors.forEach { it.runCatching { close() } }
        cacheParcelFileDescriptors.clear()

        // Delete the cache directory recursively
        File(cacheDirectory).runCatching {
            if (exists()) {
                val deleted = deleteRecursively()
                Timber.d("[id=${installer.id}] Cache directory deleted ($cacheDirectory): $deleted")
            } else {
                Timber.d("[id=${installer.id}] Cache directory not found, already cleared: $cacheDirectory")
            }
        }.onFailure {
            Timber.w(it, "[id=${installer.id}] Failed to delete cache directory: $cacheDirectory")
        }
    }
}
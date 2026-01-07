// 文件: NoneInstallerRepoImpl.kt
package com.rosan.installer.data.app.model.impl.appInstaller

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.provider.Settings
import androidx.core.net.toUri
import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.app.model.enums.DataType
import com.rosan.installer.data.app.model.exception.InstallFailedMissingInstallPermissionException
import com.rosan.installer.data.app.repo.InstallerRepo
import com.rosan.installer.data.app.util.PackageManagerUtil
import com.rosan.installer.data.app.util.sourcePath
import com.rosan.installer.data.recycle.util.deletePaths
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.IOException

object NoneInstallerRepoImpl : InstallerRepo, KoinComponent {
    private val context by inject<Context>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("RequestInstallPackagesPolicy")
    override suspend fun doInstallWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    ) {
        val result = runCatching {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = "package:${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                throw InstallFailedMissingInstallPermissionException("Please make sure you have granted \"Install unknown apps\" permission!")
            }

            val packageInstaller = context.packageManager.packageInstaller
            var session: PackageInstaller.Session? = null

            try {
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                entities.firstOrNull()?.packageName?.let { params.setAppPackageName(it) }

                val sessionId = packageInstaller.createSession(params)
                session = packageInstaller.openSession(sessionId)

                for (entity in entities) {
                    session.openWrite(entity.name, 0, -1).use { outputStream ->
                        entity.data.getInputStreamWhileNotEmpty()?.use { inputStream ->
                            inputStream.copyTo(outputStream)
                            session.fsync(outputStream)
                        } ?: throw IOException("Failed to open input stream for ${entity.name}")
                    }
                }

                val receiver = IBinderInstallerRepoImpl.LocalIntentReceiver()
                session.commit(receiver.getIntentSender())

                PackageManagerUtil.installResultVerify(context, receiver)

            } catch (e: Exception) {
                session?.abandon()
                throw e
            } finally {
                session?.close()
            }
        }

        doFinishWork(config, entities, extra, result)
        // If the installation failed, re-throw the exception to notify the caller.
        result.onFailure {
            throw it
        }
    }

    private fun doFinishWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extraInfo: InstallExtraInfoEntity,
        result: Result<Unit>
    ) {
        Timber.tag("doFinishWork").d("isSuccess: ${result.isSuccess}")
        if (result.isSuccess) {
            // Enable autoDelete only when the containerType is not MULTI_APK_ZIP.
            if (config.autoDelete && entities.first().sourceType != DataType.MULTI_APK_ZIP) {
                Timber.tag("doFinishWork").d("autoDelete is enabled, starting delete work.")
                // Launch a background coroutine to handle file deletion.
                coroutineScope.launch {
                    // Use runCatching to prevent deletion errors from crashing the app.
                    runCatching {
                        onDeleteWork(entities)
                    }.onFailure {
                        Timber.e(it, "An error occurred during the onDeleteWork execution.")
                    }
                }
            }
        }
    }

    /**
     * Handles the actual file deletion using standard, non-privileged APIs.
     */
    private fun onDeleteWork(
        entities: List<InstallEntity>,
    ) {
        // Extract the source file paths from the install entities.
        val pathsToDelete = entities.sourcePath()
        if (pathsToDelete.isEmpty()) {
            Timber.tag("onDeleteWork").w("No source paths found to delete.")
            return
        }

        Timber.tag("onDeleteWork").d("Attempting to delete paths: ${pathsToDelete.joinToString()}")
        // Use the robust deletePaths utility function which handles errors gracefully.
        deletePaths(pathsToDelete)
    }

    override suspend fun doUninstallWork(
        config: ConfigEntity,
        packageName: String,
        extra: InstallExtraInfoEntity
    ) {
        val result = runCatching {
            // Get the standard PackageInstaller
            val packageInstaller = context.packageManager.packageInstaller

            // Create a receiver to handle the result (Reusing the one from IBinderInstallerRepoImpl as you did in doInstallWork)
            val receiver = IBinderInstallerRepoImpl.LocalIntentReceiver()

            // Request uninstallation using the standard API
            // This will trigger a system dialog asking the user to confirm uninstallation
            packageInstaller.uninstall(packageName, receiver.getIntentSender())

            // Block and wait for the result verification
            PackageManagerUtil.uninstallResultVerify(context, receiver)
        }

        // Handle result failure if needed, similar to install logic
        result.onFailure {
            throw it
        }
    }

    override suspend fun approveSession(config: ConfigEntity, sessionId: Int, granted: Boolean) {
        throw UnsupportedOperationException("Session Approve is not supported in None authorizer")
    }
}
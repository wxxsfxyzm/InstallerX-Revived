// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appInstaller

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.provider.Settings
import androidx.core.net.toUri
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.data.engine.executor.PackageManagerUtil
import com.rosan.installer.domain.engine.exception.InstallException
import com.rosan.installer.domain.engine.model.DataType
import com.rosan.installer.domain.engine.model.InstallEntity
import com.rosan.installer.domain.engine.model.InstallErrorType
import com.rosan.installer.domain.engine.model.sourcePath
import com.rosan.installer.domain.engine.repository.AppInstallerRepository
import com.rosan.installer.domain.privileged.model.PostInstallTaskInfo
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException

class NoneAppInstallerRepoImpl(
    private val context: Context,
    private val reflect: ReflectionProvider,
    private val postInstallTaskProvider: PostInstallTaskProvider
) : AppInstallerRepository {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("RequestInstallPackagesPolicy")
    override suspend fun doInstallWork(
        config: ConfigModel,
        entities: List<InstallEntity>,
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
                throw InstallException(
                    InstallErrorType.BLACKLISTED_PACKAGE,
                    "Please make sure you have granted \"Install unknown apps\" permission!"
                )
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

                val receiver = LocalIntentReceiver(reflect)
                session.commit(receiver.getIntentSender())

                PackageManagerUtil.installResultVerify(context, receiver)

            } catch (e: Exception) {
                session?.abandon()
                throw e
            } finally {
                session?.close()
            }
        }

        doFinishWork(config, entities, result)
        // If the installation failed, re-throw the exception to notify the caller.
        result.onFailure {
            throw it
        }
    }

    private fun doFinishWork(
        config: ConfigModel,
        entities: List<InstallEntity>,
        result: Result<Unit>
    ) {
        Timber.tag("doFinishWork").d("isSuccess: ${result.isSuccess}")
        if (result.isSuccess) {
            val packageName = entities.firstOrNull()?.packageName ?: return

            // Determine if the source type is capable of being deleted
            val isDeleteCapable = entities.firstOrNull()?.sourceType !in listOf(
                DataType.MULTI_APK_ZIP,
                DataType.MIXED_MODULE_APK,
                DataType.MIXED_MODULE_ZIP
            )

            val shouldDelete = config.autoDelete && (isDeleteCapable || config.autoDeleteZip)
            val pathsToDelete = if (shouldDelete) entities.sourcePath().toList() else emptyList()

            // Launch a background coroutine to handle post-install tasks (like deletion).
            coroutineScope.launch {
                runCatching {
                    postInstallTaskProvider.executeTasks(
                        authorizer = Authorizer.None,
                        customizeAuthorizer = config.customizeAuthorizer,
                        info = PostInstallTaskInfo(
                            packageName = packageName,
                            enableDexopt = config.enableManualDexopt,
                            dexoptMode = config.dexoptMode.value,
                            forceDexopt = config.forceDexopt,
                            enableAutoDelete = shouldDelete,
                            deletePaths = pathsToDelete
                        )
                    )
                }.onFailure { e ->
                    Timber.e(e, "Async post-install tasks failed")
                }
            }
        }
    }

    override suspend fun doUninstallWork(
        config: ConfigModel,
        packageName: String
    ) {
        val result = runCatching {
            // Get the standard PackageInstaller
            val packageInstaller = context.packageManager.packageInstaller

            // Instantiate the receiver
            val receiver = LocalIntentReceiver(reflect)

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

    override suspend fun approveSession(config: ConfigModel, sessionId: Int, granted: Boolean) {
        throw UnsupportedOperationException("Session Approve is not supported in None authorizer")
    }
}

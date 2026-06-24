package com.rosan.installer.data.engine.executor.moduleinstaller

import com.rosan.installer.ICommandOutputListener
import com.rosan.installer.data.engine.executor.ModuleInstallerUtils
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.exception.ModuleInstallException
import com.rosan.installer.domain.engine.model.error.ModuleInstallErrorType
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.repository.ModuleInstallerRepository
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.model.preferences.RootMode
import com.rosan.installer.framework.privileged.util.useUserService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

/**
 * A module installer that works by executing shell commands via a REMOTE privileged process (Binder).
 */
class ShizukuModuleInstallerRepoImpl(
    private val capabilityProvider: DeviceCapabilityProvider
) : ModuleInstallerRepository {

    override fun doInstallWork(
        config: ConfigModel,
        module: AppEntity.ModuleEntity,
        useRoot: Boolean,
        rootMode: RootMode
    ): Flow<String> = callbackFlow {
        // 1. Resolve Path using Helper
        val modulePath = ModuleInstallerUtils.getModulePathOrThrow(module)

        // 2. Get Command Array using Helper (Best for IPC/exec)
        val command = ModuleInstallerUtils.getInstallCommandArgs(rootMode, modulePath)

        Timber.d("Executing remote module install command via IPC: ${command.joinToString(" ")}")

        val listener = object : ICommandOutputListener.Stub() {
            override fun onOutput(line: String) {
                trySend(line)
            }

            override fun onError(line: String) {
                trySend(line)
            }

            override fun onComplete(exitCode: Int) {
                if (exitCode == 0) {
                    close()
                } else {
                    close(
                        ModuleInstallException(
                            errorType = ModuleInstallErrorType.EXIT_CODE_NON_ZERO,
                            message = "Remote command failed with exit code $exitCode"
                        )
                    )
                }
            }
        }

        try {
            useUserService(
                isSystemApp = capabilityProvider.isSystemApp,
                authorizer = config.authorizer
            ) { userService ->
                userService.privileged.execArrWithCallback(command, listener)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initiate remote module installation.")
            close(
                ModuleInstallException(
                    errorType = ModuleInstallErrorType.CMD_INIT_FAILED,
                    message = "Failed to initiate remote command: ${e.message}",
                    cause = e
                )
            )
        }

        awaitClose {
            Timber.d("Remote module installation flow cancelled.")
        }
    }
}

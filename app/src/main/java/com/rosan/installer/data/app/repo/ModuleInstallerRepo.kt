package com.rosan.installer.data.app.repo

import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.model.RootImplementation
import kotlinx.coroutines.flow.Flow

/**
 * Interface for module installation operations.
 */
interface ModuleInstallerRepo {
    /**
     * Installs a module and returns a Flow of output lines.
     * The flow will emit each line of stdout/stderr as a String.
     * It completes successfully if the exit code is 0, otherwise it emits a ModuleInstallException.
     */
    fun doInstallWork(
        config: ConfigModel,
        module: AppEntity.ModuleEntity,
        useRoot: Boolean,
        rootImplementation: RootImplementation
    ): Flow<String> // Return a Flow of strings
}
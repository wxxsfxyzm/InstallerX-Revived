package com.rosan.installer.data.app.repo

import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.enums.RootImplementation
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
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
        config: ConfigEntity,
        module: AppEntity.ModuleEntity,
        useRoot: Boolean,
        rootImplementation: RootImplementation
    ): Flow<String> // Return a Flow of strings
}
package com.rosan.installer.data.app.util

import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.enums.RootImplementation
import com.rosan.installer.data.app.model.exception.ModuleInstallException

object ModuleInstallerUtils {
    /**
     * Resolves the module path or throws an exception if not found.
     */
    fun getModulePathOrThrow(module: AppEntity.ModuleEntity): String {
        return module.data.sourcePath()
            ?: throw ModuleInstallException("Could not resolve module file path for ${module.name}")
    }

    /**
     * Returns the raw command arguments for installing a module.
     * @return An array of strings representing the command and its arguments.
     */
    fun getInstallCommandArgs(rootImplementation: RootImplementation, modulePath: String): Array<String> {
        return when (rootImplementation) {
            RootImplementation.Magisk -> arrayOf("magisk", "--install-module", modulePath)
            RootImplementation.KernelSU -> arrayOf("ksud", "module", "install", modulePath)
            RootImplementation.APatch -> arrayOf("apd", "module", "install", modulePath)
        }
    }

    /**
     * Converts raw arguments into a shell-safe command string.
     * Specifically quotes the file path to prevent shell expansion issues.
     */
    fun buildShellCommandString(rootImplementation: RootImplementation, modulePath: String): String {
        // We manually quote the path for safety when running in "su -c"
        // Escaping double quotes inside the path just in case
        val safePath = "\"${modulePath.replace("\"", "\\\"")}\""

        return when (rootImplementation) {
            RootImplementation.Magisk -> "magisk --install-module $safePath"
            RootImplementation.KernelSU -> "ksud module install $safePath"
            RootImplementation.APatch -> "apd module install $safePath"
        }
    }
}
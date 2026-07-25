// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.executor

import com.rosan.installer.domain.engine.exception.ModuleInstallException
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.error.ModuleInstallErrorType
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.settings.model.preferences.RootMode
import java.io.File

object ModuleInstallerUtils {
    /**
     * Resolves the local path passed directly to the selected root implementation.
     *
     * Magisk, KernelSU, and APatch accept a local absolute file path rather than a content URI or
     * retained file descriptor. Descriptor-backed modules are materialized before this point, so
     * installation must use the current backing file rather than the original source identity.
     */
    fun getModulePathOrThrow(module: AppEntity.ModuleEntity): String {
        val data = module.data as? DataEntity.FileEntity
        val localFile = data
            ?.takeUnless { it is DataEntity.FileDescriptorEntity }
            ?.let { File(it.path) }
            ?.takeIf { it.isAbsolute && it.isFile }
        return localFile?.absolutePath
            ?: throw ModuleInstallException(
                errorType = ModuleInstallErrorType.GENERIC_FAILED,
                message = "Could not resolve a local module file for ${module.name}"
            )
    }

    /**
     * Returns the raw command arguments for installing a module.
     * @return An array of strings representing the command and its arguments.
     */
    fun getInstallCommandArgs(rootMode: RootMode, modulePath: String): Array<String> =
        when (rootMode) {
            RootMode.Magisk -> arrayOf("magisk", "--install-module", modulePath)
            RootMode.KernelSU -> arrayOf("ksud", "module", "install", modulePath)
            RootMode.APatch -> arrayOf("apd", "module", "install", modulePath)
            RootMode.None -> throw IllegalStateException("Cannot install module in None mode")
        }

    /**
     * Converts raw arguments into a shell-safe command string.
     * Specifically quotes the file path to prevent shell expansion issues.
     */
    fun buildShellCommandString(rootMode: RootMode, modulePath: String): String {
        // We manually quote the path for safety when running in "su -c"
        // Escaping double quotes inside the path just in case
        val safePath = "\"${modulePath.replace("\"", "\\\"")}\""

        return when (rootMode) {
            RootMode.Magisk -> "magisk --install-module $safePath"
            RootMode.KernelSU -> "ksud module install $safePath"
            RootMode.APatch -> "apd module install $safePath"
            RootMode.None -> throw IllegalStateException("Cannot install module in None mode")
        }
    }
}

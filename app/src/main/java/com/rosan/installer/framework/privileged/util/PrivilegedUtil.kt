// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.framework.privileged.util

import com.rosan.installer.domain.settings.model.config.Authorizer
import timber.log.Timber
import java.io.File

const val SHELL_ROOT = "su"
const val SHELL_SYSTEM = "su 1000"
const val SHELL_SH = "sh"

const val SU_ARGS = "-M"

const val SHELL_COMMAND_PLACEHOLDER = "{command}"

data class ShellCommand(val parts: List<String>) {
    init {
        require(parts.isNotEmpty()) { "Shell command must not be empty." }
        require(parts.none { it.isBlank() }) { "Shell command parts must not be blank." }
    }

    override fun toString(): String = parts.joinToString(" ") { part ->
        if (part.any(Char::isWhitespace)) "'${part.replace("'", "'\\''")}'" else part
    }

    companion object {
        fun of(vararg parts: String): ShellCommand = ShellCommand(parts.toList())

        fun parse(command: String): ShellCommand {
            val parts = mutableListOf<String>()
            val current = StringBuilder()
            var quote: Char? = null
            var escaping = false

            fun push() {
                if (current.isEmpty()) return
                parts += current.toString()
                current.clear()
            }

            for (char in command.trim()) {
                when {
                    escaping -> {
                        current.append(char)
                        escaping = false
                    }

                    char == '\\' -> escaping = true

                    quote != null -> {
                        if (char == quote) quote = null else current.append(char)
                    }

                    char == '\'' || char == '"' -> quote = char

                    char.isWhitespace() -> push()

                    else -> current.append(char)
                }
            }

            if (escaping) current.append('\\')
            push()

            return ShellCommand(parts)
        }
    }
}

sealed interface AppProcessTerminal {
    data object Root : AppProcessTerminal
    data object RootSystem : AppProcessTerminal
    data class Customize(val command: ShellCommand) : AppProcessTerminal
}

private const val DELETE_TAG = "DELETE_PATH"

fun deletePaths(paths: List<String>) {
    for (path in paths) {
        val file = File(path)

        Timber.tag(DELETE_TAG).d("Processing path for deletion: $path")

        try {
            if (file.exists()) {
                if (file.deleteRecursively()) {
                    Timber.tag(DELETE_TAG).d("Successfully deleted: $path")
                } else {
                    Timber.tag(DELETE_TAG).w("Failed to delete: $path. Check for permissions or lock issues.")
                }
            } else {
                Timber.tag(DELETE_TAG).d("File/Directory does not exist, no action needed: $path")
            }
        } catch (e: SecurityException) {
            Timber.tag(DELETE_TAG).e(e, "SecurityException on deleting $path. Permission denied.")
        } catch (e: Exception) {
            Timber.tag(DELETE_TAG).e(e, "An unexpected error occurred while processing $path")
        }
    }
}

/**
 * Helper to generate the special auth command (e.g. "su 1000") for Root mode.
 * This ensures different methods reuse the same 'su 1000' service process.
 */
fun getSpecialAuth(
    authorizer: Authorizer,
    specialAuth: AppProcessTerminal = AppProcessTerminal.RootSystem
): (() -> AppProcessTerminal?)? =
    if (authorizer == Authorizer.Root) {
        { specialAuth }
    } else null

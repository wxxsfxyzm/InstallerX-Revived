// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.framework.privileged.core.infrastructure.process

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

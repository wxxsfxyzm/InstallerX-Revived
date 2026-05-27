package com.rosan.installer.domain.settings.model.preferences

enum class RootMode {
    Magisk,
    KernelSU,
    APatch,
    None;

    companion object {
        /**
         * Converts a string to a RootMode enum, defaulting to Magisk if the string is invalid.
         * @param value The string to convert.
         * @return The corresponding RootImplementation instance.
         */
        fun fromString(value: String?): RootMode = try {
            value?.let { valueOf(it) } ?: Magisk
        } catch (_: IllegalArgumentException) {
            Magisk
        }
    }
}

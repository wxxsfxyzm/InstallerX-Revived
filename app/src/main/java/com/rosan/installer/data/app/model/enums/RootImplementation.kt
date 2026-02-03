package com.rosan.installer.data.app.model.enums

enum class RootImplementation {
    Magisk,
    KernelSU,
    APatch;

    companion object {
        /**
         * Converts a string to a RootImplementation enum, defaulting to Magisk if the string is invalid.
         * @param value The string to convert.
         * @return The corresponding RootImplementation instance.
         */
        fun fromString(value: String?): RootImplementation = try {
            value?.let { valueOf(it) } ?: Magisk
        } catch (e: IllegalArgumentException) {
            Magisk
        }
    }
}
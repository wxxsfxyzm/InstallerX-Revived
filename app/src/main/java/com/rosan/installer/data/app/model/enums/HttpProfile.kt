package com.rosan.installer.data.app.model.enums

enum class HttpProfile {
    ALLOW_ALL,
    ALLOW_LOCAL,
    ALLOW_SECURE;

    companion object {
        /**
         * Safely converts a string to HttpProfile.
         * Defaults to ALLOW_SECURE if the value is null or invalid.
         */
        fun fromString(value: String?): HttpProfile = try {
            value?.let { valueOf(it) } ?: ALLOW_SECURE
        } catch (e: IllegalArgumentException) {
            ALLOW_SECURE
        }
    }
}
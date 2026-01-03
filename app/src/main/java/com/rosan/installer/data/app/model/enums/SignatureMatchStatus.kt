package com.rosan.installer.data.app.model.enums

/**
 * Represents the result of comparing the signature of a new APK
 * with an already installed version of the same app.
 */
enum class SignatureMatchStatus {
    /** The package is not currently installed on the device. */
    NOT_INSTALLED,

    /** The signatures match. It's a safe update. */
    MATCH,

    /** The signatures do NOT match. This is a potential security risk. */
    MISMATCH,

    /** Could not determine the signature for either the APK or the installed app. */
    UNKNOWN_ERROR
}
package com.rosan.installer.data.app.model.enums

import com.rosan.installer.R

/**
 * Enumeration mapping legacy install error codes to their respective string resources.
 */
enum class InstallErrorType(val legacyCode: Int, val stringResId: Int) {
    ALREADY_EXISTS(-1, R.string.exception_install_failed_already_exists),
    INVALID_APK(-2, R.string.exception_install_failed_invalid_apk),
    INVALID_URI(-3, R.string.exception_install_failed_invalid_uri),
    INSUFFICIENT_STORAGE(-4, R.string.exception_install_failed_insufficient_storage),
    DUPLICATE_PACKAGE(-5, R.string.exception_install_failed_duplicate_package),
    NO_SHARED_USER(-6, R.string.exception_install_failed_no_shared_user),
    UPDATE_INCOMPATIBLE(-7, R.string.exception_install_failed_update_incompatible),
    SHARED_USER_INCOMPATIBLE(-8, R.string.exception_install_failed_shared_user_incompatible),
    MISSING_SHARED_LIBRARY(-9, R.string.exception_install_failed_missing_shared_library),
    REPLACE_COULDNT_DELETE(-10, R.string.exception_install_failed_replace_couldnt_delete),
    DEXOPT(-11, R.string.exception_install_failed_dexopt),
    OLDER_SDK(-12, R.string.exception_install_failed_older_sdk),
    CONFLICTING_PROVIDER(-13, R.string.exception_install_failed_conflicting_provider),
    NEWER_SDK(-14, R.string.exception_install_failed_newer_sdk),
    TEST_ONLY(-15, R.string.exception_install_failed_test_only),
    CPU_ABI_INCOMPATIBLE(-16, R.string.exception_install_failed_cpu_abi_incompatible),
    MISSING_FEATURE(-17, R.string.exception_install_failed_missing_feature),
    CONTAINER_ERROR(-18, R.string.exception_install_failed_container_error),
    INVALID_INSTALL_LOCATION(-19, R.string.exception_install_failed_invalid_install_location),
    MEDIA_UNAVAILABLE(-20, R.string.exception_install_failed_media_unavailable),
    VERIFICATION_TIMEOUT(-21, R.string.exception_install_failed_verification_timeout),
    VERIFICATION_FAILURE(-22, R.string.exception_install_failed_verification_failure),
    PACKAGE_CHANGED(-23, R.string.exception_install_failed_package_changed),
    UID_CHANGED(-24, R.string.exception_install_failed_uid_changed),
    VERSION_DOWNGRADE(-25, R.string.exception_install_failed_version_downgrade),
    MISSING_SPLIT(-28, R.string.exception_install_failed_missing_split),
    DEPRECATED_SDK_VERSION(-29, R.string.exception_install_failed_deprecated_sdk_version),
    PARSE_FAILED_UNEXPECTED_EXCEPTION(-102, R.string.exception_install_parse_failed_unexpected_exception),
    PARSE_FAILED_NO_CERTIFICATES(-103, R.string.exception_install_parse_failed_no_certificates),
    INTERNAL_ERROR(-110, R.string.exception_install_failed_unknown),
    USER_RESTRICTED(-111, R.string.exception_install_failed_user_restricted),
    DUPLICATE_PERMISSION(-112, R.string.exception_install_failed_duplicate_permission),
    NO_MATCHING_ABIS(-113, R.string.exception_install_failed_cpu_abi_incompatible),
    ABORTED(-115, R.string.exception_install_failed_aborted),
    PARSE_FAILED_SKIPPED(-125, R.string.exception_install_parse_failed_skipped),
    BLACK_LIST(-903, R.string.exception_install_failed_origin_os_blacklist),
    HYPEROS_ISOLATION_VIOLATION(-1000, R.string.exception_install_failed_hyperos_isolation_violation),
    REJECTED_BY_BUILDTYPE(-3001, R.string.exception_install_failed_rejected_by_build_type),

    // --- Custom Internal Errors (Positive Codes) ---
    BLACKLISTED_PACKAGE(1, R.string.exception_install_failed_blacklisted_package),
    MISSING_INSTALL_PERMISSION(2, R.string.exception_install_failed_missing_install_permission),

    // Fallback for unknown status codes
    UNKNOWN(Int.MAX_VALUE, R.string.exception_install_failed_unknown);

    companion object {
        fun fromLegacyCode(code: Int): InstallErrorType {
            return entries.find { it.legacyCode == code } ?: UNKNOWN
        }
    }
}
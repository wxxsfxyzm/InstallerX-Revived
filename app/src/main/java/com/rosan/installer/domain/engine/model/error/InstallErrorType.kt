// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.error

import android.content.pm.PackageManagerHidden
import androidx.annotation.StringRes
import com.rosan.installer.R

/**
 * Enumeration mapping legacy install error codes to their respective string resources.
 */
enum class InstallErrorType(val legacyCode: Int, @param:StringRes val stringResId: Int) {
    // --- Legacy Install Error Codes From Package Manager Service ---
    ALREADY_EXISTS(PackageManagerHidden.INSTALL_FAILED_ALREADY_EXISTS, R.string.exception_install_failed_already_exists),
    INVALID_APK(PackageManagerHidden.INSTALL_FAILED_INVALID_APK, R.string.exception_install_failed_invalid_apk),
    INVALID_URI(PackageManagerHidden.INSTALL_FAILED_INVALID_URI, R.string.exception_install_failed_invalid_uri),
    INSUFFICIENT_STORAGE(PackageManagerHidden.INSTALL_FAILED_INSUFFICIENT_STORAGE, R.string.exception_install_failed_insufficient_storage),
    DUPLICATE_PACKAGE(PackageManagerHidden.INSTALL_FAILED_DUPLICATE_PACKAGE, R.string.exception_install_failed_duplicate_package),
    NO_SHARED_USER(PackageManagerHidden.INSTALL_FAILED_NO_SHARED_USER, R.string.exception_install_failed_no_shared_user),
    UPDATE_INCOMPATIBLE(PackageManagerHidden.INSTALL_FAILED_UPDATE_INCOMPATIBLE, R.string.exception_install_failed_update_incompatible),
    SHARED_USER_INCOMPATIBLE(PackageManagerHidden.INSTALL_FAILED_SHARED_USER_INCOMPATIBLE, R.string.exception_install_failed_shared_user_incompatible),
    MISSING_SHARED_LIBRARY(PackageManagerHidden.INSTALL_FAILED_MISSING_SHARED_LIBRARY, R.string.exception_install_failed_missing_shared_library),
    REPLACE_COULDNT_DELETE(PackageManagerHidden.INSTALL_FAILED_REPLACE_COULDNT_DELETE, R.string.exception_install_failed_replace_couldnt_delete),
    DEXOPT(PackageManagerHidden.INSTALL_FAILED_DEXOPT, R.string.exception_install_failed_dexopt),
    OLDER_SDK(PackageManagerHidden.INSTALL_FAILED_OLDER_SDK, R.string.exception_install_failed_older_sdk),
    CONFLICTING_PROVIDER(PackageManagerHidden.INSTALL_FAILED_CONFLICTING_PROVIDER, R.string.exception_install_failed_conflicting_provider),
    NEWER_SDK(PackageManagerHidden.INSTALL_FAILED_NEWER_SDK, R.string.exception_install_failed_newer_sdk),
    TEST_ONLY(PackageManagerHidden.INSTALL_FAILED_TEST_ONLY, R.string.exception_install_failed_test_only),
    CPU_ABI_INCOMPATIBLE(PackageManagerHidden.INSTALL_FAILED_CPU_ABI_INCOMPATIBLE, R.string.exception_install_failed_cpu_abi_incompatible),
    MISSING_FEATURE(PackageManagerHidden.INSTALL_FAILED_MISSING_FEATURE, R.string.exception_install_failed_missing_feature),
    CONTAINER_ERROR(PackageManagerHidden.INSTALL_FAILED_CONTAINER_ERROR, R.string.exception_install_failed_container_error),
    INVALID_INSTALL_LOCATION(PackageManagerHidden.INSTALL_FAILED_INVALID_INSTALL_LOCATION, R.string.exception_install_failed_invalid_install_location),
    MEDIA_UNAVAILABLE(PackageManagerHidden.INSTALL_FAILED_MEDIA_UNAVAILABLE, R.string.exception_install_failed_media_unavailable),
    VERIFICATION_TIMEOUT(PackageManagerHidden.INSTALL_FAILED_VERIFICATION_TIMEOUT, R.string.exception_install_failed_verification_timeout),
    VERIFICATION_FAILURE(PackageManagerHidden.INSTALL_FAILED_VERIFICATION_FAILURE, R.string.exception_install_failed_verification_failure),
    PACKAGE_CHANGED(PackageManagerHidden.INSTALL_FAILED_PACKAGE_CHANGED, R.string.exception_install_failed_package_changed),
    UID_CHANGED(PackageManagerHidden.INSTALL_FAILED_UID_CHANGED, R.string.exception_install_failed_uid_changed),
    VERSION_DOWNGRADE(PackageManagerHidden.INSTALL_FAILED_VERSION_DOWNGRADE, R.string.exception_install_failed_version_downgrade),
    INSTALL_FAILED_PERMISSION_MODEL_DOWNGRADE(PackageManagerHidden.INSTALL_FAILED_PERMISSION_MODEL_DOWNGRADE, R.string.exception_install_failed_permission_model_downgrade),
    INSTALL_FAILED_SANDBOX_VERSION_DOWNGRADE(PackageManagerHidden.INSTALL_FAILED_SANDBOX_VERSION_DOWNGRADE, R.string.exception_install_failed_sandbox_version_downgrade),
    MISSING_SPLIT(PackageManagerHidden.INSTALL_FAILED_MISSING_SPLIT, R.string.exception_install_failed_missing_split),
    DEPRECATED_SDK_VERSION(PackageManagerHidden.INSTALL_FAILED_DEPRECATED_SDK_VERSION, R.string.exception_install_failed_deprecated_sdk_version),
    PARSE_FAILED_UNEXPECTED_EXCEPTION(PackageManagerHidden.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, R.string.exception_install_parse_failed_unexpected_exception),
    PARSE_FAILED_NO_CERTIFICATES(PackageManagerHidden.INSTALL_PARSE_FAILED_NO_CERTIFICATES, R.string.exception_install_parse_failed_no_certificates),
    PARSE_FAILED_BAD_SHARED_USER_ID(PackageManagerHidden.INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID, R.string.exception_install_parse_failed_bad_shared_user_id),
    INSTALL_PARSE_FAILED_MANIFEST_EMPTY(PackageManagerHidden.INSTALL_PARSE_FAILED_MANIFEST_EMPTY, R.string.exception_install_parse_failed_manifest_empty),
    INTERNAL_ERROR(PackageManagerHidden.INSTALL_FAILED_INTERNAL_ERROR, R.string.exception_uninstall_failed_internal_error),
    USER_RESTRICTED(PackageManagerHidden.INSTALL_FAILED_USER_RESTRICTED, R.string.exception_install_failed_user_restricted),
    DUPLICATE_PERMISSION(PackageManagerHidden.INSTALL_FAILED_DUPLICATE_PERMISSION, R.string.exception_install_failed_duplicate_permission),
    NO_MATCHING_ABIS(PackageManagerHidden.INSTALL_FAILED_NO_MATCHING_ABIS, R.string.exception_install_failed_cpu_abi_incompatible),
    ABORTED(PackageManagerHidden.INSTALL_FAILED_ABORTED, R.string.exception_install_failed_aborted),
    PARSE_FAILED_SKIPPED(PackageManagerHidden.INSTALL_PARSE_FAILED_SKIPPED, R.string.exception_install_parse_failed_skipped),
    INSTALL_FAILED_DUPLICATE_PERMISSION_GROUP(PackageManagerHidden.INSTALL_FAILED_DUPLICATE_PERMISSION_GROUP, R.string.exception_install_failed_duplicate_permission_group),
    INSTALL_FAILED_BAD_PERMISSION_GROUP(PackageManagerHidden.INSTALL_FAILED_BAD_PERMISSION_GROUP, R.string.exception_install_failed_bad_permission_group),
    INSTALL_FAILED_PRE_APPROVAL_NOT_AVAILABLE(PackageManagerHidden.INSTALL_FAILED_PRE_APPROVAL_NOT_AVAILABLE, R.string.exception_install_failed_pre_approval_not_available),
    INSTALL_FAILED_MULTI_ARCH_NOT_MATCH_ALL_NATIVE_ABIS(PackageManagerHidden.INSTALL_FAILED_MULTI_ARCH_NOT_MATCH_ALL_NATIVE_ABIS, R.string.exception_install_failed_multi_arch_not_match_all_native_abis),
    BLACK_LIST(-903, R.string.exception_install_failed_origin_os_blacklist),
    HYPEROS_ISOLATION_VIOLATION(-1000, R.string.exception_install_failed_hyperos_isolation_violation),
    REJECTED_BY_BUILDTYPE(-3001, R.string.exception_install_failed_rejected_by_build_type),
    INSTALL_FAILED_OEM_FRP_LOCKED(-9477, R.string.exception_install_failed_oem_frp_locked),

    // --- Custom Internal Errors (Positive Codes) ---
    BLACKLISTED_PACKAGE(1, R.string.exception_install_failed_blacklisted_package),
    MISSING_INSTALL_PERMISSION(2, R.string.exception_install_failed_missing_install_permission),
    BLOCKED_BY_PROFILE(3, R.string.exception_install_failed_blocked_by_profile),
    ALL_AUTHORIZERS_FAILED(4, R.string.exception_install_failed_all_authorizers_failed),
    BLOCKED_BY_PROFILE_SIGNATURE_MISMATCH(5, R.string.exception_install_failed_blocked_by_profile_signature_mismatch),
    BLOCKED_BY_PROFILE_SIGNATURE_UNKNOWN(6, R.string.exception_install_failed_blocked_by_profile_signature_unknown),

    // --- Fallback for unknown status codes ---
    UNKNOWN(Int.MAX_VALUE, R.string.exception_install_failed_unknown);

    companion object {
        fun fromLegacyCode(code: Int) = entries.find { it.legacyCode == code } ?: UNKNOWN
    }
}

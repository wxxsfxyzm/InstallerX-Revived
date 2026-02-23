package com.rosan.installer.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.data.app.model.exception.AnalyseFailedAllFilesUnsupportedException
import com.rosan.installer.data.app.model.exception.AuthenticationFailedException
import com.rosan.installer.data.app.model.exception.InstallFailedAbortedException
import com.rosan.installer.data.app.model.exception.InstallFailedAlreadyExistsException
import com.rosan.installer.data.app.model.exception.InstallFailedBlacklistedPackageException
import com.rosan.installer.data.app.model.exception.InstallFailedConflictingProviderException
import com.rosan.installer.data.app.model.exception.InstallFailedContainerErrorException
import com.rosan.installer.data.app.model.exception.InstallFailedCpuAbiIncompatibleException
import com.rosan.installer.data.app.model.exception.InstallFailedDeprecatedSdkVersion
import com.rosan.installer.data.app.model.exception.InstallFailedDexOptException
import com.rosan.installer.data.app.model.exception.InstallFailedDuplicatePackageException
import com.rosan.installer.data.app.model.exception.InstallFailedDuplicatePermissionException
import com.rosan.installer.data.app.model.exception.InstallFailedHyperOSIsolationViolationException
import com.rosan.installer.data.app.model.exception.InstallFailedInsufficientStorageException
import com.rosan.installer.data.app.model.exception.InstallFailedInvalidAPKException
import com.rosan.installer.data.app.model.exception.InstallFailedInvalidInstallLocationException
import com.rosan.installer.data.app.model.exception.InstallFailedInvalidURIException
import com.rosan.installer.data.app.model.exception.InstallFailedMediaUnavailableException
import com.rosan.installer.data.app.model.exception.InstallFailedMissingFeatureException
import com.rosan.installer.data.app.model.exception.InstallFailedMissingInstallPermissionException
import com.rosan.installer.data.app.model.exception.InstallFailedMissingSharedLibraryException
import com.rosan.installer.data.app.model.exception.InstallFailedMissingSplitException
import com.rosan.installer.data.app.model.exception.InstallFailedNewerSDKException
import com.rosan.installer.data.app.model.exception.InstallFailedNoSharedUserException
import com.rosan.installer.data.app.model.exception.InstallFailedOlderSdkException
import com.rosan.installer.data.app.model.exception.InstallFailedOriginOSBlacklistException
import com.rosan.installer.data.app.model.exception.InstallFailedPackageChangedException
import com.rosan.installer.data.app.model.exception.InstallFailedRejectedByBuildTypeException
import com.rosan.installer.data.app.model.exception.InstallFailedReplaceCouldntDeleteException
import com.rosan.installer.data.app.model.exception.InstallFailedSharedUserIncompatibleException
import com.rosan.installer.data.app.model.exception.InstallFailedTestOnlyException
import com.rosan.installer.data.app.model.exception.InstallFailedUidChangedException
import com.rosan.installer.data.app.model.exception.InstallFailedUpdateIncompatibleException
import com.rosan.installer.data.app.model.exception.InstallFailedUserRestrictedException
import com.rosan.installer.data.app.model.exception.InstallFailedVerificationFailureException
import com.rosan.installer.data.app.model.exception.InstallFailedVerificationTimeoutException
import com.rosan.installer.data.app.model.exception.InstallFailedVersionDowngradeException
import com.rosan.installer.data.app.model.exception.InstallParseFailedNoCertificatesException
import com.rosan.installer.data.app.model.exception.InstallParseFailedSkippedException
import com.rosan.installer.data.app.model.exception.InstallParseFailedUnexpectedException
import com.rosan.installer.data.app.model.exception.ModuleInstallCmdInitException
import com.rosan.installer.data.app.model.exception.ModuleInstallException
import com.rosan.installer.data.app.model.exception.ModuleInstallExitCodeNonZeroException
import com.rosan.installer.data.app.model.exception.ModuleInstallFailedIncompatibleAuthorizerException
import com.rosan.installer.data.app.model.exception.UninstallFailedAbortedException
import com.rosan.installer.data.app.model.exception.UninstallFailedHyperOSSystemAppException
import com.rosan.installer.data.app.model.exception.UninstallFailedInternalErrorException
import com.rosan.installer.data.installer.model.exception.HttpNotAllowedException
import com.rosan.installer.data.installer.model.exception.HttpRestrictedForLocalhostException
import com.rosan.installer.data.installer.model.exception.ResolveException
import com.rosan.installer.data.installer.model.exception.ResolveFailedLinkNotValidException
import com.rosan.installer.data.installer.model.exception.ResolvedFailedNoInternetAccessException
import com.rosan.installer.data.recycle.model.exception.AppProcessNotWorkException
import com.rosan.installer.data.recycle.model.exception.DhizukuDeadServiceException
import com.rosan.installer.data.recycle.model.exception.DhizukuNotWorkException
import com.rosan.installer.data.recycle.model.exception.RootNotWorkException
import com.rosan.installer.data.recycle.model.exception.ShizukuNotWorkException
import java.util.zip.ZipException

/**
 * Common implementation.
 *
 * A private helper function that serves as the single source of truth,
 * responsible for mapping a {@link Throwable} to its corresponding
 * string resource ID.
 *
 * @author iamr0s wxxsfxyzm
 * @return The string resource ID defined in `R.string`.
 */
private fun Throwable.getStringRes() =
    when (this) {
        is ResolveException -> R.string.exception_resolve_failed
        is ResolveFailedLinkNotValidException -> R.string.exception_resolve_failed_link_not_valid
        is ResolvedFailedNoInternetAccessException -> R.string.exception_resolve_failed_no_internet_access
        is AnalyseFailedAllFilesUnsupportedException -> R.string.exception_analyse_failed_all_files_unsupported
        is AuthenticationFailedException -> R.string.exception_authentication_failed
        is InstallFailedAlreadyExistsException -> R.string.exception_install_failed_already_exists
        is InstallFailedBlacklistedPackageException -> R.string.exception_install_failed_blacklisted_package
        is InstallFailedInvalidAPKException -> R.string.exception_install_failed_invalid_apk
        is InstallFailedInvalidURIException -> R.string.exception_install_failed_invalid_uri
        is InstallFailedInsufficientStorageException -> R.string.exception_install_failed_insufficient_storage
        is InstallFailedDuplicatePackageException -> R.string.exception_install_failed_duplicate_package
        is InstallFailedDuplicatePermissionException -> R.string.exception_install_failed_duplicate_permission
        is InstallFailedNoSharedUserException -> R.string.exception_install_failed_no_shared_user
        is InstallFailedUpdateIncompatibleException -> R.string.exception_install_failed_update_incompatible
        is InstallFailedSharedUserIncompatibleException -> R.string.exception_install_failed_shared_user_incompatible
        is InstallFailedMissingSharedLibraryException -> R.string.exception_install_failed_missing_shared_library
        is InstallFailedMissingInstallPermissionException -> R.string.exception_install_failed_missing_install_permission
        is InstallFailedReplaceCouldntDeleteException -> R.string.exception_install_failed_replace_couldnt_delete
        is InstallFailedDexOptException -> R.string.exception_install_failed_dexopt
        is InstallFailedOlderSdkException -> R.string.exception_install_failed_older_sdk
        is InstallFailedConflictingProviderException -> R.string.exception_install_failed_conflicting_provider
        is InstallFailedNewerSDKException -> R.string.exception_install_failed_newer_sdk
        is InstallFailedTestOnlyException -> R.string.exception_install_failed_test_only
        is InstallFailedCpuAbiIncompatibleException -> R.string.exception_install_failed_cpu_abi_incompatible
        is InstallFailedAbortedException -> R.string.exception_install_failed_aborted
        is InstallFailedMissingFeatureException -> R.string.exception_install_failed_missing_feature
        is InstallFailedContainerErrorException -> R.string.exception_install_failed_container_error
        is InstallFailedInvalidInstallLocationException -> R.string.exception_install_failed_invalid_install_location
        is InstallFailedMediaUnavailableException -> R.string.exception_install_failed_media_unavailable
        is InstallFailedVerificationTimeoutException -> R.string.exception_install_failed_verification_timeout
        is InstallFailedVerificationFailureException -> R.string.exception_install_failed_verification_failure
        is InstallFailedPackageChangedException -> R.string.exception_install_failed_package_changed
        is InstallFailedUidChangedException -> R.string.exception_install_failed_uid_changed
        is InstallFailedVersionDowngradeException -> R.string.exception_install_failed_version_downgrade
        is InstallFailedMissingSplitException -> R.string.exception_install_failed_missing_split
        is InstallFailedDeprecatedSdkVersion -> R.string.exception_install_failed_deprecated_sdk_version
        is InstallFailedRejectedByBuildTypeException -> R.string.exception_install_failed_rejected_by_build_type
        is InstallFailedOriginOSBlacklistException -> R.string.exception_install_failed_origin_os_blacklist
        is InstallFailedHyperOSIsolationViolationException -> R.string.exception_install_failed_hyperos_isolation_violation
        is InstallParseFailedUnexpectedException -> R.string.exception_install_parse_failed_unexpected_exception
        is InstallParseFailedNoCertificatesException -> R.string.exception_install_parse_failed_no_certificates
        is InstallParseFailedSkippedException -> R.string.exception_install_parse_failed_skipped
        is InstallFailedUserRestrictedException -> R.string.exception_install_failed_user_restricted
        is ShizukuNotWorkException -> R.string.exception_shizuku_not_work
        is DhizukuNotWorkException -> R.string.exception_dhizuku_not_work
        is DhizukuDeadServiceException -> R.string.exception_dhizuku_dead_service
        is UninstallFailedHyperOSSystemAppException -> R.string.exception_uninstall_failed_hyperos_system_app
        is UninstallFailedInternalErrorException -> R.string.exception_uninstall_failed_internal_error
        is UninstallFailedAbortedException -> R.string.exception_uninstall_failed_aborted
        is PackageManager.NameNotFoundException -> R.string.exception_package_manager_name_not_found
        is RootNotWorkException -> R.string.exception_root_not_work
        is AppProcessNotWorkException -> R.string.exception_app_process_not_work
        is ModuleInstallException -> R.string.exception_module_install_failed
        is ModuleInstallCmdInitException -> R.string.exception_module_cmd_init_failed
        is ModuleInstallFailedIncompatibleAuthorizerException -> R.string.exception_module_install_failed_incompatible_authorizer
        is ModuleInstallExitCodeNonZeroException -> R.string.exception_module_install_exit_code_non_zero
        is HttpNotAllowedException -> R.string.exception_http_not_allowed
        is HttpRestrictedForLocalhostException -> R.string.exception_http_restricted_for_localhost
        is ZipException -> R.string.exception_zip_exception

        else -> R.string.exception_install_failed_unknown
    }

/**
 * Returns a user-friendly error message for this [Throwable]
 * in a Jetpack Compose environment.
 *
 * This is the Compose-specific API and should be used inside
 * composable functions.
 *
 * @receiver The [Throwable] to be converted into a readable message.
 * @return A localized error message string.
 *
 * @author iamr0s
 */
@Composable
fun Throwable.help() = stringResource(this.getStringRes())

/**
 * Returns a user-friendly error message for this [Throwable]
 * in a non-Compose environment.
 *
 * This function is intended for use in components such as
 * Service, BroadcastReceiver, or other framework classes
 * where Compose is not available.
 *
 * @receiver The [Throwable] to be converted into a readable message.
 * @param context The [Context] used to resolve the string resource.
 * @return A localized error message string.
 *
 * @author wxxsfxyzm
 */
fun Throwable.getErrorMessage(context: Context) = context.getString(this.getStringRes())

package com.rosan.installer.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.data.app.model.exception.InstallFailedAlreadyExistsException
import com.rosan.installer.data.app.model.exception.InstallFailedConflictingProviderException
import com.rosan.installer.data.app.model.exception.InstallFailedContainerErrorException
import com.rosan.installer.data.app.model.exception.InstallFailedCpuAbiIncompatibleException
import com.rosan.installer.data.app.model.exception.InstallFailedDexOptException
import com.rosan.installer.data.app.model.exception.InstallFailedDuplicatePackageException
import com.rosan.installer.data.app.model.exception.InstallFailedInsufficientStorageException
import com.rosan.installer.data.app.model.exception.InstallFailedInvalidAPKException
import com.rosan.installer.data.app.model.exception.InstallFailedInvalidInstallLocationException
import com.rosan.installer.data.app.model.exception.InstallFailedInvalidURIException
import com.rosan.installer.data.app.model.exception.InstallFailedMediaUnavailableException
import com.rosan.installer.data.app.model.exception.InstallFailedMissingFeatureException
import com.rosan.installer.data.app.model.exception.InstallFailedMissingSharedLibraryException
import com.rosan.installer.data.app.model.exception.InstallFailedNewerSDKException
import com.rosan.installer.data.app.model.exception.InstallFailedNoSharedUserException
import com.rosan.installer.data.app.model.exception.InstallFailedOlderSdkException
import com.rosan.installer.data.app.model.exception.InstallFailedPackageChangedException
import com.rosan.installer.data.app.model.exception.InstallFailedRejectedByBuildTypeException
import com.rosan.installer.data.app.model.exception.InstallFailedReplaceCouldntDeleteException
import com.rosan.installer.data.app.model.exception.InstallFailedSharedUserIncompatibleException
import com.rosan.installer.data.app.model.exception.InstallFailedTestOnlyException
import com.rosan.installer.data.app.model.exception.InstallFailedUidChangedException
import com.rosan.installer.data.app.model.exception.InstallFailedUpdateIncompatibleException
import com.rosan.installer.data.app.model.exception.InstallFailedVerificationFailureException
import com.rosan.installer.data.app.model.exception.InstallFailedVerificationTimeoutException
import com.rosan.installer.data.app.model.exception.InstallFailedVersionDowngradeException
import com.rosan.installer.data.recycle.model.exception.AppProcessNotWorkException
import com.rosan.installer.data.recycle.model.exception.DhizukuNotWorkException
import com.rosan.installer.data.recycle.model.exception.RootNotWorkException
import com.rosan.installer.data.recycle.model.exception.ShizukuNotWorkException

@Composable
fun Throwable.help(): String {
    return when (this) {
        is InstallFailedAlreadyExistsException -> stringResource(R.string.exception_install_failed_already_exists)
        is InstallFailedInvalidAPKException -> stringResource(R.string.exception_install_failed_invalid_apk)
        is InstallFailedInvalidURIException -> stringResource(R.string.exception_install_failed_invalid_uri)
        is InstallFailedInsufficientStorageException -> stringResource(R.string.exception_install_failed_insufficient_storage)
        is InstallFailedDuplicatePackageException -> stringResource(R.string.exception_install_failed_duplicate_package)
        is InstallFailedNoSharedUserException -> stringResource(R.string.exception_install_failed_no_shared_user)
        is InstallFailedUpdateIncompatibleException -> stringResource(R.string.exception_install_failed_update_incompatible)
        is InstallFailedSharedUserIncompatibleException -> stringResource(R.string.exception_install_failed_shared_user_incompatible)
        is InstallFailedMissingSharedLibraryException -> stringResource(R.string.exception_install_failed_missing_shared_library)
        is InstallFailedReplaceCouldntDeleteException -> stringResource(R.string.exception_install_failed_replace_couldnt_delete)
        is InstallFailedDexOptException -> stringResource(R.string.exception_install_failed_dexopt)
        is InstallFailedOlderSdkException -> stringResource(R.string.exception_install_failed_older_sdk)
        is InstallFailedConflictingProviderException -> stringResource(R.string.exception_install_failed_conflicting_provider)
        is InstallFailedNewerSDKException -> stringResource(R.string.exception_install_failed_newer_sdk)
        is InstallFailedTestOnlyException -> stringResource(R.string.exception_install_failed_test_only)
        is InstallFailedCpuAbiIncompatibleException -> stringResource(R.string.exception_install_failed_cpu_abi_incompatible)
        is InstallFailedMissingFeatureException -> stringResource(R.string.exception_install_failed_missing_feature)
        is InstallFailedContainerErrorException -> stringResource(R.string.exception_install_failed_container_error)
        is InstallFailedInvalidInstallLocationException -> stringResource(R.string.exception_install_failed_invalid_install_location)
        is InstallFailedMediaUnavailableException -> stringResource(R.string.exception_install_failed_media_unavailable)
        is InstallFailedVerificationTimeoutException -> stringResource(R.string.exception_install_failed_verification_timeout)
        is InstallFailedVerificationFailureException -> stringResource(R.string.exception_install_failed_verification_failure)
        is InstallFailedPackageChangedException -> stringResource(R.string.exception_install_failed_package_changed)
        is InstallFailedUidChangedException -> stringResource(R.string.exception_install_failed_uid_changed)
        is InstallFailedVersionDowngradeException -> stringResource(R.string.exception_install_failed_version_downgrade)
        is InstallFailedRejectedByBuildTypeException -> stringResource(R.string.exception_install_failed_rejected_by_build_type)

        is ShizukuNotWorkException -> stringResource(R.string.exception_shizuku_not_work)
        is DhizukuNotWorkException -> stringResource(R.string.exception_dhizuku_not_work)
        is RootNotWorkException -> stringResource(R.string.exception_root_not_work)
        is AppProcessNotWorkException -> stringResource(R.string.exception_app_process_not_work)
        else -> stringResource(R.string.exception_install_failed_unknown)
    }
}
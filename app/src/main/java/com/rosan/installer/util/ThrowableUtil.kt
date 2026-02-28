// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.data.app.model.enums.InstallErrorType
import com.rosan.installer.data.app.model.exception.AnalyseFailedAllFilesUnsupportedException
import com.rosan.installer.data.app.model.exception.AuthenticationFailedException
import com.rosan.installer.data.app.model.exception.InstallException
import com.rosan.installer.data.app.model.exception.ModuleInstallCmdInitException
import com.rosan.installer.data.app.model.exception.ModuleInstallException
import com.rosan.installer.data.app.model.exception.ModuleInstallExitCodeNonZeroException
import com.rosan.installer.data.app.model.exception.ModuleInstallFailedIncompatibleAuthorizerException
import com.rosan.installer.data.app.model.exception.UninstallException
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
        is InstallException -> this.errorType.stringResId
        is UninstallException -> this.errorType.stringResId
        is ShizukuNotWorkException -> R.string.exception_shizuku_not_work
        is DhizukuNotWorkException -> R.string.exception_dhizuku_not_work
        is DhizukuDeadServiceException -> R.string.exception_dhizuku_dead_service
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
 */
fun Throwable.getErrorMessage(context: Context) = context.getString(this.getStringRes())

/**
 * Returns a [Boolean] indicating whether this [Throwable]
 * has a specific [InstallErrorType].
 */
fun Throwable.hasErrorType(vararg types: InstallErrorType): Boolean =
    this is InstallException && this.errorType in types

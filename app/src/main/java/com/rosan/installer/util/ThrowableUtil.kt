// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.data.app.model.enums.InstallErrorType
import com.rosan.installer.data.app.model.exception.AuthenticationFailedException
import com.rosan.installer.data.app.model.exception.InstallException
import com.rosan.installer.data.app.model.exception.InstallerException
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
        is InstallerException -> this.getStringResId()
        is ZipException -> R.string.exception_zip_exception
        is PackageManager.NameNotFoundException -> R.string.exception_package_manager_name_not_found
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

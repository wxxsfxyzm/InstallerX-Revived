// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.util

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.device.model.Manufacturer
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.model.InstallErrorType
import com.rosan.installer.domain.engine.model.InstallOption
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.util.hasErrorType
import org.koin.compose.koinInject

private const val SYSTEM_INSTALLER_PACKAGE_NAME = "com.android.shell"

// Shared data model for suggestions
data class ErrorSuggestion(
    @param:StringRes val labelRes: Int,
    @param:StringRes val descriptionRes: Int? = null,
    val icon: ImageVector? = null,
    val onClick: () -> Unit
)

@Composable
fun rememberErrorSuggestions(
    error: Throwable,
    viewModel: InstallerViewModel,
    onShowUninstallConfirm: (keepData: Boolean, conflictingPkg: String?) -> Unit
): List<ErrorSuggestion> {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config = uiState.config
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val hasMiPackageInstaller = capabilityProvider.hasMiPackageInstaller
    val shizukuIcon = ImageVector.vectorResource(R.drawable.ic_shizuku)

    return remember(error, config) {
        buildList {
            // Calculate this first so we can use it to suppress unnecessary uninstall suggestions
            val canAllowDowngrade = config.authorizer == Authorizer.Root ||
                    (config.authorizer == Authorizer.None && capabilityProvider.isSystemApp) ||
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE && config.authorizer == Authorizer.Shizuku)

            if (error.hasErrorType(InstallErrorType.TEST_ONLY)) {
                add(
                    ErrorSuggestion(
                        labelRes = R.string.suggestion_allow_test_app,
                        descriptionRes = R.string.suggestion_allow_test_app_desc,
                        icon = AppIcons.BugReport,
                        onClick = {
                            viewModel.toggleInstallFlag(InstallOption.AllowTest.value, true)
                            viewModel.dispatch(InstallerViewAction.Install(false))
                        }
                    )
                )
            }

            val canShowProviderConflict = config.authorizer != Authorizer.None ||
                    !(DeviceConfig.currentManufacturer == Manufacturer.XIAOMI && hasMiPackageInstaller)

            if (canShowProviderConflict) {
                if (error.hasErrorType(InstallErrorType.CONFLICTING_PROVIDER)) {
                    add(
                        ErrorSuggestion(
                            labelRes = R.string.suggestion_uninstall_and_retry,
                            descriptionRes = R.string.suggestion_uninstall_and_retry_desc,
                            icon = AppIcons.Delete,
                            onClick = {
                                val conflictingPkg = Regex("used by ([\\w.]+)")
                                    .find(error.message ?: "")?.groupValues?.get(1)
                                onShowUninstallConfirm(false, conflictingPkg)
                            }
                        )
                    )
                }

                if (error.hasErrorType(InstallErrorType.DUPLICATE_PERMISSION)) {
                    add(
                        ErrorSuggestion(
                            labelRes = R.string.suggestion_uninstall_and_retry,
                            descriptionRes = R.string.suggestion_uninstall_and_retry_desc,
                            icon = AppIcons.Delete,
                            onClick = {
                                val conflictingPkg = Regex("already owned by ([\\w.]+)")
                                    .find(error.message ?: "")?.groupValues?.get(1)
                                onShowUninstallConfirm(false, conflictingPkg)
                            }
                        )
                    )
                }

                val hasIncompatibleError = error.hasErrorType(InstallErrorType.UPDATE_INCOMPATIBLE)
                val hasDowngradeError = error.hasErrorType(InstallErrorType.VERSION_DOWNGRADE)

                // Only show normal uninstall for downgrade if we CANNOT just allow it directly
                if (hasIncompatibleError || (!canAllowDowngrade && hasDowngradeError)) {
                    add(
                        ErrorSuggestion(
                            labelRes = R.string.suggestion_uninstall_and_retry,
                            descriptionRes = R.string.suggestion_uninstall_and_retry_desc,
                            icon = AppIcons.Delete,
                            onClick = { onShowUninstallConfirm(false, null) }
                        )
                    )
                }
            }

            val isAndroid14to15Downgrade = Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
                            (DeviceConfig.currentManufacturer == Manufacturer.SAMSUNG || DeviceConfig.currentManufacturer == Manufacturer.REALME))

            // Only show keep-data uninstall if we CANNOT allow direct downgrade
            if (!canAllowDowngrade && isAndroid14to15Downgrade && config.authorizer == Authorizer.Shizuku) {
                if (error.hasErrorType(InstallErrorType.VERSION_DOWNGRADE)) {
                    add(
                        ErrorSuggestion(
                            labelRes = R.string.suggestion_uninstall_and_retry_keep_data,
                            descriptionRes = R.string.suggestion_uninstall_and_retry_keep_data_desc,
                            icon = AppIcons.Delete,
                            onClick = { onShowUninstallConfirm(true, null) }
                        )
                    )
                }
            }

            // The actual "Allow Downgrade" option
            if (canAllowDowngrade && error.hasErrorType(InstallErrorType.VERSION_DOWNGRADE)) {
                add(
                    ErrorSuggestion(
                        labelRes = R.string.suggestion_allow_downgrade,
                        descriptionRes = R.string.suggestion_allow_downgrade_desc,
                        icon = AppIcons.Delete,
                        onClick = {
                            viewModel.toggleInstallFlag(InstallOption.AllowDowngrade.value, true)
                            viewModel.dispatch(InstallerViewAction.Install(false))
                        }
                    )
                )
            }

            if (error.hasErrorType(InstallErrorType.HYPEROS_ISOLATION_VIOLATION)) {
                if (config.authorizer != Authorizer.Dhizuku) {
                    add(
                        ErrorSuggestion(
                            labelRes = R.string.suggestion_mi_isolation,
                            descriptionRes = R.string.suggestion_mi_isolation_desc,
                            icon = AppIcons.InstallSource,
                            onClick = {
                                // Update immutable config through ViewModel
                                viewModel.updateConfig {
                                    it.copy(
                                        installer = SYSTEM_INSTALLER_PACKAGE_NAME,
                                        callingFromUid = null
                                    )
                                }
                                viewModel.dispatch(InstallerViewAction.Install(false))
                            }
                        )
                    )
                } else {
                    add(
                        ErrorSuggestion(
                            labelRes = R.string.suggestion_shizuku_isolation,
                            descriptionRes = R.string.suggestion_shizuku_isolation_desc,
                            icon = shizukuIcon,
                            onClick = {
                                // Update immutable config through ViewModel
                                viewModel.updateConfig {
                                    it.copy(
                                        installer = SYSTEM_INSTALLER_PACKAGE_NAME,
                                        authorizer = Authorizer.Shizuku
                                    )
                                }
                                viewModel.dispatch(InstallerViewAction.Install(false))
                            }
                        )
                    )
                }
            }

            if (error.hasErrorType(InstallErrorType.USER_RESTRICTED)) {
                add(
                    ErrorSuggestion(
                        labelRes = R.string.suggestion_user_restricted,
                        descriptionRes = R.string.suggestion_user_restricted_desc,
                        icon = AppIcons.Developer,
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                                viewModel.dispatch(InstallerViewAction.Close)
                            } catch (_: ActivityNotFoundException) {
                                viewModel.dispatch(InstallerViewAction.ShowToast("Developer options screen not found."))
                            }
                        }
                    )
                )
            }

            if (error.hasErrorType(InstallErrorType.DEPRECATED_SDK_VERSION)) {
                add(
                    ErrorSuggestion(
                        labelRes = R.string.suggestion_bypass_low_target_sdk,
                        descriptionRes = R.string.suggestion_bypass_low_target_sdk_desc,
                        icon = AppIcons.InstallBypassLowTargetSdk,
                        onClick = {
                            viewModel.toggleInstallFlag(InstallOption.BypassLowTargetSdkBlock.value, true)
                            viewModel.dispatch(InstallerViewAction.Install(false))
                        }
                    )
                )
            }

            if (error.hasErrorType(InstallErrorType.BLACKLISTED_PACKAGE)) {
                add(
                    ErrorSuggestion(
                        labelRes = R.string.suggestion_bypass_blacklist_set_by_user,
                        descriptionRes = R.string.suggestion_bypass_blacklist_set_by_user_desc,
                        icon = AppIcons.BugReport,
                        onClick = {
                            viewModel.toggleBypassBlacklist(true)
                            viewModel.dispatch(InstallerViewAction.Install(false))
                        }
                    )
                )
            }

            if (error.hasErrorType(InstallErrorType.MISSING_INSTALL_PERMISSION)) {
                add(
                    ErrorSuggestion(
                        labelRes = R.string.retry,
                        descriptionRes = R.string.suggestion_retry_install_desc,
                        icon = AppIcons.Retry,
                        onClick = { viewModel.dispatch(InstallerViewAction.Install(false)) }
                    )
                )
            }
        }
    }
}

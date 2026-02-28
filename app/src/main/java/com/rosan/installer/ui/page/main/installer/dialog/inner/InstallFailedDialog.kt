package com.rosan.installer.ui.page.main.installer.dialog.inner

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Manufacturer
import com.rosan.installer.data.app.model.enums.InstallErrorType
import com.rosan.installer.data.app.util.InstallOption
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.common.LocalMiPackageInstallerPresent
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.widget.chip.Chip
import com.rosan.installer.ui.page.main.widget.dialog.UninstallConfirmationDialog
import com.rosan.installer.util.hasErrorType
import kotlinx.coroutines.delay
import timber.log.Timber

@Composable
fun installFailedDialog(
    installer: InstallerRepo, viewModel: InstallerViewModel
): DialogParams {
    installer.analysisResults.firstOrNull()?.packageName ?: ""

    // Call InstallInfoDialog for base structure
    val baseParams = installInfoDialog(
        installer = installer,
        viewModel = viewModel,
        onTitleExtraClick = {}
    )

    // Override text and buttons
    return baseParams.copy(
        text = DialogInnerParams(
            DialogParamsType.InstallerInstallFailed.id,
            {
                ErrorTextBlock(
                    installer.error,
                    suggestions = {
                        if (viewModel.viewSettings.showSmartSuggestion)
                            ErrorSuggestions(
                                error = installer.error,
                                viewModel = viewModel,
                                installer = installer
                            )
                    }
                )
            }
        ),
        buttons = dialogButtons(
            DialogParamsType.InstallerInstallFailed.id
        ) {
            listOf(
                DialogButton(stringResource(R.string.close)) {
                    viewModel.dispatch(InstallerViewAction.Close)
                }
            )
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ErrorSuggestions(
    error: Throwable,
    viewModel: InstallerViewModel,
    installer: InstallerRepo
) {
    val context = LocalContext.current
    var showUninstallConfirmDialog by remember { mutableStateOf(false) }
    var confirmKeepData by remember { mutableStateOf(false) }
    val hasMiPackageInstaller = LocalMiPackageInstallerPresent.current
    val shizukuIcon = ImageVector.vectorResource(R.drawable.ic_shizuku)

    // Refactored SuggestionChipInfo to use a matching lambda predicate
    class SuggestionChipInfo(
        val isMatch: (Throwable) -> Boolean,
        val selected: () -> Boolean,
        val onClick: () -> Unit,
        @param:StringRes val labelRes: Int,
        val icon: ImageVector
    )

    var pendingConflictingPackage by remember { mutableStateOf<String?>(null) }

    val possibleSuggestions = remember(installer) {
        buildList {
            add(
                SuggestionChipInfo(
                    isMatch = { it.hasErrorType(InstallErrorType.TEST_ONLY) },
                    selected = { true },
                    onClick = {
                        viewModel.toggleInstallFlag(InstallOption.AllowTest.value, true)
                        viewModel.dispatch(InstallerViewAction.Install(false))
                    },
                    labelRes = R.string.suggestion_allow_test_app,
                    icon = AppIcons.BugReport
                )
            )

            if (installer.config.authorizer != ConfigEntity.Authorizer.None ||
                (installer.config.authorizer == ConfigEntity.Authorizer.None &&
                        !(RsConfig.currentManufacturer == Manufacturer.XIAOMI && hasMiPackageInstaller))
            ) {
                add(
                    SuggestionChipInfo(
                        isMatch = { it.hasErrorType(InstallErrorType.CONFLICTING_PROVIDER) },
                        selected = { true }, // This is an action, not a state toggle.
                        onClick = {
                            val conflictingPkg = Regex("used by ([\\w.]+)")
                                .find(error.message ?: "")?.groupValues?.get(1)
                            confirmKeepData = false
                            pendingConflictingPackage = conflictingPkg
                            showUninstallConfirmDialog = true
                        },
                        labelRes = R.string.suggestion_uninstall_and_retry, // Not keep data
                        icon = AppIcons.Delete
                    )
                )
                add(
                    SuggestionChipInfo(
                        isMatch = { it.hasErrorType(InstallErrorType.DUPLICATE_PERMISSION) },
                        selected = { true },
                        onClick = {
                            val conflictingPkg = Regex("already owned by ([\\w.]+)")
                                .find(error.message ?: "")?.groupValues?.get(1)

                            confirmKeepData = false
                            pendingConflictingPackage = conflictingPkg
                            showUninstallConfirmDialog = true
                        },
                        labelRes = R.string.suggestion_uninstall_and_retry,
                        icon = AppIcons.Delete
                    )
                )
                add(
                    SuggestionChipInfo(
                        isMatch = {
                            it.hasErrorType(
                                InstallErrorType.UPDATE_INCOMPATIBLE,
                                InstallErrorType.VERSION_DOWNGRADE
                            )
                        },
                        selected = { true }, // This is an action, not a state toggle.
                        onClick = {
                            confirmKeepData = false
                            showUninstallConfirmDialog = true
                        },
                        labelRes = R.string.suggestion_uninstall_and_retry, // Not keep data
                        icon = AppIcons.Delete
                    )
                )
            }

            if (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA && // Must be lower than Android 16
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && // Must be Android 14 or higher
                !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && // And is Android 15 or higher
                        (RsConfig.currentManufacturer == Manufacturer.SAMSUNG ||         // and the manufacturer is Samsung
                                RsConfig.currentManufacturer == Manufacturer.REALME)) &&        // or the manufacturer is realme -> This combination is excluded
                (installer.config.authorizer == ConfigEntity.Authorizer.Root ||    // Authorization must be
                        installer.config.authorizer == ConfigEntity.Authorizer.Shizuku)   // Root or Shizuku
            ) {
                add(
                    SuggestionChipInfo(
                        isMatch = { it.hasErrorType(InstallErrorType.VERSION_DOWNGRADE) },
                        selected = { true }, // This is an action, not a state toggle.
                        onClick = {
                            confirmKeepData = true
                            showUninstallConfirmDialog = true
                        },
                        labelRes = R.string.suggestion_uninstall_and_retry_keep_data, // Keep data
                        icon = AppIcons.Delete
                    )
                )
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                (installer.config.authorizer == ConfigEntity.Authorizer.Root || installer.config.authorizer == ConfigEntity.Authorizer.Shizuku)
            ) {
                add(
                    SuggestionChipInfo(
                        isMatch = { it.hasErrorType(InstallErrorType.VERSION_DOWNGRADE) },
                        selected = { true }, // This is an action, not a state toggle.
                        onClick = {
                            viewModel.toggleInstallFlag(InstallOption.AllowDowngrade.value, true)
                            viewModel.dispatch(InstallerViewAction.Install(false))
                        },
                        labelRes = R.string.suggestion_allow_downgrade,
                        icon = AppIcons.Delete
                    )
                )
            }

            if (installer.config.authorizer != ConfigEntity.Authorizer.Dhizuku) {
                add(
                    SuggestionChipInfo(
                        isMatch = { it.hasErrorType(InstallErrorType.HYPEROS_ISOLATION_VIOLATION) },
                        selected = { true },
                        onClick = {
                            // Set available installer
                            installer.config.installer = "com.miui.packageinstaller"
                            // Wipe originatingUid
                            installer.config.callingFromUid = null
                            viewModel.dispatch(InstallerViewAction.Install(false))
                        },
                        labelRes = R.string.suggestion_mi_isolation,
                        icon = AppIcons.InstallSource
                    )
                )
            } else {
                add(
                    SuggestionChipInfo(
                        isMatch = { it.hasErrorType(InstallErrorType.HYPEROS_ISOLATION_VIOLATION) },
                        selected = { true },
                        onClick = {
                            installer.config.installer = "com.miui.packageinstaller"
                            installer.config.authorizer = ConfigEntity.Authorizer.Shizuku
                            viewModel.dispatch(InstallerViewAction.Install(false))
                        },
                        labelRes = R.string.suggestion_shizuku_isolation,
                        icon = shizukuIcon
                    )
                )
            }

            add(
                SuggestionChipInfo(
                    isMatch = { it.hasErrorType(InstallErrorType.USER_RESTRICTED) },
                    selected = { true }, // This is an action, not a state toggle.
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                            // Add this flag because we are starting an activity from a non-activity context.
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            viewModel.dispatch(InstallerViewAction.Close)
                        } catch (_: ActivityNotFoundException) {
                            // In case the activity is not found on some strange devices,
                            // show a toast to the user.
                            viewModel.toast("Developer options screen not found.")
                        }
                    },
                    labelRes = R.string.suggestion_user_restricted,
                    icon = AppIcons.Developer
                )
            )

            add(
                SuggestionChipInfo(
                    isMatch = { it.hasErrorType(InstallErrorType.DEPRECATED_SDK_VERSION) },
                    selected = { true }, // This is an action, not a state toggle.
                    onClick = {
                        viewModel.toggleInstallFlag(InstallOption.BypassLowTargetSdkBlock.value, true)
                        viewModel.dispatch(InstallerViewAction.Install(false))
                    },
                    labelRes = R.string.suggestion_bypass_low_target_sdk,
                    icon = AppIcons.InstallBypassLowTargetSdk
                )
            )

            // Custom internal exceptions implemented via positive error codes
            add(
                SuggestionChipInfo(
                    isMatch = { it.hasErrorType(InstallErrorType.BLACKLISTED_PACKAGE) },
                    selected = { true }, // This is an action, not a state toggle.
                    onClick = {
                        viewModel.toggleBypassBlacklist(true)
                        viewModel.dispatch(InstallerViewAction.Install(false))
                    },
                    labelRes = R.string.suggestion_bypass_blacklist_set_by_user,
                    icon = AppIcons.BugReport
                )
            )

            add(
                SuggestionChipInfo(
                    isMatch = { it.hasErrorType(InstallErrorType.MISSING_INSTALL_PERMISSION) },
                    selected = { true },
                    onClick = { viewModel.dispatch(InstallerViewAction.Install(false)) },
                    labelRes = R.string.retry,
                    icon = AppIcons.Retry
                )
            )
        }
    }

    val visibleSuggestions = remember(error) {
        possibleSuggestions.filter { suggestion -> suggestion.isMatch(error) }
    }

    Timber.tag("suggestion")
        .d("Visible suggestions: ${visibleSuggestions.size} for error: ${error::class.java.simpleName}")

    // Requirement: If there is at least one chip to show, create the FlowRow.
    if (visibleSuggestions.isNotEmpty()) {
        FlowRow(
            // modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalArrangement = Arrangement.spacedBy((-9).dp)
        ) {
            visibleSuggestions.forEachIndexed { index, suggestion ->
                var animatedVisibility by remember { mutableStateOf(false) }

                // Staggered animation for each chip to float in one by one.
                LaunchedEffect(suggestion.labelRes) {
                    delay(50L + index * 50L) // Staggered delay
                    animatedVisibility = true
                }

                AnimatedVisibility(
                    visible = animatedVisibility,
                    enter = fadeIn(animationSpec = tween(durationMillis = 200)) + slideInVertically { it / 2 },
                    exit = fadeOut(animationSpec = tween(durationMillis = 150))
                ) {
                    Chip(
                        selected = suggestion.selected(),
                        onClick = suggestion.onClick,
                        useHaptic = suggestion.selected(),
                        label = stringResource(id = suggestion.labelRes),
                        icon = suggestion.icon
                    )
                }
            }
        }
        UninstallConfirmationDialog(
            showDialog = showUninstallConfirmDialog,
            onDismiss = { showUninstallConfirmDialog = false },
            onConfirm = {
                // When the user confirms, we dispatch the action to the ViewModel.
                viewModel.dispatch(
                    InstallerViewAction.UninstallAndRetryInstall(
                        keepData = confirmKeepData,
                        conflictingPackage = pendingConflictingPackage
                    )
                )
            },
            keepData = confirmKeepData
        )
    }
}
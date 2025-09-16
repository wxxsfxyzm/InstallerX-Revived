package com.rosan.installer.ui.page.main.installer.dialog.inner

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.build.Manufacturer
import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.app.model.exception.InstallFailedBlacklistedPackageException
import com.rosan.installer.data.app.model.exception.InstallFailedConflictingProviderException
import com.rosan.installer.data.app.model.exception.InstallFailedDeprecatedSdkVersion
import com.rosan.installer.data.app.model.exception.InstallFailedHyperOSIsolationViolationException
import com.rosan.installer.data.app.model.exception.InstallFailedTestOnlyException
import com.rosan.installer.data.app.model.exception.InstallFailedUpdateIncompatibleException
import com.rosan.installer.data.app.model.exception.InstallFailedUserRestrictedException
import com.rosan.installer.data.app.model.exception.InstallFailedVersionDowngradeException
import com.rosan.installer.data.app.util.InstallOption
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.installer.dialog.DialogViewAction
import com.rosan.installer.ui.page.main.installer.dialog.DialogViewModel
import com.rosan.installer.ui.page.main.widget.chip.Chip
import com.rosan.installer.ui.page.main.widget.chip.SuggestionChipInfo
import com.rosan.installer.ui.page.main.widget.dialog.UninstallConfirmationDialog
import kotlinx.coroutines.delay
import timber.log.Timber

// Assume errorText is accessible

@Composable
fun installFailedDialog( // 小写开头
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    val context = LocalContext.current
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    // val preInstallAppInfo by viewModel.preInstallAppInfo.collectAsState()

    val packageName = currentPackageName ?: // Fallback logic is now simpler
    installer.analysisResults.firstOrNull()?.packageName ?: ""

    // Call InstallInfoDialog for base structure
    val baseParams = installInfoDialog(
        installer = installer,
        viewModel = viewModel,
        onTitleExtraClick = {
            if (packageName.isNotEmpty()) {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    )

    // Override text and buttons
    return baseParams.copy(
        text = DialogInnerParams(
            DialogParamsType.InstallerInstallFailed.id,
            // errorTextBlock(installer.error)
            {
                ErrorTextBlock(
                    installer.error,
                    suggestions = {
                        if (viewModel.showIntelligentSuggestion)
                            ErrorSuggestions(
                                error = installer.error,
                                viewModel = viewModel,
                                installer = installer
                            )
                    }
                )
            }
        ),
        buttons = DialogButtons(
            DialogParamsType.InstallerInstallFailed.id
        ) {
            listOf(
                /*DialogButton(stringResource(R.string.previous)) {
                    viewModel.dispatch(DialogViewAction.InstallPrepare)
                },*/
                DialogButton(stringResource(R.string.close)) {
                    viewModel.dispatch(DialogViewAction.Close)
                }
            )
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ErrorSuggestions(
    error: Throwable,
    viewModel: DialogViewModel,
    installer: InstallerRepo
) {
    val context = LocalContext.current
    var showUninstallConfirmDialog by remember { mutableStateOf(false) }
    var confirmKeepData by remember { mutableStateOf(false) }

    val possibleSuggestions = remember(installer) {
        buildList {
            add(
                SuggestionChipInfo(
                    InstallFailedTestOnlyException::class,
                    selected = { true },
                    onClick = {
                        viewModel.toggleInstallFlag(InstallOption.AllowTest.value, true)
                        viewModel.dispatch(DialogViewAction.Install)
                    },
                    labelRes = R.string.suggestion_allow_test_app,
                    icon = AppIcons.BugReport
                )
            )
            add(
                SuggestionChipInfo(
                    InstallFailedUpdateIncompatibleException::class,
                    InstallFailedVersionDowngradeException::class,
                    InstallFailedConflictingProviderException::class,
                    selected = { true }, // This is an action, not a state toggle.
                    onClick = {
                        confirmKeepData = false
                        showUninstallConfirmDialog = true
                    },// onUninstall,
                    labelRes = R.string.suggestion_uninstall_and_retry,// Not keep data
                    icon = AppIcons.Delete
                )
            )
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && // Must be Android 14 or higher

                !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && // And is Android 15 or higher
                        (RsConfig.currentManufacturer == Manufacturer.SAMSUNG ||         // and the manufacturer is Samsung
                                RsConfig.currentManufacturer == Manufacturer.REALME)) &&        // or the manufacturer is realme -> This combination is excluded

                (installer.config.authorizer == ConfigEntity.Authorizer.Root ||    // Authorization must be
                        installer.config.authorizer == ConfigEntity.Authorizer.Shizuku)   // Root or Shizuku
            ) {
                add(
                    SuggestionChipInfo(
                        InstallFailedVersionDowngradeException::class,
                        selected = { true }, // This is an action, not a state toggle.
                        onClick = {
                            confirmKeepData = true
                            showUninstallConfirmDialog = true
                        },// onUninstall,
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
                        InstallFailedVersionDowngradeException::class,
                        selected = { true }, // This is an action, not a state toggle.
                        onClick = {
                            viewModel.toggleInstallFlag(InstallOption.AllowDowngrade.value, true)
                            viewModel.dispatch(DialogViewAction.Install)
                        },
                        labelRes = R.string.suggestion_allow_downgrade,
                        icon = AppIcons.Delete
                    )
                )
            }
            add(
                SuggestionChipInfo(
                    InstallFailedHyperOSIsolationViolationException::class,
                    selected = { true }, // This is an action, not a state toggle.
                    onClick = {
                        installer.config.installer = "com.miui.packageinstaller"
                        viewModel.toast("可在设置中配置一个有效的安装来源")
                        viewModel.dispatch(DialogViewAction.Install)
                    },
                    labelRes = R.string.suggestion_mi_isolation,
                    icon = AppIcons.InstallSource
                )
            )
            add(
                SuggestionChipInfo(
                    InstallFailedUserRestrictedException::class,
                    selected = { true }, // This is an action, not a state toggle.
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                            // Add this flag because we are starting an activity from a non-activity context.
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            viewModel.dispatch(DialogViewAction.Close)
                        } catch (e: ActivityNotFoundException) {
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
                    InstallFailedDeprecatedSdkVersion::class,
                    selected = { true }, // This is an action, not a state toggle.
                    onClick = {
                        viewModel.toggleInstallFlag(InstallOption.BypassLowTargetSdkBlock.value, true)
                        viewModel.dispatch(DialogViewAction.Install)
                    },
                    labelRes = R.string.suggestion_bypass_low_target_sdk,
                    icon = AppIcons.InstallBypassLowTargetSdk
                )
            )
            add(
                SuggestionChipInfo(
                    InstallFailedBlacklistedPackageException::class,
                    selected = { true }, // This is an action, not a state toggle.
                    onClick = {
                        viewModel.toggleBypassBlacklist(true)
                        viewModel.dispatch(DialogViewAction.Install)
                    },
                    labelRes = R.string.suggestion_bypass_blacklist_set_by_user,
                    icon = AppIcons.BugReport
                )
            )
        }
    }

    val visibleSuggestions = remember(error) {
        possibleSuggestions.filter { suggestion ->
            suggestion.errorClasses.any { errorClass ->
                errorClass.isInstance(error)
            }
        }
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
                viewModel.dispatch(DialogViewAction.UninstallAndRetryInstall(keepData = confirmKeepData))
            },
            keepData = confirmKeepData
        )
    }
}
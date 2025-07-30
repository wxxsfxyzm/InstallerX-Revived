package com.rosan.installer.ui.page.installer.dialog.inner

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import com.rosan.installer.data.app.model.exception.InstallFailedDeprecatedSdkVersion
import com.rosan.installer.data.app.model.exception.InstallFailedHyperOSIsolationViolationException
import com.rosan.installer.data.app.model.exception.InstallFailedTestOnlyException
import com.rosan.installer.data.app.model.exception.InstallFailedUpdateIncompatibleException
import com.rosan.installer.data.app.model.exception.InstallFailedUserRestrictedException
import com.rosan.installer.data.app.model.exception.InstallFailedVersionDowngradeException
import com.rosan.installer.data.app.util.InstallOption
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.installer.dialog.DialogParams
import com.rosan.installer.ui.page.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.installer.dialog.DialogViewAction
import com.rosan.installer.ui.page.installer.dialog.DialogViewModel
import com.rosan.installer.ui.widget.chip.Chip
import com.rosan.installer.ui.widget.chip.SuggestionChipInfo
import kotlinx.coroutines.delay
import timber.log.Timber

// Assume errorText is accessible

@Composable
fun installFailedDialog( // 小写开头
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    val context = LocalContext.current

    val preInstallAppInfo by viewModel.preInstallAppInfo.collectAsState()
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val packageName = currentPackageName ?: installer.entities.filter { it.selected }.map { it.app }
        .firstOrNull()?.packageName ?: ""

    // Call InstallInfoDialog for base structure
    val baseParams = installInfoDialog(
        installer = installer,
        viewModel = viewModel,
        preInstallAppInfo = preInstallAppInfo,
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
                                installer = installer,
                                onUninstall = {
                                    // Define the action for uninstalling and retrying.
                                    // This might involve calling a method on the viewModel.
                                }
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
    installer: InstallerRepo,
    onUninstall: () -> Unit
) {
    val context = LocalContext.current
    val possibleSuggestions = remember(installer, onUninstall) {
        listOf(
            SuggestionChipInfo(
                InstallFailedTestOnlyException::class,
                selected = { true },
                onClick = {
                    viewModel.toggleInstallFlag(InstallOption.AllowTest.value, true)
                    viewModel.dispatch(DialogViewAction.Install)
                },
                labelRes = R.string.suggestion_allow_test_app,
                icon = AppIcons.BugReport
            ),
            SuggestionChipInfo(
                InstallFailedUpdateIncompatibleException::class,
                InstallFailedVersionDowngradeException::class,
                selected = { false }, // This is an action, not a state toggle.
                onClick = { viewModel.toast("Not yet implemented!") },// onUninstall,
                labelRes = R.string.suggestion_uninstall_and_retry,
                icon = AppIcons.Delete
            ),
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
            ),
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
            ),
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
            //modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
    }
}
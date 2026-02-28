package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Manufacturer
import com.rosan.installer.data.app.model.enums.InstallErrorType
import com.rosan.installer.data.app.util.InstallOption
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.common.LocalMiPackageInstallerPresent
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixErrorTextBlock
import com.rosan.installer.ui.page.miuix.widgets.MiuixNavigationItemWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixUninstallConfirmationDialog
import com.rosan.installer.ui.theme.LocalIsDark
import com.rosan.installer.ui.theme.miuixSheetCardColorDark
import com.rosan.installer.ui.util.isGestureNavigation
import com.rosan.installer.util.hasErrorType
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor

@Composable
fun InstallFailedContent(
    appInfo: AppInfoState,
    installer: InstallerRepo,
    viewModel: InstallerViewModel,
    onClose: () -> Unit
) {
    val isDarkMode = LocalIsDark.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppInfoSlot(appInfo = appInfo)
        Spacer(modifier = Modifier.height(32.dp))
        MiuixErrorTextBlock(
            error = installer.error,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false) // Removed to allow suggestions to push content
        )
        Spacer(modifier = Modifier.height(16.dp))
        MiuixErrorSuggestions(
            isDarkMode = isDarkMode,
            error = installer.error,
            viewModel = viewModel,
            installer = installer
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
        ) {
            TextButton(
                onClick = onClose,
                text = stringResource(R.string.close),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MiuixErrorSuggestions(
    isDarkMode: Boolean,
    error: Throwable,
    viewModel: InstallerViewModel,
    installer: InstallerRepo
) {
    val context = LocalContext.current

    val showUninstallConfirmDialogState = remember { mutableStateOf(false) }
    var confirmKeepData by remember { mutableStateOf(false) }
    val hasMiPackageInstaller = LocalMiPackageInstallerPresent.current

    data class SuggestionItem(
        val isMatch: (Throwable) -> Boolean,
        val onClick: () -> Unit,
        @param:StringRes val labelRes: Int,
        @param:StringRes val descriptionRes: Int
    )

    var pendingConflictingPackage by remember { mutableStateOf<String?>(null) }

    val possibleSuggestions = remember(installer) {
        buildList {
            add(
                SuggestionItem(
                    isMatch = { it.hasErrorType(InstallErrorType.TEST_ONLY) },
                    onClick = {
                        viewModel.toggleInstallFlag(InstallOption.AllowTest.value, true)
                        viewModel.dispatch(InstallerViewAction.Install(true))
                    },
                    labelRes = R.string.suggestion_allow_test_app,
                    descriptionRes = R.string.suggestion_allow_test_app_desc,
                )
            )

            if (installer.config.authorizer != ConfigEntity.Authorizer.None ||
                (installer.config.authorizer == ConfigEntity.Authorizer.None &&
                        !(RsConfig.currentManufacturer == Manufacturer.XIAOMI && hasMiPackageInstaller))
            ) {
                add(
                    SuggestionItem(
                        isMatch = { it.hasErrorType(InstallErrorType.CONFLICTING_PROVIDER) },
                        onClick = {
                            val conflictingPkg = Regex("used by ([\\w.]+)")
                                .find(error.message ?: "")?.groupValues?.get(1)
                            confirmKeepData = false
                            pendingConflictingPackage = conflictingPkg
                            showUninstallConfirmDialogState.value = true
                        },
                        labelRes = R.string.suggestion_uninstall_and_retry,
                        descriptionRes = R.string.suggestion_uninstall_and_retry_desc
                    )
                )
                add(
                    SuggestionItem(
                        isMatch = { it.hasErrorType(InstallErrorType.DUPLICATE_PERMISSION) },
                        onClick = {
                            val conflictingPkg = Regex("already owned by ([\\w.]+)")
                                .find(error.message ?: "")?.groupValues?.get(1)

                            confirmKeepData = false
                            pendingConflictingPackage = conflictingPkg
                            showUninstallConfirmDialogState.value = true
                        },
                        labelRes = R.string.suggestion_uninstall_and_retry,
                        descriptionRes = R.string.suggestion_uninstall_and_retry_desc
                    )
                )
                add(
                    SuggestionItem(
                        isMatch = {
                            it.hasErrorType(
                                InstallErrorType.UPDATE_INCOMPATIBLE,
                                InstallErrorType.VERSION_DOWNGRADE
                            )
                        },
                        onClick = {
                            confirmKeepData = false
                            showUninstallConfirmDialogState.value = true
                        },
                        labelRes = R.string.suggestion_uninstall_and_retry,
                        descriptionRes = R.string.suggestion_uninstall_and_retry_desc
                    )
                )
            }

            if (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
                        (RsConfig.currentManufacturer == Manufacturer.SAMSUNG || RsConfig.currentManufacturer == Manufacturer.REALME)) &&
                (installer.config.authorizer == ConfigEntity.Authorizer.Root || installer.config.authorizer == ConfigEntity.Authorizer.Shizuku)
            ) {
                add(
                    SuggestionItem(
                        isMatch = { it.hasErrorType(InstallErrorType.VERSION_DOWNGRADE) },
                        onClick = {
                            confirmKeepData = true
                            showUninstallConfirmDialogState.value = true
                        },
                        labelRes = R.string.suggestion_uninstall_and_retry_keep_data,
                        descriptionRes = R.string.suggestion_uninstall_and_retry_keep_data_desc
                    )
                )
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                (installer.config.authorizer == ConfigEntity.Authorizer.Root || installer.config.authorizer == ConfigEntity.Authorizer.Shizuku)
            ) {
                add(
                    SuggestionItem(
                        isMatch = { it.hasErrorType(InstallErrorType.VERSION_DOWNGRADE) },
                        onClick = {
                            viewModel.toggleInstallFlag(InstallOption.AllowDowngrade.value, true)
                            viewModel.dispatch(InstallerViewAction.Install(false))
                        },
                        labelRes = R.string.suggestion_allow_downgrade,
                        descriptionRes = R.string.suggestion_allow_downgrade_desc
                    )
                )
            }

            if (installer.config.authorizer != ConfigEntity.Authorizer.Dhizuku) {
                add(
                    SuggestionItem(
                        isMatch = { it.hasErrorType(InstallErrorType.HYPEROS_ISOLATION_VIOLATION) },
                        onClick = {
                            // Set available installer
                            installer.config.installer = "com.android.shell"
                            // Wipe originatingUid
                            installer.config.callingFromUid = null
                            viewModel.dispatch(InstallerViewAction.Install(false))
                        },
                        labelRes = R.string.suggestion_mi_isolation,
                        descriptionRes = R.string.suggestion_mi_isolation_desc
                    )
                )
            } else {
                add(
                    SuggestionItem(
                        isMatch = { it.hasErrorType(InstallErrorType.HYPEROS_ISOLATION_VIOLATION) },
                        onClick = {
                            installer.config.installer = "com.android.shell"
                            installer.config.authorizer = ConfigEntity.Authorizer.Shizuku
                            viewModel.dispatch(InstallerViewAction.Install(false))
                        },
                        labelRes = R.string.suggestion_shizuku_isolation,
                        descriptionRes = R.string.suggestion_shizuku_isolation_desc
                    )
                )
            }

            add(
                SuggestionItem(
                    isMatch = { it.hasErrorType(InstallErrorType.USER_RESTRICTED) },
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            viewModel.dispatch(InstallerViewAction.Close)
                        } catch (_: ActivityNotFoundException) {
                            viewModel.toast("Developer options screen not found.")
                        }
                    },
                    labelRes = R.string.suggestion_user_restricted,
                    descriptionRes = R.string.suggestion_user_restricted_desc
                )
            )

            add(
                SuggestionItem(
                    isMatch = { it.hasErrorType(InstallErrorType.DEPRECATED_SDK_VERSION) },
                    onClick = {
                        viewModel.toggleInstallFlag(InstallOption.BypassLowTargetSdkBlock.value, true)
                        viewModel.dispatch(InstallerViewAction.Install(false))
                    },
                    labelRes = R.string.suggestion_bypass_low_target_sdk,
                    descriptionRes = R.string.suggestion_bypass_low_target_sdk_desc
                )
            )

            // Custom internal exceptions implemented via positive error codes
            add(
                SuggestionItem(
                    isMatch = { it.hasErrorType(InstallErrorType.BLACKLISTED_PACKAGE) },
                    onClick = {
                        viewModel.toggleBypassBlacklist(true)
                        viewModel.dispatch(InstallerViewAction.Install(false))
                    },
                    labelRes = R.string.suggestion_bypass_blacklist_set_by_user,
                    descriptionRes = R.string.suggestion_bypass_blacklist_set_by_user_desc
                )
            )

            add(
                SuggestionItem(
                    isMatch = { it.hasErrorType(InstallErrorType.MISSING_INSTALL_PERMISSION) },
                    onClick = { viewModel.dispatch(InstallerViewAction.Install(false)) },
                    labelRes = R.string.retry,
                    descriptionRes = R.string.suggestion_retry_install_desc
                )
            )
        }
    }

    val visibleSuggestions = remember(error) {
        possibleSuggestions.filter { suggestion -> suggestion.isMatch(error) }
    }

    if (visibleSuggestions.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SmallTitle(
                text = stringResource(R.string.smart_suggestions),
                insideMargin = PaddingValues(12.dp, 8.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardColors(
                    color = if (isDynamicColor) MiuixTheme.colorScheme.surfaceContainer else
                        if (isDarkMode) miuixSheetCardColorDark else Color.White,
                    contentColor = MiuixTheme.colorScheme.onSurface
                )
            ) {
                visibleSuggestions.forEach { suggestion ->
                    MiuixNavigationItemWidget(
                        title = stringResource(id = suggestion.labelRes),
                        description = stringResource(id = suggestion.descriptionRes),
                        onClick = suggestion.onClick,
                        insideMargin = PaddingValues(12.dp)
                    )
                }
            }
        }
    }

    MiuixUninstallConfirmationDialog(
        showState = showUninstallConfirmDialogState,
        onDismiss = { showUninstallConfirmDialogState.value = false },
        onConfirm = {
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

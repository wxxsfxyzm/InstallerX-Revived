package com.rosan.installer.ui.page.main.installer.dialog.inner

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.model.entity.SignatureMatchStatus
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.installer.dialog.DialogViewAction
import com.rosan.installer.ui.page.main.installer.dialog.DialogViewModel
import com.rosan.installer.ui.page.main.widget.chip.Chip
import timber.log.Timber

// Assume pausingIcon is accessible

@Composable
private fun installPrepareEmptyDialog(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    return DialogParams(
        icon = DialogInnerParams(
            DialogParamsType.IconPausing.id, pausingIcon
        ), title = DialogInnerParams(
            DialogParamsType.InstallerPrepare.id,
        ) {
            Text(stringResource(R.string.installer_prepare_install))
        }, text = DialogInnerParams(
            DialogParamsType.InstallerPrepareEmpty.id
        ) {
            Text(stringResource(R.string.installer_prepare_install_empty))
        }, buttons = DialogButtons(
            DialogParamsType.ButtonsCancel.id
        ) {
            listOf(DialogButton(stringResource(R.string.previous)) {
                viewModel.dispatch(DialogViewAction.InstallChoice)
            }, DialogButton(stringResource(R.string.cancel)) {
                viewModel.dispatch(DialogViewAction.Close)
            })
        })
}

@Composable
private fun installPrepareTooManyDialog(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    return DialogParams(
        icon = DialogInnerParams(
            DialogParamsType.IconPausing.id, pausingIcon
        ), title = DialogInnerParams(
            DialogParamsType.InstallerPrepare.id,
        ) {
            Text(stringResource(R.string.installer_prepare_install))
        }, text = DialogInnerParams(
            DialogParamsType.InstallerPrepareTooMany.id
        ) {
            Text(stringResource(R.string.installer_prepare_install_too_many))
        }, buttons = DialogButtons(
            DialogParamsType.ButtonsCancel.id
        ) {
            listOf(DialogButton(stringResource(R.string.previous)) {
                viewModel.dispatch(DialogViewAction.InstallChoice)
            }, DialogButton(stringResource(R.string.cancel)) {
                viewModel.dispatch(DialogViewAction.Close)
            })
        })
}


@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun installPrepareDialog( // 小写开头
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    val context = LocalContext.current
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val currentPackage = installer.analysisResults.find { it.packageName == currentPackageName }

    // If there is no specific package to prepare, show an empty/error dialog.
    if (currentPackage == null) {
        return if (installer.analysisResults.size > 1) {
            installPrepareTooManyDialog(installer, viewModel)
        } else {
            installPrepareEmptyDialog(installer, viewModel)
        }
    }

    val selectedEntities = currentPackage.appEntities.filter { it.selected }.map { it.app }.sortedBest()
    if (selectedEntities.isEmpty()) return installPrepareEmptyDialog(installer, viewModel)

    val primaryEntity = selectedEntities.first()
    val entityToInstall = selectedEntities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
    val containerType = primaryEntity.containerType
    val preInstallAppInfo = currentPackage.installedAppInfo // Get pre-install info from the new model

    Timber.tag("AppEntity")
        .d("Package: ${primaryEntity.packageName}, Container: $containerType, Total files: ${installer.analysisResults.size}")

    var showChips by remember { mutableStateOf(false) }
    var autoDelete by remember { mutableStateOf(installer.config.autoDelete) }
    var displaySdk by remember { mutableStateOf(installer.config.displaySdk) }

    LaunchedEffect(autoDelete, displaySdk) {
        val currentConfig = installer.config
        if (currentConfig.autoDelete != autoDelete) installer.config.autoDelete = autoDelete
        if (currentConfig.displaySdk != displaySdk) installer.config.displaySdk = displaySdk
    }

    // Call InstallInfoDialog for base structure
    val baseParams = installInfoDialog(
        installer = installer,
        viewModel = viewModel,
        onTitleExtraClick = { showChips = !showChips }
    )

    val errorColor = MaterialTheme.colorScheme.error
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // This single block calculates all warnings and the final button text based on priority.
    val (warningMessages, buttonTextId) = remember(currentPackage, entityToInstall, preInstallAppInfo) {
        val newEntity = entityToInstall
        val oldInfo = currentPackage.installedAppInfo // Use currentPackage directly
        val signatureStatus = currentPackage.signatureMatchStatus

        val warnings = mutableListOf<Pair<String, Color>>()
        var finalButtonTextId = R.string.install // Default button text

        // Determine the base operation type (upgrade, downgrade, etc.)
        // This helps set the initial button text for non-error cases.
        if (newEntity != null) {
            if (oldInfo == null) {
                finalButtonTextId = R.string.install // New install
            } else {
                when {
                    newEntity.versionCode > oldInfo.versionCode -> {
                        finalButtonTextId = R.string.upgrade
                    }

                    newEntity.versionCode < oldInfo.versionCode -> {
                        warnings.add(
                            context.getString(R.string.installer_prepare_type_downgrade) to errorColor
                        )
                        finalButtonTextId = R.string.install_anyway
                    }

                    oldInfo.isArchived -> {
                        finalButtonTextId = R.string.unarchive
                    }

                    newEntity.versionName == oldInfo.versionName -> {
                        finalButtonTextId = R.string.reinstall
                    }

                    else -> {
                        finalButtonTextId = R.string.install // Sidegrade
                    }
                }
            }
        }

        // Check for signature status. This has HIGHER priority and can OVERRIDE the button text.
        // Currently only enable for APK and APKS
        if (containerType == DataType.APK || containerType == DataType.APKS)
            when (signatureStatus) {
                SignatureMatchStatus.MISMATCH -> {
                    // Add the signature warning to the top of the list for prominence.
                    warnings.add(
                        0, // Add to the beginning
                        context.getString(R.string.installer_prepare_signature_mismatch) to errorColor
                    )
                    // CRITICAL: If signatures mismatch, ALWAYS force the button to "Install Anyway".
                    finalButtonTextId = R.string.install_anyway
                }

                SignatureMatchStatus.UNKNOWN_ERROR -> {
                    warnings.add(
                        0,
                        context.getString(R.string.installer_prepare_signature_unknown) to tertiaryColor
                    )
                }

                else -> {
                    // Signatures match or app not installed, do nothing.
                }
            }

        // Final check for SDK incompatibility. This is a blocking error.
        val newMinSdk = newEntity?.minSdk?.toIntOrNull()
        if (newMinSdk != null && newMinSdk > Build.VERSION.SDK_INT) {
            warnings.add(
                0,
                context.getString(R.string.installer_prepare_sdk_incompatible) to errorColor
            )
        }

        // Return the final list of warnings and the determined button text ID
        Pair(warnings, finalButtonTextId)
    }

    // Override text and buttons
    return baseParams.copy(
        // Subtitle is inherited from InstallInfoDialog (shows new version + package name)
        text = DialogInnerParams(
            DialogParamsType.InstallerPrepareInstall.id
        ) {
            LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                item { WarningTextBlock(warnings = warningMessages) }
                // --- NEW LOGIC: Show package name and version with animation ---
                item {
                    // Use AnimatedVisibility to show package name and version
                    AnimatedVisibility(
                        visible = showChips,
                        enter = fadeIn() + expandVertically(), // 进入动画：淡入 + 垂直展开
                        exit = fadeOut() + shrinkVertically()  // 退出动画：淡出 + 垂直收起
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Chip(
                                selected = autoDelete,
                                onClick = {
                                    val newValue = !autoDelete
                                    autoDelete = newValue
                                    installer.config.autoDelete = newValue
                                },
                                label = stringResource(id = R.string.config_auto_delete),
                                icon = AppIcons.Delete
                            )
                            Chip(
                                selected = displaySdk,
                                onClick = {
                                    val newValue = !displaySdk
                                    displaySdk = newValue
                                    installer.config.displaySdk = newValue
                                },
                                label = stringResource(id = R.string.config_display_sdk_version),
                                icon = AppIcons.Info
                            )
                        }
                    }
                }
                // --- LOGIC END ---
            }
        },
        buttons = DialogButtons(
            DialogParamsType.InstallerPrepareInstall.id
        ) {
            // --- NEW LOGIC: Use buildList to dynamically create buttons ---
            // Use buildList to create a list of buttons
            buildList {
                // Install button is shown if the entity's minSdk is compatible
                val canInstall = entityToInstall?.minSdk?.toIntOrNull()?.let { it <= Build.VERSION.SDK_INT } ?: true
                // only when the entity is a split APK, XAPK, or APKM
                if (canInstall && viewModel.showExtendedMenu && (containerType == DataType.APKS || containerType == DataType.XAPK || containerType == DataType.APKM)) {
                    add(DialogButton(stringResource(R.string.install_choice), 1f) {
                        viewModel.dispatch(DialogViewAction.InstallChoice)
                    })
                }
                if (canInstall) {
                    add(DialogButton(stringResource(buttonTextId), 1f) {
                        viewModel.dispatch(DialogViewAction.Install)
                    })
                }
                // else if app can be installed and extended menu is shown
                if (canInstall && viewModel.showExtendedMenu) {
                    add(DialogButton(stringResource(R.string.menu), 2f) {
                        viewModel.dispatch(DialogViewAction.InstallExtendedMenu)
                    })
                }
                if (canInstall && !viewModel.showExtendedMenu && (containerType == DataType.APKS || containerType == DataType.XAPK || containerType == DataType.APKM))
                    add(DialogButton(stringResource(R.string.install_choice), 1f) {
                        viewModel.dispatch(DialogViewAction.InstallChoice)
                    })
                // Cancel button always shown
                add(DialogButton(stringResource(R.string.cancel), 1f) {
                    viewModel.dispatch(DialogViewAction.Close)
                })
            }
            // --- LOGIC END ---
        }
    )
}
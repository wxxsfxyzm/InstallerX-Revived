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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Manufacturer
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.enums.DataType
import com.rosan.installer.data.app.model.enums.SignatureMatchStatus
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.widget.chip.Chip

// Assume pausingIcon is accessible

@Composable
private fun installPrepareEmptyDialog(
    viewModel: InstallerViewModel
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
        }, buttons = dialogButtons(
            DialogParamsType.ButtonsCancel.id
        ) {
            listOf(DialogButton(stringResource(R.string.previous)) {
                viewModel.dispatch(InstallerViewAction.InstallChoice)
            }, DialogButton(stringResource(R.string.cancel)) {
                viewModel.dispatch(InstallerViewAction.Close)
            })
        })
}

@Composable
private fun installPrepareTooManyDialog(
    installer: InstallerRepo, viewModel: InstallerViewModel
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
        }, buttons = dialogButtons(
            DialogParamsType.ButtonsCancel.id
        ) {
            listOf(DialogButton(stringResource(R.string.previous)) {
                viewModel.dispatch(InstallerViewAction.InstallChoice)
            }, DialogButton(stringResource(R.string.cancel)) {
                viewModel.dispatch(InstallerViewAction.Close)
            })
        })
}


@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun installPrepareDialog( // 小写开头
    installer: InstallerRepo, viewModel: InstallerViewModel
): DialogParams {
    val context = LocalContext.current
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val currentPackage = installer.analysisResults.find { it.packageName == currentPackageName }
    val settings = viewModel.viewSettings

    // If there is no specific package to prepare, show an empty/error dialog.
    if (currentPackage == null) {
        return if (installer.analysisResults.size > 1) {
            installPrepareTooManyDialog(installer, viewModel)
        } else {
            installPrepareEmptyDialog(viewModel)
        }
    }

    val allAvailableApps = currentPackage.appEntities.map { it.app }

    val selectedEntities = currentPackage.appEntities.filter { it.selected }.map { it.app }
    if (selectedEntities.isEmpty()) return installPrepareEmptyDialog(viewModel)

    val effectivePrimaryEntity = selectedEntities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
        ?: selectedEntities.filterIsInstance<AppEntity.ModuleEntity>().firstOrNull()
        ?: selectedEntities.filterIsInstance<AppEntity.SplitEntity>().firstOrNull()
        ?: selectedEntities.firstOrNull()
        ?: allAvailableApps.sortedBest().firstOrNull()
    val primaryEntity = effectivePrimaryEntity ?: return installPrepareEmptyDialog(viewModel)

    val entityToInstall = selectedEntities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
    val containerType = primaryEntity.sourceType
    val preInstallAppInfo = currentPackage.installedAppInfo // Get pre-install info from the new model

    val isPureSplit = primaryEntity is AppEntity.SplitEntity
    val isBundleSplitUpdate = primaryEntity is AppEntity.BaseEntity &&
            entityToInstall == null &&
            selectedEntities.isNotEmpty()

    val isSplitUpdateMode = (isBundleSplitUpdate || isPureSplit) && preInstallAppInfo != null

    var showChips by remember { mutableStateOf(false) }
    var autoDelete by remember { mutableStateOf(installer.config.autoDelete) }
    var displaySdk by remember { mutableStateOf(installer.config.displaySdk) }
    var displaySize by remember { mutableStateOf(installer.config.displaySize) }
    var showOPPOSpecial by remember { mutableStateOf(settings.showOPPOSpecial) }

    LaunchedEffect(autoDelete, displaySdk, displaySize) {
        val currentConfig = installer.config
        if (currentConfig.autoDelete != autoDelete) installer.config.autoDelete = autoDelete
        if (currentConfig.displaySdk != displaySdk) installer.config.displaySdk = displaySdk
        if (currentConfig.displaySize != displaySize) installer.config.displaySize = displaySize
    }

    // Call InstallInfoDialog for base structure
    val baseParams = installInfoDialog(
        installer = installer,
        viewModel = viewModel,
        onTitleExtraClick = { showChips = !showChips }
    )

    val errorColor = MaterialTheme.colorScheme.error
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val downgradeWarning = stringResource(R.string.installer_prepare_type_downgrade)
    val sigMismatchWarning = stringResource(R.string.installer_prepare_signature_mismatch)
    val sigUnknownWarning = stringResource(R.string.installer_prepare_signature_unknown)
    val sdkIncompatibleWarning = stringResource(R.string.installer_prepare_sdk_incompatible)
    val (warningMessages, buttonTextId) = remember(currentPackage, entityToInstall, preInstallAppInfo) {
        val oldInfo = currentPackage.installedAppInfo // Use currentPackage directly
        val signatureStatus = currentPackage.signatureMatchStatus

        val warnings = mutableListOf<Pair<String, Color>>()
        var finalButtonTextId = R.string.install // Default button text

        // Determine the base operation type (upgrade, downgrade, etc.)
        // This helps set the initial button text for non-error cases.
        if (entityToInstall != null) {
            if (oldInfo == null) {
                finalButtonTextId = R.string.install // New install
            } else {
                when {
                    entityToInstall.versionCode > oldInfo.versionCode -> {
                        finalButtonTextId = R.string.upgrade
                    }

                    entityToInstall.versionCode < oldInfo.versionCode -> {
                        warnings.add(downgradeWarning to errorColor)
                        finalButtonTextId = R.string.install_anyway
                    }

                    oldInfo.isArchived -> {
                        finalButtonTextId = R.string.unarchive
                    }

                    entityToInstall.versionName == oldInfo.versionName -> {
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
        if (!isSplitUpdateMode && (containerType == DataType.APK || containerType == DataType.APKS))
            when (signatureStatus) {
                SignatureMatchStatus.MISMATCH -> {
                    // Add the signature warning to the top of the list for prominence.
                    warnings.add(
                        0, // Add to the beginning
                        sigMismatchWarning to errorColor
                    )
                    // CRITICAL: If signatures mismatch, ALWAYS force the button to "Install Anyway".
                    finalButtonTextId = R.string.install_anyway
                }

                SignatureMatchStatus.UNKNOWN_ERROR -> {
                    warnings.add(
                        0,
                        sigUnknownWarning to tertiaryColor
                    )
                }

                else -> {
                    // Signatures match or app not installed, do nothing.
                }
            }

        // Final check for SDK incompatibility. This is a blocking error.
        val newMinSdk = entityToInstall?.minSdk?.toIntOrNull()
        if (newMinSdk != null && newMinSdk > Build.VERSION.SDK_INT) {
            warnings.add(
                0,
                sdkIncompatibleWarning to errorColor
            )
        }

        // Return the final list of warnings and the determined button text ID
        Pair(warnings, finalButtonTextId)
    }

    return baseParams.copy(
        // Subtitle is inherited from InstallInfoDialog (shows new version + package name)
        text = DialogInnerParams(
            DialogParamsType.InstallerPrepareInstall.id
        ) {
            LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                item { WarningTextBlock(warnings = warningMessages) }
                item {
                    AnimatedVisibility(
                        visible = (primaryEntity is AppEntity.ModuleEntity) &&
                                primaryEntity.description.isNotBlank() &&
                                displaySdk,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = (primaryEntity as AppEntity.ModuleEntity).description,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                item {
                    AnimatedVisibility(
                        visible = showChips,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
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
                            Chip(
                                selected = displaySize,
                                onClick = {
                                    val newValue = !displaySize
                                    displaySize = newValue
                                    installer.config.displaySize = newValue
                                },
                                label = stringResource(id = R.string.config_display_size),
                                icon = AppIcons.ShowSize
                            )
                            if (RsConfig.currentManufacturer == Manufacturer.OPPO || RsConfig.currentManufacturer == Manufacturer.ONEPLUS)
                                Chip(
                                    selected = showOPPOSpecial,
                                    onClick = {
                                        val newValue = !showOPPOSpecial
                                        showOPPOSpecial = newValue
                                        settings.copy(showOPPOSpecial = newValue)
                                    },
                                    label = stringResource(id = R.string.installer_show_oem_special),
                                    icon = AppIcons.OEMSpecial
                                )
                        }
                    }
                }
            }
        },
        buttons = dialogButtons(
            DialogParamsType.InstallerPrepareInstall.id
        ) {
            // --- Use buildList to dynamically create buttons ---
            buildList {
                val isAPK =
                    containerType == DataType.APKS || containerType == DataType.XAPK || containerType == DataType.APKM || containerType == DataType.MIXED_MODULE_APK

                val canInstallBaseEntity = (primaryEntity as? AppEntity.BaseEntity)?.let { base ->
                    if (entityToInstall != null) {
                        // Installing Base: Check SDK
                        base.minSdk?.toIntOrNull()?.let { sdk -> sdk <= Build.VERSION.SDK_INT } ?: true
                    } else {
                        // Bundle Split Update: Allowed if installed
                        isSplitUpdateMode
                    }
                } ?: false

                val canInstallModuleEntity = (primaryEntity as? AppEntity.ModuleEntity)?.let {
                    settings.enableModuleInstall
                } ?: false

                val canInstallSplitEntity = (primaryEntity as? AppEntity.SplitEntity)?.let {
                    currentPackage.installedAppInfo != null
                } ?: false

                val canInstall = canInstallBaseEntity || canInstallModuleEntity || canInstallSplitEntity
                // only when the entity is a split APK, XAPK, or APKM
                if (canInstall && settings.showExtendedMenu && isAPK) {
                    add(DialogButton(stringResource(R.string.install_choice), 1f) {
                        viewModel.dispatch(InstallerViewAction.InstallChoice)
                    })
                }
                if (canInstall) {
                    add(DialogButton(stringResource(buttonTextId), 1f) {
                        viewModel.dispatch(InstallerViewAction.Install(true))
                        if (settings.autoSilentInstall && !viewModel.isInstallingModule)
                            viewModel.dispatch(InstallerViewAction.Background)
                    })
                }
                // else if app can be installed and extended menu is shown
                if (canInstall && settings.showExtendedMenu && primaryEntity !is AppEntity.ModuleEntity) {
                    add(DialogButton(stringResource(R.string.menu), 2f) {
                        viewModel.dispatch(InstallerViewAction.InstallExtendedMenu)
                    })
                }
                if (canInstall && !settings.showExtendedMenu && isAPK)
                    add(DialogButton(stringResource(R.string.install_choice), 1f) {
                        viewModel.dispatch(InstallerViewAction.InstallChoice)
                    })
                // Cancel button always shown
                add(DialogButton(stringResource(R.string.cancel), 1f) {
                    viewModel.dispatch(InstallerViewAction.Close)
                })
            }
            // --- BuildList END ---
        }
    )
}
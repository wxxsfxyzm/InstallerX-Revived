package com.rosan.installer.ui.page.installer.dialog.inner

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.TrendingDown
import androidx.compose.material.icons.twotone.BugReport
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.People
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.installer.dialog.DialogParams
import com.rosan.installer.ui.page.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.installer.dialog.DialogViewAction
import com.rosan.installer.ui.page.installer.dialog.DialogViewModel

// Assume pausingIcon is accessible

@Composable
private fun InstallPrepareEmptyDialog(
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
private fun InstallPrepareTooManyDialog(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Chip(
    selected: Boolean, onClick: () -> Unit, label: String, icon: ImageVector
) {
    FilterChip(selected = selected, onClick = onClick, leadingIcon = {
        Icon(
            modifier = Modifier.size(FilterChipDefaults.IconSize),
            imageVector = icon,
            contentDescription = null
        )
    }, label = {
        Text(label)
    })
}

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun installPrepareDialog( // 小写开头
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    val entities = installer.entities.filter { it.selected }.map { it.app }.sortedBest()
    if (entities.isEmpty()) return InstallPrepareEmptyDialog(installer, viewModel)
    if (entities.groupBy { it.packageName }.size > 1) return InstallPrepareTooManyDialog(
        installer, viewModel
    )

    val preInstallAppInfo by viewModel.preInstallAppInfo.collectAsState()
    var showChips by remember { mutableStateOf(false) }

    var forAllUser by remember { mutableStateOf(installer.config.forAllUser) }
    var allowTestOnly by remember { mutableStateOf(installer.config.allowTestOnly) }
    var allowDowngrade by remember { mutableStateOf(installer.config.allowDowngrade) }
    var autoDelete by remember { mutableStateOf(installer.config.autoDelete) }
    var displaySdk by remember { mutableStateOf(installer.config.displaySdk) }

    LaunchedEffect(forAllUser, allowTestOnly, allowDowngrade, autoDelete, displaySdk) {
        val currentConfig = installer.config
        if (currentConfig.forAllUser != forAllUser) installer.config.forAllUser = forAllUser
        if (currentConfig.allowTestOnly != allowTestOnly) installer.config.allowTestOnly =
            allowTestOnly
        if (currentConfig.allowDowngrade != allowDowngrade) installer.config.allowDowngrade =
            allowDowngrade
        if (currentConfig.autoDelete != autoDelete) installer.config.autoDelete = autoDelete
        if (currentConfig.displaySdk != displaySdk) installer.config.displaySdk = displaySdk
    }

    // Call InstallInfoDialog for base structure
    val baseParams = InstallInfoDialog(
        installer = installer,
        viewModel = viewModel,
        preInstallAppInfo = preInstallAppInfo,
        onTitleExtraClick = { showChips = !showChips }
    )

    // Override text and buttons
    return baseParams.copy(
        // Subtitle is inherited from InstallInfoDialog (shows new version + package name)
        text = DialogInnerParams(
            DialogParamsType.InstallerPrepareInstall.id
        ) {
            LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                item {
                    Text(stringResource(R.string.installer_prepare_install_dsp))
                }
                if (showChips) item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Chip(
                            selected = forAllUser,
                            onClick = {
                                val newValue = !forAllUser
                                forAllUser = newValue
                                installer.config.forAllUser = newValue
                            },
                            label = stringResource(id = R.string.config_for_all_user),
                            icon = Icons.TwoTone.People
                        )
                        Chip(
                            selected = allowTestOnly,
                            onClick = {
                                val newValue = !allowTestOnly
                                allowTestOnly = newValue
                                installer.config.allowTestOnly = newValue
                            },
                            label = stringResource(id = R.string.config_allow_test_only),
                            icon = Icons.TwoTone.BugReport
                        )
                        Chip(
                            selected = allowDowngrade,
                            onClick = {
                                val newValue = !allowDowngrade
                                allowDowngrade = newValue
                                installer.config.allowDowngrade = newValue
                            },
                            label = stringResource(id = R.string.config_allow_downgrade),
                            icon = Icons.AutoMirrored.TwoTone.TrendingDown
                        )
                        Chip(
                            selected = autoDelete,
                            onClick = {
                                val newValue = !autoDelete
                                autoDelete = newValue
                                installer.config.autoDelete = newValue
                            },
                            label = stringResource(id = R.string.config_auto_delete),
                            icon = Icons.TwoTone.Delete
                        )
                        Chip(
                            selected = displaySdk,
                            onClick = {
                                val newValue = !displaySdk
                                displaySdk = newValue
                                installer.config.displaySdk = newValue
                            },
                            label = stringResource(id = R.string.config_display_sdk_version),
                            icon = Icons.TwoTone.Info
                        )
                    }
                }
            }
        },
        buttons = DialogButtons(
            DialogParamsType.InstallerPrepareInstall.id
        ) {
            listOf(
                DialogButton(stringResource(R.string.install)) { viewModel.dispatch(DialogViewAction.Install) },
                DialogButton(stringResource(R.string.install_choice), 2f) {
                    viewModel.dispatch(
                        DialogViewAction.InstallChoice
                    )
                },
                DialogButton(stringResource(R.string.cancel), 1f) {
                    viewModel.dispatch(
                        DialogViewAction.Close
                    )
                }
            )
        }
    )
}

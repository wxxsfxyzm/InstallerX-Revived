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
import androidx.compose.material.icons.twotone.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.dialog.*

@Composable
private fun installPrepareEmptyDialog(
    viewModel: DialogViewModel
): DialogParams {
    return DialogParams(icon = DialogInnerParams(
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
    viewModel: DialogViewModel
): DialogParams {
    return DialogParams(icon = DialogInnerParams(
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
fun installPrepareDialog(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    val entities = installer.entities.filter { it.selected }.map { it.app }.sortedBest()
    if (entities.isEmpty()) return installPrepareEmptyDialog(viewModel)
    if (entities.groupBy { it.packageName }.size > 1) return installPrepareTooManyDialog(
        viewModel
    )
    var showChips by remember {
        mutableStateOf(false)
    }
    return installInfoDialog(installer, viewModel) {
        showChips = !showChips
    }.copy(text = DialogInnerParams(
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
                    var forAllUser by remember {
                        mutableStateOf(installer.config.forAllUser)
                    }
                    var allowTestOnly by remember {
                        mutableStateOf(installer.config.allowTestOnly)
                    }
                    var allowDowngrade by remember {
                        mutableStateOf(installer.config.allowDowngrade)
                    }
                    var autoDelete by remember {
                        mutableStateOf(installer.config.autoDelete)
                    }
                    installer.config.forAllUser = forAllUser
                    installer.config.allowTestOnly = allowTestOnly
                    installer.config.allowDowngrade = allowDowngrade
                    installer.config.autoDelete = autoDelete
                    Chip(
                        selected = forAllUser,
                        onClick = { forAllUser = !forAllUser },
                        label = stringResource(
                            id = R.string.config_for_all_user
                        ),
                        icon = Icons.TwoTone.People
                    )
                    Chip(
                        selected = allowTestOnly,
                        onClick = { allowTestOnly = !allowTestOnly },
                        label = stringResource(
                            id = R.string.config_allow_test_only
                        ),
                        icon = Icons.TwoTone.BugReport
                    )
                    Chip(
                        selected = allowDowngrade,
                        onClick = { allowDowngrade = !allowDowngrade },
                        label = stringResource(
                            id = R.string.config_allow_downgrade
                        ),
                        icon = Icons.AutoMirrored.TwoTone.TrendingDown
                    )
                    Chip(
                        selected = autoDelete,
                        onClick = { autoDelete = !autoDelete },
                        label = stringResource(
                            id = R.string.config_auto_delete
                        ),
                        icon = Icons.TwoTone.Delete
                    )
                }
            }
        }
    }, buttons = DialogButtons(
        DialogParamsType.InstallerPrepareInstall.id
    ) {
        listOf(DialogButton(stringResource(R.string.install)) {
            viewModel.dispatch(DialogViewAction.Install)
        }, DialogButton(stringResource(R.string.previous), 2f) {
            viewModel.dispatch(DialogViewAction.InstallChoice)
        }, DialogButton(stringResource(R.string.cancel), 1f) {
            viewModel.dispatch(DialogViewAction.Close)
        })
    })
}
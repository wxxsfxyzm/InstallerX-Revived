package com.rosan.installer.ui.page.installer.dialog.inner

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.util.ConfigUtil
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

    val entityToInstall = entities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
    val preInstallAppInfo by viewModel.preInstallAppInfo.collectAsState()
    var showChips by remember { mutableStateOf(false) }
    val showDialogInstallExtendedMenu by produceState(initialValue = false) {
        value = ConfigUtil.getShowDialogInstallExtendedMenu()
    }

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

    // --- NEW LOGIC: Determine message, color, and error state ---
    val context = LocalContext.current
    val (summaryText, summaryColorKey) = remember(entityToInstall, preInstallAppInfo) {
        val newEntity = entityToInstall as? AppEntity.BaseEntity
        val oldInfo = preInstallAppInfo

        // Highest priority: SDK check
        val newMinSdk = newEntity?.minSdk?.toIntOrNull()
        if (newMinSdk != null && newMinSdk > Build.VERSION.SDK_INT) {
            return@remember Pair(
                context.getString(R.string.installer_prepare_sdk_incompatible),
                "error"
            )
        }

        // Determine install type
        if (newEntity != null) {
            if (oldInfo == null) {
                Pair(context.getString(R.string.installer_prepare_type_new_install), "default")
            } else {
                when {
                    newEntity.versionCode > oldInfo.versionCode ->
                        Pair(context.getString(R.string.installer_prepare_type_upgrade), "default")

                    newEntity.versionCode < oldInfo.versionCode ->
                        Pair(context.getString(R.string.installer_prepare_type_downgrade), "error")
                    // newEntity.versionCode == oldInfo.versionCode already inferred
                    newEntity.versionName == oldInfo.versionName ->
                        Pair(
                            context.getString(R.string.installer_prepare_type_reinstall),
                            "default"
                        )
                    // versionCode same, versionName different
                    else ->
                        Pair(
                            context.getString(R.string.installer_prepare_type_sidegrade),
                            "default"
                        )
                }
            }
        } else {
            // Fallback for splits or other cases
            Pair(context.getString(R.string.installer_prepare_type_unknown_confirm), "default")
        }
    }

    val summaryColor = if (summaryColorKey == "error") {
        MaterialTheme.colorScheme.error
    } else {
        Color.Unspecified
    }
    // --- LOGIC END ---

    // Override text and buttons
    return baseParams.copy(
        // Subtitle is inherited from InstallInfoDialog (shows new version + package name)
        text = DialogInnerParams(
            DialogParamsType.InstallerPrepareInstall.id
        ) {
            LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                item {
                    Text(
                        text = summaryText,
                        color = summaryColor,
                        textAlign = TextAlign.Center
                    )
                }
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
                entities.first().minSdk?.let {
                    if (it.toInt() <= Build.VERSION.SDK_INT)
                        add(DialogButton(stringResource(R.string.install)) {
                            viewModel.dispatch(DialogViewAction.Install)
                        })
                }
                // if there are multiple entities, show the install choice button
                if (entities.size > 1) {
                    add(DialogButton(stringResource(R.string.install_choice), 2f) {
                        viewModel.dispatch(DialogViewAction.InstallChoice)
                    })
                }
                // TODO make a new dialog only for installing apk file
                // Add Permission review and comparison button if needed
                // Add more buttons as needed
                else if (showDialogInstallExtendedMenu) {
                    add(DialogButton("菜单", 2f) {
                        viewModel.dispatch(DialogViewAction.InstallExtendedMenu)
                    })
                }
                // Cancel button always shown
                add(DialogButton(stringResource(R.string.cancel), 1f) {
                    viewModel.dispatch(DialogViewAction.Close)
                })
            }
            // --- LOGIC END ---
        }
    )
}
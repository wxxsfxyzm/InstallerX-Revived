package com.rosan.installer.ui.page.miuix.widgets

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.datastore.entity.SharedUid
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.util.openUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.extra.SpinnerEntry
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperSpinner
import java.io.File

data class AuthorizerInfo(
    @param:StringRes val labelResId: Int,
    val icon: ImageVector
)

/**
 * A MIUI-style setting item for selecting a data authorizer.
 * It displays the current selection and reveals a dropdown menu on click.
 */
@Composable
fun MiuixDataAuthorizerWidget(
    modifier: Modifier = Modifier,
    currentAuthorizer: ConfigEntity.Authorizer,
    changeAuthorizer: (ConfigEntity.Authorizer) -> Unit,
    trailingContent: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val shizukuIcon = ImageVector.vectorResource(R.drawable.ic_shizuku)

    // The data source remains unchanged.
    val authorizerOptions = remember {
        mapOf(
            /*ConfigEntity.Authorizer.None to AuthorizerInfo(
                R.string.config_authorizer_none,
                AppIcons.None
            ),*/
            ConfigEntity.Authorizer.Root to AuthorizerInfo(
                R.string.config_authorizer_root,
                AppIcons.Root
            ),
            ConfigEntity.Authorizer.Shizuku to AuthorizerInfo(
                R.string.config_authorizer_shizuku,
                shizukuIcon
            ),
            ConfigEntity.Authorizer.Dhizuku to AuthorizerInfo(
                R.string.config_authorizer_dhizuku,
                AppIcons.InstallAllowRestrictedPermissions
            ),
        )
    }

    //    Convert the authorizerOptions Map into a List<SpinnerEntry>
    //    which is required by the SuperSpinner component.
    //    This is done once and remembered.
    val spinnerEntries = remember(authorizerOptions) {
        authorizerOptions.values.map { authorizerInfo ->
            SpinnerEntry(
                //icon = { Icon(imageVector = authorizerInfo.icon, contentDescription = null) },
                title = context.getString(authorizerInfo.labelResId)
            )
        }
    }

    // 2. SuperSpinner requires an integer index for the selected item.
    //    Find the index of the currentAuthorizer from the map's keys.
    val selectedIndex = remember(currentAuthorizer, authorizerOptions) {
        authorizerOptions.keys.indexOf(currentAuthorizer).coerceAtLeast(0)
    }

    // Use SuperSpinner directly, as shown in your TextComponent.kt example.
    // This single component replaces the entire ListItem + FlowRow + InputChip structure.
    SuperSpinner(
        modifier = modifier,
        // The main title for the setting item.
        title = stringResource(id = R.string.config_authorizer),
        // The summary can display the currently selected option's title.
        summary = spinnerEntries[selectedIndex].title,
        // Provide the list of SpinnerEntry objects.
        items = spinnerEntries,
        // Provide the current selected index.
        selectedIndex = selectedIndex,
        // When the index changes, find the corresponding Authorizer enum
        // and call the changeAuthorizer callback.
        onSelectedIndexChange = { newIndex ->
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            val newAuthorizer = authorizerOptions.keys.elementAt(newIndex)
            if (currentAuthorizer != newAuthorizer) {
                changeAuthorizer(newAuthorizer)
            }
        },
        // SuperSpinner has a disabled state, so we disable the "None" option by checking its index.
        enabled = selectedIndex != authorizerOptions.keys.indexOf(ConfigEntity.Authorizer.None)
    )

    // The trailingContent from your original function signature is preserved and called here.
    trailingContent()

}

data class InstallModeInfo(
    @param:StringRes val labelResId: Int,
    val icon: ImageVector
)

/**
 * @author wxxsfxyzm
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MiuixDataInstallModeWidget(
    modifier: Modifier = Modifier,
    currentInstallMode: ConfigEntity.InstallMode,
    changeInstallMode: (ConfigEntity.InstallMode) -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // The data source definition remains the same.
    val installModeOptions = remember {
        mapOf(
            ConfigEntity.InstallMode.Dialog to InstallModeInfo(
                R.string.config_install_mode_dialog,
                AppIcons.Dialog
            ),
            ConfigEntity.InstallMode.AutoDialog to InstallModeInfo(
                R.string.config_install_mode_auto_dialog,
                AppIcons.AutoDialog
            ),
            ConfigEntity.InstallMode.Notification to InstallModeInfo(
                R.string.config_install_mode_notification,
                AppIcons.Notification
            ),
            ConfigEntity.InstallMode.AutoNotification to InstallModeInfo(
                R.string.config_install_mode_auto_notification,
                AppIcons.AutoNotification
            )
        )
    }

    // Convert the installModeOptions Map into a List<SpinnerEntry>
    // for the SuperSpinner component.
    val spinnerEntries = remember(installModeOptions) {
        installModeOptions.values.map { modeInfo ->
            SpinnerEntry(
                // icon = { Icon(imageVector = modeInfo.icon, contentDescription = null) },
                title = context.getString(modeInfo.labelResId)
            )
        }
    }

    // Determine the selected index based on the currentInstallMode.
    val selectedIndex = remember(currentInstallMode, installModeOptions) {
        installModeOptions.keys.indexOf(currentInstallMode).coerceAtLeast(0)
    }

    // Replace the old ListItem and FlowRow with a single SuperSpinner component.
    SuperSpinner(
        modifier = modifier,
        // The main title for the setting item.
        title = stringResource(id = R.string.config_install_mode),
        // The summary shows the name of the currently selected mode.
        summary = spinnerEntries[selectedIndex].title,
        // Provide the converted list of items.
        items = spinnerEntries,
        // Set the current selection index.
        selectedIndex = selectedIndex,
        // When selection changes, convert the index back to an enum and call the callback.
        onSelectedIndexChange = { newIndex ->
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            val newMode = installModeOptions.keys.elementAt(newIndex)
            if (currentInstallMode != newMode) {
                changeInstallMode(newMode)
            }
        }
        // The `onClick` parameter from the function signature is not used here,
        // because SuperSpinner handles its own interaction logic. This matches
        // the behavior of your original code where the outer ListItem was not clickable.
    )
}

@Composable
fun MiuixDisableAdbVerify(
    checked: Boolean,
    isError: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    MiuixSwitchWidget(
        icon = AppIcons.DisableAdbVerify,
        title = stringResource(R.string.disable_adb_install_verify),
        description = if (!isError) stringResource(R.string.disable_adb_install_verify_desc)
        else stringResource(R.string.disable_adb_install_verify_not_support_dhizuku_desc),
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange
    )
}

/**
 * A setting pkg for requesting to ignore battery optimizations.
 *
 * @param checked Whether the app is currently ignoring battery optimizations.
 * @param onCheckedChange Callback invoked when the user toggles the switch.
 */
@Composable
fun MiuixIgnoreBatteryOptimizationSetting(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    MiuixSwitchWidget(
        icon = AppIcons.BatteryOptimization, // You need to add this icon
        title = stringResource(R.string.ignore_battery_optimizations),
        description = if (enabled) stringResource(R.string.ignore_battery_optimizations_desc)
        else stringResource(R.string.ignore_battery_optimizations_desc_disabled),
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange
    )
}

@Composable
fun MiuixDefaultInstaller(lock: Boolean, onClick: () -> Unit) {
    BasicComponent(
        title = stringResource(
            if (lock) R.string.lock_default_installer else R.string.unlock_default_installer
        ),
        summary = stringResource(
            if (lock) R.string.lock_default_installer_desc else R.string.unlock_default_installer_desc
        ),
        onClick = onClick
    )
}

@Composable
fun MiuixClearCache() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inProgress by remember {
        mutableStateOf(false)
    }
    BasicComponent(
        // The 'enabled' parameter directly implements your original logic to prevent clicks.
        enabled = !inProgress,
        title = stringResource(id = R.string.clear_cache),
        onClick = {
            // The core logic here is identical to your original code.
            // The 'if (inProgress)' check is now handled by the 'enabled' parameter above.
            inProgress = true
            scope.launch(Dispatchers.IO) {
                val paths = listOfNotNull(
                    context.externalCacheDir?.absolutePath
                )

                fun clearFile(file: File) {
                    if (!file.exists()) return
                    if (file.isDirectory) file.listFiles()?.forEach {
                        clearFile(it)
                    }
                    file.delete()
                }

                paths.forEach {
                    clearFile(File(it))
                }
                inProgress = false
            }
        }
    )
}

@Composable
fun MiuixSettingsAboutItemWidget(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    imageContentDescription: String? = null,
    headlineContentText: String,
    supportingContentText: String? = null,
    onClick: () -> Unit
) {
    // Use the library's BasicComponent as the foundation.
    BasicComponent(
        modifier = modifier, // Pass the modifier to the root component.
        title = headlineContentText,
        summary = supportingContentText,
        onClick = onClick
        // No rightActions are needed as this item has no trailing content.
    )
}

/**
 * A setting pkg that navigates to a secondary page, built upon BaseWidget.
 * It includes an icon, title, description, and a trailing arrow.
 *
 * @param icon The leading icon for the pkg.
 * @param title The main title text of the pkg.
 * @param description The supporting description text.
 * @param onClick The callback to be invoked when this pkg is clicked.
 */
@Composable
fun MiuixSettingsNavigationItemWidget(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    // Call the BaseWidget and pass the parameters accordingly.
    SuperArrow(
        title = title,
        summary = description,
        onClick = onClick
    )
}

@Composable
fun MiuixBottomSheetContent(
    title: String
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxWidth() // 填充横向宽度
            .padding(16.dp, 0.dp, 16.dp, 16.dp), // 整体内边距
        horizontalAlignment = Alignment.CenterHorizontally // 左对齐内容
    ) {
        // 标题
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium, // 使用合适的标题样式
            modifier = Modifier.padding(bottom = 20.dp) // 标题下方留白
        )

        // GitHub 按钮
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                // 点击按钮时调用 openUrl 工具函数
                openUrl(context, "https://github.com/wxxsfxyzm/InstallerX-Revived/releases")
            },
            modifier = Modifier.fillMaxWidth() // 按钮填充横向宽度
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_github),
                contentDescription = "GitHub Icon", // 辅助功能描述
                modifier = Modifier.size(24.dp) // 图标大小
            )
            Spacer(modifier = Modifier.width(8.dp)) // 图标与文字之间的间隔
            Text(text = "GitHub") // 按钮文本
        }
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                openUrl(context, "https://t.me/installerx_revived")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_telegram),
                contentDescription = "Telegram Icon",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Telegram") // 按钮文本
        }
        Spacer(modifier = Modifier.size(60.dp)) // 按钮下方留白
    }
}

/**
 * A reusable widget to display and manage a list of NamedPackage items.
 * It is stateless and relies on callbacks to handle data modifications.
 *
 * @param noContentTitle The title if no packages are available.
 * @param packages The list of NamedPackage items to display.
 * @param onAddPackage A callback invoked when a new package should be added.
 * @param onRemovePackage A callback invoked when an existing package should be removed.
 * @param modifier The modifier to be applied to the widget's container.
 */
@Composable
fun MiuixManagedPackagesWidget(
    modifier: Modifier = Modifier,
    noContentTitle: String,
    noContentDescription: String = stringResource(R.string.config_add_one_to_get_started),
    packages: List<NamedPackage>,
    infoText: String? = null,
    isInfoVisible: Boolean = false,
    infoColor: Color = MaterialTheme.colorScheme.primary,
    onAddPackage: (NamedPackage) -> Unit,
    onRemovePackage: (NamedPackage) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<NamedPackage?>(null) }

    // Main container for the widget
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        // Display each package in the list
        if (packages.isEmpty()) {
            ListItem(
                headlineContent = { Text(noContentTitle) },
                supportingContent = { Text(noContentDescription) },
                leadingContent = {
                    Icon(
                        // imageVector = AppIcons.Info,
                        imageVector = Icons.Default.Info, // Placeholder icon
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        } else {
            packages.forEach { item ->
                ListItem(
                    headlineContent = { Text(item.name) },
                    supportingContent = { Text(item.packageName) },
                    leadingContent = {
                        Icon(
                            imageVector = AppIcons.Android, // Placeholder icon
                            contentDescription = "Icon Placeholder",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { showDeleteConfirmation = item }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        // "Add New Package" button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically // 垂直居中对齐
        ) {
            // 1. 左侧新增的 AnimatedVisibility 文本区域
            AnimatedVisibility(
                visible = isInfoVisible && !infoText.isNullOrBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                // 使用一个 Box 来应用背景和圆角
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50)) // 50%的圆角使其成为胶囊形状
                        .background(infoColor.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = infoText!!, // 确定不为空时才显示
                        color = infoColor,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // 2. 一个带权重的 Spacer，它会“推开”两边的元素，占据所有可用空间
            Spacer(modifier = Modifier.weight(1f))

            // 3. 右侧原有的 "添加" 按钮
            TextButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp)) // 使用 width 比 size 更精确
                Text(stringResource(R.string.add))
            }
        }
    }

    // --- Dialogs ---

    // Dialog for adding a new package
    if (showAddDialog) {
        MiuixAddPackageDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newItem ->
                onAddPackage(newItem) // Use the callback
                showAddDialog = false
            }
        )
    }

    // Dialog for confirming deletion
    showDeleteConfirmation?.let { itemToDelete ->
        MiuixDeleteNamedPackageConfirmationDialog(
            item = itemToDelete,
            onDismiss = { showDeleteConfirmation = null },
            onConfirm = {
                onRemovePackage(itemToDelete) // Use the callback
                showDeleteConfirmation = null
            }
        )
    }
}

/**
 * A reusable widget to display and manage a list of NamedPackage items.
 * It is stateless and relies on callbacks to handle data modifications.
 *
 * @param noContentTitle The title if no packages are available.
 * @param packages The list of NamedPackage items to display.
 * @param onAddPackage A callback invoked when a new package should be added.
 * @param onRemovePackage A callback invoked when an existing package should be removed.
 * @param modifier The modifier to be applied to the widget's container.
 */
@Composable
fun MiuixManagedUidsWidget(
    noContentTitle: String,
    uids: List<SharedUid>,
    onAddUid: (SharedUid) -> Unit,
    onRemoveUid: (SharedUid) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<SharedUid?>(null) }

    // Main container for the widget
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        // Display each package in the list
        if (uids.isEmpty()) {
            ListItem(
                headlineContent = { Text(noContentTitle) },
                supportingContent = { Text(stringResource(R.string.config_add_one_to_get_started)) },
                leadingContent = {
                    Icon(
                        // imageVector = AppIcons.Info,
                        imageVector = Icons.Default.Info, // Placeholder icon
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        } else {
            uids.forEach { item ->
                ListItem(
                    headlineContent = { Text(item.uidName) },
                    supportingContent = { Text("UID: ${item.uidValue}") },
                    leadingContent = {
                        Icon(
                            imageVector = AppIcons.BugReport, // Placeholder icon
                            contentDescription = "Icon Placeholder",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { showDeleteConfirmation = item }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        // "Add New Package" button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.add))
            }
        }
    }

    // --- Dialogs ---

    // Dialog for adding a new package
    if (showAddDialog) {
        MiuixAddUidDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newUID ->
                onAddUid(newUID)
                showAddDialog = false
            }
        )
    }

    // Dialog for confirming deletion
    showDeleteConfirmation?.let { uidToDelete ->
        MiuixDeleteSharedUidConfirmationDialog(
            item = uidToDelete,
            onDismiss = { showDeleteConfirmation = null },
            onConfirm = {
                onRemoveUid(uidToDelete) // Use the callback
                showDeleteConfirmation = null
            }
        )
    }
}

/**
 * An AlertDialog for adding a new NamedPackage.
 */
@Composable
private fun MiuixAddPackageDialog(
    onDismiss: () -> Unit,
    onConfirm: (NamedPackage) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    val isConfirmEnabled = name.isNotBlank() && packageName.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.config_add_new_package)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.config_name)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text(stringResource(R.string.config_package_name)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(NamedPackage(name, packageName)) },
                enabled = isConfirmEnabled
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * An AlertDialog for adding a new SharedUid.
 */
@Composable
private fun MiuixAddUidDialog(
    onDismiss: () -> Unit,
    onConfirm: (SharedUid) -> Unit
) {
    var uidName by remember { mutableStateOf("") }
    var uidValueString by remember { mutableStateOf("") }

    // Confirm button is enabled if both name and value are not blank
    val isConfirmEnabled = uidName.isNotBlank() && uidValueString.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.config_add_new_shared_uid)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uidName,
                    onValueChange = { uidName = it },
                    label = { Text(stringResource(R.string.config_shared_uid_name)) }, // "Shared UID 名称"
                    singleLine = true
                )
                OutlinedTextField(
                    value = uidValueString,
                    onValueChange = { uidValueString = it },
                    label = { Text(stringResource(R.string.config_shared_uid_value)) }, // "Shared UID 值"
                    singleLine = true,
                    // Set the keyboard type to Number
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Convert uidValueString to Int before creating SharedUid
                    val uidValue = uidValueString.toInt()
                    onConfirm(SharedUid(uidName, uidValue))
                },
                enabled = isConfirmEnabled
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * An AlertDialog to confirm the deletion of an pkg.
 */
@Composable
private fun MiuixDeleteNamedPackageConfirmationDialog(
    item: NamedPackage,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.config_confirm_deletion)) },
        text = { Text(stringResource(R.string.config_confirm_deletion_desc, item.name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * An AlertDialog to confirm the deletion of an pkg.
 */
@Composable
private fun MiuixDeleteSharedUidConfirmationDialog(
    item: SharedUid,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.config_confirm_deletion)) },
        text = { Text(stringResource(R.string.config_confirm_deletion_desc, item.uidName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
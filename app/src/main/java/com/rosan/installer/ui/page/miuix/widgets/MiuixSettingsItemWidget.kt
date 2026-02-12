package com.rosan.installer.ui.page.miuix.widgets

import androidx.annotation.StringRes
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosan.installer.R
import com.rosan.installer.data.app.util.PackageManagerUtil
import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.datastore.entity.SharedUid
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.common.LocalSessionInstallSupported
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.theme.m3color.ThemeMode
import com.rosan.installer.ui.util.rememberCacheInfo
import com.rosan.installer.util.hasFlag
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SpinnerEntry
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperSpinner
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val shizukuIcon = ImageVector.vectorResource(R.drawable.ic_shizuku)

    val isSessionInstallSupported = LocalSessionInstallSupported.current
    val authorizerOptions = remember {
        buildMap {
            if (isSessionInstallSupported)
                put(
                    ConfigEntity.Authorizer.None,
                    AuthorizerInfo(R.string.config_authorizer_none, AppIcons.None)
                )
            put(
                ConfigEntity.Authorizer.Root,
                AuthorizerInfo(R.string.config_authorizer_root, AppIcons.Root)
            )
            put(
                ConfigEntity.Authorizer.Shizuku,
                AuthorizerInfo(R.string.config_authorizer_shizuku, shizukuIcon)
            )
            put(
                ConfigEntity.Authorizer.Dhizuku,
                AuthorizerInfo(R.string.config_authorizer_dhizuku, AppIcons.InstallAllowRestrictedPermissions)
            )
        }
    }

    // Convert the authorizerOptions Map into a List<SpinnerEntry>
    // which is required by the SuperSpinner component.
    // This is done once and remembered.
    val spinnerEntries = remember(authorizerOptions) {
        authorizerOptions.values.map { authorizerInfo ->
            SpinnerEntry(
                // icon = { Icon(imageVector = authorizerInfo.icon, contentDescription = null) },
                title = context.getString(authorizerInfo.labelResId)
            )
        }
    }

    // SuperSpinner requires an integer index for the selected item.
    // Find the index of the currentAuthorizer from the map's keys.
    val selectedIndex = remember(currentAuthorizer, authorizerOptions) {
        authorizerOptions.keys.indexOf(currentAuthorizer).coerceAtLeast(0)
    }

    SuperSpinner(
        modifier = modifier,
        title = stringResource(id = R.string.config_authorizer),
        summary = stringResource(R.string.config_app_authorizer_desc),
        items = spinnerEntries,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            val newAuthorizer = authorizerOptions.keys.elementAt(newIndex)
            if (currentAuthorizer != newAuthorizer) {
                changeAuthorizer(newAuthorizer)
            }
        }
    )
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
    trailingContent: @Composable () -> Unit = {},
) {
    val context = LocalContext.current

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
            SpinnerEntry(title = context.getString(modeInfo.labelResId))
        }
    }

    // Determine the selected index based on the currentInstallMode.
    val selectedIndex = remember(currentInstallMode, installModeOptions) {
        installModeOptions.keys.indexOf(currentInstallMode).coerceAtLeast(0)
    }

    SuperSpinner(
        modifier = modifier,
        title = stringResource(id = R.string.config_install_mode),
        summary = stringResource(R.string.config_install_mode_desc),
        items = spinnerEntries,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            val newMode = installModeOptions.keys.elementAt(newIndex)
            if (currentInstallMode != newMode) {
                changeInstallMode(newMode)
            }
        }
    )
    trailingContent()
}

/**
 * Widget for selecting notification auto-clear time.
 */
@Composable
fun MiuixAutoClearNotificationTimeWidget(
    modifier: Modifier = Modifier,
    currentValue: Int,
    onValueChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val options = remember { listOf(0, 3, 5, 10, 15, 20, 30) } // 0 means "Never"

    val entries = remember(options) {
        options.map { time ->
            val text = if (time == 0) {
                context.getString(R.string.installer_settings_auto_clear_time_never)
            } else {
                context.getString(R.string.installer_settings_auto_clear_time_seconds_format, time)
            }
            SpinnerEntry(title = text)
        }
    }

    val selectedIndex = remember(currentValue, options) {
        options.indexOf(currentValue).coerceAtLeast(0)
    }

    SuperSpinner(
        modifier = modifier,
        title = stringResource(id = R.string.installer_settings_auto_clear_success_notification),
        summary = stringResource(id = R.string.installer_settings_auto_clear_success_notification_desc),
        items = entries,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            val newValue = options[newIndex]
            if (currentValue != newValue) {
                onValueChange(newValue)
            }
        }
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
        title = stringResource(R.string.ignore_battery_optimizations),
        description = if (enabled) stringResource(R.string.ignore_battery_optimizations_desc)
        else stringResource(R.string.ignore_battery_optimizations_desc_disabled),
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange
    )
}

@Composable
fun MiuixAutoLockInstaller(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    MiuixSwitchWidget(
        title = stringResource(R.string.auto_lock_default_installer),
        description = stringResource(R.string.auto_lock_default_installer_desc),
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange
    )
}

@Composable
fun MiuixDefaultInstaller(
    lock: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    BasicComponent(
        title = stringResource(
            if (lock) R.string.lock_default_installer else R.string.unlock_default_installer
        ),
        summary = stringResource(
            if (lock) R.string.lock_default_installer_desc else R.string.unlock_default_installer_desc
        ),
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
fun MiuixClearCache() {
    val cacheState = rememberCacheInfo()
    BasicComponent(
        enabled = !cacheState.inProgress,
        title = stringResource(id = R.string.clear_cache),
        summary = cacheState.description,
        onClick = { cacheState.onClear() }
    )
}

@Composable
fun MiuixSettingsAboutItemWidget(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    onClick: () -> Unit
) {
    // Use the library's BasicComponent as the foundation.
    BasicComponent(
        modifier = modifier, // Pass the modifier to the root component.
        title = title,
        summary = summary,
        summaryColor = summaryColor,
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
fun MiuixNavigationItemWidget(
    icon: ImageVector? = null,
    title: String,
    description: String,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    onClick: () -> Unit
) {
    // Call the BaseWidget and pass the parameters accordingly.
    SuperArrow(
        title = title,
        summary = description,
        insideMargin = insideMargin,
        onClick = onClick
    )
}

/**
 * Theme Engine selection widget using SuperSpinner, following the provided pattern.
 * Simplified version without data class and icons.
 *
 * @param currentThemeIsMiuix True if MIUIX theme is selected, false if Google theme is selected.
 * @param onThemeChange Callback when the selection changes. Boolean parameter indicates new selection (true = MIUIX).
 */
@Composable
fun MiuixThemeEngineWidget(
    modifier: Modifier = Modifier,
    currentThemeIsMiuix: Boolean,
    onThemeChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    val themeOptions = remember {
        mapOf(
            true to R.string.theme_settings_miuix_ui, // Key = true -> MIUIX UI string resource
            false to R.string.theme_settings_google_ui // Key = false -> Google UI string resource
        )
    }

    // Convert map entries to List<SpinnerEntry> for SuperSpinner.
    // Ensure the order matches the keys: index 0 = true, index 1 = false.
    val spinnerEntries = remember(themeOptions) {
        themeOptions.entries.sortedByDescending { it.key }.map { entry ->
            SpinnerEntry(
                title = context.getString(entry.value)
            )
        }
    }

    // Determine selected index based on currentThemeIsMiuix state.
    // Index 0 corresponds to true (MIUIX), Index 1 corresponds to false (Google).
    val selectedIndex = remember(currentThemeIsMiuix) {
        if (currentThemeIsMiuix) 0 else 1
    }

    SuperSpinner(
        modifier = modifier,
        title = stringResource(id = R.string.theme_settings_ui_engine),
        // summary = spinnerEntries[selectedIndex].title,
        items = spinnerEntries,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            // Convert index back to boolean key (0 -> true, 1 -> false)
            val newModeIsMiuix = themeOptions.keys.sortedDescending().elementAt(newIndex)
            if (currentThemeIsMiuix != newModeIsMiuix) {
                onThemeChange(newModeIsMiuix)
            }
        }
    )
}

@Composable
fun MiuixManagedPackagesWidget(
    modifier: Modifier = Modifier,
    noContentTitle: String,
    noContentDescription: String = stringResource(R.string.config_add_one_to_get_started),
    packages: List<NamedPackage>,
    infoText: String? = null,
    isInfoVisible: Boolean = false,
    infoColor: Color = MiuixTheme.colorScheme.primary,
    onAddPackage: (NamedPackage) -> Unit,
    onRemovePackage: (NamedPackage) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<NamedPackage?>(null) }

    Column(modifier = modifier) {
        if (packages.isEmpty()) {
            BasicComponent(
                title = noContentTitle,
                summary = noContentDescription
            )
        } else {
            packages.forEach { item ->
                BasicComponent(
                    title = item.name,
                    summary = item.packageName,
                    endActions = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    MiuixTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.2f
                                    )
                                )
                                .clickable { showDeleteConfirmation = item }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.delete),
                                color = MiuixTheme.colorScheme.primary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            AnimatedVisibility(
                visible = isInfoVisible && !infoText.isNullOrBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(infoColor.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = infoText!!,
                        color = infoColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                modifier = Modifier.padding(bottom = 8.dp),
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            ) {
                Icon(
                    imageVector = AppIcons.Add,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.add),
                    color = MiuixTheme.colorScheme.primary
                )
            }
        }
    }

    if (showAddDialog) {
        MiuixAddPackageDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newItem ->
                onAddPackage(newItem)
                showAddDialog = false
            }
        )
    }

    showDeleteConfirmation?.let { itemToDelete ->
        MiuixDeleteNamedPackageConfirmationDialog(
            item = itemToDelete,
            onDismiss = { showDeleteConfirmation = null },
            onConfirm = {
                onRemovePackage(itemToDelete)
                showDeleteConfirmation = null
            }
        )
    }
}

/**
 * A SuperSpinner widget for selecting the application's theme mode (Light, Dark, or System).
 *
 * @param modifier The modifier to be applied to the SuperSpinner.
 * @param currentThemeMode The currently selected ThemeMode.
 * @param onThemeModeChange A callback that is invoked when the theme mode selection changes.
 */
@Composable
fun MiuixThemeModeWidget(
    modifier: Modifier = Modifier,
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val context = LocalContext.current

    // Map of ThemeMode enum to its corresponding string resource ID.
    val themeModeOptions = remember {
        // The order in the map definition determines the order in the spinner.
        mapOf(
            ThemeMode.LIGHT to R.string.theme_settings_theme_mode_light,
            ThemeMode.DARK to R.string.theme_settings_theme_mode_dark,
            ThemeMode.SYSTEM to R.string.theme_settings_theme_mode_system
        )
    }

    // Convert the map of options to a list of SpinnerEntry for the SuperSpinner component.
    // The order of items in the list is important for index mapping.
    val spinnerEntries = remember(themeModeOptions) {
        themeModeOptions.entries.map { entry ->
            SpinnerEntry(title = context.getString(entry.value))
        }
    }

    // Calculate the selected index based on the current theme mode.
    // It finds the index of the currentThemeMode in the ordered list of keys.
    val selectedIndex = remember(currentThemeMode, themeModeOptions) {
        themeModeOptions.keys.indexOf(currentThemeMode).coerceAtLeast(0)
    }

    SuperSpinner(
        modifier = modifier,
        title = stringResource(id = R.string.theme_settings_theme_mode),
        items = spinnerEntries,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            // Retrieve the new ThemeMode based on the selected index.
            val newMode = themeModeOptions.keys.elementAt(newIndex)
            // Invoke the callback only if the mode has actually changed.
            if (currentThemeMode != newMode) {
                onThemeModeChange(newMode)
            }
        }
    )
}

/**
 * A Miuix-style dialog for adding a new NamedPackage.
 *
 * @param onDismiss Callback invoked when the dialog is dismissed.
 * @param onConfirm Callback invoked with the new NamedPackage when confirmed.
 */
@Composable
private fun MiuixAddPackageDialog(
    onDismiss: () -> Unit,
    onConfirm: (NamedPackage) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    val isConfirmEnabled = name.isNotBlank() && packageName.isNotBlank()
    val showState = remember { mutableStateOf(true) }

    SuperDialog(
        show = showState,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.config_add_new_package),
        content = {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.config_name),
                    useLabelAsPlaceholder = true,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = stringResource(R.string.config_package_name),
                    useLabelAsPlaceholder = true,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.confirm),
                        onClick = { onConfirm(NamedPackage(name, packageName)) },
                        enabled = isConfirmEnabled,
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    )
}

/**
 * A Miuix-style dialog to confirm the deletion of a NamedPackage.
 *
 * @param item The item to be deleted.
 * @param onDismiss Callback invoked when the dialog is dismissed.
 * @param onConfirm Callback invoked when the deletion is confirmed.
 */
@Composable
private fun MiuixDeleteNamedPackageConfirmationDialog(
    item: NamedPackage,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val showState = remember { mutableStateOf(true) }

    SuperDialog(
        show = showState,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.config_confirm_deletion),
        content = {
            Column {
                Text(stringResource(R.string.config_confirm_deletion_desc, item.name))
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.delete),
                        colors = ButtonDefaults.textButtonColors(
                            textColor = MaterialTheme.colorScheme.error
                        ),
                        onClick = onConfirm
                    )
                }
            }
        }
    )
}

/**
 * A reusable Miuix-style widget to display and manage a list of SharedUid items.
 * It is stateless and relies on callbacks to handle data modifications.
 *
 * @param modifier The modifier to be applied to the widget's container.
 * @param noContentTitle The title to display if no uids are available.
 * @param uids The list of SharedUid items to display.
 * @param onAddUid A callback invoked when a new uid should be added.
 * @param onRemoveUid A callback invoked when an existing uid should be removed.
 */
@Composable
fun MiuixManagedUidsWidget(
    modifier: Modifier = Modifier,
    noContentTitle: String,
    uids: List<SharedUid>,
    onAddUid: (SharedUid) -> Unit,
    onRemoveUid: (SharedUid) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<SharedUid?>(null) }

    // Main container for the widget
    Column(modifier = modifier) {
        // Display each UID in the list or a placeholder message
        if (uids.isEmpty()) {
            BasicComponent(
                title = noContentTitle,
                summary = stringResource(R.string.config_add_one_to_get_started)
            )
        } else {
            uids.forEach { item ->
                BasicComponent(
                    title = item.uidName,
                    summary = "UID: ${item.uidValue}",
                    endActions = {
                        // Custom delete button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50)) // Pill shape
                                .background(
                                    MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                )
                                .clickable { showDeleteConfirmation = item }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.delete),
                                color = MiuixTheme.colorScheme.primary,
                                style = MiuixTheme.textStyles.button
                            )
                        }
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                modifier = Modifier.padding(bottom = 8.dp),
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            ) {
                Icon(
                    imageVector = AppIcons.Add,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.add),
                    color = MiuixTheme.colorScheme.primary
                )
            }
        }
    }

    // --- Dialogs ---

    // Dialog for adding a new UID
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
                onRemoveUid(uidToDelete)
                showDeleteConfirmation = null
            }
        )
    }
}

/**
 * A Miuix-style dialog for adding a new SharedUid.
 *
 * @param onDismiss Callback invoked when the dialog is dismissed.
 * @param onConfirm Callback invoked with the new SharedUid when confirmed.
 */
@Composable
private fun MiuixAddUidDialog(
    onDismiss: () -> Unit,
    onConfirm: (SharedUid) -> Unit
) {
    var uidName by remember { mutableStateOf("") }
    var uidValueString by remember { mutableStateOf("") }
    val showState = remember { mutableStateOf(true) }

    // Confirm button is enabled if both name and value are not blank and value is a valid integer.
    val isConfirmEnabled = uidName.isNotBlank() && uidValueString.toIntOrNull() != null

    SuperDialog(
        show = showState,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.config_add_new_shared_uid),
        content = {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = uidName,
                    onValueChange = { uidName = it },
                    label = stringResource(R.string.config_shared_uid_name),
                    useLabelAsPlaceholder = true,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = uidValueString,
                    onValueChange = { uidValueString = it },
                    label = stringResource(R.string.config_shared_uid_value),
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.confirm),
                        onClick = {
                            val uidValue = uidValueString.toInt()
                            onConfirm(SharedUid(uidName, uidValue))
                        },
                        enabled = isConfirmEnabled,
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    )
}

/**
 * A Miuix-style dialog to confirm the deletion of a SharedUid.
 *
 * @param item The item to be deleted.
 * @param onDismiss Callback invoked when the dialog is dismissed.
 * @param onConfirm Callback invoked when the deletion is confirmed.
 */
@Composable
private fun MiuixDeleteSharedUidConfirmationDialog(
    item: SharedUid,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val showState = remember { mutableStateOf(true) }

    SuperDialog(
        show = showState,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.config_confirm_deletion),
        content = {
            Column {
                Text(stringResource(R.string.config_confirm_deletion_desc, item.uidName))
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.delete),
                        colors = ButtonDefaults.textButtonColors(
                            textColor = MaterialTheme.colorScheme.error
                        ),
                        onClick = onConfirm
                    )
                }
            }
        }
    )
}

@Composable
fun MiuixUninstallKeepDataWidget(viewModel: PreferredViewModel) {
    MiuixSwitchWidget(
        title = stringResource(id = R.string.uninstall_keep_data),
        description = stringResource(id = R.string.uninstall_keep_data_desc),
        checked = viewModel.state.uninstallFlags.hasFlag(PackageManagerUtil.DELETE_KEEP_DATA),
        onCheckedChange = {
            viewModel.dispatch(PreferredViewAction.ToggleGlobalUninstallFlag(PackageManagerUtil.DELETE_KEEP_DATA, it))
        }
    )
}

@Composable
fun MiuixUninstallForAllUsersWidget(viewModel: PreferredViewModel) {
    MiuixSwitchWidget(
        icon = AppIcons.InstallForAllUsers,
        title = stringResource(id = R.string.uninstall_all_users),
        description = stringResource(id = R.string.uninstall_all_users_desc),
        checked = viewModel.state.uninstallFlags.hasFlag(PackageManagerUtil.DELETE_ALL_USERS),
        onCheckedChange = {
            viewModel.dispatch(PreferredViewAction.ToggleGlobalUninstallFlag(PackageManagerUtil.DELETE_ALL_USERS, it))
        }
    )
}

@Composable
fun MiuixUninstallSystemAppWidget(viewModel: PreferredViewModel) {
    MiuixSwitchWidget(
        title = stringResource(id = R.string.uninstall_delete_system_app),
        description = stringResource(id = R.string.uninstall_delete_system_app_desc),
        checked = viewModel.state.uninstallFlags.hasFlag(PackageManagerUtil.DELETE_SYSTEM_APP),
        onCheckedChange = {
            viewModel.dispatch(
                PreferredViewAction.ToggleGlobalUninstallFlag(
                    PackageManagerUtil.DELETE_SYSTEM_APP,
                    it
                )
            )
        }
    )
}

@Composable
fun MiuixUninstallRequireBiometricAuthWidget(viewModel: PreferredViewModel) {
    if (BiometricManager
            .from(LocalContext.current)
            .canAuthenticate(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    ) {
        MiuixSwitchWidget(
            icon = AppIcons.BiometricAuth,
            title = stringResource(R.string.uninstaller_settings_require_biometric_auth),
            description = stringResource(R.string.uninstaller_settings_require_biometric_auth_desc),
            checked = viewModel.state.uninstallerRequireBiometricAuth,
            onCheckedChange = {
                viewModel.dispatch(PreferredViewAction.ChangeBiometricAuth(it, false))
            }
        )
    }
}
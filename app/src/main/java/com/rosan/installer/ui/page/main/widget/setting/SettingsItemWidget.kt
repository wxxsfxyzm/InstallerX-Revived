package com.rosan.installer.ui.page.main.widget.setting

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.datastore.entity.SharedUid
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.util.MIN_FEEDBACK_DURATION_MS
import com.rosan.installer.ui.util.formatSize
import com.rosan.installer.ui.util.getDirectorySize
import com.rosan.installer.util.openUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @author iamr0s
 */
/*@Composable
fun DataAuthorizerWidget(viewModel: PreferredViewModel) {
    val authorizer = viewModel.state.authorizer
    val data = mapOf(
        ConfigEntity.Authorizer.None to stringResource(R.string.config_authorizer_none),
        ConfigEntity.Authorizer.Root to stringResource(R.string.config_authorizer_root),
        ConfigEntity.Authorizer.Shizuku to stringResource(R.string.config_authorizer_shizuku),
        ConfigEntity.Authorizer.Dhizuku to stringResource(R.string.config_authorizer_dhizuku),
        ConfigEntity.Authorizer.Customize to stringResource(R.string.config_authorizer_customize),
    )
    DropDownMenuWidget(
        icon = Icons.TwoTone.Memory,
        title = stringResource(R.string.config_authorizer),
        description = if (data.containsKey(authorizer)) data[authorizer] else null,
        choice = data.keys.toList().indexOf(authorizer),
        data = data.values.toList(),
    ) {
        data.keys.toList().getOrNull(it)?.let {
            viewModel.dispatch(PreferredViewAction.ChangeGlobalAuthorizer(it))
        }
    }
}*/

data class AuthorizerInfo(
    @param:StringRes val labelResId: Int,
    val icon: ImageVector
)

/**
 * @author wxxsfxyzm
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DataAuthorizerWidget(
    modifier: Modifier = Modifier,
    currentAuthorizer: ConfigEntity.Authorizer,
    changeAuthorizer: (ConfigEntity.Authorizer) -> Unit,
    trailingContent: @Composable () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val shizukuIcon = ImageVector.vectorResource(R.drawable.ic_shizuku)

    val authorizerOptions = mapOf(
        ConfigEntity.Authorizer.None to AuthorizerInfo(
            R.string.config_authorizer_none,
            AppIcons.None
        ),
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
        /*        ConfigEntity.Authorizer.Customize to AuthorizerInfo(
                    R.string.config_authorizer_customize,
                    AppIcons.Customize
                ),*/
    )

    ListItem(
        // 左侧图标，使用原代码的图标
        leadingContent = {
            Icon(
                imageVector = AppIcons.Authorizer,
                contentDescription = null
            )
        },
        // 标题
        headlineContent = { Text(stringResource(R.string.config_authorizer)) },
        // 下方的 InputChip 区域
        supportingContent = {
            // 使用 FlowRow 可以让 Chip 自动换行，适应不同宽度的屏幕
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                modifier = modifier
            ) {
                // 遍历 Map 来动态创建 InputChip
                authorizerOptions.forEach { (authorizerType, authorizerInfo) ->
                    InputChip(
                        enabled = when (authorizerType) {
                            ConfigEntity.Authorizer.None -> !RsConfig.isMiui
                            else -> true
                        },
                        selected = currentAuthorizer == authorizerType,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            if (currentAuthorizer != authorizerType) {
                                changeAuthorizer(authorizerType)
                            }
                        },
                        label = { Text(text = stringResource(authorizerInfo.labelResId)) },
                        // 为每个 Chip 设置 leadingIcon
                        leadingIcon = {
                            Icon(
                                imageVector = authorizerInfo.icon,
                                contentDescription = null, // 装饰性图标
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        }
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        /*        // 整个 ListItem 的点击事件
                modifier = Modifier.clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onClick()
                }*/
    )

    trailingContent()
}

@Composable
fun DataCustomizeAuthorizerWidget(viewModel: PreferredViewModel) {
    if (!viewModel.state.authorizerCustomize) return
    val customizeAuthorizer = viewModel.state.customizeAuthorizer
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .focusable(),
        leadingIcon = {
            Icon(imageVector = AppIcons.Terminal, contentDescription = null)
        },
        label = {
            Text(stringResource(R.string.config_customize_authorizer))
        },
        value = customizeAuthorizer,
        onValueChange = { viewModel.dispatch(PreferredViewAction.ChangeGlobalCustomizeAuthorizer(it)) },
        maxLines = 8,
    )
}

/**
 * @author iamr0s
 */
/*@Composable
fun DataInstallModeWidget(viewModel: PreferredViewModel) {
    val installMode = viewModel.state.installMode
    val data = mapOf(
        ConfigEntity.InstallMode.Dialog to stringResource(R.string.config_install_mode_dialog),
        ConfigEntity.InstallMode.AutoDialog to stringResource(R.string.config_install_mode_auto_dialog),
        ConfigEntity.InstallMode.Notification to stringResource(R.string.config_install_mode_notification),
        ConfigEntity.InstallMode.AutoNotification to stringResource(R.string.config_install_mode_auto_notification),
        ConfigEntity.InstallMode.Ignore to stringResource(R.string.config_install_mode_ignore),
    )
    DropDownMenuWidget(
        icon = Icons.TwoTone.Downloading,
        title = stringResource(R.string.config_install_mode),
        description = if (data.containsKey(installMode)) data[installMode] else null,
        choice = data.keys.toList().indexOf(installMode),
        data = data.values.toList(),
    ) {
        data.keys.toList().getOrNull(it)?.let {
            viewModel.dispatch(PreferredViewAction.ChangeGlobalInstallMode(it))
        }
    }
}*/

data class InstallModeInfo(
    @param:StringRes val labelResId: Int,
    val icon: ImageVector
)

/**
 * @author wxxsfxyzm
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DataInstallModeWidget(
    modifier: Modifier = Modifier,
    currentInstallMode: ConfigEntity.InstallMode,
    changeInstallMode: (ConfigEntity.InstallMode) -> Unit,
    onClick: () -> Unit = {} // 提供一个默认的空实现
) {
    val haptic = LocalHapticFeedback.current

    // 使用新的数据类来定义选项
    val installModeOptions = mapOf(
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

    ListItem(
        leadingContent = {
            Icon(
                imageVector = AppIcons.InstallMode, // 来自原代码的图标
                contentDescription = null
            )
        },
        headlineContent = { Text(stringResource(R.string.config_install_mode)) }, // 来自原代码的标题
        supportingContent = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                modifier = modifier
            ) {
                installModeOptions.forEach { (modeType, modeInfo) ->
                    InputChip(
                        selected = currentInstallMode == modeType,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            if (currentInstallMode != modeType) {
                                changeInstallMode(modeType)
                            }
                        },
                        label = { Text(text = stringResource(modeInfo.labelResId)) },
                        leadingIcon = {
                            Icon(
                                imageVector = modeInfo.icon,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        }
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun DisableAdbVerify(
    checked: Boolean,
    isError: Boolean,
    enabled: Boolean,
    isM3E: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    SwitchWidget(
        icon = AppIcons.DisableAdbVerify,
        title = stringResource(R.string.disable_adb_install_verify),
        description = if (!isError) stringResource(R.string.disable_adb_install_verify_desc)
        else stringResource(R.string.disable_adb_install_verify_not_support_dhizuku_desc),
        isError = isError,
        checked = checked,
        enabled = enabled,
        isM3E = isM3E,
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
fun IgnoreBatteryOptimizationSetting(
    checked: Boolean,
    enabled: Boolean,
    isM3E: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    SwitchWidget(
        icon = AppIcons.BatteryOptimization,
        title = stringResource(R.string.ignore_battery_optimizations),
        description = if (enabled) stringResource(R.string.ignore_battery_optimizations_desc)
        else stringResource(R.string.ignore_battery_optimizations_desc_disabled),
        checked = checked,
        enabled = enabled,
        isM3E = isM3E,
        onCheckedChange = onCheckedChange
    )
}

@Composable
fun AutoLockInstaller(
    checked: Boolean,
    enabled: Boolean,
    isM3E: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    SwitchWidget(
        icon = AppIcons.BugReport,
        title = stringResource(R.string.auto_lock_default_installer),
        description = stringResource(R.string.auto_lock_default_installer_desc),
        checked = checked,
        enabled = enabled,
        isM3E = isM3E,
        onCheckedChange = onCheckedChange
    )
}

@Composable
fun DefaultInstaller(lock: Boolean, onClick: () -> Unit) {
    BaseWidget(
        icon = if (lock) AppIcons.LockDefault else AppIcons.UnlockDefault,
        title =
            stringResource(if (lock) R.string.lock_default_installer else R.string.unlock_default_installer),
        description =
            stringResource(if (lock) R.string.lock_default_installer_desc else R.string.unlock_default_installer_desc),
        onClick = onClick
    ) {}
}

@Composable
fun ClearCache() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inProgress by remember {
        mutableStateOf(false)
    }
    var cacheSize by remember { mutableLongStateOf(0L) }
    // A trigger to recalculate the cache size
    var calculationTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(calculationTrigger) {
        withContext(Dispatchers.IO) {
            val internalCache = context.cacheDir?.getDirectorySize() ?: 0L
            val externalCache = context.externalCacheDir?.getDirectorySize() ?: 0L
            cacheSize = internalCache + externalCache
        }
    }

    BaseWidget(
        icon = AppIcons.ClearAll,
        title = stringResource(id = R.string.clear_cache),
        description = if (inProgress) stringResource(R.string.clearing_cache)
        else if (cacheSize == 0L) stringResource(R.string.no_cache)
        else stringResource(
            R.string.cache_size,
            cacheSize.formatSize()
        ),
        enabled = !inProgress,
        onClick = {
            if (inProgress) return@BaseWidget

            scope.launch {
                inProgress = true
                val startTime = System.currentTimeMillis()

                // Perform the actual clearing operation on the IO dispatcher
                withContext(Dispatchers.IO) {
                    val paths = listOfNotNull(
                        context.cacheDir,
                        context.externalCacheDir
                    )

                    fun clearFile(file: File) {
                        if (!file.exists()) return
                        if (file.isDirectory) {
                            file.listFiles()?.forEach {
                                clearFile(it)
                            }
                        }
                        file.delete()
                    }
                    paths.forEach { clearFile(it) }
                }

                val elapsedTime = System.currentTimeMillis() - startTime

                // If the operation was too fast, wait for the remaining time
                if (elapsedTime < MIN_FEEDBACK_DURATION_MS) {
                    delay(MIN_FEEDBACK_DURATION_MS - elapsedTime)
                }

                cacheSize = 0L
                inProgress = false
                // Trigger a recalculation of the cache size
                calculationTrigger++
            }
        }
    ) {}
}

@Composable
fun SettingsAboutItemWidget(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    imageContentDescription: String? = null,
    headlineContentText: String,
    supportingContentText: String? = null,
    onClick: () -> Unit
) {
    BaseWidget(
        icon = imageVector,
        title = headlineContentText,
        description = supportingContentText,
        onClick = onClick
    ) {
        // This pkg has no trailing content, so this lambda is empty.
    }
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
fun SettingsNavigationItemWidget(
    icon: ImageVector? = null,
    iconPlaceholder: Boolean = true,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    // Call the BaseWidget and pass the parameters accordingly.
    BaseWidget(
        icon = icon,
        iconPlaceholder = iconPlaceholder,
        title = title,
        description = description,
        onClick = onClick
    ) {
        // The content lambda of BaseWidget is used for the trailing content.
        // We place the navigation arrow Icon here.
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null
        )
    }
}

/**
 * A custom composable for radio-button-like selection within a setting list.
 * Mimics the appearance of SwitchWidget but provides selection behavior.
 */
@Composable
fun SelectableSettingItem(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium) // Ensure consistent shape for click feedback
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp), // Adjust padding to match SwitchWidget
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun BottomSheetContent(
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
                context.openUrl("https://github.com/wxxsfxyzm/InstallerX-Revived/releases")
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
                context.openUrl("https://t.me/installerx_revived")
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
fun ManagedPackagesWidget(
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
        AddPackageDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newItem ->
                onAddPackage(newItem) // Use the callback
                showAddDialog = false
            }
        )
    }

    // Dialog for confirming deletion
    showDeleteConfirmation?.let { itemToDelete ->
        DeleteNamedPackageConfirmationDialog(
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
 * @param uids The list of SharedUid items to display.
 * @param onAddUid A callback invoked when a new uid should be added.
 * @param onRemoveUid A callback invoked when an existing uid should be removed.
 * @param modifier The modifier to be applied to the widget's container.
 */
@Composable
fun ManagedUidsWidget(
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
        AddUidDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newUID ->
                onAddUid(newUID)
                showAddDialog = false
            }
        )
    }

    // Dialog for confirming deletion
    showDeleteConfirmation?.let { uidToDelete ->
        DeleteSharedUidConfirmationDialog(
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
private fun AddPackageDialog(
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
private fun AddUidDialog(
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
private fun DeleteNamedPackageConfirmationDialog(
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
private fun DeleteSharedUidConfirmationDialog(
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
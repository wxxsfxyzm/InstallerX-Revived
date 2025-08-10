package com.rosan.installer.ui.widget.setting

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.model.impl.DSRepoImpl
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.util.ConfigUtil
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.settings.preferred.PreferredViewModel
import com.rosan.installer.util.help
import com.rosan.installer.util.openUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    // 直接传入当前选中的授权者，而不是整个 ViewModel
    currentAuthorizer: ConfigEntity.Authorizer,
    // 使用一个回调函数来处理变更
    changeAuthorizer: (ConfigEntity.Authorizer) -> Unit,
    trailingContent: @Composable () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val shizukuIcon = ImageVector.vectorResource(R.drawable.ic_shizuku)
    // 数据源和原代码保持一致
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
        ),
        /*modifier = Modifier.clickable {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        }*/
    )
}

@Composable
fun DefaultInstaller(snackBarHostState: SnackbarHostState, lock: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exception: Throwable by remember {
        mutableStateOf(Throwable())
    }
    var showException by remember {
        mutableStateOf(false)
    }

    fun workIt() {
        synchronized(scope) {
            scope.launch(Dispatchers.IO) {
                val exceptionOrNull = runCatching {
                    DSRepoImpl.doWork(ConfigUtil.getByPackageName(null), lock)
                }.exceptionOrNull()
                exceptionOrNull?.printStackTrace()

                snackBarHostState.currentSnackbarData?.dismiss()
                if (exceptionOrNull == null) snackBarHostState.showSnackbar(
                    context.getString(
                        if (lock) R.string.lock_default_installer_success
                        else R.string.unlock_default_installer_success
                    )
                )
                else {
                    val result = snackBarHostState.showSnackbar(
                        context.getString(
                            if (lock) R.string.lock_default_installer_failed
                            else R.string.unlock_default_installer_failed
                        ),
                        context.getString(R.string.details),
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        exception = exceptionOrNull
                        showException = true
                    }
                }
            }
        }
    }

    BaseWidget(
        icon = if (lock) AppIcons.LockDefault else AppIcons.UnlockDefault,
        title =
            stringResource(if (lock) R.string.lock_default_installer else R.string.unlock_default_installer),
        description =
            stringResource(if (lock) R.string.lock_default_installer_dsp else R.string.unlock_default_installer_dsp),
        onClick = {
            workIt()
        }
    ) {}
    if (!showException) return
    AlertDialog(onDismissRequest = {
        showException = false
    }, title = {
        Text(stringResource(if (lock) R.string.lock_default_installer_failed else R.string.unlock_default_installer_failed))
    }, text = {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onErrorContainer) {
            LazyColumn(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .fillMaxWidth()
                    .padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(exception.help(), fontWeight = FontWeight.Bold)
                }
                item {
                    SelectionContainer {
                        Text(exception.stackTraceToString().trim())
                    }
                }
            }
        }
    }, confirmButton = {
        TextButton(onClick = {
            showException = false
            workIt()
        }) {
            Text(stringResource(R.string.retry))
        }
    }, dismissButton = {
        TextButton(onClick = {
            showException = false
        }) {
            Text(stringResource(R.string.cancel))
        }
    })
}

@Composable
fun ClearCache() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inProgress by remember {
        mutableStateOf(false)
    }
    BaseWidget(
        icon = AppIcons.ClearAll,
        title = stringResource(id = R.string.clear_cache),
        onClick = {
            if (inProgress) return@BaseWidget
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
        // This item has no trailing content, so this lambda is empty.
    }
}

/**
 * A setting item that navigates to a secondary page, built upon BaseWidget.
 * It includes an icon, title, description, and a trailing arrow.
 *
 * @param icon The leading icon for the item.
 * @param title The main title text of the item.
 * @param description The supporting description text.
 * @param onClick The callback to be invoked when this item is clicked.
 */
@Composable
fun SettingsNavigationItemWidget(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    // Call the BaseWidget and pass the parameters accordingly.
    BaseWidget(
        icon = icon,
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
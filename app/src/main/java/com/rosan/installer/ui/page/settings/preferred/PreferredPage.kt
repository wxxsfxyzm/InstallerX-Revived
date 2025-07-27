package com.rosan.installer.ui.page.settings.preferred

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.build.Level
import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.app.model.impl.DSRepoImpl
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.util.ConfigUtil
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.settings.SettingsScreen
import com.rosan.installer.ui.widget.setting.BaseWidget
import com.rosan.installer.ui.widget.setting.IntNumberPickerWidget
import com.rosan.installer.ui.widget.setting.LabelWidget
import com.rosan.installer.ui.widget.setting.SettingsAboutItemWidget
import com.rosan.installer.ui.widget.setting.SwitchWidget
import com.rosan.installer.util.help
import com.rosan.installer.util.openUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferredPage(
    navController: NavController,
    windowInsets: WindowInsets,
    viewModel: PreferredViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val revLevel = when (RsConfig.LEVEL) {
        Level.STABLE -> stringResource(id = R.string.stable)
        Level.PREVIEW -> stringResource(id = R.string.preview)
        Level.UNSTABLE -> stringResource(id = R.string.unstable)
    }

    LaunchedEffect(true) {
        viewModel.dispatch(PreferredViewAction.Init)
    }

    val snackBarHostState = remember {
        SnackbarHostState()
    }
    var showBottomSheet by remember {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            //.windowInsetsPadding(windowInsets)
            .fillMaxSize(),
        contentWindowInsets = windowInsets,
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = {
                    Text(text = stringResource(id = R.string.preferred))
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item { LabelWidget(stringResource(R.string.global)) }
            // item { DataAuthorizerWidget(viewModel) }
            item {
                DataAuthorizerWidget(
                    currentAuthorizer = state.authorizer,
                    changeAuthorizer = { newAuthorizer ->
                        viewModel.dispatch(PreferredViewAction.ChangeGlobalAuthorizer(newAuthorizer))
                    },
                    onClick = {}
                )
            }
            item {
                AnimatedVisibility(
                    visible = state.authorizer == ConfigEntity.Authorizer.Dhizuku,
                    enter = fadeIn() + expandVertically(), // 进入动画：淡入 + 垂直展开
                    exit = fadeOut() + shrinkVertically()  // 退出动画：淡出 + 垂直收起
                ) {
                    IntNumberPickerWidget(
                        icon = AppIcons.Working,
                        title = stringResource(R.string.set_countdown),
                        description = stringResource(R.string.dhizuku_auto_close_countdown_desc),
                        value = state.dhizukuAutoCloseCountDown,
                        startInt = 1,
                        endInt = 10
                    ) {
                        viewModel.dispatch(
                            PreferredViewAction.ChangeDhizukuAutoCloseCountDown(it)
                        )
                    }
                }
            }
            // item { DataCustomizeAuthorizerWidget(viewModel) }
            // item { DataInstallModeWidget(viewModel) }
            item {
                DataInstallModeWidget(
                    currentInstallMode = state.installMode,
                    changeInstallMode = { newMode ->
                        viewModel.dispatch(PreferredViewAction.ChangeGlobalInstallMode(newMode))
                    },
                    onClick = {}
                )
            }
            item {
                // Outer AnimatedVisibility: Controls the visibility of the entire section.
                // It handles the main expansion/shrinking and fading for the whole group.
                AnimatedVisibility(
                    visible = state.installMode == ConfigEntity.InstallMode.Dialog || state.installMode == ConfigEntity.InstallMode.AutoDialog,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.animateContentSize()) {
                        // Inner AnimatedVisibility: Specifically controls the first SwitchWidget.
                        // Its animation should be simple and not conflict with the parent's size animation.
                        // Using only fadeIn/fadeOut is perfect for this.
                        AnimatedVisibility(
                            visible = state.installMode == ConfigEntity.InstallMode.Dialog,
                            enter = fadeIn(), // Only fade in, let the parent handle the expansion.
                            exit = fadeOut()  // Only fade out, let the parent handle the shrinking.
                        ) {
                            // This SwitchWidget will now smoothly fade in/out when toggling
                            // between Dialog and AutoDialog modes.
                            SwitchWidget(
                                icon = AppIcons.MenuOpen,
                                title = stringResource(id = R.string.show_dialog_install_extended_menu),
                                description = stringResource(id = R.string.show_dialog_install_extended_menu_desc),
                                checked = viewModel.state.showDialogInstallExtendedMenu,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        PreferredViewAction.ChangeShowDialogInstallExtendedMenu(it)
                                    )
                                }
                            )
                        }
                        // These SwitchWidgets are always present when the container is visible.
                        SwitchWidget(
                            icon = AppIcons.Suggestion,
                            title = stringResource(id = R.string.show_intelligent_suggestion),
                            description = stringResource(id = R.string.show_intelligent_suggestion_desc),
                            checked = viewModel.state.showIntelligentSuggestion,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    PreferredViewAction.ChangeShowIntelligentSuggestion(it)
                                )
                            }
                        )
                        SwitchWidget(
                            icon = AppIcons.NotificationDisabled,
                            title = stringResource(id = R.string.disable_notification),
                            description = stringResource(id = R.string.close_immediately_on_dialog_dismiss),
                            checked = viewModel.state.disableNotificationForDialogInstall,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    PreferredViewAction.ChangeShowDisableNotificationForDialogInstall(
                                        it
                                    )
                                )
                            }
                        )
                    }
                }
            }
            item {
                AnimatedVisibility(
                    visible = state.installMode == ConfigEntity.InstallMode.Notification ||
                            state.installMode == ConfigEntity.InstallMode.AutoNotification,
                    enter = fadeIn() + expandVertically(), // 进入动画：淡入 + 垂直展开
                    exit = fadeOut() + shrinkVertically()  // 退出动画：淡出 + 垂直收起
                ) {
                    Column(modifier = Modifier.animateContentSize()) {
                        SwitchWidget(
                            icon = AppIcons.Dialog,
                            title = stringResource(id = R.string.show_dialog_when_pressing_notification),
                            description = stringResource(id = R.string.change_notification_touch_behavior),
                            checked = viewModel.state.showDialogWhenPressingNotification,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    PreferredViewAction.ChangeShowDialogWhenPressingNotification(it)
                                )
                            }
                        )
                        AnimatedVisibility(
                            visible = viewModel.state.showDialogWhenPressingNotification,
                            enter = fadeIn(), // Only fade in, let the parent handle the expansion.
                            exit = fadeOut()  // Only fade out, let the parent handle the shrinking.
                        ) {
                            SwitchWidget(
                                icon = AppIcons.NotificationDisabled,
                                title = stringResource(id = R.string.disable_notification_on_dismiss),
                                description = stringResource(id = R.string.close_notification_immediately_on_dialog_dismiss),
                                checked = viewModel.state.disableNotificationForDialogInstall,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        PreferredViewAction.ChangeShowDisableNotificationForDialogInstall(it)
                                    )
                                }
                            )
                        }
                    }
                }
            }
            item { LabelWidget(stringResource(R.string.basic)) }
            item { DefaultInstaller(snackBarHostState, true) }
            item { DefaultInstaller(snackBarHostState, false) }
            item { ClearCache() }
            // item { LabelWidget(label = stringResource(id = R.string.more)) }
            // item { UserTerms() }
            // item { PrivacyPolicy() }
            item { LabelWidget(stringResource(R.string.other)) }
            item {
                SettingsAboutItemWidget(
                    imageVector = AppIcons.Info,
                    headlineContentText = stringResource(R.string.about_detail),
                    supportingContentText = "$revLevel ${RsConfig.VERSION_NAME}",
                    onClick = { navController.navigate(SettingsScreen.About.route) }
                )
            }
            item {
                SettingsAboutItemWidget(
                    imageVector = AppIcons.Update,
                    headlineContentText = stringResource(R.string.get_update),
                    supportingContentText = stringResource(R.string.get_update_detail),
                    onClick = { showBottomSheet = true }
                )
            }
        }
    }
    if (showBottomSheet) ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
        BottomSheetContent(
            context = context,
            title = stringResource(R.string.get_update)
        )
    }
}

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
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

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
            AppIcons.Android
        ),
        ConfigEntity.Authorizer.Dhizuku to AuthorizerInfo(
            R.string.config_authorizer_dhizuku,
            AppIcons.Android
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
        // 整个 ListItem 的点击事件
        modifier = Modifier.clickable {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        }
    )
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
        ),
        /*        ConfigEntity.InstallMode.Ignore to InstallModeInfo(
                    R.string.config_install_mode_ignore,
                    AppIcons.Ignore
                ),*/
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
        modifier = Modifier.clickable {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        }
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

/*@Composable
fun UserTerms() {
    val context = LocalContext.current
    BaseWidget(
        icon = Icons.TwoTone.Gavel,
        title = stringResource(id = R.string.user_terms),
        onClick = {
            openUrl(context, "https://iamr0s.github.io/InstallerXDocs/terms")
        }
    ) {}
}*/

/*
@Composable
fun PrivacyPolicy() {
    val context = LocalContext.current
    BaseWidget(
        icon = Icons.TwoTone.PrivacyTip,
        title = stringResource(id = R.string.privacy_policy),
        onClick = {
            openUrl(context, "https://iamr0s.github.io/InstallerXDocs/privacy")
        }
    ) {}
}*/

@Composable
private fun BottomSheetContent(
    context: Context,
    title: String
) {
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

        Spacer(modifier = Modifier.size(60.dp)) // 按钮下方留白
    }
}
package com.rosan.installer.ui.page.settings.config.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.automirrored.twotone.More
import androidx.compose.material.icons.automirrored.twotone.TrendingDown
import androidx.compose.material.icons.twotone.BugReport
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Downloading
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.Face
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.Memory
import androidx.compose.material.icons.twotone.People
import androidx.compose.material.icons.twotone.PsychologyAlt
import androidx.compose.material.icons.twotone.Save
import androidx.compose.material.icons.twotone.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.theme.none
import com.rosan.installer.ui.widget.setting.DropDownMenuWidget
import com.rosan.installer.ui.widget.setting.LabelWidget
import com.rosan.installer.ui.widget.setting.SwitchWidget
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.absoluteValue

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun EditPage(
    navController: NavController,
    id: Long? = null,
    viewModel: EditViewModel = koinViewModel { parametersOf(id) }
) {
    LaunchedEffect(true) {
        viewModel.dispatch(EditViewAction.Init)
    }

    val showFloatingState = remember {
        mutableStateOf(true)
    }
    val showFloating by showFloatingState
    val listState = rememberLazyListState()
    // 新增: 判断是否滚动到底部的状态
    val isScrolledToEnd by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0) {
                false
            } else {
                val lastVisibleItem = visibleItemsInfo.lastOrNull()
                // 如果最后一个可见项的索引是列表总数减一，则认为到达了底部
                lastVisibleItem?.index == layoutInfo.totalItemsCount - 1
            }
        }
    }
    val snackBarHostState = remember {
        SnackbarHostState()
    }
    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditViewEvent.SnackBar -> {
                    snackBarHostState.showSnackbar(
                        message = event.message,
                        withDismissAction = true,
                    )
                }

                is EditViewEvent.Saved -> {
                    navController.navigateUp()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .nestedScroll(ShowFloatingActionButtonNestedScrollConnection(showFloatingState)),
        contentWindowInsets = WindowInsets.none,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = if (id == null) R.string.add else R.string.update))
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        shapes = IconButtonShapes(
                            shape = IconButtonDefaults.smallRoundShape,
                            pressedShape = IconButtonDefaults.smallPressedShape
                        ),
                        colors = IconButtonDefaults.iconButtonColors(
                            // 指定“启用”状态下的内容（图标）颜色
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,

                            // （可选）指定“启用”状态下的容器（背景）颜色
                            containerColor = MaterialTheme.colorScheme.primaryContainer, // 标准 IconButton 背景是透明的

                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.TwoTone.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                // 新增: 当滚动到底部时，在 actions 中显示 FAB
                actions = {
                    AnimatedVisibility(
                        visible = isScrolledToEnd, // 只有在滚动到底部时可见
                        enter = scaleIn(),
                        exit = scaleOut()
                    ) {
                        IconButton(
                            onClick = { viewModel.dispatch(EditViewAction.SaveData) },
                            shapes = IconButtonShapes(
                                shape = IconButtonDefaults.smallRoundShape,
                                pressedShape = IconButtonDefaults.smallPressedShape
                            ),
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.primaryContainer, // 标准 IconButton 背景是透明的
                            )
                        ) {
                            Icon(
                                imageVector = Icons.TwoTone.Save,
                                contentDescription = stringResource(R.string.save)
                            )
                        }
                    }
                }
            )
        },
        // 修改: 只有在未滚动到底部时，才在右下角显示 FAB
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isScrolledToEnd && showFloating, // 在未滚动到底部且 showFloating 为 true 时可见
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                val text = stringResource(R.string.save)
                SmallExtendedFloatingActionButton(
                    icon = {
                        Icon(
                            imageVector = Icons.TwoTone.Save,
                            contentDescription = text
                        )
                    },
                    text = {
                        Text(text)
                    },
                    onClick = {
                        viewModel.dispatch(EditViewAction.SaveData)
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            state = listState, // 关键: 将 state 传入 LazyColumn
        ) {
            item { DataNameWidget(viewModel = viewModel) }
            item { DataDescriptionWidget(viewModel = viewModel) }
            item { DataAuthorizerWidget(viewModel = viewModel) }
            item { DataCustomizeAuthorizerWidget(viewModel = viewModel) }
            item { DataInstallModeWidget(viewModel = viewModel) }
            item { LabelWidget(label = "Install Settings") }
            item { DataDeclareInstallerWidget(viewModel = viewModel) }
            item { DataInstallerWidget(viewModel = viewModel) }
            item { DataAutoDeleteWidget(viewModel = viewModel) }
            item { DisplaySdkWidget(viewModel = viewModel) }
            item { LabelWidget(label = "Install Flags") }
            item { DataForAllUserWidget(viewModel = viewModel) }
            item { DataAllowTestOnlyWidget(viewModel = viewModel) }
            item { DataAllowDowngradeWidget(viewModel = viewModel) }

        }
    }
}

class ShowFloatingActionButtonNestedScrollConnection(
    private val showFloatingState: MutableState<Boolean>
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y.absoluteValue > 1) showFloatingState.value = available.y >= 0
        return super.onPreScroll(available, source)
    }
}

@Composable
fun DataNameWidget(viewModel: EditViewModel) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .focusable(),
        leadingIcon = {
            Icon(imageVector = Icons.TwoTone.Edit, contentDescription = null)
        },
        label = {
            Text(text = stringResource(id = R.string.config_name))
        },
        value = viewModel.state.data.name,
        onValueChange = {
            viewModel.dispatch(EditViewAction.ChangeDataName(it))
        },
        singleLine = true,
        // TODO do not allow create another Default name
        isError = viewModel.state.data.errorName
    )
}

@Composable
fun DataDescriptionWidget(viewModel: EditViewModel) {
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .focusable(),
        leadingIcon = {
            Icon(imageVector = Icons.AutoMirrored.TwoTone.More, contentDescription = null)
        },
        label = {
            Text(text = stringResource(id = R.string.config_description))
        },
        value = viewModel.state.data.description,
        onValueChange = { viewModel.dispatch(EditViewAction.ChangeDataDescription(it)) },
        maxLines = 8,
    )
}

@Composable
fun DataAuthorizerWidget(viewModel: EditViewModel) {
    val stateAuthorizer = viewModel.state.data.authorizer
    val globalAuthorizer = viewModel.globalAuthorizer
    val data = mapOf(
        ConfigEntity.Authorizer.Global to stringResource(
            R.string.config_authorizer_global_desc,
            when (globalAuthorizer) {
                ConfigEntity.Authorizer.None -> stringResource(R.string.config_authorizer_none)
                ConfigEntity.Authorizer.Root -> stringResource(R.string.config_authorizer_root)
                ConfigEntity.Authorizer.Shizuku -> stringResource(R.string.config_authorizer_shizuku)
                ConfigEntity.Authorizer.Dhizuku -> stringResource(R.string.config_authorizer_dhizuku)
                ConfigEntity.Authorizer.Customize -> stringResource(R.string.config_authorizer_customize)
                else -> stringResource(R.string.config_authorizer_global)
            }
        ),
        ConfigEntity.Authorizer.None to stringResource(R.string.config_authorizer_none),
        ConfigEntity.Authorizer.Root to stringResource(R.string.config_authorizer_root),
        ConfigEntity.Authorizer.Shizuku to stringResource(R.string.config_authorizer_shizuku),
        ConfigEntity.Authorizer.Dhizuku to stringResource(R.string.config_authorizer_dhizuku),
        ConfigEntity.Authorizer.Customize to stringResource(R.string.config_authorizer_customize)
    )
    DropDownMenuWidget(
        icon = Icons.TwoTone.Memory,
        title = stringResource(R.string.config_authorizer),
        description = if (data.containsKey(stateAuthorizer)) data[stateAuthorizer] else null,
        choice = data.keys.toList().indexOf(stateAuthorizer),
        data = data.values.toList(),
    ) {
        data.keys.toList().getOrNull(it)?.let {
            viewModel.dispatch(EditViewAction.ChangeDataAuthorizer(it))
        }
    }
}

@Composable
fun DataCustomizeAuthorizerWidget(viewModel: EditViewModel) {
    if (!viewModel.state.data.authorizerCustomize) return
    val customizeAuthorizer = viewModel.state.data.customizeAuthorizer
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .focusable(),
        leadingIcon = {
            Icon(imageVector = Icons.TwoTone.Terminal, contentDescription = null)
        },
        label = {
            Text(text = stringResource(R.string.config_customize_authorizer))
        },
        value = customizeAuthorizer,
        onValueChange = { viewModel.dispatch(EditViewAction.ChangeDataCustomizeAuthorizer(it)) },
        maxLines = 8,
        isError = viewModel.state.data.errorCustomizeAuthorizer
    )
}

@Composable
fun DataInstallModeWidget(viewModel: EditViewModel) {
    val stateInstallMode = viewModel.state.data.installMode
    val globalInstallMode = viewModel.globalInstallMode
    val data = mapOf(
        ConfigEntity.InstallMode.Global to stringResource(
            R.string.config_install_mode_global_desc,
            when (globalInstallMode) {
                ConfigEntity.InstallMode.Dialog -> stringResource(R.string.config_install_mode_dialog)
                ConfigEntity.InstallMode.AutoDialog -> stringResource(R.string.config_install_mode_auto_dialog)
                ConfigEntity.InstallMode.Notification -> stringResource(R.string.config_install_mode_notification)
                ConfigEntity.InstallMode.AutoNotification -> stringResource(R.string.config_install_mode_auto_notification)
                ConfigEntity.InstallMode.Ignore -> stringResource(R.string.config_install_mode_ignore)
                else -> stringResource(R.string.config_install_mode_global)
            }
        ),
        ConfigEntity.InstallMode.Dialog to stringResource(R.string.config_install_mode_dialog),
        ConfigEntity.InstallMode.AutoDialog to stringResource(R.string.config_install_mode_auto_dialog),
        ConfigEntity.InstallMode.Notification to stringResource(R.string.config_install_mode_notification),
        ConfigEntity.InstallMode.AutoNotification to stringResource(R.string.config_install_mode_auto_notification),
        ConfigEntity.InstallMode.Ignore to stringResource(R.string.config_install_mode_ignore),
    )
    DropDownMenuWidget(
        icon = Icons.TwoTone.Downloading,
        title = stringResource(R.string.config_install_mode),
        description = if (data.containsKey(stateInstallMode)) data[stateInstallMode] else null,
        choice = data.keys.toList().indexOf(stateInstallMode),
        data = data.values.toList(),
    ) {
        data.keys.toList().getOrNull(it)?.let {
            viewModel.dispatch(EditViewAction.ChangeDataInstallMode(it))
        }
    }
}

@Composable
fun DataDeclareInstallerWidget(viewModel: EditViewModel) {
    val stateAuthorizer = viewModel.state.data.authorizer
    val globalAuthorizer = viewModel.globalAuthorizer

    val isDhizuku = when (stateAuthorizer) {
        // 如果当前授权方式是 Dhizuku，结果直接为 true
        ConfigEntity.Authorizer.Dhizuku -> true
        // 如果当前授权方式是 Global，则取决于全局授权方式是否为 Dhizuku
        ConfigEntity.Authorizer.Global -> globalAuthorizer == ConfigEntity.Authorizer.Dhizuku
        // 其他任何情况，结果都为 false
        else -> false
    }

    val description =
        if (isDhizuku) stringResource(R.string.dhizuku_cannot_set_installer_desc) // 假设你有一个这样的字符串资源
        else null // 其他模式下没有特殊描述

    SwitchWidget(
        icon = Icons.TwoTone.Face,
        title = stringResource(id = R.string.config_declare_installer),
        checked = viewModel.state.data.declareInstaller,
        onCheckedChange = {
            // 这是该组件唯一允许的 dispatch，即响应用户的直接交互
            viewModel.dispatch(EditViewAction.ChangeDataDeclareInstaller(it))
        },
        // 将从 ViewModel 获取的状态直接传递给下一层
        description = description,
        enabled = !isDhizuku,
        isError = isDhizuku
    )
}

@Composable
fun DataInstallerWidget(viewModel: EditViewModel) {
    val installer = viewModel.state.data.installer
    AnimatedVisibility(
        visible = viewModel.state.data.declareInstaller,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .focusable(),
            leadingIcon = {
                Icon(imageVector = Icons.TwoTone.PsychologyAlt, contentDescription = null)
            },
            label = {
                Text(text = stringResource(id = R.string.config_installer))
            },
            value = installer,
            onValueChange = {
                viewModel.dispatch(EditViewAction.ChangeDataInstaller(it))
            },
            singleLine = true,
            isError = viewModel.state.data.errorInstaller
        )
    }
}

@Composable
fun DataForAllUserWidget(viewModel: EditViewModel) {
    SwitchWidget(
        icon = Icons.TwoTone.People,
        title = stringResource(id = R.string.config_for_all_user),
        checked = viewModel.state.data.forAllUser,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeDataForAllUser(it)) }
    )
}

@Composable
fun DataAllowTestOnlyWidget(viewModel: EditViewModel) {
    SwitchWidget(
        icon = Icons.TwoTone.BugReport,
        title = stringResource(id = R.string.config_allow_test_only),
        description = stringResource(id = R.string.config_allow_test_only_dsp),
        checked = viewModel.state.data.allowTestOnly,
        onCheckedChange = {
            viewModel.dispatch(EditViewAction.ChangeDataAllowTestOnly(it))
        }
    )
}

@Composable
fun DataAllowDowngradeWidget(viewModel: EditViewModel) {
    SwitchWidget(
        icon = Icons.AutoMirrored.TwoTone.TrendingDown,
        title = stringResource(id = R.string.config_allow_downgrade),
        description = stringResource(id = R.string.config_allow_downgrade_dsp),
        checked = viewModel.state.data.allowDowngrade,
        onCheckedChange = {
            viewModel.dispatch(EditViewAction.ChangeDataAllowDowngrade(it))
        }
    )
}

@Composable
fun DataAutoDeleteWidget(viewModel: EditViewModel) {
    SwitchWidget(
        icon = Icons.TwoTone.Delete,
        title = stringResource(id = R.string.config_auto_delete),
        description = stringResource(id = R.string.config_auto_delete_dsp),
        checked = viewModel.state.data.autoDelete,
        onCheckedChange = {
            viewModel.dispatch(EditViewAction.ChangeDataAutoDelete(it))
        }
    )
}

@Composable
fun DisplaySdkWidget(viewModel: EditViewModel) {
    SwitchWidget(
        icon = Icons.TwoTone.Info,
        title = stringResource(id = R.string.config_display_sdk_version),
        description = stringResource(id = R.string.config_display_sdk_version_sdp),
        checked = viewModel.state.data.displaySdk,
        onCheckedChange = {
            viewModel.dispatch(EditViewAction.ChangeDisplaySdk(it))
        }
    )
}

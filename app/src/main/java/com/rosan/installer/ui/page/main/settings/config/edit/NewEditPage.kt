package com.rosan.installer.ui.page.main.settings.config.edit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.widget.dialog.UnsavedChangesDialog
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.DataAllowAllRequestedPermissionsWidget
import com.rosan.installer.ui.page.main.widget.setting.DataAllowDowngradeWidget
import com.rosan.installer.ui.page.main.widget.setting.DataAllowRestrictedPermissionsWidget
import com.rosan.installer.ui.page.main.widget.setting.DataAllowTestOnlyWidget
import com.rosan.installer.ui.page.main.widget.setting.DataAuthorizerWidget
import com.rosan.installer.ui.page.main.widget.setting.DataAutoDeleteWidget
import com.rosan.installer.ui.page.main.widget.setting.DataBypassLowTargetSdkWidget
import com.rosan.installer.ui.page.main.widget.setting.DataCustomizeAuthorizerWidget
import com.rosan.installer.ui.page.main.widget.setting.DataDeclareInstallerWidget
import com.rosan.installer.ui.page.main.widget.setting.DataDescriptionWidget
import com.rosan.installer.ui.page.main.widget.setting.DataForAllUserWidget
import com.rosan.installer.ui.page.main.widget.setting.DataInstallModeWidget
import com.rosan.installer.ui.page.main.widget.setting.DataManualDexoptWidget
import com.rosan.installer.ui.page.main.widget.setting.DataNameWidget
import com.rosan.installer.ui.page.main.widget.setting.DataUserWidget
import com.rosan.installer.ui.page.main.widget.setting.DisplaySdkWidget
import com.rosan.installer.ui.page.main.widget.setting.SplicedColumnGroup
import com.rosan.installer.ui.theme.none
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun NewEditPage(
    navController: NavController,
    id: Long? = null,
    viewModel: EditViewModel = koinViewModel { parametersOf(id) }
) {
    LaunchedEffect(Unit) {
        viewModel.dispatch(EditViewAction.Init)
    }

    val showFloatingState = remember { mutableStateOf(true) }
    val showFloating by showFloatingState
    val listState = rememberLazyListState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showUnsavedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collectLatest { (index, offset) ->
                val isScrollingDown = when {
                    index > previousIndex -> true
                    index < previousIndex -> false
                    else -> offset > previousOffset
                }

                previousIndex = index
                previousOffset = offset

                val newShowFloating = !isScrollingDown
                if (showFloatingState.value != newShowFloating) {
                    showFloatingState.value = newShowFloating
                }
            }
    }

    UnsavedChangesDialog(
        show = showUnsavedDialog,
        onDismiss = {
            showUnsavedDialog = false
        },
        onConfirm = {
            showUnsavedDialog = false
            navController.navigateUp()
        },
        // Pass the list of active error messages from the ViewModel.
        errorMessages = viewModel.activeErrorMessages
    )
    // The condition for interception is now expanded to include errors.
    // If there are unsaved changes OR if there are validation errors, we should intercept.
    val shouldInterceptBackPress = viewModel.hasUnsavedChanges || viewModel.hasErrors

    // Use this new combined condition for the BackHandler.
    BackHandler(enabled = shouldInterceptBackPress) {
        showUnsavedDialog = true
    }

    LaunchedEffect(Unit) {
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

    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Set indication to null to hide the ripple effect
            ) {
                focusManager.clearFocus()
            },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets.none,
        topBar = {
            TopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = { Text(text = stringResource(id = if (id == null) R.string.add else R.string.update)) },
                navigationIcon = {
                    AppBackButton(
                        onClick = { navController.navigateUp() },
                        icon = Icons.AutoMirrored.TwoTone.ArrowBack,
                        modifier = Modifier.size(36.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                actions = {
                    AnimatedVisibility(
                        visible = !showFloating, // 只有在滚动到底部时可见
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
                                imageVector = AppIcons.Save,
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
                visible = showFloating, // 在未滚动到底部且 showFloating 为 true 时可见
                enter = scaleIn(),
                exit = scaleOut(),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                val text = stringResource(R.string.save)
                SmallExtendedFloatingActionButton(
                    icon = {
                        Icon(
                            imageVector = AppIcons.Save,
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
            // --- Group 1: Main Settings ---
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.config_label_main_settings),
                    content = buildList {
                        add { DataNameWidget(viewModel, { DataDescriptionWidget(viewModel) }) }
                        add { DataAuthorizerWidget(viewModel) }
                        // Add customize authorizer widget only if the condition is met.
                        if (viewModel.state.data.authorizerCustomize)
                            add { DataCustomizeAuthorizerWidget(viewModel) }
                        add { DataInstallModeWidget(viewModel) }
                    }
                )
            }

            // --- Group 2: Installer Settings ---
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.config_label_installer_settings),
                    content = buildList {
                        add { DataUserWidget(viewModel) }
                        add { DataDeclareInstallerWidget(viewModel) }
                        add { DataManualDexoptWidget(viewModel) }
                        add { DataAutoDeleteWidget(viewModel) }
                        add { DisplaySdkWidget(viewModel) }
                    }
                )
            }

            // --- Group 3: Install Options ---
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.config_label_install_options),
                    content = buildList {
                        add { DataForAllUserWidget(viewModel) }
                        add { DataAllowTestOnlyWidget(viewModel) }
                        add { DataAllowDowngradeWidget(viewModel) }
                        add { DataBypassLowTargetSdkWidget(viewModel) }
                        add { DataAllowRestrictedPermissionsWidget(viewModel) }
                        add { DataAllowAllRequestedPermissionsWidget(viewModel) }
                    }
                )
            }
        }
    }
}
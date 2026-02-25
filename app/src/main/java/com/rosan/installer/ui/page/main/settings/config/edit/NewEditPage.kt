package com.rosan.installer.ui.page.main.settings.config.edit

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.dialog.UnsavedChangesDialog
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.DataAllowAllRequestedPermissionsWidget
import com.rosan.installer.ui.page.main.widget.setting.DataAllowDowngradeWidget
import com.rosan.installer.ui.page.main.widget.setting.DataAllowTestOnlyWidget
import com.rosan.installer.ui.page.main.widget.setting.DataApkChooseAllWidget
import com.rosan.installer.ui.page.main.widget.setting.DataAuthorizerWidget
import com.rosan.installer.ui.page.main.widget.setting.DataAutoDeleteWidget
import com.rosan.installer.ui.page.main.widget.setting.DataBypassLowTargetSdkWidget
import com.rosan.installer.ui.page.main.widget.setting.DataCustomizeAuthorizerWidget
import com.rosan.installer.ui.page.main.widget.setting.DataDeclareInstallerWidget
import com.rosan.installer.ui.page.main.widget.setting.DataDescriptionWidget
import com.rosan.installer.ui.page.main.widget.setting.DataForAllUserWidget
import com.rosan.installer.ui.page.main.widget.setting.DataInstallModeWidget
import com.rosan.installer.ui.page.main.widget.setting.DataInstallReasonWidget
import com.rosan.installer.ui.page.main.widget.setting.DataInstallRequesterWidget
import com.rosan.installer.ui.page.main.widget.setting.DataManualDexoptWidget
import com.rosan.installer.ui.page.main.widget.setting.DataNameWidget
import com.rosan.installer.ui.page.main.widget.setting.DataPackageSourceWidget
import com.rosan.installer.ui.page.main.widget.setting.DataSplitChooseAllWidget
import com.rosan.installer.ui.page.main.widget.setting.DataUserWidget
import com.rosan.installer.ui.page.main.widget.setting.DisplaySdkWidget
import com.rosan.installer.ui.page.main.widget.setting.DisplaySizeWidget
import com.rosan.installer.ui.page.main.widget.setting.SplicedColumnGroup
import com.rosan.installer.ui.theme.getM3TopBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.none
import com.rosan.installer.ui.theme.rememberMaterial3HazeStyle
import com.rosan.installer.ui.util.isNoneActive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
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
    viewModel: EditViewModel = koinViewModel { parametersOf(id) },
    useBlur: Boolean
) {
    LaunchedEffect(Unit) {
        viewModel.dispatch(EditViewAction.Init)
    }

    val showFloatingState = remember { mutableStateOf(true) }
    val showFloating by showFloatingState
    val listState = rememberLazyListState()
    val snackBarHostState = remember { SnackbarHostState() }
    val topAppBarState = rememberTopAppBarState()
    val hazeState = if (useBlur) remember { HazeState() } else null
    val hazeStyle = rememberMaterial3HazeStyle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    var showUnsavedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        topAppBarState.heightOffset = topAppBarState.heightOffsetLimit
    }

    val stateAuthorizer = viewModel.state.data.authorizer
    val globalAuthorizer = viewModel.globalAuthorizer

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
            LargeFlexibleTopAppBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = { Text(text = stringResource(id = if (id == null) R.string.add else R.string.update)) },
                navigationIcon = {
                    Row {
                        AppBackButton(
                            onClick = { navController.navigateUp() },
                            icon = Icons.AutoMirrored.TwoTone.ArrowBack,
                            modifier = Modifier.size(36.dp),
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = hazeState.getM3TopBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = hazeState.getM3TopBarColor()
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
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
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
        // Only show FAB when not scrolling to the bottom
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFloating, // Visible when not scrolling to the bottom
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
                    text = { Text(text) },
                    onClick = { viewModel.dispatch(EditViewAction.SaveData) }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(hazeState?.let { Modifier.hazeSource(it) } ?: Modifier),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding()
            ),
            state = listState,
        ) {
            // --- Group 1: Main Settings ---
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.config_label_main_settings)
                ) {
                    item { DataNameWidget(viewModel, { DataDescriptionWidget(viewModel) }) }
                    item { DataAuthorizerWidget(viewModel) }
                    item(visible = viewModel.state.data.authorizerCustomize) {
                        DataCustomizeAuthorizerWidget(viewModel)
                    }
                    item { DataInstallModeWidget(viewModel) }
                }
            }

            if (isNoneActive(stateAuthorizer, globalAuthorizer)) {
                item { InfoTipCard(text = stringResource(R.string.config_authorizer_none_tips)) }
            }

            // --- Group 2: Installer Settings ---
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.config_label_installer_settings)
                ) {
                    item { DataUserWidget(viewModel) }
                    item { DataInstallReasonWidget(viewModel) }
                    item(visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        DataPackageSourceWidget(viewModel)
                    }
                    item(visible = viewModel.state.isCustomInstallRequesterEnabled) {
                        DataInstallRequesterWidget(viewModel)
                    }
                    item { DataDeclareInstallerWidget(viewModel) }
                    item { DataManualDexoptWidget(viewModel) }
                    item { DataAutoDeleteWidget(viewModel) }
                    item { DisplaySdkWidget(viewModel) }
                    item { DisplaySizeWidget(viewModel) }
                }
            }

            // --- Group 3: Install Options ---
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.config_label_install_options)
                ) {
                    item { DataForAllUserWidget(viewModel) }
                    item { DataAllowTestOnlyWidget(viewModel) }
                    item { DataAllowDowngradeWidget(viewModel) }
                    item(visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        DataBypassLowTargetSdkWidget(viewModel)
                    }
                    item { DataAllowAllRequestedPermissionsWidget(viewModel) }
                }
            }

            // --- Group 4: Preferences Settings ---
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.config_label_preferences)
                ) {
                    item { DataSplitChooseAllWidget(viewModel) }
                    item { DataApkChooseAllWidget(viewModel) }
                }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
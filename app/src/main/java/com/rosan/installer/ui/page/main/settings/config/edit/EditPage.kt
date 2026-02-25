package com.rosan.installer.ui.page.main.settings.config.edit

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.rosan.installer.ui.page.main.widget.setting.LabelWidget
import com.rosan.installer.ui.theme.none
import com.rosan.installer.ui.util.isNoneActive
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

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

    val showFloatingState = remember { mutableStateOf(true) }
    val showFloating by showFloatingState
    val listState = rememberLazyListState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showUnsavedDialog by remember { mutableStateOf(false) }

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
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.none,
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(id = if (id == null) R.string.add else R.string.update)) },
                navigationIcon = { AppBackButton(onClick = { navController.navigateUp() }) },
                // 新增: 当滚动到底部时，在 actions 中显示 FAB
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
                                containerColor = MaterialTheme.colorScheme.surfaceContainer, // 标准 IconButton 背景是透明的
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
                    text = { Text(text) },
                    onClick = { viewModel.dispatch(EditViewAction.SaveData) }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            state = listState,
        ) {
            item { DataNameWidget(viewModel = viewModel) }
            item { DataDescriptionWidget(viewModel = viewModel) }
            item { DataAuthorizerWidget(viewModel = viewModel) }
            item { DataCustomizeAuthorizerWidget(viewModel = viewModel) }
            item { DataInstallModeWidget(viewModel = viewModel) }
            if (isNoneActive(stateAuthorizer, globalAuthorizer))
                item { InfoTipCard(text = stringResource(R.string.config_authorizer_none_tips)) }

            item { LabelWidget(label = stringResource(R.string.config_label_installer_settings)) }
            item { DataUserWidget(viewModel = viewModel, isM3E = false) }
            item { DataInstallReasonWidget(viewModel = viewModel, isM3E = false) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                item { DataPackageSourceWidget(viewModel, isM3E = false) }
            if (viewModel.state.isCustomInstallRequesterEnabled)
                item { DataInstallRequesterWidget(viewModel = viewModel, isM3E = false) }
            item { DataDeclareInstallerWidget(viewModel = viewModel, isM3E = false) }
            item { DataManualDexoptWidget(viewModel, isM3E = false) }
            item { DataAutoDeleteWidget(viewModel = viewModel, isM3E = false) }
            item { DisplaySdkWidget(viewModel = viewModel, isM3E = false) }
            item { DisplaySizeWidget(viewModel, isM3E = false) }

            item { LabelWidget(label = stringResource(R.string.config_label_install_options)) }
            item { DataForAllUserWidget(viewModel = viewModel, isM3E = false) }
            item { DataAllowTestOnlyWidget(viewModel = viewModel, isM3E = false) }
            item { DataAllowDowngradeWidget(viewModel = viewModel, isM3E = false) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                item { DataBypassLowTargetSdkWidget(viewModel = viewModel, isM3E = false) }
            item { DataAllowAllRequestedPermissionsWidget(viewModel = viewModel, isM3E = false) }

            item { LabelWidget(label = stringResource(R.string.config_label_preferences)) }
            item { DataSplitChooseAllWidget(viewModel = viewModel, isM3E = false) }
            item { DataApkChooseAllWidget(viewModel = viewModel, isM3E = false) }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
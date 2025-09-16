package com.rosan.installer.ui.page.miuix.settings.config.edit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewAction
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewEvent
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataAllowAllRequestedPermissionsWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataAllowDowngradeWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataAllowRestrictedPermissionsWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataAllowTestOnlyWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataAuthorizerWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataAutoDeleteWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataBypassLowTargetSdkWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataCustomizeAuthorizerWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataDeclareInstallerWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataDescriptionWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataForAllUserWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataInstallModeWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataManualDexoptWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataNameWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataUserWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDisplaySdkWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixUnsavedChangesDialog
import com.rosan.installer.ui.theme.none
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Cancel
import top.yukonga.miuix.kmp.icon.icons.useful.Confirm
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun MiuixEditPage(
    navController: NavController,
    id: Long? = null,
    viewModel: EditViewModel = koinViewModel { parametersOf(id) }
) {
    LaunchedEffect(true) {
        viewModel.dispatch(EditViewAction.Init)
    }

    val snackBarHostState = remember { SnackbarHostState() }
    val scrollBehavior = MiuixScrollBehavior()
    val showUnsavedDialogState = remember { mutableStateOf(false) }

    MiuixUnsavedChangesDialog(
        showState = showUnsavedDialogState,
        onDismiss = {
            showUnsavedDialogState.value = false
        },
        onConfirm = {
            showUnsavedDialogState.value = false
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
        showUnsavedDialogState.value = true
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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.none,
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = stringResource(id = if (id == null) R.string.add else R.string.update),
                navigationIcon = {
                    MiuixBackButton(
                        modifier = Modifier.padding(start = 16.dp),
                        icon = MiuixIcons.Useful.Cancel,
                        onClick = { navController.navigateUp() }
                    )
                },
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = 16.dp),
                        onClick = { viewModel.dispatch(EditViewAction.SaveData) },
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Useful.Confirm,
                            contentDescription = stringResource(R.string.save)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .padding(it),
            overscrollEffect = null,
        ) {
            item { MiuixDataNameWidget(viewModel = viewModel) }
            item { MiuixDataDescriptionWidget(viewModel = viewModel) }
            item { SmallTitle(stringResource(R.string.config)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp)
                ) {
                    MiuixDataAuthorizerWidget(viewModel = viewModel)
                    MiuixDataCustomizeAuthorizerWidget(viewModel = viewModel)
                    MiuixDataInstallModeWidget(viewModel = viewModel)
                }
            }
            item { SmallTitle(stringResource(R.string.config_label_installer_settings)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp)
                ) {
                    MiuixDataUserWidget(viewModel = viewModel)
                    MiuixDataDeclareInstallerWidget(viewModel = viewModel)
                    MiuixDataManualDexoptWidget(viewModel)
                    MiuixDataAutoDeleteWidget(viewModel = viewModel)
                    MiuixDisplaySdkWidget(viewModel = viewModel)
                }
            }
            item { SmallTitle(stringResource(R.string.config_label_install_options)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp)
                ) {
                    MiuixDataForAllUserWidget(viewModel = viewModel)
                    MiuixDataAllowTestOnlyWidget(viewModel = viewModel)
                    MiuixDataAllowDowngradeWidget(viewModel = viewModel)
                    MiuixDataBypassLowTargetSdkWidget(viewModel = viewModel)
                    MiuixDataAllowRestrictedPermissionsWidget(viewModel = viewModel)
                    MiuixDataAllowAllRequestedPermissionsWidget(viewModel = viewModel)
                }
            }
        }
    }
}
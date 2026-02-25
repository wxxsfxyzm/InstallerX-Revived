package com.rosan.installer.ui.page.miuix.settings.config.edit

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataAllowTestOnlyWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataApkChooseAllWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataAuthorizerWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataAutoDeleteWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataBypassLowTargetSdkWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataCustomizeAuthorizerWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataDeclareInstallerWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataDescriptionWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataForAllUserWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataInstallModeWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataInstallRequesterWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataManualDexoptWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataNameWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataPackageSourceWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataSplitChooseAllWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataUserWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDisplaySdkWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDisplaySizeWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixInstallReasonWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixSettingsTipCard
import com.rosan.installer.ui.page.miuix.widgets.MiuixUnsavedChangesDialog
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.rememberMiuixHazeStyle
import com.rosan.installer.ui.util.isNoneActive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
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
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixEditPage(
    navController: NavController,
    id: Long? = null,
    viewModel: EditViewModel = koinViewModel { parametersOf(id) },
    useBlur: Boolean
) {
    LaunchedEffect(true) {
        viewModel.dispatch(EditViewAction.Init)
    }

    val snackBarHostState = remember { SnackbarHostState() }
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = if (useBlur) remember { HazeState() } else null
    val hazeStyle = rememberMiuixHazeStyle()
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
        errorMessages = viewModel.activeErrorMessages
    )
    // The condition for interception is now expanded to include errors.
    // If there are unsaved changes OR if there are validation errors, we should intercept.
    val shouldInterceptBackPress = viewModel.hasUnsavedChanges || viewModel.hasErrors

    BackHandler(enabled = shouldInterceptBackPress) {
        showUnsavedDialogState.value = true
    }

    val stateAuthorizer = viewModel.state.data.authorizer
    val globalAuthorizer = viewModel.globalAuthorizer

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
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                color = hazeState.getMiuixAppBarColor(),
                scrollBehavior = scrollBehavior,
                title = stringResource(id = if (id == null) R.string.add else R.string.update),
                navigationIcon = {
                    MiuixBackButton(
                        modifier = Modifier.padding(start = 16.dp),
                        icon = MiuixIcons.Regular.Close,
                        onClick = { navController.navigateUp() }
                    )
                },
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = 16.dp),
                        onClick = { viewModel.dispatch(EditViewAction.SaveData) },
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Ok,
                            contentDescription = stringResource(R.string.save)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(hazeState?.let { Modifier.hazeSource(it) } ?: Modifier)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
            overscrollEffect = null,
        ) {
            item { Spacer(modifier = Modifier.size(12.dp)) }
            item { MiuixDataNameWidget(viewModel = viewModel) }
            item { MiuixDataDescriptionWidget(viewModel = viewModel) }
            item { SmallTitle(stringResource(R.string.config)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixDataAuthorizerWidget(viewModel = viewModel)
                    MiuixDataCustomizeAuthorizerWidget(viewModel = viewModel)
                    MiuixDataInstallModeWidget(viewModel = viewModel)
                }
            }
            if (isNoneActive(stateAuthorizer, globalAuthorizer))
                item { MiuixSettingsTipCard(stringResource(R.string.config_authorizer_none_tips)) }
            item { SmallTitle(stringResource(R.string.config_label_installer_settings)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixDataUserWidget(viewModel = viewModel)
                    MiuixInstallReasonWidget(viewModel = viewModel)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) MiuixDataPackageSourceWidget(viewModel = viewModel)
                    if (viewModel.state.isCustomInstallRequesterEnabled)
                        MiuixDataInstallRequesterWidget(viewModel = viewModel)
                    MiuixDataDeclareInstallerWidget(viewModel = viewModel)
                    MiuixDataManualDexoptWidget(viewModel)
                    MiuixDataAutoDeleteWidget(viewModel = viewModel)
                    MiuixDisplaySdkWidget(viewModel = viewModel)
                    MiuixDisplaySizeWidget(viewModel = viewModel)
                }
            }
            item { SmallTitle(stringResource(R.string.config_label_install_options)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp)
                ) {
                    MiuixDataForAllUserWidget(viewModel = viewModel)
                    MiuixDataAllowTestOnlyWidget(viewModel = viewModel)
                    MiuixDataAllowDowngradeWidget(viewModel = viewModel)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                        MiuixDataBypassLowTargetSdkWidget(viewModel = viewModel)
                    MiuixDataAllowAllRequestedPermissionsWidget(viewModel = viewModel)
                }
            }
            item { SmallTitle(stringResource(R.string.config_label_preferences)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp)
                ) {
                    MiuixDataSplitChooseAllWidget(viewModel = viewModel)
                    MiuixDataApkChooseAllWidget(viewModel = viewModel)
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
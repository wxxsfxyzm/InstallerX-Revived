package com.rosan.installer.ui.page.miuix.settings.preferred.subpage.lab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.RootImplementation
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixRootImplementationDialog
import com.rosan.installer.ui.page.miuix.widgets.MiuixSettingsTipCard
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SpinnerEntry
import top.yukonga.miuix.kmp.extra.SuperSpinner
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixLabPage(
    navController: NavHostController,
    viewModel: PreferredViewModel
) {
    val state = viewModel.state
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = remember { HazeState() }
    val hazeStyle = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.surface,
        tint = HazeTint(MiuixTheme.colorScheme.surface.copy(0.8f))
    )
    val showRootImplementationDialog = remember { mutableStateOf(false) }

    MiuixRootImplementationDialog(
        showState = showRootImplementationDialog,
        onDismiss = { showRootImplementationDialog.value = false },
        onConfirm = { selectedImplementation ->
            // When the user confirms, dismiss the dialog.
            showRootImplementationDialog.value = false
            // Dispatch actions to update the root implementation AND enable the flashing feature.
            viewModel.dispatch(PreferredViewAction.LabChangeRootImplementation(selectedImplementation))
            viewModel.dispatch(PreferredViewAction.LabChangeRootModuleFlash(true))
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.hazeEffect(hazeState) {
                    style = hazeStyle
                    blurRadius = 30.dp
                    noiseFactor = 0f
                },
                title = stringResource(R.string.lab),
                navigationIcon = {
                    MiuixBackButton(modifier = Modifier.padding(start = 16.dp), onClick = { navController.navigateUp() })
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
            overscrollEffect = null
        ) {
            item { MiuixSettingsTipCard(stringResource(R.string.lab_tip)) }
            item { Spacer(modifier = Modifier.size(12.dp)) }
            item { SmallTitle(stringResource(R.string.config_authorizer_shizuku)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.lab_use_hook_mode),
                        description = stringResource(R.string.lab_use_hook_mode_desc),
                        checked = state.labShizukuHookMode,
                        onCheckedChange = { viewModel.dispatch(PreferredViewAction.LabChangeShizukuHookMode(it)) }
                    )
                }
            }
            item { SmallTitle(stringResource(R.string.config_authorizer_root)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.lab_module_flashing),
                        description = stringResource(R.string.lab_module_flashing_desc),
                        checked = state.labRootEnableModuleFlash,
                        onCheckedChange = { isEnabling ->
                            if (isEnabling) {
                                showRootImplementationDialog.value = true
                            } else {
                                viewModel.dispatch(PreferredViewAction.LabChangeRootModuleFlash(false))
                            }
                        }
                    )
                    AnimatedVisibility(
                        visible = state.labRootEnableModuleFlash,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        val currentRootImpl = state.labRootImplementation
                        val data = remember {
                            mapOf(
                                RootImplementation.Magisk to "Magisk",
                                RootImplementation.KernelSU to "KernelSU",
                                RootImplementation.APatch to "APatch"
                            )
                        }

                        val spinnerEntries = remember(data) {
                            data.values.map { modeName ->
                                SpinnerEntry(title = modeName)
                            }
                        }

                        val selectedIndex = remember(currentRootImpl, data) {
                            data.keys.toList().indexOf(currentRootImpl).coerceAtLeast(0)
                        }

                        SuperSpinner(
                            title = stringResource(R.string.lab_module_select_root_impl),
                            items = spinnerEntries,
                            selectedIndex = selectedIndex,
                            onSelectedIndexChange = { newIndex ->
                                data.keys.elementAtOrNull(newIndex)?.let { impl ->
                                    viewModel.dispatch(PreferredViewAction.LabChangeRootImplementation(impl))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
package com.rosan.installer.ui.page.miuix.settings.preferred.subpage.lab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.app.model.enums.HttpProfile
import com.rosan.installer.data.app.model.enums.RootImplementation
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixRootImplementationDialog
import com.rosan.installer.ui.page.miuix.widgets.MiuixSettingsTipCard
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.rememberMiuixHazeStyle
import com.rosan.installer.util.OSUtils
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SpinnerEntry
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SuperSpinner
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixLabPage(
    navController: NavHostController,
    viewModel: PreferredViewModel
) {
    val state = viewModel.state
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = if (state.useBlur) remember { HazeState() } else null
    val hazeStyle = rememberMiuixHazeStyle()
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
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                color = hazeState.getMiuixAppBarColor(),
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
                .then(hazeState?.let { Modifier.hazeSource(it) } ?: Modifier)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
            overscrollEffect = null
        ) {
            item { MiuixSettingsTipCard(stringResource(R.string.lab_tip)) }
            item { Spacer(modifier = Modifier.size(12.dp)) }
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

                        Column {
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
                            MiuixSwitchWidget(
                                title = stringResource(R.string.lab_module_flashing_show_art),
                                description = stringResource(R.string.lab_module_flashing_show_art_desc),
                                checked = state.labRootShowModuleArt,
                                onCheckedChange = { viewModel.dispatch(PreferredViewAction.LabChangeRootShowModuleArt(it)) }
                            )
                            if (OSUtils.isSystemApp)
                                MiuixSwitchWidget(
                                    title = stringResource(R.string.lab_module_always_use_root),
                                    description = stringResource(R.string.lab_module_always_use_root_desc),
                                    checked = state.labRootModuleAlwaysUseRoot,
                                    onCheckedChange = { viewModel.dispatch(PreferredViewAction.LabChangeRootModuleAlwaysUseRoot(it)) }
                                )
                        }
                    }
                }
            }
            item { SmallTitle(stringResource(R.string.lab_unstable_features)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.lab_set_install_requester),
                        description = stringResource(R.string.lab_set_install_requester_desc),
                        checked = state.labSetInstallRequester,
                        onCheckedChange = { viewModel.dispatch(PreferredViewAction.LabChangeSetInstallRequester(it)) }
                    )
                }
            }
            if (RsConfig.isInternetAccessEnabled) {
                item { SmallTitle(stringResource(R.string.internet_access_enabled)) }
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        // TODO
                        /*MiuixSwitchWidget(
                            title = stringResource(R.string.lab_http_save_file),
                            description = stringResource(R.string.lab_http_save_file_desc),
                            checked = state.labHttpSaveFile,
                            onCheckedChange = { viewModel.dispatch(PreferredViewAction.LabChangeHttpSaveFile(it)) }
                        )*/

                        val context = LocalContext.current
                        val currentProfile = state.labHttpProfile
                        val allowSecureString = stringResource(R.string.lab_http_profile_secure)
                        val allowLocalString = stringResource(R.string.lab_http_profile_local)
                        val allowAllString = stringResource(R.string.lab_http_profile_all)
                        val profileData = remember {
                            mapOf(
                                HttpProfile.ALLOW_SECURE to allowSecureString,
                                HttpProfile.ALLOW_LOCAL to allowLocalString,
                                HttpProfile.ALLOW_ALL to allowAllString
                            )
                        }

                        val profileEntries = remember(profileData) {
                            profileData.values.map { name ->
                                SpinnerEntry(title = name)
                            }
                        }

                        val profileIndex = remember(currentProfile, profileData) {
                            profileData.keys.toList().indexOf(currentProfile).coerceAtLeast(0)
                        }

                        SuperSpinner(
                            title = stringResource(R.string.lab_http_profile),
                            items = profileEntries,
                            selectedIndex = profileIndex,
                            onSelectedIndexChange = { newIndex ->
                                profileData.keys.elementAtOrNull(newIndex)?.let { profile ->
                                    viewModel.dispatch(PreferredViewAction.LabChangeHttpProfile(profile))
                                }
                            }
                        )
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
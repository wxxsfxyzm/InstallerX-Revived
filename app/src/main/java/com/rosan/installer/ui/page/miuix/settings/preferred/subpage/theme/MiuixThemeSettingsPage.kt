package com.rosan.installer.ui.page.miuix.settings.preferred.subpage.theme

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kieronquinn.monetcompat.core.MonetCompat
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.widget.card.ColorSwatchPreview
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixHideLauncherIconWarningDialog
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixThemeEngineWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixThemeModeWidget
import com.rosan.installer.ui.theme.m3color.PresetColors
import com.rosan.installer.ui.theme.m3color.RawColor
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
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.collections.chunked

@Composable
fun MiuixThemeSettingsPage(
    navController: NavController,
    viewModel: PreferredViewModel,
) {
    val state = viewModel.state
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = remember { HazeState() }
    val hazeStyle = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.surface,
        tint = HazeTint(MiuixTheme.colorScheme.surface.copy(0.8f))
    )
    val showHideLauncherIconDialog = remember { mutableStateOf(false) }

    MiuixHideLauncherIconWarningDialog(
        showState = showHideLauncherIconDialog,
        onDismiss = { showHideLauncherIconDialog.value = false },
        onConfirm = {
            showHideLauncherIconDialog.value = false
            viewModel.dispatch(PreferredViewAction.ChangeShowLauncherIcon(false))
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
                title = stringResource(R.string.theme_settings),
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
            item { Spacer(modifier = Modifier.size(12.dp)) }
            item { SmallTitle(stringResource(R.string.theme_settings_ui_style)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixThemeEngineWidget(
                        currentThemeIsMiuix = state.showMiuixUI,
                        onThemeChange = { useMiuix ->
                            viewModel.dispatch(PreferredViewAction.ChangeUseMiuix(useMiuix))
                        }
                    )
                }
            }
            item { SmallTitle(stringResource(R.string.theme_settings_miuix_ui)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixThemeModeWidget(
                        currentThemeMode = state.themeMode,
                        onThemeModeChange = { newMode ->
                            viewModel.dispatch(PreferredViewAction.SetThemeMode(newMode))
                        }
                    )
                    MiuixSwitchWidget(
                        title = stringResource(R.string.theme_settings_miuix_custom_colors),
                        description = stringResource(R.string.theme_settings_miuix_custom_colors_desc),
                        checked = state.useMiuixMonet,
                        onCheckedChange = {
                            viewModel.dispatch(PreferredViewAction.SetUseMiuixMonet(it))
                        }
                    )
                    AnimatedVisibility(
                        visible = state.useMiuixMonet,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixSwitchWidget(
                            title = stringResource(R.string.theme_settings_dynamic_color),
                            description = stringResource(R.string.theme_settings_dynamic_color_desc),
                            checked = state.useDynamicColor,
                            onCheckedChange = {
                                viewModel.dispatch(PreferredViewAction.SetUseDynamicColor(it))
                            }
                        )
                    }
                    AnimatedVisibility(
                        visible = state.useMiuixMonet,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixSwitchWidget(
                            title = stringResource(R.string.theme_settings_dynamic_color_follow_icon),
                            description = stringResource(R.string.theme_settings_dynamic_color_follow_icon_desc),
                            checked = state.useDynColorFollowPkgIcon,
                            onCheckedChange = {
                                viewModel.dispatch(PreferredViewAction.SetDynColorFollowPkgIcon(it))
                            }
                        )
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && state.showLiveActivity)
                        MiuixSwitchWidget(
                            title = stringResource(R.string.theme_settings_live_activity_dynamic_color_follow_icon),
                            description = stringResource(R.string.theme_settings_live_activity_dynamic_color_follow_icon_desc),
                            checked = state.useDynColorFollowPkgIconForLiveActivity,
                            onCheckedChange = {
                                viewModel.dispatch(PreferredViewAction.SetDynColorFollowPkgIconForLiveActivity(it))
                            }
                        )
                }
            }
            val showColorGrid =
                state.useMiuixMonet && (!state.useDynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            item {
                AnimatedVisibility(
                    visible = showColorGrid,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                            expandVertically(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                            shrinkVertically(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing))
                ) {
                    Column {
                        SmallTitle(stringResource(R.string.theme_settings_theme_color))
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                        ) {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 16.dp)
                            ) {
                                val itemMinWidth = 88.dp

                                val columns = (this.maxWidth / itemMinWidth).toInt().coerceAtLeast(1)

                                val chunkedColors: List<List<Any>> = if (state.useDynamicColor && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                    var wallpaperColors by remember { mutableStateOf<List<Int>?>(null) }

                                    LaunchedEffect(Unit) {
                                        wallpaperColors = MonetCompat.getInstance().getAvailableWallpaperColors()
                                    }

                                    wallpaperColors?.chunked(columns) ?: PresetColors.chunked(columns)
                                } else PresetColors.chunked(columns)

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    chunkedColors.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            rowItems.forEach { rawColor ->
                                                Box(
                                                    modifier = Modifier.weight(1f),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (rawColor is RawColor) {
                                                        ColorSwatchPreview(
                                                            rawColor = rawColor,
                                                            currentStyle = state.paletteStyle,
                                                            textStyle = MiuixTheme.textStyles.footnote1,
                                                            textColor = MiuixTheme.colorScheme.onSurface,
                                                            isSelected = !state.useDynamicColor && state.seedColor == rawColor.color
                                                        ) {
                                                            viewModel.dispatch(
                                                                PreferredViewAction.SetSeedColor(
                                                                    rawColor.color
                                                                )
                                                            )
                                                        }
                                                    } else if (rawColor is Int) {
                                                        val rawColor = RawColor(rawColor.toHexString(), Color(rawColor))
                                                        ColorSwatchPreview(
                                                            rawColor,
                                                            currentStyle = state.paletteStyle,
                                                            textStyle = MiuixTheme.textStyles.footnote1,
                                                            textColor = MiuixTheme.colorScheme.onSurface,
                                                            isSelected = state.seedColor == rawColor.color
                                                        ) {
                                                            viewModel.dispatch(
                                                                PreferredViewAction.SetSeedColor(
                                                                    rawColor.color
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            val remaining = columns - rowItems.size
                                            if (remaining > 0) {
                                                repeat(remaining) {
                                                    Spacer(Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { SmallTitle(stringResource(R.string.theme_settings_package_icons)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.theme_settings_prefer_system_icon),
                        description = stringResource(R.string.theme_settings_prefer_system_icon_desc),
                        checked = state.preferSystemIcon,
                        onCheckedChange = {
                            viewModel.dispatch(
                                PreferredViewAction.ChangePreferSystemIcon(it)
                            )
                        }
                    )
                }
            }
            item { SmallTitle(stringResource(R.string.theme_settings_launcher_icons)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.theme_settings_hide_launcher_icon),
                        description = stringResource(R.string.theme_settings_hide_launcher_icon_desc),
                        checked = !state.showLauncherIcon,
                        onCheckedChange = { newCheckedState ->
                            if (newCheckedState) {
                                showHideLauncherIconDialog.value = true
                            } else {
                                viewModel.dispatch(PreferredViewAction.ChangeShowLauncherIcon(true))
                            }
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}
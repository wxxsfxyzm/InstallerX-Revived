package com.rosan.installer.ui.page.miuix.settings.preferred.subpage.theme

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.settings.preferred.subpage.theme.ColorSwatchPreview
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixHideLauncherIconWarningDialog
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixThemeEngineWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixThemeModeWidget
import com.rosan.installer.ui.theme.m3color.PaletteStyle
import com.rosan.installer.ui.theme.m3color.PresetColors
import com.rosan.installer.ui.theme.m3color.RawColor
import com.rosan.installer.ui.theme.m3color.dynamicColorScheme
import com.rosan.installer.ui.util.getDisplayName
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
                        title = stringResource(R.string.theme_settings_dynamic_color),
                        description = stringResource(R.string.theme_settings_dynamic_color_miuix_desc),
                        checked = state.useMiuixMonet,
                        onCheckedChange = {
                            viewModel.dispatch(PreferredViewAction.SetUseMiuixMonet(it))
                        }
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        AnimatedVisibility(
                            visible = state.useMiuixMonet,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            MiuixSwitchWidget(
                                title = stringResource(R.string.theme_settings_dynamic_color_follow_system),
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

                                val chunkedColors = PresetColors.chunked(columns)

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
                                                    ColorSwatchPreview(
                                                        rawColor = rawColor,
                                                        currentStyle = state.paletteStyle,
                                                        isSelected = !state.useDynamicColor && state.seedColor == rawColor.color
                                                    ) {
                                                        viewModel.dispatch(PreferredViewAction.SetSeedColor(rawColor.color))
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

@Composable
private fun ColorSwatchPreview(
    rawColor: RawColor,
    currentStyle: PaletteStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDarkForPreview = false
    val scheme = remember(rawColor.color, currentStyle, isDarkForPreview) {
        dynamicColorScheme(
            keyColor = rawColor.color,
            isDark = isDarkForPreview,
            style = currentStyle
        )
    }

    val primaryForSwatch = scheme.primaryContainer.copy(alpha = 0.9f)
    val secondaryForSwatch = scheme.secondaryContainer.copy(alpha = 0.6f)
    val tertiaryForSwatch = scheme.tertiaryContainer.copy(alpha = 0.9f)

    val squircleBackgroundColor = scheme.primary.copy(alpha = 0.3f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(color = squircleBackgroundColor, shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = primaryForSwatch,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                    drawArc(
                        color = tertiaryForSwatch,
                        startAngle = 90f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                    drawArc(
                        color = secondaryForSwatch,
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = scheme.inversePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = rawColor.getDisplayName(LocalContext.current),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
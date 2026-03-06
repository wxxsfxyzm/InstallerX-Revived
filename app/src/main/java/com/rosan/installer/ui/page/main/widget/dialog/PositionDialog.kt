package com.rosan.installer.ui.page.main.widget.dialog

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rosan.installer.ui.util.WindowBlurEffect
import com.rosan.installer.ui.util.isPhoneLandscape

@Composable
fun PositionDialog(
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    useBlur: Boolean = false,
    onDismissRequest: () -> Unit,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    leftIcon: @Composable (() -> Unit)? = null,
    centerIcon: @Composable (() -> Unit)? = null,
    rightIcon: @Composable (() -> Unit)? = null,
    leftTitle: @Composable (() -> Unit)? = null,
    centerTitle: @Composable (() -> Unit)? = null,
    rightTitle: @Composable (() -> Unit)? = null,
    leftSubtitle: @Composable (() -> Unit)? = null,
    centerSubtitle: @Composable (() -> Unit)? = null,
    rightSubtitle: @Composable (() -> Unit)? = null,
    leftText: @Composable (() -> Unit)? = null,
    centerText: @Composable (() -> Unit)? = null,
    rightText: @Composable (() -> Unit)? = null,
    leftContent: @Composable (() -> Unit)? = null,
    centerContent: @Composable (() -> Unit)? = null,
    rightContent: @Composable (() -> Unit)? = null,
    leftButton: @Composable (() -> Unit)? = null,
    centerButton: @Composable (() -> Unit)? = null,
    rightButton: @Composable (() -> Unit)? = null
) {
    val dialogProperties = DialogProperties(
        dismissOnBackPress = properties.dismissOnBackPress,
        dismissOnClickOutside = properties.dismissOnClickOutside,
        securePolicy = properties.securePolicy,
        usePlatformDefaultWidth = false // Disable system width constraints
    )

    Dialog(onDismissRequest = onDismissRequest, properties = dialogProperties) {
        WindowBlurEffect(useBlur = useBlur)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(null) {
                    detectTapGestures(onTap = {
                        onDismissRequest()
                    })
                }) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    // Add padding so the dialog doesn't touch screen edges in landscape
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    // Set a reasonable maximum width for landscape and tablets
                    .widthIn(max = 640.dp)
                    .pointerInput(null) {
                        detectTapGestures(onTap = {})
                    }) {
                Surface(
                    modifier = modifier,
                    shape = shape,
                    color = containerColor,
                    tonalElevation = tonalElevation
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .sizeIn(minWidth = MinWidth, maxHeight = MaxHeight)
                            .padding(DialogPadding)
                    ) {
                        val isLandscape = isPhoneLandscape

                        if (isLandscape) {
                            LandscapeLayout(
                                iconContentColor, titleContentColor, textContentColor,
                                leftIcon, centerIcon, rightIcon,
                                leftTitle, centerTitle, rightTitle,
                                leftSubtitle, centerSubtitle, rightSubtitle,
                                leftText, centerText, rightText,
                                leftContent, centerContent, rightContent,
                                leftButton, centerButton, rightButton
                            )
                        } else {
                            PortraitLayout(
                                iconContentColor, titleContentColor, textContentColor,
                                leftIcon, centerIcon, rightIcon,
                                leftTitle, centerTitle, rightTitle,
                                leftSubtitle, centerSubtitle, rightSubtitle,
                                leftText, centerText, rightText,
                                leftContent, centerContent, rightContent,
                                leftButton, centerButton, rightButton
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PortraitLayout(
    iconContentColor: Color, titleContentColor: Color, textContentColor: Color,
    leftIcon: @Composable (() -> Unit)?, centerIcon: @Composable (() -> Unit)?, rightIcon: @Composable (() -> Unit)?,
    leftTitle: @Composable (() -> Unit)?, centerTitle: @Composable (() -> Unit)?, rightTitle: @Composable (() -> Unit)?,
    leftSubtitle: @Composable (() -> Unit)?, centerSubtitle: @Composable (() -> Unit)?, rightSubtitle: @Composable (() -> Unit)?,
    leftText: @Composable (() -> Unit)?, centerText: @Composable (() -> Unit)?, rightText: @Composable (() -> Unit)?,
    leftContent: @Composable (() -> Unit)?, centerContent: @Composable (() -> Unit)?, rightContent: @Composable (() -> Unit)?,
    leftButton: @Composable (() -> Unit)?, centerButton: @Composable (() -> Unit)?, rightButton: @Composable (() -> Unit)?
) {
    var buttonHeightPx by remember { mutableIntStateOf(0) }
    val buttonHeight = (buttonHeightPx / LocalDensity.current.density).dp
    val animatedButtonHeight by animateDpAsState(
        targetValue = buttonHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "button_height"
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { buttonHeightPx = it.height }
        ) {
            RenderButtons(leftButton, centerButton, rightButton)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = animatedButtonHeight.coerceAtLeast(0.dp))
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
        ) {
            RenderHeader(
                iconContentColor, titleContentColor,
                leftIcon, centerIcon, rightIcon,
                leftTitle, centerTitle, rightTitle,
                leftSubtitle, centerSubtitle, rightSubtitle
            )
            RenderContent(
                textContentColor,
                leftText, centerText, rightText,
                leftContent, centerContent, rightContent
            )
        }
    }
}

@Composable
private fun LandscapeLayout(
    iconContentColor: Color, titleContentColor: Color, textContentColor: Color,
    leftIcon: @Composable (() -> Unit)?, centerIcon: @Composable (() -> Unit)?, rightIcon: @Composable (() -> Unit)?,
    leftTitle: @Composable (() -> Unit)?, centerTitle: @Composable (() -> Unit)?, rightTitle: @Composable (() -> Unit)?,
    leftSubtitle: @Composable (() -> Unit)?, centerSubtitle: @Composable (() -> Unit)?, rightSubtitle: @Composable (() -> Unit)?,
    leftText: @Composable (() -> Unit)?, centerText: @Composable (() -> Unit)?, rightText: @Composable (() -> Unit)?,
    leftContent: @Composable (() -> Unit)?, centerContent: @Composable (() -> Unit)?, rightContent: @Composable (() -> Unit)?,
    leftButton: @Composable (() -> Unit)?, centerButton: @Composable (() -> Unit)?, rightButton: @Composable (() -> Unit)?
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Wrap the left column in a Box to fill max height and center content vertically
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Add vertical scroll to the left column to prevent clipping of long App Info Cards
                    .verticalScroll(rememberScrollState())
            ) {
                RenderHeader(
                    iconContentColor, titleContentColor,
                    leftIcon, centerIcon, rightIcon,
                    leftTitle, centerTitle, rightTitle,
                    leftSubtitle, centerSubtitle, rightSubtitle,
                    isLandscape = true // Pass true to enable horizontal compact layout
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            var buttonHeightPx by remember { mutableIntStateOf(0) }
            val buttonHeight = (buttonHeightPx / LocalDensity.current.density).dp
            val animatedButtonHeight by animateDpAsState(
                targetValue = buttonHeight,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "button_height_landscape"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { buttonHeightPx = it.height }
            ) {
                RenderButtons(leftButton, centerButton, rightButton)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = animatedButtonHeight.coerceAtLeast(0.dp))
            ) {
                RenderContent(
                    textContentColor,
                    leftText, centerText, rightText,
                    leftContent, centerContent, rightContent
                )
            }
        }
    }
}

@Composable
private fun RenderHeader(
    iconContentColor: Color, titleContentColor: Color,
    leftIcon: @Composable (() -> Unit)?, centerIcon: @Composable (() -> Unit)?, rightIcon: @Composable (() -> Unit)?,
    leftTitle: @Composable (() -> Unit)?, centerTitle: @Composable (() -> Unit)?, rightTitle: @Composable (() -> Unit)?,
    leftSubtitle: @Composable (() -> Unit)?, centerSubtitle: @Composable (() -> Unit)?, rightSubtitle: @Composable (() -> Unit)?,
    isLandscape: Boolean = false
) {
    // Check if we can safely combine center icon and center title horizontally in landscape
    val canCombineIconAndTitle = isLandscape && centerIcon != null && centerTitle != null

    if (canCombineIconAndTitle) {
        // Smart horizontal layout for Landscape mode: Icon on left, Title on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues.Absolute(left = DialogSinglePadding, right = DialogSinglePadding, bottom = 12.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                Box(modifier = Modifier.padding(end = 16.dp)) { centerIcon?.invoke() }
            }
            CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                    Box { centerTitle.invoke() }
                }
            }
        }
    } else {
        // Default vertical stacking logic (Portrait or missing components)
        PositionChildWidget(left = leftIcon, center = centerIcon, right = rightIcon) { icon ->
            CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                Box(modifier = Modifier.padding(IconPadding)) { icon?.invoke() }
            }
        }
        PositionChildWidget(left = leftTitle, center = centerTitle, right = rightTitle) { title ->
            CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                    Box(modifier = Modifier.padding(TitlePadding)) { title?.invoke() }
                }
            }
        }
    }

    // Subtitle rendering (App Info Card + Chips)
    PositionChildWidget(left = leftSubtitle, center = centerSubtitle, right = rightSubtitle) { subtitle ->
        CompositionLocalProvider(LocalContentColor provides titleContentColor) {
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                // Remove bottom padding dynamically if in landscape mode to save vertical space
                val padding = if (isLandscape) {
                    PaddingValues.Absolute(left = DialogSinglePadding, right = DialogSinglePadding, bottom = 0.dp)
                } else {
                    SubtitlePadding
                }
                Box(modifier = Modifier.padding(padding)) { subtitle?.invoke() }
            }
        }
    }
}

@Composable
private fun ColumnScope.RenderContent(
    textContentColor: Color,
    leftText: @Composable (() -> Unit)?, centerText: @Composable (() -> Unit)?, rightText: @Composable (() -> Unit)?,
    leftContent: @Composable (() -> Unit)?, centerContent: @Composable (() -> Unit)?, rightContent: @Composable (() -> Unit)?
) {
    val contentMode = leftContent != null || centerContent != null || rightContent != null
    PositionChildWidget(
        // The weight modifier MUST be passed to the top-level Box inside PositionChildWidget
        modifier = Modifier.weight(1f, fill = false),
        left = if (contentMode) leftContent else leftText,
        center = if (contentMode) centerContent else centerText,
        right = if (contentMode) rightContent else rightText
    ) { text ->
        CompositionLocalProvider(LocalContentColor provides textContentColor) {
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (contentMode) ContentPadding else TextPadding)
                ) {
                    text?.invoke()
                }
            }
        }
    }
}

@Composable
private fun RenderButtons(
    leftButton: @Composable (() -> Unit)?, centerButton: @Composable (() -> Unit)?, rightButton: @Composable (() -> Unit)?
) {
    PositionChildWidget(left = leftButton, center = centerButton, right = rightButton) { button ->
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
            ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
                Box(modifier = Modifier.padding(ButtonPadding)) { button?.invoke() }
            }
        }
    }
}

@Composable
private fun PositionChildWidget(
    modifier: Modifier = Modifier,
    left: @Composable (() -> Unit)? = null,
    center: @Composable (() -> Unit)? = null,
    right: @Composable (() -> Unit)? = null,
    parent: @Composable ((child: @Composable (() -> Unit)?) -> Unit)
) {
    if (left == null && center == null && right == null) return
    Box(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            parent.invoke(left)
        }
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            parent.invoke(center)
        }
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            parent.invoke(right)
        }
    }
}

private val ButtonsMainAxisSpacing = 8.dp
private val ButtonsCrossAxisSpacing = 12.dp

private val DialogSinglePadding = 16.dp

private val DialogPadding = PaddingValues(top = DialogSinglePadding, bottom = DialogSinglePadding)
private val IconPadding = PaddingValues.Absolute(left = DialogSinglePadding, right = DialogSinglePadding, bottom = 12.dp)
private val TitlePadding = PaddingValues.Absolute(left = DialogSinglePadding, right = DialogSinglePadding, bottom = 12.dp)
private val SubtitlePadding = PaddingValues.Absolute(left = DialogSinglePadding, right = DialogSinglePadding, bottom = 12.dp)
private val TextPadding = PaddingValues.Absolute(left = DialogSinglePadding, right = DialogSinglePadding, bottom = 12.dp)
private val ContentPadding = PaddingValues.Absolute(bottom = 8.dp)
private val ButtonPadding = PaddingValues(start = DialogSinglePadding, end = DialogSinglePadding, bottom = 0.dp)

private val MinWidth = 280.dp
private val MaxHeight = 650.dp
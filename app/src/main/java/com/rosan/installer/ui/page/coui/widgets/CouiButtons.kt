// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.widgets

import android.graphics.Paint
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.effect.CouiContour
import com.itos.coui.effect.CouiContourStyle
import com.itos.coui.effect.LocalCouiContourGlowEnabled
import com.itos.coui.effect.SpotlightPreset
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.effect.rememberCouiSpotlight
import com.itos.coui.icons.CouiIcons
import com.itos.coui.motion.couiPressMotion
import com.itos.coui.motion.couiTouchMotion
import com.itos.coui.motion.rememberCouiPressMotion
import com.itos.coui.motion.rememberCouiTouchMotion
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.TopAppBarButton
import com.rosan.installer.R
import androidx.compose.material3.Text as M3Text

/**
 * Top bar icon button using COUI's floating glass TopAppBarButton on API 34+,
 * falling back to a standard IconButton on API 31-33.
 */
@Composable
fun CouiTopBarIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        TopAppBarButton(
            modifier = modifier,
            onClick = onClick,
            enabled = enabled,
        ) {
            content()
        }
    } else {
        IconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
        ) {
            content()
        }
    }
}

@Composable
fun CouiBackButton(
    modifier: Modifier = Modifier,
    icon: ImageVector = CouiIcons.Filled.Back,
    contentDescription: String = stringResource(id = R.string.back),
    onClick: () -> Unit,
) {
    CouiTopBarIconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CouiActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    primary: Boolean = false,
    destructive: Boolean = false,
    shape: Shape = CouiCapsuleShape,
    minHeight: Dp = 44.dp,
) {
    val dark = CouiTheme.isDark
    val scheme = CouiTheme.colorScheme
    val isGlass = LocalCouiGlassSurface.current
    val glow = LocalCouiContourGlowEnabled.current
    val isFilled = primary || destructive
    val destructiveAccent = if (dark) Color(0xFFFF6C61) else Color(0xFFDB382C)

    val containerColor = when {
        !enabled && destructive -> destructiveAccent.copy(alpha = 0.30f)
        !enabled && primary -> scheme.controlAccent.copy(alpha = 0.30f)
        !enabled -> if (dark) Color(0x1AFFFFFF) else Color(0x14000000)
        destructive -> destructiveAccent
        primary -> scheme.controlAccent
        isGlass -> if (dark) Color(0x2EFFFFFF) else Color(0xF5FFFFFF)
        else -> if (dark) Color(0x24FFFFFF) else Color(0x14000000)
    }
    val contentColor = when {
        !enabled && isFilled -> Color.White.copy(alpha = 0.55f)
        !enabled -> scheme.onSurface.copy(alpha = 0.35f)
        isFilled -> Color.White
        else -> scheme.onSurface
    }

    val source = remember { MutableInteractionSource() }
    val motion = rememberCouiTouchMotion(enabled)
    val press = rememberCouiPressMotion()
    val (lightInput, drawLight) = rememberCouiSpotlight(
        interactive = enabled,
        dark = dark,
        preset = if (isFilled) SpotlightPreset.OpaqueMedium1 else SpotlightPreset.OpaqueMedium2,
        cancelOnConsumedMove = true,
    )
    val contour = remember(glow) {
        if (glow && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            CouiContour(style = CouiContourStyle.Button)
        } else {
            null
        }
    }
    val contourPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .graphicsLayer {
                scaleX = motion.scaleX.value + press.scale.value - 1f
                scaleY = motion.scaleY.value + press.scale.value - 1f
                translationX = motion.translationX.value
                translationY = motion.translationY.value
            }
            .couiTouchMotion(motion, enabled)
            .couiPressMotion(press, enabled, cancelOnConsumedMove = true)
            .then(lightInput)
            .clip(shape)
            .drawWithContent {
                drawRect(containerColor)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && contour != null) {
                    val alpha = if (isFilled) {
                        if (dark) (if (enabled) 0.25f else 0.05f) else 0.30f
                    } else if (dark) {
                        if (enabled) 0.12f else 0.03f
                    } else {
                        if (enabled) 0.80f else 0.24f
                    }
                    val inner = if (isFilled) {
                        if (enabled) 0.10f else if (dark) 0.06f else 0.04f
                    } else if (dark) {
                        0.02f
                    } else {
                        0.06f
                    }
                    contourPaint.shader = contour.shader(
                        size = size,
                        density = density,
                        radiusPx = size.minDimension / 2f,
                        cornerWeight = 1f,
                        surfaceColor = Color.Transparent,
                        innerGlowAlpha = inner,
                        lineAlpha = alpha,
                        effectAlpha = 1f,
                        lineWidthPx = (if (isFilled) 2.5f else 3f) * density,
                    )
                    drawIntoCanvas { it.nativeCanvas.drawRect(0f, 0f, size.width, size.height, contourPaint) }
                }
                drawContent()
                drawLight?.invoke(this)
            }
            .couiButtonFeedback(dark = dark, interactionSource = source, enabled = enabled, pressFeedback = !isGlass)
            .combinedClickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        M3Text(
            text = text,
            color = contentColor,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun CouiProgressButton(
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CouiCapsuleShape,
    minHeight: Dp = 44.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val progressCoerced = progress.coerceIn(0f, 1f)
    val dark = CouiTheme.isDark
    val scheme = CouiTheme.colorScheme
    val isGlass = LocalCouiGlassSurface.current
    val glow = LocalCouiContourGlowEnabled.current
    val trackColor = when {
        isGlass -> if (dark) Color(0x2EFFFFFF) else Color(0xF5FFFFFF)
        dark -> scheme.onSurface.copy(alpha = 0.14f)
        else -> scheme.onSurface.copy(alpha = 0.08f)
    }
    val progressColor = scheme.controlAccent
    val contentColor = if (progressCoerced > 0.45f) Color.White else scheme.onSurface
    val source = remember { MutableInteractionSource() }
    val motion = rememberCouiTouchMotion(enabled)
    val press = rememberCouiPressMotion()
    val (lightInput, drawLight) = rememberCouiSpotlight(
        interactive = enabled,
        dark = dark,
        preset = SpotlightPreset.OpaqueMedium2,
        cancelOnConsumedMove = true,
    )
    val contour = remember(glow) {
        if (glow && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            CouiContour(style = CouiContourStyle.Button)
        } else {
            null
        }
    }
    val contourPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .graphicsLayer {
                scaleX = motion.scaleX.value + press.scale.value - 1f
                scaleY = motion.scaleY.value + press.scale.value - 1f
                translationX = motion.translationX.value
                translationY = motion.translationY.value
            }
            .couiTouchMotion(motion, enabled)
            .couiPressMotion(press, enabled, cancelOnConsumedMove = true)
            .then(lightInput)
            .clip(shape)
            .background(trackColor, shape)
            .drawWithContent {
                if (progressCoerced > 0f) {
                    drawRect(
                        color = progressColor,
                        size = Size(width = size.width * progressCoerced, height = size.height),
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && contour != null) {
                    contourPaint.shader = contour.shader(
                        size = size,
                        density = density,
                        radiusPx = size.minDimension / 2f,
                        cornerWeight = 1f,
                        surfaceColor = Color.Transparent,
                        innerGlowAlpha = if (dark) 0.03f else 0.06f,
                        lineAlpha = if (dark) 0.14f else 0.72f,
                        effectAlpha = 1f,
                        lineWidthPx = 3f * density,
                    )
                    drawIntoCanvas { it.nativeCanvas.drawRect(0f, 0f, size.width, size.height, contourPaint) }
                }
                drawContent()
                drawLight?.invoke(this)
            }
            .couiButtonFeedback(dark = dark, interactionSource = source, enabled = enabled, pressFeedback = !isGlass)
            .combinedClickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

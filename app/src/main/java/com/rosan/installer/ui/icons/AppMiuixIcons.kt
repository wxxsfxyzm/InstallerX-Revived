package com.rosan.installer.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Settings

object AppMiuixIcons {
    // --- App Icons ---
    val Settings = MiuixIcons.Regular.Settings
    val Info = MiuixIcons.Regular.Info
    val Refresh = MiuixIcons.Regular.Refresh
    val Ok = MiuixIcons.Regular.Ok

    // --- Navigation Icons ---
    val Back = MiuixIcons.Regular.Back

    val Close: ImageVector
        get() {
            if (_closeRegular != null) return _closeRegular!!
            _closeRegular = ImageVector.Builder(
                name = "Close.Regular",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 1035.0f,
                viewportHeight = 1035.0f,
            ).apply {
                // Layer 1: Padding Group
                // Adjusted scale to 0.85f for a slightly larger icon (less padding).
                group(
                    scaleX = 0.85f,
                    scaleY = 0.85f,
                    pivotX = 517.5f, // Center of 1035.0f
                    pivotY = 517.5f  // Center of 1035.0f
                ) {
                    // Layer 2: Original Transform Group
                    group(
                        scaleX = 1.0f,
                        scaleY = -1.0f,
                        translationX = -147.5f,
                        translationY = 895.25f
                    ) {
                        addPath(
                            pathData = listOf(
                                PathNode.MoveTo(665.0f, 441.0f),
                                PathNode.LineTo(1020.0f, 798.0f),
                                PathNode.QuadTo(1030.0f, 809.0f, 1041.0f, 809.0f),
                                PathNode.QuadTo(1052.0f, 809.0f, 1062.0f, 798.0f),
                                PathNode.LineTo(1084.0f, 776.0f),
                                PathNode.QuadTo(1094.0f, 766.0f, 1094.0f, 755.5f),
                                PathNode.QuadTo(1094.0f, 745.0f, 1084.0f, 734.0f),
                                PathNode.LineTo(729.0f, 377.0f),
                                PathNode.LineTo(1085.0f, 21.0f),
                                PathNode.QuadTo(1096.0f, 11.0f, 1096.0f, 0.0f),
                                PathNode.QuadTo(1096.0f, -11.0f, 1085.0f, -21.0f),
                                PathNode.LineTo(1063.0f, -43.0f),
                                PathNode.QuadTo(1042.0f, -64.0f, 1021.0f, -43.0f),
                                PathNode.LineTo(665.0f, 313.0f),
                                PathNode.LineTo(308.0f, -42.0f),
                                PathNode.QuadTo(298.0f, -52.0f, 287.0f, -52.0f),
                                PathNode.QuadTo(276.0f, -52.0f, 266.0f, -42.0f),
                                PathNode.LineTo(244.0f, -20.0f),
                                PathNode.QuadTo(234.0f, -10.0f, 234.0f, 1.0f),
                                PathNode.QuadTo(234.0f, 12.0f, 244.0f, 22.0f),
                                PathNode.LineTo(601.0f, 377.0f),
                                PathNode.LineTo(245.0f, 734.0f),
                                PathNode.QuadTo(235.0f, 745.0f, 235.0f, 755.5f),
                                PathNode.QuadTo(235.0f, 766.0f, 245.0f, 776.0f),
                                PathNode.LineTo(267.0f, 798.0f),
                                PathNode.QuadTo(278.0f, 809.0f, 288.5f, 809.0f),
                                PathNode.QuadTo(299.0f, 809.0f, 309.0f, 798.0f),
                                PathNode.Close,
                            ),
                            fill = SolidColor(Color.Black),
                            fillAlpha = 1f,
                            pathFillType = PathFillType.NonZero,
                        )
                    }
                }
            }.build()
            return _closeRegular!!
        }

    private var _closeRegular: ImageVector? = null

}
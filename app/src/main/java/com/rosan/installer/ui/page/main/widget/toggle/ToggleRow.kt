package com.rosan.installer.ui.page.main.widget.toggle

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ToggleRow(
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = RoundedCornerShape(8.dp),
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    spacing: Dp = 1.dp,
    indicator: @Composable (positions: List<TogglePosition>) -> Unit = @Composable { positions ->
        val indicatorShape = if (shape !is CornerBasedShape) RoundedCornerShape(0.dp)
        else {
            val left by animateDpAsState((if (selectedIndex == 0) 8 else 0).dp)
            val right by animateDpAsState((if (selectedIndex + 1 == positions.size) 8 else 0).dp)
            RoundedCornerShape(
                topStart = left, bottomStart = left, topEnd = right, bottomEnd = right
            )
        }

        val transition = updateTransition(selectedIndex, label = "indicator")
        val left by transition.animateDp(label = "left", transitionSpec = {
            spring(stiffness = if (initialState < targetState) 100f else 200f)
        }) {
            positions[it].left
        }

        val right by transition.animateDp(label = "right", transitionSpec = {
            spring(stiffness = if (initialState < targetState) 200f else 100f)
        }) {
            positions[it].right
        }

        val width = right - left

        Box(modifier = Modifier.fillMaxSize()) {
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .offset(left)
                    .width(width)
                    .clip(indicatorShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            positions.forEachIndexed { index, togglePosition ->
                if (index < positions.size - 1)
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .offset(togglePosition.left + togglePosition.width)
                            .width(spacing)
                            .background(MaterialTheme.colorScheme.primary)
                    )
            }
        }
    },
    toggles: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = backgroundColor,
        contentColor = contentColor,
        shape = shape,
        border = border
    ) {
        SubcomposeLayout(
            modifier = Modifier
                .wrapContentSize(Alignment.CenterStart)
                .horizontalScroll(rememberScrollState())
                .selectableGroup()
        ) { constraints ->
            val togglePlaceables = subcompose(ToggleSlots.Toggles, toggles).map {
                it.measure(constraints)
            }
            val toggleCount = togglePlaceables.size
            val togglesWidth = togglePlaceables.fold(0) { curr, measurable ->
                curr + measurable.width
            }

            val spacingWidth = spacing.roundToPx()

            val layoutWidth = togglesWidth + (toggleCount - 1) * spacingWidth

            val layoutHeight = togglePlaceables.maxBy { it.height }.height

            layout(layoutWidth, layoutHeight) {
                val positions = mutableListOf<TogglePosition>()

                togglePlaceables.foldIndexed(0) { index, curr, measurable ->
                    positions.add(TogglePosition(curr.toDp(), measurable.width.toDp()))
                    curr + measurable.width + if (index < toggleCount - 1) spacingWidth else 0
                }

                subcompose(ToggleSlots.Indicator) {
                    indicator.invoke(positions)
                }.forEach {
                    it.measure(Constraints.fixed(layoutWidth, layoutHeight)).placeRelative(0, 0)
                }

                togglePlaceables.foldIndexed(0) { index, curr, measurable ->
                    measurable.placeRelative(curr, 0)
                    curr + measurable.width + if (index < toggleCount - 1) spacingWidth else 0
                }
            }
        }
    }
}

data class TogglePosition(val left: Dp, val width: Dp) {
    val right = left + width
}

private enum class ToggleSlots {
    Toggles, Spacing, Indicator
}
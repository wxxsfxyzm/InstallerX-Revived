package com.rosan.installer.ui.animation.predictiveback

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigationevent.NavigationEvent.Companion.EDGE_LEFT
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import com.rosan.installer.ui.util.rememberDeviceCornerRadius

class ScalePredictiveBackAnimation(
    private val exitDirection: PredictiveExitDirection = PredictiveExitDirection.ALWAYS_RIGHT
) : PredictiveBackAnimationHandler {
    private var exitingPageKey by mutableStateOf<String?>(null)
    private val exitAnimatable = Animatable(0f)

    override suspend fun onBackPressed(
        transitionState: NavigationEventTransitionState?,
        currentPageKey: NavKey?,
    ) {
        if (transitionState is InProgress) {
            exitingPageKey = currentPageKey.toString()
            exitAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 200,
                    easing = FastOutSlowInEasing
                )
            )
            exitAnimatable.snapTo(0f)
        }
    }

    @Composable
    override fun Modifier.predictiveBackAnimationDecorator(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: NavKey?,
    ): Modifier {
        val windowInfo = LocalWindowInfo.current
        val navContent = LocalNavAnimatedContentScope.current

        val containerHeightPx = windowInfo.containerSize.height
        val containerWidthPx = windowInfo.containerSize.width.toFloat()
        val pageKey = contentPageKey.toString()
        val transition = navContent.transition
        val deviceCornerRadius = rememberDeviceCornerRadius()

        val tripe =
            if (pageKey == currentPageKey.toString() || exitingPageKey == pageKey) {
                // Calculate the page scale
                val animatedScale by transition.animateFloat(
                    transitionSpec = { tween(300) },
                    label = "PredictiveScale"
                ) { state ->
                    when (state) {
                        EnterExitState.PostExit -> 0.85f
                        else -> 1f
                    }
                }

                val progressInProgress = (transitionState as? InProgress)
                val edge = progressInProgress?.latestEvent?.swipeEdge ?: 0
                val touchY = progressInProgress?.latestEvent?.touchY

                // Pivot Y calculation based on touch point
                val currentPivotY = if (touchY != null && containerHeightPx > 0) {
                    (touchY / containerHeightPx).coerceIn(0.1f, 0.9f)
                } else 0.5f

                // Pivot X follows the gesture to ensure scaling feels attached to the finger
                val currentPivotX = if (edge == EDGE_LEFT) 0.8f else 0.2f

                // Determine the translation direction multiplier based on the provided parameter
                val directionMultiplier = when (exitDirection) {
                    PredictiveExitDirection.FOLLOW_GESTURE -> if (edge == EDGE_LEFT) 1f else -1f
                    PredictiveExitDirection.ALWAYS_RIGHT -> 1f
                    PredictiveExitDirection.ALWAYS_LEFT -> -1f
                }

                // Apply translation during the final exit phase
                val exitProgress = if (pageKey != currentPageKey.toString()) 1f else exitAnimatable.value
                val animatedTranslationX = containerWidthPx * exitProgress * directionMultiplier

                val modifier = this.graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    translationX = animatedTranslationX
                    transformOrigin = TransformOrigin(currentPivotX, currentPivotY)
                }

                Pair(modifier, deviceCornerRadius)
            } else {
                // Dimming logic for the background page
                val modifier = if (transitionState is InProgress) {
                    val progress = exitAnimatable.value
                    val dynamicAlpha = 0.5f * (1f - progress)

                    this
                        .graphicsLayer()
                        .drawWithContent {
                            drawContent()
                            drawRect(color = Color.Black.copy(alpha = dynamicAlpha))
                        }
                } else Modifier

                Pair(modifier, 0.dp)
            }

        return tripe.first.clip(RoundedCornerShape(tripe.second))
    }

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onPredictivePopTransitionSpec(
        swipeEdge: Int
    ): ContentTransform = ContentTransform(
        targetContentEnter = EnterTransition.None,
        initialContentExit = ExitTransition.None,
        sizeTransform = null
    )

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onPopTransitionSpec(): ContentTransform =
        ContentTransform(
            targetContentEnter = slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn(),
            initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
            sizeTransform = null
        )

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onTransitionSpec(): ContentTransform =
        ContentTransform(
            targetContentEnter = slideInHorizontally(initialOffsetX = { it }),
            initialContentExit = fadeOut(),
            sizeTransform = null
        )
}

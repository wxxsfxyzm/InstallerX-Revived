package com.rosan.installer.ui.animation.predictiveback

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Box
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

class ScalePredictiveBackAnimation : PredictiveBackAnimationHandler {
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
    override fun PredictiveBackAnimationDecorator(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: NavKey?,
        content: @Composable (() -> Unit)
    ) {
        val windowInfo = LocalWindowInfo.current
        val navContent = LocalNavAnimatedContentScope.current

        val containerHeightPx = windowInfo.containerSize.height
        val containerWidthPx = windowInfo.containerSize.width.toFloat()
        val pageKey = contentPageKey.toString()
        val transition = navContent.transition

        val tripe =
            if (pageKey == currentPageKey.toString() || exitingPageKey == pageKey
            ) {
                // calculate the corner shape
                val roundedCornerSize by transition.animateDp(
                    transitionSpec = {
                        tween(300)
                    },
                    label = "DynamicCornerShape"
                ) { state ->
                    when (state) {
                        EnterExitState.PostExit -> rememberDeviceCornerRadius()
                        else -> 0.dp
                    }
                }

                // calculate the page scale
                val animatedScale by transition.animateFloat(
                    transitionSpec = {
                        tween(300)
                    },
                    label = "PredictiveScale"
                ) { state ->
                    when (state) {
                        EnterExitState.PostExit -> 0.85f
                        else -> 1f
                    }
                }

                // calculate WHERE is the scaled page
                val touchY = (transitionState as? InProgress)?.latestEvent?.touchY

                val currentPivotY =
                    if (touchY != null && containerHeightPx > 0) {
                        (touchY / containerHeightPx).coerceIn(
                            0.1f,
                            0.9f
                        )
                    } else 0.5f

                // if the navigation gesture originates from the left edge, we let it scale to right
                // otherwise, scale to left
                val edge =
                    (transitionState as? InProgress)?.latestEvent?.swipeEdge ?: 0

                val directionMultiplier =
                    if (edge == EDGE_LEFT) 1f else -1f

                // take some space to the screen edge
                // currentPivotX is 0.0f-1.0f,
                // We use 0.2f for the card in left, 0.8f for the card in right
                val currentPivotX =
                    if (edge == EDGE_LEFT) 0.8f else 0.2f

                // if we are playing the exit animation, calculate the scaled Page's TranslationX,
                // navigation gesture left -> exit to right
                // navigation gesture right -> exit to left
                val progress =
                    if (pageKey != currentPageKey.toString()) 1f else exitAnimatable.value
                val animatedTranslationX =
                    containerWidthPx * progress * directionMultiplier

                // render this animation
                val modifier = Modifier.graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    translationX = animatedTranslationX
                    transformOrigin = TransformOrigin(
                        currentPivotX,
                        currentPivotY
                    )
                }

                Pair(
                    modifier,
                    roundedCornerSize
                )
            } else {
                val modifier =
                    if (transitionState is InProgress) {
                        // We calculate the new page's black dim alpha in here
                        // If we are in PredictiveBackAnimation, always 0.5f dim
                        // If we are playing the exit animation, dynamic calculate the dim with exit animation's progress
                        // alpha = 0.5 * (1f - animationProgress) (decrease alpha when increase progress)
                        // so, alpha will always in 0 - 0.5f
                        val progress = exitAnimatable.value
                        val dynamicAlpha = 0.5f * (1f - progress)

                        Modifier
                            .graphicsLayer()
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    color = Color.Black.copy(
                                        alpha = dynamicAlpha
                                    )
                                )
                            }
                    } else Modifier

                Pair(modifier, 0.dp)
            }

        Box(
            modifier = tripe.first
                .clip(RoundedCornerShape(tripe.second)),
        ) {
            content()
        }
    }

    // We directly make this Spec to ALL None,
    // because if we set any animation in here, it will override our custom animations
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
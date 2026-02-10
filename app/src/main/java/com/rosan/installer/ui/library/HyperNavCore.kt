package app.hypershell.ui.library.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Represents a top-level destination in the navigation graph.
 *
 * @property route The unique route identifier for this destination.
 * @property content The composable content to be displayed for this destination.
 */
@Immutable
data class HyperDestination(
    val route: String,
    val content: @Composable (PaddingValues) -> Unit
)

/**
 * Controller for managing navigation within a [HyperNavHost].
 *
 * It handles switching between top-level destinations (tabs) using a [PagerState]
 * and manages simple stack-based sub-navigation for each tab.
 *
 * @property pagerState The state of the horizontal pager used for tab switching.
 * @property destinations The list of available top-level destinations.
 * @property scope A coroutine scope for performing navigation actions like scrolling.
 */
@Stable
class HyperNavController(
    val pagerState: PagerState,
    val destinations: List<HyperDestination>,
    private val scope: CoroutineScope
) {
    private val _stackStates = mutableStateMapOf<String, String?>()

    /**
     * Navigates to a specific route.
     *
     * The route can be a top-level route (e.g., "home") or a nested route (e.g., "home/settings").
     * If it's a top-level route, the pager will animate to that page.
     * If it includes a sub-route, that sub-route will be set as the active stack state for the parent.
     *
     * @param route The destination route string.
     */
    fun navigate(route: String) {
        val parts = route.split("/")
        val parentRoute = parts[0]
        val subRoute = parts.getOrNull(1)

        val tabIndex = destinations.indexOfFirst { it.route == parentRoute }
        if (tabIndex != -1 && pagerState.targetPage != tabIndex) {
            scope.launch {
                pagerState.animateScrollToPage(tabIndex)
            }
        }

        if (subRoute != null && _stackStates[parentRoute] != subRoute) {
            _stackStates[parentRoute] = subRoute
        }
    }

    /**
     * Returns the currently active sub-route for a given parent route.
     *
     * @param parentRoute The route of the top-level destination.
     * @return The active sub-route, or null if no sub-route is active.
     */
    fun getActiveSubRoute(parentRoute: String): String? = _stackStates[parentRoute]

    /**
     * Clears the active sub-route for the specified parent route, effectively "popping" the stack.
     *
     * @param parentRoute The route of the top-level destination.
     */
    fun popStack(parentRoute: String) {
        if (_stackStates[parentRoute] != null) {
            _stackStates[parentRoute] = null
        }
    }
}

/**
 * CompositionLocal for accessing the [HyperNavController] within the composition tree.
 */
val LocalHyperNavController = staticCompositionLocalOf<HyperNavController> {
    error("HyperNavController not provided.")
}

/**
 * Creates and remembers a navigation graph (list of destinations).
 *
 * @param builder A lambda for configuring the graph using [HyperGraphBuilder].
 * @return A remembered list of [HyperDestination]s.
 */
@Composable
fun rememberHyperGraph(builder: HyperGraphBuilder.() -> Unit): List<HyperDestination> {
    return remember { HyperGraphBuilder().apply(builder).build() }
}

/**
 * Creates and remembers a [HyperNavController].
 *
 * @param destinations The list of destinations in the navigation graph.
 * @param startRoute The initial route to navigate to. Defaults to the first destination.
 * @return A remembered [HyperNavController].
 */
@Composable
fun rememberHyperNavController(
    destinations: List<HyperDestination>,
    startRoute: String? = null
): HyperNavController {
    val scope = rememberCoroutineScope()
    val initialPage = remember(destinations, startRoute) {
        if (startRoute != null) {
            destinations.indexOfFirst { it.route == startRoute }.coerceAtLeast(0)
        } else 0
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { destinations.size }
    )
    return remember(pagerState, destinations, scope) {
        HyperNavController(pagerState, destinations, scope)
    }
}

/**
 * DSL builder for creating a navigation graph.
 */
class HyperGraphBuilder {
    private val _destinations = mutableListOf<HyperDestination>()

    /**
     * Adds a composable destination to the graph.
     *
     * @param route The unique route identifier.
     * @param content The composable content for this route.
     */
    fun composable(route: String, content: @Composable (PaddingValues) -> Unit) {
        _destinations.add(HyperDestination(route, content))
    }

    /**
     * Internal function to build the destination list.
     */
    fun build() = _destinations.toList()
}

/**
 * A navigation host that displays destinations in a [HorizontalPager].
 *
 * It manages tab switching and provides the [HyperNavController] to its children.
 *
 * @param navController The controller to manage navigation.
 * @param modifier Modifier for the nav host.
 * @param contentPadding Padding to be applied to the content of each destination.
 * @param onStackStateChanged Callback triggered when the sub-navigation stack state changes.
 */
@Composable
fun HyperNavHost(
    navController: HyperNavController,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onStackStateChanged: (Boolean) -> Unit = {}
) {
    val destinations = navController.destinations
    val pagerState = navController.pagerState

    val currentTab = destinations.getOrNull(pagerState.currentPage)?.route
    val isStackActive = currentTab?.let { navController.getActiveSubRoute(it) != null } == true

    LaunchedEffect(isStackActive) {
        onStackStateChanged(isStackActive)
    }

    CompositionLocalProvider(LocalHyperNavController provides navController) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier.fillMaxSize(),
            userScrollEnabled = !isStackActive,
        ) { pageIndex ->
            destinations.getOrNull(pageIndex)?.content?.invoke(contentPadding)
        }
    }
}

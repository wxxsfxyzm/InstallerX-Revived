package com.rosan.installer.ui.page.miuix.settings.config.all

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.page.main.settings.config.all.AllViewAction
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel
import com.rosan.installer.ui.page.main.settings.config.all.AllViewState
import com.rosan.installer.ui.page.miuix.widgets.MiuixScopeTipCard
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun MiuixAllPage(
    navController: NavController,
    viewModel: AllViewModel,
    hazeState: HazeState,
    title: String,
    outerPadding: PaddingValues
) {
    LaunchedEffect(Unit) {
        viewModel.navController = navController
    }

    val listState = rememberLazyStaggeredGridState()

    val scrollBehavior = MiuixScrollBehavior()
    val hazeStyle = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.surface,
        tint = HazeTint(MiuixTheme.colorScheme.surface.copy(alpha = 0.8f))
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.hazeEffect(hazeState) {
                    style = hazeStyle
                    blurRadius = 30.dp
                    noiseFactor = 0f
                },
                title = title,
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        when (viewModel.state.data.progress) {
            is AllViewState.Data.Progress.Loading if viewModel.state.data.configs.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InfiniteProgressIndicator()
                        Text(
                            text = stringResource(id = R.string.loading),
                            style = MiuixTheme.textStyles.main
                        )
                    }
                }
            }

            is AllViewState.Data.Progress.Loaded if viewModel.state.data.configs.isEmpty() -> {
                // TODO Add error handling
                // Since we don't allow removing default profile,
                // There is no need to handle an empty state.
            }

            else -> {
                val configs = viewModel.state.data.configs
                val minId = configs.minByOrNull { it.id }?.id

                LazyVerticalStaggeredGrid(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(hazeState)
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    columns = StaggeredGridCells.Adaptive(350.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding() + 16.dp,
                        bottom = outerPadding.calculateBottomPadding() + 16.dp
                    ),
                    verticalItemSpacing = 16.dp,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    overscrollEffect = null,
                    state = listState,
                ) {

                    if (!viewModel.state.userReadScopeTips)
                        item {
                            MiuixScopeTipCard(viewModel = viewModel)
                        }
                    else item { Spacer(modifier = Modifier.size(6.dp)) }

                    items(configs) {
                        DataItemWidget(viewModel, it, it.id == minId)
                    }
                }
            }
        }
    }
}

@Composable
private fun DataItemWidget(
    viewModel: AllViewModel,
    entity: ConfigEntity,
    isDefault: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = entity.name,
                        style = MiuixTheme.textStyles.title4,
                        fontWeight = FontWeight.Medium
                    )
                    if (isDefault) {
                        Spacer(modifier = Modifier.size(8.dp))
                        Card(
                            colors = CardDefaults.defaultColors(
                                color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                contentColor = MiuixTheme.colorScheme.onSurface
                            )
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(
                                    text = stringResource(R.string.config_global_default),
                                    color = MiuixTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.size(4.dp))
                if (entity.description.isNotEmpty()) {
                    Text(
                        text = entity.description,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.subtitle
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                minHeight = 35.dp,
                minWidth = 35.dp,
                backgroundColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                onClick = { viewModel.dispatch(AllViewAction.MiuixEditDataConfig(entity)) }) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = MiuixIcons.Regular.Edit,
                    tint = MiuixTheme.colorScheme.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.7f else 0.9f),
                    contentDescription = stringResource(id = R.string.edit)
                )
            }
            if (!isDefault)
                IconButton(
                    minHeight = 35.dp,
                    minWidth = 35.dp,
                    backgroundColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                    onClick = { viewModel.dispatch(AllViewAction.DeleteDataConfig(entity)) }) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = MiuixIcons.Regular.Delete,
                        tint = MiuixTheme.colorScheme.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.7f else 0.9f),
                        contentDescription = stringResource(id = R.string.delete)
                    )
                }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                minHeight = 35.dp,
                minWidth = 35.dp,
                backgroundColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                onClick = { viewModel.dispatch(AllViewAction.ApplyConfig(entity)) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = MiuixIcons.Regular.SelectAll,
                        tint = MiuixTheme.colorScheme.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.7f else 0.9f),
                        contentDescription = stringResource(id = R.string.apply)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        modifier = Modifier.padding(end = 3.dp),
                        text = stringResource(R.string.config_scope),
                        color = MiuixTheme.colorScheme.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.7f else 0.9f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
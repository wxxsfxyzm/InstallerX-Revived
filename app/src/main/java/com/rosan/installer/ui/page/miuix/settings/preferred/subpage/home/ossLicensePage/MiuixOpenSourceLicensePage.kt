package com.rosan.installer.ui.page.miuix.settings.preferred.subpage.home.ossLicensePage

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.rememberMiuixHazeStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixOpenSourceLicensePage(
    navController: NavController,
    viewModel: PreferredViewModel
) {
    val state = viewModel.state
    val libraries by produceLibraries(R.raw.aboutlibraries)
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = if (state.useBlur) remember { HazeState() } else null
    val hazeStyle = rememberMiuixHazeStyle()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                color = hazeState.getMiuixAppBarColor(),
                title = stringResource(id = R.string.open_source_license),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    MiuixBackButton(
                        modifier = Modifier.padding(start = 16.dp),
                        onClick = { navController.navigateUp() })
                }
            )
        },
    ) { paddingValues ->
        LibrariesContainer(
            modifier = Modifier
                .fillMaxSize()
                .then(hazeState?.let { Modifier.hazeSource(it) } ?: Modifier)
                .scrollEndHaptic()
                .overScrollVertical()
                .padding(top = paddingValues.calculateTopPadding())
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            libraries = libraries
        )
    }
}

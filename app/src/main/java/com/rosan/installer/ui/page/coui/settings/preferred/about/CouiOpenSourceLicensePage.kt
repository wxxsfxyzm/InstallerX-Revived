// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.preferred.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.Dialog
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Scaffold
import com.itos.coui.menu.rememberCouiWindowBackdrop
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.LargeTopAppBar
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.rosan.installer.R
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiBadge
import com.rosan.installer.ui.page.coui.widgets.CouiCard
import com.rosan.installer.ui.page.coui.widgets.rememberCouiDialogMaterial

@Composable
fun CouiOpenSourceLicensePage(useBlur: Boolean) {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val libraries by produceLibraries(R.raw.aboutlibraries)
    val listState = rememberLazyListState()
    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior {
        listState.canScrollForward || listState.canScrollBackward
    }
    val density = LocalDensity.current

    var selectedLibrary by remember { mutableStateOf<Library?>(null) }
    var showLicenseDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(id = R.string.open_source_license)) },
                navigationIcon = {
                    CouiBackButton(onClick = { navigator.pop() })
                },
                scrollBehavior = scrollBehavior,
                useCouiBlur = useBlur,
            )
        },
    ) { insets ->
        val contentPadding = remember(scrollBehavior.state, insets, density) {
            scrollBehavior.state.contentPadding(insets, density)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(insets),
            contentPadding = PaddingValues(
                start = CouiPageDefaults.horizontalMargin,
                end = CouiPageDefaults.horizontalMargin,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(libraries?.libraries ?: emptyList(), key = { it.uniqueId }) { library ->
                CouiLibraryCard(
                    library = library,
                    onClick = {
                        selectedLibrary = library
                        showLicenseDialog = true
                    },
                )
            }
        }
    }

    val backdrop = rememberCouiWindowBackdrop()
    val material = rememberCouiDialogMaterial()
    val website = selectedLibrary?.website?.takeIf { it.isNotBlank() }

    Dialog(
        visible = showLicenseDialog,
        onDismissRequest = { showLicenseDialog = false },
        title = selectedLibrary?.name,
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.close),
            onClick = { showLicenseDialog = false },
            style = CouiDialogActionStyle.Primary,
        ),
        dismissAction = website?.let { url ->
            CouiDialogAction(
                text = stringResource(R.string.visit_home_page),
                onClick = { uriHandler.openUri(url) },
            )
        },
        material = material,
        backdrop = backdrop,
    ) {
        selectedLibrary?.let { library ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CouiDialogDefaults.contentPadding)
                    .padding(bottom = CouiDialogDefaults.messageBottom),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                library.licenses.forEach { license ->
                    CouiCard(
                        color = CouiTheme.colorScheme.onSurface.copy(
                            alpha = if (CouiTheme.isDark) 0.08f else 0.04f,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = license.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CouiTheme.colorScheme.accentOnSurface,
                                modifier = Modifier.clickable {
                                    license.url?.let { uriHandler.openUri(it) }
                                },
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = license.licenseContent ?: stringResource(R.string.no_license_text),
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = CouiTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CouiLibraryCard(library: Library, onClick: () -> Unit) {
    val dark = CouiTheme.isDark
    val scheme = CouiTheme.colorScheme
    val source = remember { MutableInteractionSource() }

    CouiCard(
        modifier = Modifier
            .fillMaxWidth()
            .couiButtonFeedback(dark = dark, interactionSource = source)
            .clickable(
                interactionSource = source,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = library.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                library.artifactVersion?.takeIf { it.isNotBlank() }?.let { version ->
                    Text(
                        text = version,
                        fontSize = 12.sp,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            if (library.author.isNotBlank()) {
                Text(
                    text = library.author,
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (library.licenses.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    library.licenses.forEach { license ->
                        CouiBadge(text = license.name)
                    }
                }
            }
        }
    }
}

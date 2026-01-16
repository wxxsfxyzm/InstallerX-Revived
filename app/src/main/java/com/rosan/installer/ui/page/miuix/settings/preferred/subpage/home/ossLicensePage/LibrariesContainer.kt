package com.rosan.installer.ui.page.miuix.settings.preferred.subpage.home.ossLicensePage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.rosan.installer.R
import com.rosan.installer.ui.page.miuix.widgets.MiuixInstallerTipCard
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun LibrariesContainer(
    modifier: Modifier = Modifier,
    libraries: Libs?
) {
    val uriHandler = LocalUriHandler.current
    var selectedLibrary by remember { mutableStateOf<Library?>(null) }
    val showState = remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        overscrollEffect = null
    ) {
        item { Spacer(modifier = Modifier.size(16.dp)) }
        items(libraries?.libraries ?: listOf()) { library ->
            LibraryCard(
                library = library,
                onClick = {
                    selectedLibrary = library
                    showState.value = true
                }
            )
        }
        item { Spacer(Modifier.navigationBarsPadding()) }
    }

    val onDismiss = {
        showState.value = false
    }

    selectedLibrary?.let { library ->
        WindowDialog(
            show = showState,
            onDismissRequest = onDismiss,
            title = library.name,
            content = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .scrollEndHaptic()
                            .overScrollVertical(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        overscrollEffect = null
                    ) {
                        item {
                            MiuixInstallerTipCard(
                                text = stringResource(
                                    R.string.license,
                                    library.licenses.joinToString(", ") { it.name }
                                )
                            )
                        }

                        items(library.licenses.toList()) { license ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.defaultColors(
                                    color = MiuixTheme.colorScheme.surfaceContainerHighest
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = license.name,
                                        style = MiuixTheme.textStyles.title4,
                                        color = MiuixTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                license.url?.let { uriHandler.openUri(it) }
                                            }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = license.licenseContent
                                            ?: stringResource(R.string.no_license_text),
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.onSurfaceContainer
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!library.website.isNullOrBlank()) {
                            TextButton(
                                modifier = Modifier.weight(1f),
                                onClick = { uriHandler.openUri(library.website!!) },
                                text = stringResource(R.string.visit_home_page)
                            )
                        }

                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = onDismiss,
                            text = stringResource(R.string.close),
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun LibraryCard(
    library: Library,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = library.name,
                    style = MiuixTheme.textStyles.title2,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 5.dp)
                )
                library.artifactVersion?.let {
                    Text(
                        text = it,
                        style = MiuixTheme.textStyles.headline1,
                        color = MiuixTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = library.author,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            library.licenses.forEach { license ->
                Card(
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        contentColor = MiuixTheme.colorScheme.onSurface
                    )
                ) {
                    Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(
                            text = license.name,
                            color = MiuixTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
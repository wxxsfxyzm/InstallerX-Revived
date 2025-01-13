package com.rosan.installer.ui.page.installer.dialog.inner

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.dialog.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun installChoiceDialog(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    val entities = installer.entities.toMutableStateList()
    return DialogParams(icon = DialogInnerParams(
        DialogParamsType.IconWorking.id, workingIcon
    ), title = DialogInnerParams(
        DialogParamsType.InstallChoice.id,
    ) {
        Text(stringResource(R.string.installer_select_install))
    }, content = DialogInnerParams(DialogParamsType.InstallChoice.id) {
        LazyColumn {
            itemsIndexed(entities) { index, item ->
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        entities[index] = item.copy(selected = !item.selected)
                    }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Checkbox(modifier = Modifier.align(Alignment.CenterVertically),
                        checked = item.selected,
                        onCheckedChange = {
                            entities[index] = item.copy(selected = it)
                        })
                    Column(
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        val app = item.app
                        when (app) {
                            is AppEntity.BaseEntity -> {
                                Text(
                                    app.label ?: app.packageName,
                                    modifier = Modifier.basicMarquee(),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    stringResource(
                                        R.string.installer_version,
                                        app.versionName,
                                        app.versionCode
                                    ),
                                    modifier = Modifier.basicMarquee(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    stringResource(
                                        R.string.installer_package_name,
                                        app.packageName,
                                    ),
                                    modifier = Modifier.basicMarquee(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    stringResource(
                                        R.string.installer_file_path,
                                        app.data.getSourceTop().toString()
                                    ),
                                    modifier = Modifier.basicMarquee(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            is AppEntity.SplitEntity -> {
                                Text(
                                    app.splitName,
                                    modifier = Modifier.basicMarquee(),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    stringResource(
                                        R.string.installer_package_name,
                                        app.packageName,
                                    ),
                                    modifier = Modifier.basicMarquee(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    stringResource(
                                        R.string.installer_file_path,
                                        app.data.getSourceTop().toString()
                                    ),
                                    modifier = Modifier.basicMarquee(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            is AppEntity.DexMetadataEntity -> {
                                Text(
                                    app.dmName,
                                    modifier = Modifier.basicMarquee(),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    stringResource(
                                        R.string.installer_package_name,
                                        app.packageName,
                                    ),
                                    modifier = Modifier.basicMarquee(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    stringResource(
                                        R.string.installer_file_path,
                                        app.data.getSourceTop().toString()
                                    ),
                                    modifier = Modifier.basicMarquee(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }, buttons = DialogButtons(
        DialogParamsType.InstallChoice.id
    ) {
        listOf(DialogButton(stringResource(R.string.next)) {
            installer.entities = entities
            viewModel.dispatch(DialogViewAction.InstallPrepare)
        }, DialogButton(stringResource(R.string.cancel)) {
            viewModel.dispatch(DialogViewAction.Close)
        })
    })
}
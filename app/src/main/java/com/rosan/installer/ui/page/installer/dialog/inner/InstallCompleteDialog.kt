package com.rosan.installer.ui.page.installer.dialog.inner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.DoneAll
import androidx.compose.material.icons.twotone.SmsFailed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.installer.model.entity.InstallResult
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.installer.dialog.DialogParams
import com.rosan.installer.ui.page.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.installer.dialog.DialogViewAction
import com.rosan.installer.ui.page.installer.dialog.DialogViewModel

@Composable
fun installCompletedDialog(
    installer: InstallerRepo,
    viewModel: DialogViewModel,
    results: List<InstallResult>
): DialogParams {
    val successCount = results.count { it.success }
    val failureCount = results.size - successCount

    return DialogParams(
        icon = DialogInnerParams(
            DialogParamsType.InstallerInstallCompleted.id,
            {
                Icon(
                    imageVector = if (failureCount == 0) Icons.TwoTone.DoneAll/*AppIcons.Success*/ else Icons.TwoTone.SmsFailed/*AppIcons.Error*/,
                    contentDescription = null
                )
            }
        ),
        title = DialogInnerParams(
            DialogParamsType.InstallerInstallCompleted.id
        ) {
            Text(stringResource(R.string.installer_install_success))
        },
        subtitle = DialogInnerParams(
            DialogParamsType.InstallerInstallCompleted.id
        ) {
            Text(stringResource(R.string.installer_completed_subtitle, successCount, failureCount))
        },
        content = DialogInnerParams(DialogParamsType.InstallerInstallCompleted.id) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                item { Spacer(modifier = Modifier.size(1.dp)) }
                items(results, key = { result ->
                    val app = result.entity.app as AppEntity.BaseEntity
                    app.packageName + app.name
                }) { result ->
                    ResultItemCard(result)
                }
                item { Spacer(modifier = Modifier.size(1.dp)) }
            }
        },
        buttons = DialogButtons(DialogParamsType.InstallerInstallCompleted.id) {
            listOf(
                DialogButton(stringResource(R.string.finish)) {
                    viewModel.dispatch(DialogViewAction.Close)
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultItemCard(
    result: InstallResult
) {
    val app = result.entity.app
    val appLabel = (app as? AppEntity.BaseEntity)?.label ?: app.packageName

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // App Info
            Text(appLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(app.packageName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.7f))
            Spacer(modifier = Modifier.height(12.dp))

            // Result Info (nested card)
            if (result.success) {
                SuccessCard()
            } else if (result.error != null) {
                // 为了复用 errorTextBlock，我们需要在安装失败时将错误信息临时存到 repo 中
                // ViewModel 的 handleMultiInstallProgress 已经这样做了
                FailureCard(result.error)
            }
        }
    }
}

@Composable
private fun SuccessCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.TwoTone.DoneAll,
                contentDescription = "Success",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(R.string.installer_install_success),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun FailureCard(error: Throwable) {
    // 这里直接调用通用的 errorTextBlock
    ErrorTextBlock(error)
}
package com.rosan.installer.ui.page.main.installer.dialog.inner

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.recycle.util.openAppPrivileged
import com.rosan.installer.data.recycle.util.openLSPosedPrivileged
import com.rosan.installer.data.settings.model.room.entity.ext.isPrivileged
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun installSuccessDialog(
    installer: InstallerRepo,
    viewModel: InstallerViewModel
): DialogParams {
    val context = LocalContext.current
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val settings = viewModel.viewSettings

    val packageName = currentPackageName ?: installer.analysisResults.firstOrNull()?.packageName ?: ""
    val currentPackage = installer.analysisResults.find { it.packageName == packageName }

    val selectedEntities = currentPackage?.appEntities
        ?.filter { it.selected }
        ?.map { it.app }
    val effectivePrimaryEntity = selectedEntities?.filterIsInstance<AppEntity.BaseEntity>()?.firstOrNull()
        ?: selectedEntities?.filterIsInstance<AppEntity.ModuleEntity>()?.firstOrNull()

    val isXposedModule = if (effectivePrimaryEntity is AppEntity.BaseEntity) effectivePrimaryEntity.isXposedModule else false

    val baseParams = installInfoDialog(
        installer = installer,
        viewModel = viewModel,
        onTitleExtraClick = {
            if (packageName.isNotEmpty()) {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            viewModel.dispatch(InstallerViewAction.Background)
        }
    )

    return baseParams.copy(
        buttons = dialogButtons(
            DialogParamsType.InstallerInstallSuccess.id
        ) {
            val launchIntent = remember(packageName) {
                if (packageName.isNotEmpty()) {
                    context.packageManager.getLaunchIntentForPackage(packageName)
                } else null
            }

            buildList {
                if (isXposedModule && installer.config.isPrivileged) {
                    add(DialogButton(stringResource(R.string.open_lsposed)) {
                        coroutineScope.launch(Dispatchers.IO) {
                            openLSPosedPrivileged(
                                config = installer.config,
                                onSuccess = { viewModel.dispatch(InstallerViewAction.Close) }
                            )
                        }
                    })
                }

                if (launchIntent != null) {
                    add(DialogButton(stringResource(R.string.open)) {
                        coroutineScope.launch(Dispatchers.IO) {
                            openAppPrivileged(
                                context = context,
                                config = installer.config,
                                packageName = packageName,
                                dhizukuAutoCloseSeconds = settings.autoCloseCountDown,
                                onSuccess = { viewModel.dispatch(InstallerViewAction.Close) }
                            )
                        }
                    })
                }

                add(DialogButton(stringResource(R.string.finish)) {
                    viewModel.dispatch(InstallerViewAction.Close)
                })
            }
        }
    )
}

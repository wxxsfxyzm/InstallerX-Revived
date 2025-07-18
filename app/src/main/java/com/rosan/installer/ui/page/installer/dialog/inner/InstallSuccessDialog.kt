package com.rosan.installer.ui.page.installer.dialog.inner

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.data.common.util.addAll
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.recycle.util.useUserService
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.page.installer.dialog.DialogParams
import com.rosan.installer.ui.page.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.installer.dialog.DialogViewAction
import com.rosan.installer.ui.page.installer.dialog.DialogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

@Composable
fun installSuccessDialog( // 小写开头
    installer: InstallerRepo,
    viewModel: DialogViewModel
): DialogParams {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Collect preInstallAppInfo (represents the state *before* the successful install)
    val currentPreInstallInfo by viewModel.preInstallAppInfo.collectAsState()
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val packageName = currentPackageName ?: installer.entities.filter { it.selected }.map { it.app }
        .firstOrNull()?.packageName ?: ""

    // Call InstallInfoDialog, passing the collected preInstallAppInfo.
    // InstallInfoDialog will now handle the logic of displaying one or two versions.
    val baseParams = installInfoDialog(
        installer = installer,
        viewModel = viewModel,
        preInstallAppInfo = currentPreInstallInfo, // Pass the potentially null old info
        onTitleExtraClick = {
            if (packageName.isNotEmpty()) {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            viewModel.dispatch(DialogViewAction.Background)
        }
    )

    // Only override the buttons
    return baseParams.copy(
        // Subtitle is now correctly generated by InstallInfoDialog
        buttons = DialogButtons(
            DialogParamsType.InstallerInstallSuccess.id
        ) {
            val list = mutableListOf<DialogButton>()
            val intent =
                if (packageName.isNotEmpty()) context.packageManager.getLaunchIntentForPackage(
                    packageName
                ) else null
            if (intent != null) {
                list.add(DialogButton(stringResource(R.string.open)) {
                    coroutineScope.launch(Dispatchers.IO) {
                        // --- 第一步：尝试高权限强制启动 (主方法) ---
                        Timber.tag("HybridStart").i("Attempting force start for $packageName...")

                        val forceStartSuccess =
                            if (installer.config.authorizer == ConfigEntity.Authorizer.Dhizuku)
                                false
                            else
                                forceStartApp(packageName, 0, installer.config)

                        if (forceStartSuccess) {
                            // 主方法成功，任务完成
                            Timber.tag("HybridStart")
                                .i("Force start succeeded for $packageName. Closing dialog.")
                            viewModel.dispatch(DialogViewAction.Close)
                        } else {
                            // --- 第二步：主方法失败，回退到您完整的原始逻辑 (备用方案) ---
                            Timber.tag("HybridStart")
                                .w("Force start failed. Falling back to original standard method.")

                            // 切换到主线程以执行UI操作 (startActivity)，这是安卓框架的要求
                            withContext(Dispatchers.Main) {
                                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                coroutineScope.launch {
                                    if (isAppInForeground(
                                            packageName,
                                            installer.config,
                                            viewModel
                                        )
                                    ) {
                                        Timber.tag("InstallSuccessDialog").d(
                                            "App $packageName is in foreground, closing dialog."
                                        )
                                        viewModel.dispatch(DialogViewAction.Close)
                                    } else {
                                        if (installer.config.authorizer == ConfigEntity.Authorizer.Dhizuku) {
                                            Timber.tag("InstallSuccessDialog").d(
                                                "App $packageName not detected in foreground after ${viewModel.autoCloseCountDown} seconds. Dialog will close itself."
                                            )
                                        } else {
                                            Timber.tag("InstallSuccessDialog").w(
                                                "App $packageName not detected in foreground after 10 seconds. Dialog will close itself."
                                            )
                                        }
                                        // ALWAYS close the dialog afterwards, regardless of whether the app
                                        // was detected in the foreground or the check timed out.
                                        viewModel.dispatch(DialogViewAction.Close)
                                    }
                                }
                            }
                        }
                    }
                })
            }
            list.addAll(
                DialogButton(stringResource(R.string.previous), 2f) {
                    viewModel.dispatch(DialogViewAction.InstallPrepare)
                },
                DialogButton(stringResource(R.string.finish), 1f) {
                    viewModel.dispatch(DialogViewAction.Close)
                }
            )
            return@DialogButtons list
        }
    )
}

/**
 * 使用高权限Shell轮询检查指定包名的应用是否位于前台。
 *
 * @param targetPackageName 要检查的应用包名。
 * @param config 用于执行高权限Shell命令的配置实体。
 * @return `true` 如果应用成功切换到前台, `false` 如果超时。
 */
private suspend fun isAppInForeground(
    targetPackageName: String,
    config: ConfigEntity,
    viewModel: DialogViewModel
): Boolean {
    if (config.authorizer == ConfigEntity.Authorizer.Dhizuku) {
        Timber.tag("isAppInForeground")
            .d("Dhizuku does not support shell commands, waiting for auto-close countdown.")
        delay(viewModel.autoCloseCountDown * 1000L) // Wait for the auto-close countdown
        Timber.tag("isAppInForeground")
            .d("Auto-close countdown finished, returning false.")
        return false // Dhizuku does not support shell commands
    }
    // Use withTimeoutOrNull to limit the execution time to 10 seconds
    val result = withTimeoutOrNull(10000L) {
        while (true) {
            // Execute the function in IO thread
            val topApp = withContext(Dispatchers.IO) {
                // Call the function to get the top app package name
                getTopApp(config)
            }

            Timber.tag("isAppInForeground").d("Checking foreground app: $topApp")

            if (topApp == targetPackageName) {
                Timber.tag("isAppInForeground")
                    .d("Target App $targetPackageName is in foreground.")
                return@withTimeoutOrNull true
            }

            delay(1000L) // Perform a check every 1 second
        }
    }
    return result == true // Return true if the app was found in foreground, false if timed out
}

/**
 * 获取当前前台应用的包名。
 *
 * @param config 用于执行高权限Shell命令的配置实体。
 * @return 当前前台应用的包名，如果无法获取则返回空字符串。
 */
// TODO Dhizuku在SDK36上调用命令会直接内部错误，不能使用
private fun getTopApp(
    config: ConfigEntity
): String {
    var topAppPackage = ""

    // Use the userService to execute privileged commands
    useUserService(config) { userService ->
        try {
            /**
             * Exec `dumpsys` command to get the current focused window.
             * This command is unstable and may change in future Android versions.
             * Tested by Shizuku:
             *  Tested on OneUI 7.0 (Android 15)
             *  Tested on HyperOS 2.0.200 (Android 15)
             */
            val command = "dumpsys window | grep mCurrentFocus"
            // in order to use shell environment, we need to use execArr
            // otherwise, pipeline symbols like '|' won't work
            val cmdArray = arrayOf("/system/bin/sh", "-c", command)
            // Call the interface method via userService.privileged
            val result = userService.privileged.execArr(cmdArray)
            Timber.tag("getTopApp")
                .d("Result of executing '${cmdArray.contentToString()}': $result")

            topAppPackage = if (result.isBlank()) {
                // Result is empty, return empty string
                ""
            } else {
                // Analyze the result to extract the package name
                val componentPart = result.split(' ').lastOrNull { it.contains('/') }
                componentPart?.substringBefore('/') ?: ""
            }
        } catch (_: UnsupportedOperationException) {
            Timber.tag("getTopApp")
                .d("Authorizer does not support shell access, returning empty string")
        } catch (e: Exception) {
            Timber.tag("getTopApp").e(e, "Exception while getting top app package")
            // return empty string if any exception occurs
            topAppPackage = ""
        }
    }
    // Return the package name of the top app extracted in the lambda
    Timber.tag("getTopApp").d("Acquired Top App Package Name: $topAppPackage")
    return topAppPackage
}

/**
 * 使用高权限强制启动一个应用程序。
 *
 * @param packageName 要启动的应用的包名。
 * @param userId 用户ID，默认为0（主用户）。
 * @param config 用于执行高权限Shell命令的配置实体。
 * @return 如果启动命令成功执行，则返回 true，否则返回 false。
 */
fun forceStartApp(
    packageName: String,
    userId: Int = 0,
    config: ConfigEntity
): Boolean {
    // 1. 解析启动 Activity
    val launchActivityComponent = getLaunchActivity(packageName, config)

    if (launchActivityComponent == null) {
        Timber.tag("forceStartApp").e("Failed to resolve launch activity for $packageName")
        return false
    }

    Timber.tag("forceStartApp")
        .d("Resolved launch activity for $packageName: $launchActivityComponent")

    // 2. 构造并执行高权限 am start 命令
    var success = false
    useUserService(config) { userService ->
        try {
            /**
             * 构造一个高权限、高优先级的 am start 命令
             * -n component: 直接指定启动的组件，绕过 Intent 解析，这是最关键的一步。
             * --user 0: 在作为 shell/root 用户执行时，明确指定为设备主用户（user 0）启动。
             * 在多用户环境下至关重要，能避免很多启动失败的问题。
             * --activity-brought-to-front: 一个标志，确保 Activity 被带到前台。
             */
            val command =
                "am start -n $launchActivityComponent --user $userId --activity-brought-to-front"
            val cmdArray = arrayOf("/system/bin/sh", "-c", command)

            // 执行命令。对于 am start，我们通常不关心其输出，只关心是否抛出异常。
            userService.privileged.execArr(cmdArray)

            // 如果没有抛出异常，我们乐观地认为命令已成功发送。
            success = true
            Timber.tag("forceStartApp")
                .d("Successfully executed force start command for $launchActivityComponent")

        } catch (e: Exception) {
            Timber.tag("forceStartApp").e(e, "Exception while force starting app $packageName")
            success = false
        }
    }

    // 注意：这里返回的 true 仅表示 am 命令成功执行，不代表应用UI一定立即渲染完成。
    // 但相比普通 Intent，它的成功率和直接性已经高出很多。
    return success
}

/**
 * 使用高权限 Shell 命令解析应用的启动 Activity。
 *
 * @param packageName 目标应用的包名。
 * @param config 用于执行高权限Shell命令的配置实体。
 * @return 启动 Activity 的完整组件名 (e.g., "com.example.app/.MainActivity")，如果找不到则返回 null。
 */
private fun getLaunchActivity(
    packageName: String,
    config: ConfigEntity
): String? {
    var launchActivity: String? = null
    useUserService(config) { userService ->
        try {
            // 命令：解析指定包名的 LAUNCHER Activity，并只保留最后一行有效输出
            val command =
                "cmd package resolve-activity --brief -c android.intent.category.LAUNCHER $packageName | tail -n 1"
            val cmdArray = arrayOf("/system/bin/sh", "-c", command)

            val result = userService.privileged.execArr(cmdArray).trim()
            Timber.tag("getLaunchActivity")
                .d("Result of resolving '$packageName': $result")

            // 如果结果不为空且包含 '/'，则认为是有效的组件名
            if (result.isNotBlank() && result.contains('/')) {
                launchActivity = result
            }
        } catch (e: Exception) {
            Timber.tag("getLaunchActivity")
                .e(e, "Exception while resolving launch activity for $packageName")
            launchActivity = null
        }
    }
    return launchActivity
}
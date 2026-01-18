package com.rosan.installer.data.installer.model.impl.installer.processor

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.entity.UnarchiveInfo
import com.rosan.installer.data.installer.repo.InstallerRepo
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber

class UnarchiveProcessor(
    private val context: Context,
    private val installer: InstallerRepo,
    private val progressFlow: MutableSharedFlow<ProgressEntity>
) {
    private val installerId get() = installer.id

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    suspend fun resolve(intent: Intent) {
        Timber.d("[id=$installerId] resolveUnarchive: Starting.")
        progressFlow.emit(ProgressEntity.UnarchiveResolving)

        val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
        // 关键：IntentSender 的 Key 必须匹配 Android 系统发送的 Key
        // 系统使用的是 "android.content.pm.extra.UNARCHIVE_INTENT_SENDER"
        val intentSender = intent.getParcelableExtra<IntentSender>("android.content.pm.extra.UNARCHIVE_INTENT_SENDER")

        if (packageName == null || intentSender == null) {
            Timber.e("[id=$installerId] resolveUnarchive: Missing params. Pkg: $packageName, Sender: $intentSender")
            // 应该发射一个明确的错误状态
            progressFlow.emit(ProgressEntity.UnarchiveFailed)
            return
        }

        try {
            val pm = context.packageManager
            // 33+ (Tiramisu) 引入了 MATCH_ARCHIVED_PACKAGES
            val flags = PackageManager.MATCH_ARCHIVED_PACKAGES.toLong()
            val appInfo = pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags))
            val appLabel = appInfo.loadLabel(pm).toString()

            // 获取安装来源信息用于展示（可选）
            val installSourceInfo = pm.getInstallSourceInfo(packageName)
            val installerPkg = installSourceInfo.updateOwnerPackageName ?: installSourceInfo.installingPackageName

            val info = UnarchiveInfo(packageName, appLabel, installerPkg ?: "Unknown", intentSender)

            Timber.d("[id=$installerId] resolveUnarchive: Ready.")
            progressFlow.emit(ProgressEntity.UnarchiveReady(info))

        } catch (e: Exception) {
            Timber.e(e, "[id=$installerId] resolveUnarchive: Failed.")
            progressFlow.emit(ProgressEntity.UnarchiveFailed)
        }
    }

    suspend fun perform(info: UnarchiveInfo) {
        Timber.d("[id=$installerId] performUnarchive: Executing.")
        progressFlow.emit(ProgressEntity.Unarchiving)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                // 核心：调用系统 API
                context.packageManager.packageInstaller.requestUnarchive(
                    info.packageName,
                    info.intentSender
                )
                Timber.d("[id=$installerId] performUnarchive: Success.")
                progressFlow.emit(ProgressEntity.UnarchiveSuccess)
                // 成功后关闭页面，剩下的交给系统/下载服务
                progressFlow.emit(ProgressEntity.Finish)
            } else {
                // 理论上不可能走到这里，因为只有 Android 15 会发这个 intent
                throw UnsupportedOperationException("Android 15+ required")
            }
        } catch (e: Exception) {
            Timber.e(e, "[id=$installerId] performUnarchive: Failed.")
            installer.error = e
            progressFlow.emit(ProgressEntity.UnarchiveFailed)
        }
    }
}
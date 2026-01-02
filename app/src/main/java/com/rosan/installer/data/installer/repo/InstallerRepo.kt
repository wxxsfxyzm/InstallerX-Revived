package com.rosan.installer.data.installer.repo

import android.app.Activity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.entity.PackageAnalysisResult
import com.rosan.installer.data.installer.model.entity.ConfirmationDetails
import com.rosan.installer.data.installer.model.entity.InstallResult
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.installer.model.entity.UninstallInfo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

interface InstallerRepo : Closeable {
    val id: String
    var error: Throwable
    var config: ConfigEntity
    var data: List<DataEntity>
    var analysisResults: List<PackageAnalysisResult>
    val progress: Flow<ProgressEntity>
    val background: Flow<Boolean>
    var multiInstallQueue: List<SelectInstallEntity>
    var multiInstallResults: MutableList<InstallResult>
    var currentMultiInstallIndex: Int
    var moduleLog: List<String>
    val uninstallInfo: StateFlow<UninstallInfo?>
    val confirmationDetails: StateFlow<ConfirmationDetails?>

    /**
     * Resolves information for a package to be installed.
     * @param activity The activity to use for resolving.
     */
    fun resolveInstall(activity: Activity)
    fun analyse()

    /**
     * Request Do Package/Module Install
     * @param triggerAuth request or not request user biometric auth
     */
    fun install(triggerAuth: Boolean)
    fun installMultiple(entities: List<SelectInstallEntity>)

    /**
     * Resolves information for a package to be uninstalled.
     * @param packageName The package name to uninstall.
     */
    fun resolveUninstall(activity: Activity, packageName: String)
    fun uninstall(packageName: String)

    fun resolveConfirmInstall(activity: Activity, sessionId: Int)
    fun approveConfirmation(sessionId: Int, granted: Boolean)

    fun reboot(reason: String)

    fun background(value: Boolean)
    fun cancel()
    override fun close()
}
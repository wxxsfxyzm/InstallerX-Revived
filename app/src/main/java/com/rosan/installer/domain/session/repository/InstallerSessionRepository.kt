package com.rosan.installer.domain.session.repository

import android.app.Activity
import com.rosan.installer.domain.engine.model.DataEntity
import com.rosan.installer.domain.engine.model.PackageAnalysisResult
import com.rosan.installer.domain.session.model.ConfirmationDetails
import com.rosan.installer.domain.session.model.InstallResult
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.model.SelectInstallEntity
import com.rosan.installer.domain.session.model.UninstallInfo
import com.rosan.installer.domain.settings.model.ConfigModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

interface InstallerSessionRepository : Closeable {
    val id: String
    var error: Throwable
    var config: ConfigModel
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
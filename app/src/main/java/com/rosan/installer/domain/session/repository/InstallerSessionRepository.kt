// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.session.repository

import android.app.Activity
import android.content.IntentSender
import com.rosan.installer.domain.engine.model.packageinfo.PackageAnalysisResult
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.session.model.ConfirmationDetails
import com.rosan.installer.domain.session.model.ConfirmationRequestType
import com.rosan.installer.domain.session.model.InstallResult
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.model.SelectInstallEntity
import com.rosan.installer.domain.session.model.UnarchiveErrorInfo
import com.rosan.installer.domain.session.model.UnarchiveInfo
import com.rosan.installer.domain.session.model.UninstallInfo
import com.rosan.installer.domain.settings.model.config.ConfigModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

interface InstallerSessionRepository : Closeable {
    val id: String
    var error: Throwable
    var config: ConfigModel
    var data: List<DataEntity>
    var sourceUris: List<String>
    var referrerUri: String?
    var analysisResults: List<PackageAnalysisResult>
    val progress: Flow<ProgressEntity>
    val toastEvents: Flow<String>
    val background: Flow<Boolean>
    val closeRequested: StateFlow<Boolean>
    var multiInstallQueue: List<SelectInstallEntity>
    var multiInstallResults: MutableList<InstallResult>
    var currentMultiInstallIndex: Int
    var moduleLog: List<String>
    val uninstallInfo: StateFlow<UninstallInfo?>
    val confirmationDetails: StateFlow<ConfirmationDetails?>
    val unarchiveInfo: StateFlow<UnarchiveInfo?>
    val unarchiveErrorInfo: StateFlow<UnarchiveErrorInfo?>

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
    fun installMultiple(entities: List<SelectInstallEntity>, triggerAuth: Boolean = true)

    /**
     * Resolves information for a package to be uninstalled.
     * @param packageName The package name to uninstall.
     */
    fun resolveUninstall(activity: Activity, packageName: String)
    fun uninstall(packageName: String)

    fun resolveConfirmInstall(
        activity: Activity,
        sessionId: Int,
        requestType: ConfirmationRequestType = ConfirmationRequestType.INSTALL
    )

    fun approveConfirmation(sessionId: Int, granted: Boolean)

    fun resolveUnarchive(activity: Activity, packageName: String, intentSender: IntentSender)
    fun startUnarchive()
    fun resolveUnarchiveError(activity: Activity, info: UnarchiveErrorInfo)
    fun openUnarchiveErrorAction()

    fun reboot(reason: String)

    fun background(value: Boolean)
    fun prepareClose()
    fun cancel()
    override fun close()
}

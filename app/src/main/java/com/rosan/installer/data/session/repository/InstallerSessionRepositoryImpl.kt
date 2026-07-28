// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.session.repository

import android.app.Activity
import android.content.IntentSender
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.packageinfo.PackageAnalysisResult
import com.rosan.installer.domain.session.model.ConfirmationDetails
import com.rosan.installer.domain.session.model.ConfirmationRequest
import com.rosan.installer.domain.session.model.ConfirmationState
import com.rosan.installer.domain.session.model.ConfirmationRequestType
import com.rosan.installer.domain.session.model.InstallResult
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.model.SelectInstallEntity
import com.rosan.installer.domain.session.model.UninstallInfo
import com.rosan.installer.domain.session.model.UnarchiveErrorInfo
import com.rosan.installer.domain.session.model.UnarchiveInfo
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.config.ConfigModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class InstallerSessionRepositoryImpl(
    override val id: String,
    private val onClose: () -> Unit
) : InstallerSessionRepository {

    private val isClosed = AtomicBoolean(false)

    // Properties implementation
    override var error: Throwable = Throwable()
    override var config: ConfigModel = ConfigModel.default
    override var data: List<DataEntity> by mutableStateOf(emptyList())
    override var sourceUris: List<String> by mutableStateOf(emptyList())
    override var referrerUri: String? by mutableStateOf(null)
    override var analysisResults: List<PackageAnalysisResult> by mutableStateOf(emptyList())
    override val progress: MutableSharedFlow<ProgressEntity> = MutableStateFlow(ProgressEntity.Ready)
    override val toastEvents: MutableSharedFlow<String> = MutableSharedFlow(extraBufferCapacity = 16)

    // Actions are single-consumer commands. State that must survive collector restarts lives in
    // StateFlow properties below instead of being replayed as commands.
    private val actionChannel = Channel<Action>(Channel.BUFFERED)
    val action: Flow<Action> = actionChannel.receiveAsFlow()

    override val background: MutableSharedFlow<Boolean> = MutableStateFlow(false)
    override val closeRequested: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override var multiInstallQueue: List<SelectInstallEntity> = emptyList()
    override var multiInstallResults: MutableList<InstallResult> = mutableListOf()
    override var currentMultiInstallIndex: Int = 0
    override var moduleLog: List<String> = emptyList()
    override val uninstallInfo: MutableStateFlow<UninstallInfo?> = MutableStateFlow(null)
    override val confirmationDetails: MutableStateFlow<ConfirmationDetails?> = MutableStateFlow(null)
    override val confirmationState: MutableStateFlow<ConfirmationState> =
        MutableStateFlow(ConfirmationState.Idle)
    override val activePlatformSessionIds: MutableStateFlow<Set<Int>> =
        MutableStateFlow(emptySet())
    override val unarchiveInfo: MutableStateFlow<UnarchiveInfo?> = MutableStateFlow(null)
    override val unarchiveErrorInfo: MutableStateFlow<UnarchiveErrorInfo?> = MutableStateFlow(null)

    override fun resolveInstall(activity: Activity) {
        Timber.d("[id=$id] resolve() called. Emitting Action.Resolve.")
        sendAction(Action.ResolveInstall(activity))
    }

    override fun analyse() {
        Timber.d("[id=$id] analyse() called. Emitting Action.Analyse.")
        sendAction(Action.Analyse)
    }

    override fun install(triggerAuth: Boolean) {
        Timber.d("[id=$id] install() called. Emitting Action.Install.")
        sendAction(Action.Install(triggerAuth))
    }

    override fun installMultiple(entities: List<SelectInstallEntity>, triggerAuth: Boolean) {
        Timber.d("[id=$id] installMultiple() called. Queue size: ${entities.size}")
        multiInstallQueue = entities
        multiInstallResults.clear()
        currentMultiInstallIndex = 0

        sendAction(Action.InstallMultiple(triggerAuth))
    }

    override fun resolveUninstall(activity: Activity, packageName: String) {
        Timber.d("[id=$id] resolveUninstall() called for $packageName. Emitting Action.ResolveUninstall.")
        sendAction(Action.ResolveUninstall(activity, packageName))
    }

    override fun uninstall(packageName: String) {
        // Store the info for handlers like ForegroundInfoHandler to access
        this.uninstallInfo.value = UninstallInfo(packageName)
        Timber.d("[id=$id] uninstall() called for $packageName. Emitting Action.Uninstall.")
        // Emit the action for the ActionHandler to process
        sendAction(Action.Uninstall(packageName))
    }

    override fun resolveConfirmInstall(
        activity: Activity,
        sessionId: Int,
        requestType: ConfirmationRequestType,
        callerUid: Int
    ) {
        Timber.d("[id=$id] resolveConfirmInstall() called for session $sessionId, type=$requestType. Emitting Action.ResolveConfirmInstall.")
        sendAction(
            Action.ResolveConfirmInstall(
                activity = activity,
                request = ConfirmationRequest(sessionId, requestType, callerUid)
            )
        )
    }

    override fun approveConfirmation(sessionId: Int, granted: Boolean) {
        Timber.d("[id=$id] approveConfirmation() called for session $sessionId, granted: $granted.")
        sendAction(Action.ApproveSession(sessionId, granted))
    }

    override fun resolveUnarchive(activity: Activity, packageName: String, intentSender: IntentSender) {
        Timber.d("[id=$id] resolveUnarchive() called for $packageName. Emitting Action.ResolveUnarchive.")
        sendAction(Action.ResolveUnarchive(activity, packageName, intentSender))
    }

    override fun startUnarchive() {
        Timber.d("[id=$id] startUnarchive() called. Emitting Action.StartUnarchive.")
        sendAction(Action.StartUnarchive)
    }

    override fun resolveUnarchiveError(activity: Activity, info: UnarchiveErrorInfo) {
        Timber.d("[id=$id] resolveUnarchiveError() called with status ${info.status}. Emitting Action.ResolveUnarchiveError.")
        sendAction(Action.ResolveUnarchiveError(activity, info))
    }

    override fun openUnarchiveErrorAction() {
        Timber.d("[id=$id] openUnarchiveErrorAction() called. Emitting Action.OpenUnarchiveErrorAction.")
        sendAction(Action.OpenUnarchiveErrorAction)
    }

    override fun reboot(reason: String) {
        Timber.d("[id=$id] reboot() called. Emitting Action.Reboot.")
        sendAction(Action.Reboot(reason))
    }

    override fun background(value: Boolean) {
        Timber.d("[id=$id] background() called with value: $value.")
        background.tryEmit(value)
    }

    override fun prepareClose() {
        Timber.d("[id=$id] prepareClose() called.")
        closeRequested.value = true
    }

    override fun cancel() {
        Timber.d("[id=$id] cancel() called. Emitting Action.Cancel.")
        sendAction(Action.Cancel)
    }

    override fun close() {
        // Ensure close is only executed once
        if (isClosed.compareAndSet(false, true)) {
            Timber.d("[id=$id] close() called. Emitting Action.Finish and triggering cleanup.")
            closeRequested.value = true

            // 1. Notify UI and Service that we are done
            sendAction(Action.Finish)

            // 2. Trigger the callback to remove from SessionManager
            onClose()
        } else {
            Timber.w("[id=$id] close() called on an already closed instance.")
        }
    }

    fun setPlatformSessionActive(sessionId: Int, active: Boolean) {
        activePlatformSessionIds.update { current ->
            if (active) current + sessionId else current - sessionId
        }
        Timber.d("[id=$id] Platform session $sessionId active=$active")
    }

    private fun sendAction(action: Action) {
        val result = actionChannel.trySend(action)
        if (result.isFailure) {
            Timber.w(
                result.exceptionOrNull(),
                "[id=$id] Failed to enqueue action ${action::class.simpleName}"
            )
        }
    }

    sealed interface Action {
        data class ResolveInstall(val activity: Activity) : Action
        data object Analyse : Action

        /**
         * Install single module/apk
         *
         * **This usually call from viewModel**
         *
         * @param triggerAuth request or not request user biometric auth
         * @see com.rosan.installer.ui.page.main.installer.InstallerViewAction.Install
         * @see com.rosan.installer.data.session.handler.ActionHandler.handleSingleInstall
         */
        data class Install(val triggerAuth: Boolean) : Action

        /**
         * Install multiple module/apk
         *
         * **This usually call from viewModel**
         * @see com.rosan.installer.ui.page.main.installer.InstallerViewAction.InstallMultiple
         * @see com.rosan.installer.data.session.handler.ActionHandler.handleMultiInstall
         */
        data class InstallMultiple(val triggerAuth: Boolean) : Action
        data class ResolveUninstall(val activity: Activity, val packageName: String) : Action
        data class Uninstall(val packageName: String) : Action
        data class ResolveConfirmInstall(
            val activity: Activity,
            val request: ConfirmationRequest
        ) : Action
        data class ApproveSession(val sessionId: Int, val granted: Boolean) : Action
        data class ResolveUnarchive(
            val activity: Activity,
            val packageName: String,
            val intentSender: IntentSender
        ) : Action
        data object StartUnarchive : Action
        data class ResolveUnarchiveError(
            val activity: Activity,
            val info: UnarchiveErrorInfo
        ) : Action
        data object OpenUnarchiveErrorAction : Action

        /**
         * Action to trigger device reboot after cleanup.
         */
        data class Reboot(val reason: String) : Action
        data object Cancel : Action
        data object Finish : Action
    }
}

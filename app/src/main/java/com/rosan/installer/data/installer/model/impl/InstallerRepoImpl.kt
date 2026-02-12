package com.rosan.installer.data.installer.model.impl

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.entity.PackageAnalysisResult
import com.rosan.installer.data.app.util.IconColorExtractor
import com.rosan.installer.data.installer.model.entity.ConfirmationDetails
import com.rosan.installer.data.installer.model.entity.InstallResult
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.installer.model.entity.UninstallInfo
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Implementation of InstallerRepo.
 * Now receives dependencies via constructor instead of internal injection.
 */
class InstallerRepoImpl(
    override val id: String,
    val context: Context,
    val appDataStore: AppDataStore,
    val iconColorExtractor: IconColorExtractor,
    private val onClose: () -> Unit
) : InstallerRepo {

    private val isClosed = AtomicBoolean(false)

    // Properties implementation
    override var error: Throwable = Throwable()
    override var config: ConfigEntity = ConfigEntity.default
    override var data: List<DataEntity> by mutableStateOf(emptyList())
    override var analysisResults: List<PackageAnalysisResult> by mutableStateOf(emptyList())
    override val progress: MutableSharedFlow<ProgressEntity> = MutableStateFlow(ProgressEntity.Ready)

    // Action flow for communication with Handlers
    val action: MutableSharedFlow<Action> = MutableSharedFlow(replay = 1, extraBufferCapacity = 1)

    override val background: MutableSharedFlow<Boolean> = MutableStateFlow(false)
    override var multiInstallQueue: List<SelectInstallEntity> = emptyList()
    override var multiInstallResults: MutableList<InstallResult> = mutableListOf()
    override var currentMultiInstallIndex: Int = 0
    override var moduleLog: List<String> = emptyList()
    override val uninstallInfo: MutableStateFlow<UninstallInfo?> = MutableStateFlow(null)
    override val confirmationDetails: MutableStateFlow<ConfirmationDetails?> = MutableStateFlow(null)

    override fun resolveInstall(activity: Activity) {
        Timber.d("[id=$id] resolve() called. Emitting Action.Resolve.")
        action.tryEmit(Action.ResolveInstall(activity))
    }

    override fun analyse() {
        Timber.d("[id=$id] analyse() called. Emitting Action.Analyse.")
        action.tryEmit(Action.Analyse)
    }

    override fun install(triggerAuth: Boolean) {
        Timber.d("[id=$id] install() called. Emitting Action.Install.")
        action.tryEmit(Action.Install(triggerAuth))
    }

    override fun installMultiple(entities: List<SelectInstallEntity>) {
        Timber.d("[id=$id] installMultiple() called. Queue size: ${entities.size}")
        multiInstallQueue = entities
        multiInstallResults.clear()
        currentMultiInstallIndex = 0

        action.tryEmit(Action.InstallMultiple)
    }

    override fun resolveUninstall(activity: Activity, packageName: String) {
        Timber.d("[id=$id] resolveUninstall() called for $packageName. Emitting Action.ResolveUninstall.")
        action.tryEmit(Action.ResolveUninstall(activity, packageName))
    }

    override fun uninstall(packageName: String) {
        // Store the info for handlers like ForegroundInfoHandler to access
        this.uninstallInfo.value = UninstallInfo(packageName)
        Timber.d("[id=$id] uninstall() called for $packageName. Emitting Action.Uninstall.")
        // Emit the action for the ActionHandler to process
        action.tryEmit(Action.Uninstall(packageName))
    }

    override fun resolveConfirmInstall(activity: Activity, sessionId: Int) {
        Timber.d("[id=$id] resolveConfirmInstall() called for session $sessionId. Emitting Action.ResolveConfirmInstall.")
        action.tryEmit(Action.ResolveConfirmInstall(activity, sessionId))
    }

    override fun approveConfirmation(sessionId: Int, granted: Boolean) {
        Timber.d("[id=$id] approveConfirmation() called for session $sessionId, granted: $granted.")
        action.tryEmit(Action.ApproveSession(sessionId, granted))
    }

    override fun reboot(reason: String) {
        Timber.d("[id=$id] reboot() called. Emitting Action.Reboot.")
        action.tryEmit(Action.Reboot(reason))
    }

    override fun background(value: Boolean) {
        Timber.d("[id=$id] background() called with value: $value.")
        background.tryEmit(value)
    }

    override fun cancel() {
        Timber.d("[id=$id] cancel() called. Emitting Action.Cancel.")
        action.tryEmit(Action.Cancel)
    }

    override fun close() {
        // Ensure close is only executed once
        if (isClosed.compareAndSet(false, true)) {
            Timber.d("[id=$id] close() called. Emitting Action.Finish and triggering cleanup.")

            // 1. Notify UI and Service that we are done
            action.tryEmit(Action.Finish)

            // 2. Trigger the callback to remove from SessionManager
            // We run this slightly later or immediately depending on requirements.
            // Here we run it immediately to ensure Manager is clean.
            onClose()

            // 3. Mark progress as finished (if not already) to satisfy Service collection loop
            // This acts as a fallback if the Action.Finish handler didn't set Progress.
            // (Optional, depends on your ProgressHandler logic)
        } else {
            Timber.w("[id=$id] close() called on an already closed instance.")
        }
    }

    sealed class Action {
        data class ResolveInstall(val activity: Activity) : Action()
        data object Analyse : Action()

        /**
         * Install single module/apk
         *
         * **This usually call from viewModel**
         *
         * @param triggerAuth request or not request user biometric auth
         * @see com.rosan.installer.ui.page.main.installer.InstallerViewAction.Install
         * @see com.rosan.installer.data.installer.model.impl.installer.ActionHandler.handleSingleInstall
         */
        data class Install(val triggerAuth: Boolean) : Action()

        /**
         * Install multiple module/apk
         *
         * **This usually call from viewModel**
         * @see com.rosan.installer.ui.page.main.installer.InstallerViewAction.InstallMultiple
         * @see com.rosan.installer.data.installer.model.impl.installer.ActionHandler.handleMultiInstall
         */
        data object InstallMultiple : Action()
        data class ResolveUninstall(val activity: Activity, val packageName: String) : Action()
        data class Uninstall(val packageName: String) : Action()
        data class ResolveConfirmInstall(val activity: Activity, val sessionId: Int) : Action()
        data class ApproveSession(val sessionId: Int, val granted: Boolean) : Action()

        /**
         * Action to trigger device reboot after cleanup.
         */
        data class Reboot(val reason: String) : Action()
        data object Cancel : Action()
        data object Finish : Action()
    }
}
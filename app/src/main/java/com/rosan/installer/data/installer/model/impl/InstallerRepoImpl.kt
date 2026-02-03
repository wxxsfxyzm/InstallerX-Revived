package com.rosan.installer.data.installer.model.impl

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.entity.PackageAnalysisResult
import com.rosan.installer.data.installer.model.entity.ConfirmationDetails
import com.rosan.installer.data.installer.model.entity.InstallResult
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.installer.model.entity.UninstallInfo
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class InstallerRepoImpl private constructor(override val id: String) : InstallerRepo,
    KoinComponent {
    companion object : KoinComponent {
        @Volatile
        private var impls = mutableMapOf<String, InstallerRepoImpl>()

        private val context by inject<Context>()

        fun getOrCreate(id: String? = null): InstallerRepo {
            Timber.d("getOrCreate called with id: ${id ?: "null (new instance requested)"}")

            // If an ID is provided, retrieve the existing instance or create a new one with that ID.
            if (id != null) {
                return synchronized(this) { // Synchronize to ensure thread-safe access to the map
                    impls.getOrPut(id) {
                        // The 'create' function handles the actual instantiation and service start.
                        create(id) as InstallerRepoImpl
                    }
                }
            }

            // If no ID is provided, this signifies a request for a new, independent installation session.
            // Always create a new instance with a unique ID to allow for parallel installations.
            val newId = UUID.randomUUID().toString()
            Timber.d("No ID provided. Creating a new instance with generated ID: $newId")
            return create(newId)
        }

        fun get(id: String): InstallerRepo? {
            return synchronized(this) {
                val instance = impls[id]
                Timber.d("get() called for id: $id. Found: ${instance != null}")
                instance
            }
        }

        private fun create(id: String): InstallerRepo {
            // This function is now the single, synchronized point of creation and registration.
            return synchronized(this) {
                // Double-check if another thread created it in the meantime before creating a new one.
                impls[id]?.let {
                    Timber.w("Instance with id $id already exists. Returning it.")
                    return@synchronized it
                }

                Timber.d("create() called for id: $id")
                val impl = InstallerRepoImpl(id)
                // Every new instance, regardless of how it was created, is now tracked in the map.
                impls[id] = impl
                Timber.d("Instance for id: $id created. Starting InstallerService.")
                val intent = Intent(InstallerService.Action.Ready.value)
                intent.component = ComponentName(context, InstallerService::class.java)
                intent.putExtra(InstallerService.EXTRA_ID, impl.id)
                ContextCompat.startForegroundService(context, intent)
                impl
            }
        }

        fun remove(id: String) {
            synchronized(this) {
                Timber.d("remove() called for id: $id")
                impls.remove(id)
            }
        }
    }

    private val isClosed = AtomicBoolean(false)

    override var error: Throwable = Throwable()
    override var config: ConfigEntity = ConfigEntity.default
    override var data: List<DataEntity> by mutableStateOf(emptyList())
    override var analysisResults: List<PackageAnalysisResult> by mutableStateOf(emptyList())
    override val progress: MutableSharedFlow<ProgressEntity> = MutableStateFlow(ProgressEntity.Ready)
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
        // 确保 close 只执行一次
        if (isClosed.compareAndSet(false, true)) {
            Timber.d("[id=$id] close() called for the first time. Emitting Action.Finish.")
            action.tryEmit(Action.Finish)
        } else {
            Timber.w("[id=$id] close() called on an already closing instance. Ignoring.")
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
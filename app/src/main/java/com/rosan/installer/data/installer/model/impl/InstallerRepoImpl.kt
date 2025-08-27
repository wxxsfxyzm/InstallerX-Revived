package com.rosan.installer.data.installer.model.impl

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rosan.installer.data.app.model.entity.DataEntity
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

        @Volatile
        private var anonymousInstance: InstallerRepoImpl? = null

        private val context by inject<Context>()

        fun getOrCreate(id: String? = null): InstallerRepo {
            Timber.d("getOrCreate called with id: ${id ?: "null (anonymous)"}")

            // Logic for named instances remains the same.
            if (id != null) {
                return impls[id] ?: synchronized(this) {
                    impls.getOrPut(id) {
                        create(id) as InstallerRepoImpl
                    }
                }
            }

            // Use Double-Checked Locking for efficient and thread-safe singleton creation.
            anonymousInstance?.let { return it }

            return synchronized(this) {
                anonymousInstance?.let { return it }

                val newId = UUID.randomUUID().toString()
                Timber.d("Creating new anonymous instance with new id: $newId")
                val newInstance = create(newId) as InstallerRepoImpl
                anonymousInstance = newInstance
                Timber.d("New anonymous instance created and tracked. Returning it.")
                newInstance
            }
        }

        fun get(id: String): InstallerRepo? {
            val instance = impls[id]
            Timber.d("get() called for id: $id. Found: ${instance != null}")
            return instance
        }

        private fun create(id: String): InstallerRepo {
            Timber.d("create() called for id: $id")
            val impl = InstallerRepoImpl(id)
            // Do not add anonymous instances to the map, they are handled by the singleton field.
            if (anonymousInstance?.id != id) {
                impls[id] = impl
            }
            Timber.d("Instance for id: $id created. Starting InstallerService.")
            val intent = Intent(InstallerService.Action.Ready.value)
            intent.component = ComponentName(context, InstallerService::class.java)
            intent.putExtra(InstallerService.EXTRA_ID, impl.id)
            context.startService(intent)
            return impl
        }

        fun remove(id: String) {
            synchronized(this) {
                Timber.d("remove() called for id: $id")
                if (id == anonymousInstance?.id) {
                    Timber.d("The removed id matches the anonymous instance. Clearing anonymousInstance.")
                    anonymousInstance = null
                }
                impls.remove(id)
            }
        }
    }

    // 添加原子关闭标志
    private val isClosed = AtomicBoolean(false)

    override var error: Throwable = Throwable()
    override var config: ConfigEntity = ConfigEntity.default
    override var data: List<DataEntity> by mutableStateOf(emptyList())
    override var entities: List<SelectInstallEntity> by mutableStateOf(emptyList())
    override val progress: MutableSharedFlow<ProgressEntity> = MutableStateFlow(ProgressEntity.Ready)
    val action: MutableSharedFlow<Action> = MutableSharedFlow(replay = 1, extraBufferCapacity = 1)
    override val background: MutableSharedFlow<Boolean> = MutableStateFlow(false)
    override val uninstallInfo: MutableStateFlow<UninstallInfo?> = MutableStateFlow(null)

    override fun resolveInstall(activity: Activity) {
        Timber.d("[id=$id] resolve() called. Emitting Action.Resolve.")
        action.tryEmit(Action.ResolveInstall(activity))
    }

    override fun analyse() {
        Timber.d("[id=$id] analyse() called. Emitting Action.Analyse.")
        action.tryEmit(Action.Analyse)
    }

    override fun install() {
        Timber.d("[id=$id] install() called. Emitting Action.Install.")
        action.tryEmit(Action.Install)
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

    override fun background(value: Boolean) {
        Timber.d("[id=$id] background() called with value: $value.")
        background.tryEmit(value)
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
        data object Install : Action()
        data class ResolveUninstall(val activity: Activity, val packageName: String) : Action()
        data class Uninstall(val packageName: String) : Action()
        data object Finish : Action()
    }
}
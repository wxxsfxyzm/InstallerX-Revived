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
        private var anonymousInstanceId: String? = null

        private val context by inject<Context>()

        fun getOrCreate(id: String? = null): InstallerRepo {
            Timber.d("getOrCreate called with id: ${id ?: "null (anonymous)"}")

            if (id != null) {
                return impls[id] ?: synchronized(this) {
                    impls[id] ?: create(id).also {
                        Timber.d("Created and cached new instance for specific id: $id")
                    }
                }
            }

            synchronized(this) {
                Timber.d("Entering synchronized block for anonymous instance.")
                anonymousInstanceId?.let { existingId ->
                    Timber.d("Found existing anonymous instance with id: $existingId. Terminating it.")
                    impls.remove(existingId)?.let { oldInstance ->
                        Timber.d("Calling close() on old instance: $existingId")
                        // 正确的终止入口点
                        oldInstance.close()
                    }
                }
                anonymousInstanceId = null

                val newId = UUID.randomUUID().toString()
                Timber.d("Creating new anonymous instance with new id: $newId")
                val newInstance = create(newId)
                anonymousInstanceId = newId
                Timber.d("New anonymous instance created and tracked. Returning it.")
                return newInstance
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
            impls[id] = impl
            Timber.d("Instance for id: $id created and stored. Starting InstallerService.")
            val intent = Intent(InstallerService.Action.Ready.value)
            intent.component = ComponentName(context, InstallerService::class.java)
            intent.putExtra(InstallerService.EXTRA_ID, impl.id)
            context.startService(intent)
            return impl
        }

        fun remove(id: String) {
            synchronized(this) {
                Timber.d("remove() called for id: $id")
                if (id == anonymousInstanceId) {
                    Timber.d("The removed id matches the anonymous instance. Clearing anonymousInstanceId.")
                    anonymousInstanceId = null
                }
                val removed = impls.remove(id)
                Timber.d("Instance for id: $id removed from map. Existed: ${removed != null}")
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

    override fun resolve(activity: Activity) {
        Timber.d("[id=$id] resolve() called. Emitting Action.Resolve.")
        action.tryEmit(Action.Resolve(activity))
    }

    override fun analyse() {
        Timber.d("[id=$id] analyse() called. Emitting Action.Analyse.")
        action.tryEmit(Action.Analyse)
    }

    override fun install() {
        Timber.d("[id=$id] install() called. Emitting Action.Install.")
        action.tryEmit(Action.Install)
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
        data class Resolve(val activity: Activity) : Action()
        data object Analyse : Action()
        data object Install : Action()
        data object Finish : Action()
    }
}
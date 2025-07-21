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
import java.util.UUID

class InstallerRepoImpl private constructor(override val id: String) : InstallerRepo,
    KoinComponent {
    companion object : KoinComponent {
        @Volatile // 增加 Volatile 保证多线程可见性
        private var impls = mutableMapOf<String, InstallerRepoImpl>()

        @Volatile // 用于追踪“匿名”安装实例的ID
        private var anonymousInstanceId: String? = null

        private val context by inject<Context>()

        fun getOrCreate(id: String? = null): InstallerRepo {
            // 如果传入了具体的ID，走标准逻辑
            if (id != null) {
                return impls[id] ?: synchronized(this) {
                    impls[id] ?: create(id)
                }
            }

            // ==================== 核心修改逻辑 ====================
            // 对于匿名调用 (id == null)，执行“后来者优先”策略
            synchronized(this) {
                // 步骤 1: 检查并主动终止任何已存在的匿名实例
                anonymousInstanceId?.let { existingId ->
                    // 从管理器中移除旧实例的引用
                    impls.remove(existingId)?.let { oldInstance ->
                        // 触发旧实例的关闭和清理流程
                        // 这会发射 ProgressEntity.Finish, 进而由 InstallerService 清理其协程等资源
                        oldInstance.close()
                    }
                }
                // 此时，旧的匿名实例（如果存在）已被移除并开始关闭，我们可以安全地清理ID
                anonymousInstanceId = null

                // 步骤 2: 创建一个全新的实例来处理新的请求
                val newId = UUID.randomUUID().toString()
                val newInstance = create(newId)
                anonymousInstanceId = newId // 记录这个新创建的匿名实例的ID
                return newInstance
            }
            // =======================================================
        }


        fun get(id: String): InstallerRepo? {
            return impls[id]
        }

        // 让 create 方法接收 ID
        private fun create(id: String): InstallerRepo {
            // 使用传入的 id 创建实例，并用该 id 作为 key 存入缓存
            val impl = InstallerRepoImpl(id)
            impls[id] = impl
            val intent = Intent(InstallerService.Action.Ready.value)
            intent.component = ComponentName(context, InstallerService::class.java)
            intent.putExtra(InstallerService.EXTRA_ID, impl.id)
            context.startService(intent)
            return impl
        }

        fun remove(id: String) {
            synchronized(this) {
                // 当一个实例被移除时，检查它是否是那个匿名实例
                if (id == anonymousInstanceId) {
                    anonymousInstanceId = null // 如果是，则清空记录，以便下次可以创建新的匿名实例
                }
                impls.remove(id)
            }
        }
    }


    override var error: Throwable = Throwable()

    override var config: ConfigEntity = ConfigEntity.default

    override var data: List<DataEntity> by mutableStateOf(emptyList())

    override var entities: List<SelectInstallEntity> by mutableStateOf(emptyList())

    override val progress: MutableSharedFlow<ProgressEntity> =
        MutableStateFlow(ProgressEntity.Ready)

    val action: MutableSharedFlow<Action> =
        MutableSharedFlow(replay = 1, extraBufferCapacity = 1)

    override val background: MutableSharedFlow<Boolean> =
        MutableStateFlow(false)

    override fun resolve(activity: Activity) {
        action.tryEmit(Action.Resolve(activity))
    }

    override fun analyse() {
        action.tryEmit(Action.Analyse)
    }

    override fun install() {
        action.tryEmit(Action.Install)
    }

    override fun background(value: Boolean) {
        background.tryEmit(value)
    }

    override fun close() {
        action.tryEmit(Action.Finish)
    }

    sealed class Action {
        data class Resolve(val activity: Activity) : Action()

        data object Analyse : Action()

        data object Install : Action()

        data object Finish : Action()
    }
}
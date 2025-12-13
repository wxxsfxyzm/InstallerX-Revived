package com.rosan.installer.data.app.model.impl.appInstaller

import android.os.IBinder
import com.rosan.app_process.AppProcess
import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.recycle.model.impl.recycler.AppProcessRecyclers
import com.rosan.installer.data.recycle.repo.Recyclable
import com.rosan.installer.data.recycle.util.SHELL_ROOT
import com.rosan.installer.data.recycle.util.SHELL_SH
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext

/**
 * 实现基于 ThreadLocal 的资源隔离，允许并发安装/卸载
 */
object ProcessInstallerRepoImpl : IBinderInstallerRepoImpl() {

    // 1. 使用 ThreadLocal 来替代成员变量。
    // 这允许我们在不改变 iBinderWrapper 签名的情况下，将 Recycler 隐式传递给当前协程。
    private val localRecycler = ThreadLocal<Recyclable<AppProcess>>()

    /**
     * 辅助函数：统一创建 Recycler 实例，解决代码重复问题 (GPT-5 建议点 5)
     */
    private fun createRecycler(config: ConfigEntity): Recyclable<AppProcess> {
        return AppProcessRecyclers.get(
            when (config.authorizer) {
                ConfigEntity.Authorizer.Root -> SHELL_ROOT
                ConfigEntity.Authorizer.Customize -> config.customizeAuthorizer
                else -> SHELL_SH
            }
        ).make()
    }

    override suspend fun doInstallWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    ) {
        // 2. 创建本次任务独享的资源
        val recycler = createRecycler(config)

        // 3. 将资源绑定到当前协程上下文 (Context Scope)
        // 在 withContext 块内部，localRecycler.get() 将返回上面的 recycler
        // 块执行完毕后，ThreadLocal 会自动恢复（对于协程来说是安全的）
        withContext(localRecycler.asContextElement(value = recycler)) {
            try {
                // 4. 执行父类逻辑（耗时操作）
                // 此时不需要 Mutex，因为每个协程都在自己的上下文中运行
                super.doInstallWork(
                    config,
                    entities,
                    extra,
                    blacklist,
                    sharedUserIdBlacklist,
                    sharedUserIdExemption
                )
            } finally {
                // 5. 生命周期闭环：谁创建，谁回收 (GPT-5 建议点 3)
                // 无论成功还是异常，这里都能确保回收
                recycler.recycle()
            }
        }
    }

    override suspend fun doUninstallWork(
        config: ConfigEntity,
        packageName: String,
        extra: InstallExtraInfoEntity
    ) {
        // 卸载逻辑同理，彻底解决之前的资源泄漏问题
        val recycler = createRecycler(config)

        withContext(localRecycler.asContextElement(value = recycler)) {
            try {
                super.doUninstallWork(config, packageName, extra)
            } finally {
                recycler.recycle()
            }
        }
    }

    override suspend fun iBinderWrapper(iBinder: IBinder): IBinder {
        // 6. 从当前协程上下文中获取资源
        val recycler = localRecycler.get()
            ?: throw IllegalStateException(
                "Recycler is null in iBinderWrapper. " +
                        "This indicates doInstallWork/doUninstallWork is not properly scoping the ThreadLocal. " +
                        "Make sure you are calling this within the managed context."
            )

        // 此时 recycler.entity 必定是当前协程所持有的那个 AppProcess
        return recycler.entity.binderWrapper(iBinder)
    }

    override suspend fun doFinishWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extraInfo: InstallExtraInfoEntity,
        result: Result<Unit>
    ) {
        // 7. doFinishWork 不再负责回收资源，只负责后续的业务逻辑（如 Dexopt）
        // 资源回收已由 doInstallWork 的 finally 块接管
        super.doFinishWork(config, entities, extraInfo, result)
    }
}
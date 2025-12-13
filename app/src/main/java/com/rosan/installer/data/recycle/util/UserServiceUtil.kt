package com.rosan.installer.data.recycle.util

import com.rosan.installer.IPrivilegedService
import com.rosan.installer.data.recycle.model.entity.DefaultPrivilegedService
import com.rosan.installer.data.recycle.model.exception.ShizukuNotWorkException
import com.rosan.installer.data.recycle.model.impl.recycler.DhizukuUserServiceRecycler
import com.rosan.installer.data.recycle.model.impl.recycler.ProcessHookRecycler
import com.rosan.installer.data.recycle.model.impl.recycler.ProcessUserServiceRecyclers
import com.rosan.installer.data.recycle.model.impl.recycler.ShizukuHookRecycler
import com.rosan.installer.data.recycle.model.impl.recycler.ShizukuUserServiceRecycler
import com.rosan.installer.data.recycle.repo.Recyclable
import com.rosan.installer.data.recycle.repo.recyclable.UserService
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import timber.log.Timber

private const val TAG = "PrivilegedService"

private object DefaultUserService : UserService {
    override val privileged: IPrivilegedService = DefaultPrivilegedService()
    override fun close() {}
}

fun useUserService(
    authorizer: ConfigEntity.Authorizer,
    customizeAuthorizer: String = "",
    useHookMode: Boolean = true,
    special: (() -> String?)? = null,
    action: (UserService) -> Unit
) {
    val recycler = getRecyclableInstance(authorizer, customizeAuthorizer, useHookMode, special)
    processRecycler(authorizer, recycler, action)
}

private fun processRecycler(
    authorizer: ConfigEntity.Authorizer,
    recycler: Recyclable<out UserService>?,
    action: (UserService) -> Unit
) {
    try {
        if (recycler != null) {
            // 这里可以把 recycler 的具体类型也打印出来，方便调试看是 Hook 还是 Service
            Timber.tag(TAG).d("Processing $authorizer with recycler: ${recycler.entity::class.java.simpleName}")
            recycler.use { action.invoke(it.entity) }
        } else {
            Timber.tag(TAG).e("Use Default User Service")
            action.invoke(DefaultUserService)
        }
    } catch (e: IllegalStateException) {
        if (authorizer == ConfigEntity.Authorizer.Shizuku && e.message?.contains("binder haven't been received") == true) {
            throw ShizukuNotWorkException("Shizuku service connection lost during privileged action.", e)
        }
        throw e
    }
}

private fun getRecyclableInstance(
    authorizer: ConfigEntity.Authorizer,
    customizeAuthorizer: String,
    useHookMode: Boolean,
    special: (() -> String?)?
): Recyclable<out UserService>? {
    Timber.tag(TAG).d("Authorizer: $authorizer, HookMode: $useHookMode")

    // 1. 先获取 specialShell，但不要直接 return
    val specialShell = special?.invoke()

    // 如果有 specialShell，打印一下，确认是 su 1000 还是其他的
    if (specialShell != null) {
        Timber.tag(TAG).d("Special shell requested: $specialShell")
    }

    return when (authorizer) {
        ConfigEntity.Authorizer.Root -> {
            // 2. 确定最终要使用的 Shell 命令
            // 如果有 special (如 "su 1000") 就用它，否则用默认 Root
            val targetShell = specialShell ?: SHELL_ROOT

            if (useHookMode) {
                Timber.tag(TAG).d("Using ProcessHookRecycler with shell: $targetShell")
                // 关键点：将 "su 1000" 传给 Hook Recycler
                ProcessHookRecycler(targetShell).make()
            } else {
                Timber.tag(TAG).d("Using ProcessUserServiceRecycler with shell: $targetShell")
                // 使用 Service 模式，同样支持 targetShell
                ProcessUserServiceRecyclers.get(targetShell).make()
            }
        }

        ConfigEntity.Authorizer.Shizuku -> {
            if (useHookMode) {
                Timber.tag(TAG).i("Using Shizuku Hook Mode.")
                ShizukuHookRecycler.make()
            } else {
                Timber.tag(TAG).i("Using Shizuku UserService Mode (Default).")
                ShizukuUserServiceRecycler.make()
            }
        }

        ConfigEntity.Authorizer.Dhizuku -> DhizukuUserServiceRecycler.make()

        ConfigEntity.Authorizer.Customize -> {
            // 自定义模式通常直接使用配置的命令
            val targetShell = customizeAuthorizer.ifBlank { SHELL_ROOT }
            ProcessUserServiceRecyclers.get(targetShell).make()
        }

        // 处理 System 或其他情况
        else -> {
            if (specialShell != null) {
                // 如果是 System 且有 special (su 1000)，通常还是走 Service 模式
                // 因为 System 模式下 Hook 的概念比较模糊（除非你是指自己 hook 自己）
                ProcessUserServiceRecyclers.get(specialShell).make()
            } else {
                null
            }
        }
    }
}
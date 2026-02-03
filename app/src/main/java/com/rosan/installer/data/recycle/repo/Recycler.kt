package com.rosan.installer.data.recycle.repo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 资源回收器基类
 *
 * 特性：
 * - 引用计数管理
 * - 延迟回收（可配置）
 * - 线程安全
 * - 支持强制回收
 */
abstract class Recycler<T : Closeable> : Closeable {

    private val lock = ReentrantLock()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var _entity: T? = null
    protected val entity: T? get() = _entity

    private val _referenceCount = AtomicInteger(0)
    val referenceCount: Int get() = _referenceCount.get()

    @Volatile
    private var recycleJob: Job? = null

    @Volatile
    private var closed = false

    /**
     * 延迟回收时间（毫秒）
     */
    protected open val delayDuration: Long = 15_000L

    /**
     * 获取资源句柄
     * @throws IllegalStateException 如果 Recycler 已关闭
     */
    fun make(): Recyclable<T> = lock.withLock {
        check(!closed) { "Recycler is closed" }

        // 取消待执行的回收任务
        recycleJob?.cancel()
        recycleJob = null

        val localEntity = _entity ?: onMake().also {
            _entity = it
            Timber.d("${this::class.simpleName}: Entity created")
        }

        _referenceCount.incrementAndGet()
        Timber.d("${this::class.simpleName}: make() called, refCount=${_referenceCount.get()}")

        return Recyclable(localEntity) {
            decrementAndScheduleRecycle()
        }
    }

    /**
     * 创建实体的工厂方法
     */
    protected abstract fun onMake(): T

    /**
     * 实体被回收时的回调
     */
    protected open fun onRecycle() {}

    /**
     * 减少引用计数并调度回收
     */
    private fun decrementAndScheduleRecycle() {
        lock.withLock {
            val count = _referenceCount.decrementAndGet()
            Timber.d("${this::class.simpleName}: recycle() called, refCount=$count")

            if (count > 0 || closed) return

            // 调度延迟回收
            recycleJob?.cancel()
            recycleJob = scope.launch {
                delay(delayDuration)
                doRecycle(force = false)
            }
        }
    }

    /**
     * 强制立即回收
     */
    fun recycleForcibly() {
        lock.withLock {
            doRecycle(force = true)
        }
    }

    /**
     * 执行实际回收逻辑
     * 必须在锁内调用
     */
    private fun doRecycle(force: Boolean) {
        lock.withLock {
            // 再次检查引用计数（除非强制回收）
            if (!force && _referenceCount.get() > 0) {
                Timber.d("${this::class.simpleName}: Recycle cancelled, refCount=${_referenceCount.get()}")
                return
            }

            recycleJob?.cancel()
            recycleJob = null

            _entity?.let { entity ->
                Timber.d("${this::class.simpleName}: Closing entity")
                runCatching { entity.close() }
                    .onFailure { Timber.e(it, "Error closing entity") }
                _entity = null
            }

            if (force) {
                _referenceCount.set(0)
            }

            runCatching { onRecycle() }
                .onFailure { Timber.e(it, "Error in onRecycle callback") }
        }
    }

    /**
     * 关闭 Recycler，释放所有资源
     */
    override fun close() {
        lock.withLock {
            if (closed) return
            closed = true
            scope.cancel()
            doRecycle(force = true)
        }
    }
}
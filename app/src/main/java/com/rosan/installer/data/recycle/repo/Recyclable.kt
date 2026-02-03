package com.rosan.installer.data.recycle.repo

import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 可回收资源的句柄
 * 线程安全，保证 recycle 只执行一次
 */
class Recyclable<T>(
    val entity: T,
    private val onRecycle: () -> Unit,
) : Closeable {

    private val recycled = AtomicBoolean(false)

    fun recycle() {
        if (recycled.compareAndSet(false, true)) {
            onRecycle()
        }
    }

    override fun close() = recycle()

    val isRecycled: Boolean get() = recycled.get()
}
package com.rosan.installer.data.recycle.model.impl.recycler

import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Recycler 管理器
 * 支持清理不再使用的 Recycler
 */
class RecyclerManager<K, V : Closeable>(
    private val factory: (K) -> V
) : Closeable {

    private val map = ConcurrentHashMap<K, V>()
    private val lock = ReentrantReadWriteLock()

    fun get(key: K): V = lock.read {
        map.getOrPut(key) { factory(key) }
    }

    fun remove(key: K): V? = lock.write {
        map.remove(key)?.also { it.close() }
    }

    fun clear() = lock.write {
        map.values.forEach { runCatching { it.close() } }
        map.clear()
    }

    override fun close() = clear()
}

object AppProcessRecyclers {
    private val manager = RecyclerManager { shell: String ->
        AppProcessRecycler(shell)
    }

    fun get(shell: String): AppProcessRecycler = manager.get(shell)

    fun clear() = manager.clear()
}
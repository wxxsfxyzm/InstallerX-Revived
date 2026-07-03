// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.core.infrastructure.lifecycle

import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Recycler Manager
 * Supports clearing unused Recyclers
 */
class RecyclerManager<K, V : Closeable>(
    private val factory: (K) -> V
) : Closeable {

    private val map = ConcurrentHashMap<K, V>()
    private val lock = ReentrantReadWriteLock()

    fun get(key: K): V {
        lock.read {
            map[key]?.let { return it }
        }

        return lock.write {
            map.getOrPut(key) { factory(key) }
        }
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

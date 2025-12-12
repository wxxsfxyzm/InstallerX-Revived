package com.rosan.installer.data.recycle.repo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.Closeable

abstract class Recycler<T : Closeable> {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    protected var entity: T? = null

    protected var referenceCount = 0
        private set

    private var recycleJob: Job? = null

    // OPTIMIZATION: Made 'open' so subclasses can override the wait time.
    // Default is 15 seconds.
    protected open val delayDuration = 15000L

    fun make(): Recyclable<T> {
        synchronized(this) {
            val localEntity = entity ?: onMake().apply {
                entity = this
            }
            referenceCount += 1
            return Recyclable(localEntity, this)
        }
    }

    abstract fun onMake(): T

    fun recycle() {
        synchronized(this) {
            referenceCount -= 1
            if (referenceCount > 0) return

            // Cancel any existing job to prevent multiple callbacks
            recycleJob?.cancel()

            recycleJob = coroutineScope.launch {
                delay(delayDuration)

                // CRITICAL FIX: Synchronize the check and action atomically.
                // Prevents a race condition where 'make()' increments refCount
                // right after delay() finishes but before recycleForcibly() runs.
                synchronized(this@Recycler) {
                    if (referenceCount > 0) return@synchronized
                    recycleForcibly()
                }
            }
        }
    }

    fun recycleForcibly() {
        synchronized(this) {
            referenceCount = 0
            entity?.runCatching { close() }
            entity = null

            // CRITICAL FIX: Explicitly call lifecycle callback.
            // This ensures chained resources (like underlying Shell processes) are released.
            onRecycle()
        }
    }

    /**
     * Called when the entity is actually disposed.
     * Override this to clean up related resources (e.g., child recyclers).
     */
    open fun onRecycle() {
    }
}
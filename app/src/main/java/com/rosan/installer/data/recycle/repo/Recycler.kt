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

    protected val delayDuration = 15000L

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
            recycleJob?.cancel()
            recycleJob = coroutineScope.launch {
                delay(delayDuration)
                if (referenceCount > 0) return@launch
                recycleForcibly()
            }
        }
    }

    fun recycleForcibly() {
        synchronized(this) {
            referenceCount = 0
            entity?.runCatching { close() }
            entity = null
        }
    }

    open fun onRecycle() {
    }
}
package com.rosan.installer.data.recycle.model.impl.recycler

import java.util.concurrent.ConcurrentHashMap

object AppProcessRecyclers {
    // Use ConcurrentHashMap for thread safety in singleton pattern
    private val map = ConcurrentHashMap<String, AppProcessRecycler>()

    fun get(shell: String): AppProcessRecycler {
        return map.getOrPut(shell) {
            AppProcessRecycler(shell)
        }
    }
}
package com.rosan.installer.data.recycle.model.impl.recycler

import java.util.concurrent.ConcurrentHashMap

object ProcessUserServiceRecyclers {
    // Use ConcurrentHashMap for thread safety
    private val map = ConcurrentHashMap<String, ProcessUserServiceRecycler>()

    fun get(shell: String): ProcessUserServiceRecycler {
        return map.getOrPut(shell) {
            ProcessUserServiceRecycler(shell)
        }
    }
}
package com.rosan.installer.data.recycle.model.impl.recycler

object AppProcessRecyclers {
    private val map = mutableMapOf<String, AppProcessRecycler>()

    fun get(shell: String): AppProcessRecycler {
        return map.getOrPut(shell) {
            AppProcessRecycler(shell)
        }
    }
}
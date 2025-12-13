package com.rosan.installer.data.recycle.model.impl.recycler

object ProcessUserServiceRecyclers {
    private val manager = RecyclerManager { shell: String ->
        ProcessUserServiceRecycler(shell)
    }

    fun get(shell: String): ProcessUserServiceRecycler = manager.get(shell)

    fun clear() = manager.clear()
}
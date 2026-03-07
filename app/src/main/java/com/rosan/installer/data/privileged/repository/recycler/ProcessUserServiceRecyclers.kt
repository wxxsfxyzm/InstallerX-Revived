package com.rosan.installer.data.privileged.repository.recycler

object ProcessUserServiceRecyclers {
    private val manager = RecyclerManager { shell: String ->
        ProcessUserServiceRecycler(shell)
    }

    fun get(shell: String): ProcessUserServiceRecycler = manager.get(shell)

    fun clear() = manager.clear()
}
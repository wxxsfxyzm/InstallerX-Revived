package com.rosan.installer.ui.activity

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

class EmptyFragmentActivity : FragmentActivity() {

    companion object {
        var onActivityReady: ((EmptyFragmentActivity) -> Unit)? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onActivityReady?.invoke(this)
        onActivityReady = null
    }
}

package com.rosan.installer.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.InstallerPage
import com.rosan.installer.ui.theme.InstallerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

class InstallerActivity : ComponentActivity(), KoinComponent {
    companion object {
        const val KEY_ID = "installer_id"
    }

    private var installer by mutableStateOf<InstallerRepo?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreInstaller(savedInstanceState)
        showContent()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_ID, installer?.id)
        super.onSaveInstanceState(outState)
    }

/*    override fun onNewIntent(intent: Intent?) {
        this.intent = intent
        super.onNewIntent(intent!!)
        restoreInstaller()
    }*/

    private var job: Job? = null

    override fun finish() {
        super.finish()
    }

    override fun onDestroy() {
        job?.cancel()
        job = null
        super.onDestroy()
    }

    private fun restoreInstaller(savedInstanceState: Bundle? = null) {
        job?.cancel()
        val installerId = if (savedInstanceState == null) intent?.getStringExtra(KEY_ID)
        else savedInstanceState.getString(KEY_ID)
        val installer: InstallerRepo = get {
            parametersOf(installerId)
        }
        installer.background(false)
        this.installer = installer
        val scope = CoroutineScope(Dispatchers.IO)
        job = scope.launch {
            launch {
                installer.progress.collect { progress ->
                    when (progress) {
                        is ProgressEntity.Ready -> {
                            installer.resolve(this@InstallerActivity)
                        }

                        is ProgressEntity.Finish -> {
                            val activity = this@InstallerActivity
                            if (!activity.isFinishing) activity.finish()
                        }

                        else -> {}
                    }
                }
            }
            launch {
                installer.background.collect {
                    if (it) this@InstallerActivity.finish()
                }
            }
        }
    }

    private fun showContent() {
        setContent {
            val installer = installer ?: return@setContent
            val background by installer.background.collectAsState(false)
            val progress by installer.progress.collectAsState(ProgressEntity.Ready)
            if (
                background ||
                progress is ProgressEntity.Ready ||
                progress is ProgressEntity.Resolving ||
                progress is ProgressEntity.Finish
            ) return@setContent
            InstallerTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    InstallerPage(installer)
                }
            }
        }
    }
}
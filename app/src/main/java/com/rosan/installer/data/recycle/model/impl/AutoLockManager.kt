package com.rosan.installer.data.recycle.model.impl

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.util.ConfigUtil
import com.rosan.installer.ui.activity.InstallerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import rikka.shizuku.Shizuku
import timber.log.Timber

object AutoLockManager : KoinComponent {

    private val context by inject<Context>()
    private val appDataStore by inject<AppDataStore>()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun init() {
        Shizuku.addBinderReceivedListener(binderListener)
    }

    private val binderListener = Shizuku.OnBinderReceivedListener {
        checkAndExecute()
    }

    private fun checkAndExecute() {
        scope.launch {
            try {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Timber.w("AutoLockManager: Shizuku permission not granted, skipping.")
                    return@launch
                }

                val autoLock = appDataStore.getBoolean(AppDataStore.AUTO_LOCK_INSTALLER, false).first()
                if (!autoLock) {
                    Timber.d("AutoLockManager: Auto lock disabled.")
                    return@launch
                }

                val authorizer = ConfigUtil.getGlobalAuthorizer()
                if (authorizer != ConfigEntity.Authorizer.Shizuku) {
                    Timber.d("AutoLockManager: Global authorizer is not Shizuku ($authorizer).")
                    return@launch
                }

                Timber.i("AutoLockManager: Conditions met. Applying Default Installer...")

                val component = ComponentName(context, InstallerActivity::class.java)

                PrivilegedManager.setDefaultInstaller(
                    authorizer = ConfigEntity.Authorizer.Shizuku,
                    component = component,
                    enable = true
                )

                Timber.i("AutoLockManager: Default Installer applied successfully.")

            } catch (e: Exception) {
                Timber.e(e, "AutoLockManager: Failed to apply default installer.")
            }
        }
    }
}
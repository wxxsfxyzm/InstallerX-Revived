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
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import rikka.shizuku.Shizuku
import timber.log.Timber

object AutoLockManager : KoinComponent {
    private val context by inject<Context>()
    private val appDataStore by inject<AppDataStore>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isInitialized = false

    fun init() {
        if (isInitialized) return
        isInitialized = true

        Shizuku.addBinderReceivedListener {
            checkAndLockForShizukuStartup()
        }

        if (Shizuku.pingBinder()) {
            checkAndLockForShizukuStartup()
        }
    }

    private fun checkAndLockForShizukuStartup() {
        scope.launch {
            try {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) return@launch

                if (!isAutoLockEnabled()) return@launch

                val globalAuthorizer = ConfigUtil.getGlobalAuthorizer()
                if (globalAuthorizer == ConfigEntity.Authorizer.Shizuku) {
                    executeLock(ConfigEntity.Authorizer.Shizuku, "ShizukuStartup")
                }
            } catch (e: Exception) {
                Timber.e(e, "AutoLockManager: Failed during Shizuku startup check.")
            }
        }
    }

    fun onResolveInstall(sessionAuthorizer: ConfigEntity.Authorizer) {
        scope.launch {
            try {
                if (!isAutoLockEnabled()) return@launch

                val allowedAuthorizers = listOf(
                    ConfigEntity.Authorizer.Root,
                    ConfigEntity.Authorizer.Shizuku,
                    ConfigEntity.Authorizer.Dhizuku
                )

                if (sessionAuthorizer in allowedAuthorizers) {
                    if (sessionAuthorizer == ConfigEntity.Authorizer.Shizuku &&
                        Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED
                    ) {
                        return@launch
                    }

                    executeLock(sessionAuthorizer, "InstallSession")
                }
            } catch (e: Exception) {
                Timber.e(e, "AutoLockManager: Failed during install session check.")
            }
        }
    }

    private suspend fun isAutoLockEnabled() =
        appDataStore.getBoolean(AppDataStore.AUTO_LOCK_INSTALLER, false).first()

    private suspend fun executeLock(authorizer: ConfigEntity.Authorizer, source: String) {
        withContext(Dispatchers.IO) {
            try {
                val component = ComponentName(context, InstallerActivity::class.java)

                Timber.d("AutoLockManager: Locking via $authorizer (Source: $source)")

                PrivilegedManager.setDefaultInstaller(
                    authorizer = authorizer,
                    component = component,
                    enable = true
                )
                Timber.i("AutoLockManager: Locked successfully via $authorizer.")
            } catch (e: Exception) {
                Timber.e(e, "AutoLockManager: Execution failed.")
            }
        }
    }
}
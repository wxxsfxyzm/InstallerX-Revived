package com.rosan.installer.data.installer.model.impl.installer.processor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import com.rosan.installer.data.app.model.impl.InstallerRepoImpl
import com.rosan.installer.data.installer.model.entity.ConfirmationDetails
import com.rosan.installer.data.recycle.util.useUserService
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import org.koin.core.component.KoinComponent
import timber.log.Timber

class SessionProcessor : KoinComponent {

    fun getSessionDetails(sessionId: Int, config: ConfigEntity): ConfirmationDetails {
        var label: CharSequence? = "N/A"
        var icon: Bitmap? = null

        Timber.d("Getting session details via service (Authorizer: ${config.authorizer})")

        // Uniformly use useUserService.
        // If authorizer is None (System App), UserServiceUtil will dispatch to the local DefaultPrivilegedService.
        // If it is Root/Shizuku, it will dispatch to the corresponding IPC service.
        var bundle: Bundle? = null
        try {
            useUserService(
                authorizer = config.authorizer,
                customizeAuthorizer = config.customizeAuthorizer,
                useHookMode = false
            ) { bundle = it.privileged.getSessionDetails(sessionId) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get session details via ${config.authorizer}")
        }

        if (bundle != null) {
            label = bundle.getCharSequence("appLabel") ?: "N/A"
            val bytes = bundle.getByteArray("appIcon")
            if (bytes != null) {
                try {
                    icon = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to decode icon bitmap")
                }
            }
        } else {
            Timber.w("Service returned null bundle for session $sessionId")
        }

        return ConfirmationDetails(sessionId, label ?: "N/A", icon)
    }

    suspend fun approveSession(sessionId: Int, granted: Boolean, config: ConfigEntity) {
        Timber.d("Approving session $sessionId (granted: $granted) via ${config.authorizer}")

        try {
            InstallerRepoImpl.approveSession(config, sessionId, granted)
            Timber.d("Session $sessionId approval processed successfully.")
        } catch (e: Exception) {
            Timber.e(e, "Failed to approve/reject session via ${config.authorizer}")
        }
    }
}